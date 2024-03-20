package kr.caredoc.careinsurance.settlement

import com.github.guepardoapps.kulid.ULID
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationByIdQuery
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationByIdQueryHandler
import kr.caredoc.careinsurance.caregiving.CaregivingChargeCalculated
import kr.caredoc.careinsurance.caregiving.CaregivingChargeModified
import kr.caredoc.careinsurance.caregiving.CaregivingRound
import kr.caredoc.careinsurance.caregiving.CaregivingRoundsByIdsQuery
import kr.caredoc.careinsurance.caregiving.CaregivingRoundsByIdsQueryHandler
import kr.caredoc.careinsurance.page
import kr.caredoc.careinsurance.reception.Reception
import kr.caredoc.careinsurance.reception.ReceptionByIdQuery
import kr.caredoc.careinsurance.reception.ReceptionByIdQueryHandler
import kr.caredoc.careinsurance.reception.ReceptionModified
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQuery
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQueryHandler
import kr.caredoc.careinsurance.reception.ReferenceReceptionNotExistsException
import kr.caredoc.careinsurance.security.accesscontrol.ReadOneAccess
import kr.caredoc.careinsurance.security.accesscontrol.checkAll
import kr.caredoc.careinsurance.security.encryption.Decryptor
import kr.caredoc.careinsurance.user.InternalCaregivingManagerByIdQuery
import kr.caredoc.careinsurance.user.InternalCaregivingManagerByIdQueryHandler
import kr.caredoc.careinsurance.user.ReferenceInternalCaregivingManagerNotExistsException
import kr.caredoc.careinsurance.user.exception.InternalCaregivingManagerNotFoundByIdException
import kr.caredoc.careinsurance.withSort
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.StringJoiner

@Service
class SettlementService(
    private val settlementRepository: SettlementRepository,
    private val receptionByIdQueryHandler: ReceptionByIdQueryHandler,
    private val receptionsByIdsQueryHandler: ReceptionsByIdsQueryHandler,
    private val internalCaregivingManagerByIdQueryHandler: InternalCaregivingManagerByIdQueryHandler,
    private val caregivingRoundsByIdsQueryHandler: CaregivingRoundsByIdsQueryHandler,
    private val externalCaregivingOrganizationByIdQueryHandler: ExternalCaregivingOrganizationByIdQueryHandler,
    private val decryptor: Decryptor,
) : SettlementsByReceptionIdQueryHandler,
    SettlementsSearchQueryHandler,
    SettlementEditingCommandHandler,
    TransactionsBySettlementIdQueryHandler,
    SettlementTransactionRecordingCommandHandler,
    SettlementByCaregivingRoundIdQueryHandler {
    @EventListener(CaregivingChargeCalculated::class)
    @Transactional
    fun handleCaregivingChargeCalculated(event: CaregivingChargeCalculated) {
        val reception = receptionByIdQueryHandler.getReception(
            ReceptionByIdQuery(
                receptionId = event.receptionId,
                subject = event.subject,
            )
        )
        settlementRepository.save(
            event.generateSettlement(
                id = ULID.random(),
                reception = reception,
            )
        )
    }

    private fun CaregivingChargeCalculated.generateSettlement(id: String, reception: Reception) = Settlement(
        id = id,
        receptionId = this.receptionId,
        caregivingRoundId = this.caregivingRoundId,
        caregivingRoundNumber = this.roundNumber,
        accidentNumber = reception.accidentInfo.accidentNumber,
        dailyCaregivingCharge = this.dailyCaregivingCharge,
        basicAmount = this.basicAmount,
        additionalAmount = this.additionalAmount,
        totalAmount = this.totalAmount,
        lastCalculationDateTime = this.calculatedDateTime,
        expectedSettlementDate = this.expectedSettlementDate,
        caregivingManagerInfo = reception.caregivingManagerInfo,
    )

    @Transactional(readOnly = true)
    override fun getSettlements(query: SettlementsByReceptionIdQuery): List<Settlement> {
        receptionByIdQueryHandler.ensureReceptionExists(ReceptionByIdQuery(query.receptionId, query.subject))

        val settlements = settlementRepository.findByReceptionId(
            query.receptionId,
            Sort.by(Sort.Direction.DESC, Settlement::caregivingRoundNumber.name),
        )

        SettlementAccessPolicy.checkAll(query.subject, ReadOneAccess, settlements)

        return settlements
    }

    @Transactional(readOnly = true)
    override fun getSettlements(query: SettlementsSearchQuery, pageRequest: Pageable): Page<Settlement> {
        val settlements = settlementRepository.searchSettlements(
            SettlementSearchingRepository.SearchingCriteria(
                progressingStatus = query.progressingStatus,
                accidentNumber = query.getKeyword(
                    propertyToExtractionKeyword = SettlementsSearchQuery.SearchingProperty.ACCIDENT_NUMBER
                ),
                patientName = query.getKeyword(
                    propertyToExtractionKeyword = SettlementsSearchQuery.SearchingProperty.PATIENT_NAME
                ),
                organizationName = query.getKeyword(
                    propertyToExtractionKeyword = SettlementsSearchQuery.SearchingProperty.ORGANIZATION_NAME
                ),
                expectedSettlementDate = query.expectedSettlementDate,
                lastTransactionDate = query.transactionDate,
                internalCaregivingOrganizationAssigned = query.internalCaregivingOrganizationAssigned(
                    propertyToExtractionKeyword = SettlementsSearchQuery.SearchingProperty.ORGANIZATION_NAME
                ),
                caregiverName = query.getKeyword(
                    propertyToExtractionKeyword = SettlementsSearchQuery.SearchingProperty.CAREGIVER_NAME
                ),
            ),
            pageable = pageRequest.withSort(query.sorting)
        )

        SettlementAccessPolicy.checkAll(query.subject, ReadOneAccess, settlements.content)

        return settlements
    }

    private fun SettlementsSearchQuery.getKeyword(
        propertyToExtractionKeyword: SettlementsSearchQuery.SearchingProperty
    ) = if (searchCondition?.searchingProperty == propertyToExtractionKeyword) {
        searchCondition.keyword
    } else {
        null
    }

    private fun SettlementsSearchQuery.internalCaregivingOrganizationAssigned(
        propertyToExtractionKeyword: SettlementsSearchQuery.SearchingProperty
    ) = if (searchCondition?.searchingProperty == propertyToExtractionKeyword) {
        "케어닥".contains(searchCondition.keyword)
    } else {
        null
    }

    @Transactional(readOnly = true)
    override fun getSettlementsAsCsv(query: SettlementsSearchQuery): String {
        val settlements = settlementRepository.searchSettlements(
            SettlementSearchingRepository.SearchingCriteria(
                progressingStatus = query.progressingStatus,
                accidentNumber = query.getKeyword(
                    propertyToExtractionKeyword = SettlementsSearchQuery.SearchingProperty.ACCIDENT_NUMBER
                ),
                patientName = query.getKeyword(
                    propertyToExtractionKeyword = SettlementsSearchQuery.SearchingProperty.PATIENT_NAME
                ),
                organizationName = query.getKeyword(
                    propertyToExtractionKeyword = SettlementsSearchQuery.SearchingProperty.ORGANIZATION_NAME
                ),
                expectedSettlementDate = query.expectedSettlementDate,
            ),
            orderBy(query.sorting)
        )

        SettlementAccessPolicy.checkAll(query.subject, ReadOneAccess, settlements)

        val caregivingRoundsById = caregivingRoundsByIdsQueryHandler.getCaregivingRounds(
            CaregivingRoundsByIdsQuery(
                caregivingRoundIds = settlements.map { it.caregivingRoundId },
                subject = query.subject,
            )
        ).associateBy { it.id }

        val receptions = receptionsByIdsQueryHandler.getReceptions(
            ReceptionsByIdsQuery(
                caregivingRoundsById.values.map { it.receptionInfo.receptionId }.toSet(),
                query.subject,
            )
        ).associateBy { it.id }

        val csvWriter = SettlementCsvWriter()

        for (settlement in settlements) {
            val caregivingRound = caregivingRoundsById[settlement.caregivingRoundId] ?: continue
            val reception = receptions[caregivingRound.receptionInfo.receptionId]
                ?: throw ReferenceReceptionNotExistsException(caregivingRound.receptionInfo.receptionId)
            val patientName = reception.inDecryptionContext(decryptor, query.subject) { decryptPatientName() }
            val caregiverOrganizationId = caregivingRound.caregiverInfo?.caregiverOrganizationId?.let {
                caregivingRound.caregiverInfo?.caregiverOrganizationId
            }
            val caregiverOrganizationName = if (caregiverOrganizationId != null) {
                externalCaregivingOrganizationByIdQueryHandler.getExternalCaregivingOrganization(
                    ExternalCaregivingOrganizationByIdQuery(
                        caregiverOrganizationId,
                        query.subject,
                    )
                ).name
            } else {
                "케어닥"
            }

            csvWriter.append(
                patientName,
                caregivingRound,
                settlement,
                caregiverOrganizationName,
            )
        }

        return csvWriter.writeCsv()
    }

    @Transactional(readOnly = true)
    override fun getSettlementsCalculate(query: SettlementsSearchQuery): List<Settlement> {
        val settlements = settlementRepository.searchSettlements(
            SettlementSearchingRepository.SearchingCriteria(
                progressingStatus = query.progressingStatus,
                accidentNumber = query.getKeyword(
                    propertyToExtractionKeyword = SettlementsSearchQuery.SearchingProperty.ACCIDENT_NUMBER
                ),
                patientName = query.getKeyword(
                    propertyToExtractionKeyword = SettlementsSearchQuery.SearchingProperty.PATIENT_NAME
                ),
                organizationName = query.getKeyword(
                    propertyToExtractionKeyword = SettlementsSearchQuery.SearchingProperty.ORGANIZATION_NAME
                ),
                expectedSettlementDate = query.expectedSettlementDate,
            ),
            orderBy(query.sorting)
        )

        SettlementAccessPolicy.checkAll(query.subject, ReadOneAccess, settlements)

        return settlements
    }

    private class SettlementCsvWriter {
        private data class SettlementCsvRecordData(
            val bank: String,
            val accountNumber: String,
            val totalAmount: Int,
            val accountHolder: String,
            val patientName: String,
            val caregiverName: String,
            val bankbookDisplay: String = "",
            val remarks: String = "",
            val cmsCode: String = "",
            val phoneNumber: String = "",
            val caregiverOrganizationName: String,
        ) {
            fun writeRecord() = "$bank,$accountNumber,$totalAmount,$accountHolder,$patientName/$caregiverName,$bankbookDisplay,$remarks,$cmsCode,$phoneNumber,$caregiverOrganizationName"
        }

        private val records: MutableList<SettlementCsvRecordData> = mutableListOf()

        fun append(
            patientName: String,
            caregivingRound: CaregivingRound,
            settlement: Settlement,
            caregiverOrganizationName: String,
        ) = records.add(
            SettlementCsvRecordData(
                bank = caregivingRound.caregiverInfo?.accountInfo?.bank ?: "-".replace("\\r\\n|\\r|\\n|\\n\\r".toRegex(),"").replace(",", "/"),
                accountNumber = caregivingRound.caregiverInfo?.accountInfo?.accountNumber ?: "-".replace("\\r\\n|\\r|\\n|\\n\\r".toRegex(),"").replace(",", "/"),
                totalAmount = settlement.totalAmount,
                accountHolder = caregivingRound.caregiverInfo?.accountInfo?.accountHolder ?: "-".replace("\\r\\n|\\r|\\n|\\n\\r".toRegex(),"").replace(",", "/"),
                patientName = patientName.replace("\\r\\n|\\r|\\n|\\n\\r".toRegex(),"").replace(",", "/"),
                caregiverName = caregivingRound.caregiverInfo?.name ?: "-".replace("\\r\\n|\\r|\\n|\\n\\r".toRegex(),"").replace(",", "/"),
                remarks = caregivingRound.remarks.replace("\\r\\n|\\r|\\n|\\n\\r".toRegex(),"").replace(",", "/"),
                caregiverOrganizationName = caregiverOrganizationName.replace("\\r\\n|\\r|\\n|\\n\\r".toRegex(),"").replace(",", "/"),
            )
        )

        fun writeCsv(): String {
            val joiner = StringJoiner("\n")

            joiner.add(writeHeader())
            records.map { it.writeRecord() }.forEach { joiner.add(it) }

            return joiner.toString()
        }

        private fun writeHeader() = "입금은행,입금계좌번호,입금액,예상예금주,입금통장표시,출금통장표시,메모,CMS코드,받는분 휴대폰번호,간병인 소속"
    }

    @Transactional
    override fun editSettlements(commands: Collection<Pair<SettlementByIdQuery, SettlementEditingCommand>>) {
        try {
            ensureSettlementManagersExists(commands.map { it.second })
        } catch (e: InternalCaregivingManagerNotFoundByIdException) {
            throw ReferenceInternalCaregivingManagerNotExistsException(e.internalCaregivingManagerId, e)
        }

        val enteredSettlementsIds = commands.map { it.first.settlementId }.toSet()
        val targetSettlements = settlementRepository.findByIdIn(enteredSettlementsIds)

        ensureAllSettlementIdsAreQueried(enteredSettlementsIds, targetSettlements)

        val editingCommandsBySettlementId = commands.associate {
            it.first.settlementId to it.second
        }

        targetSettlements.forEach { targetSettlement ->
            editingCommandsBySettlementId[targetSettlement.id]?.let { targetSettlement.edit(it) }
            settlementRepository.save(targetSettlement)
        }
    }

    private fun ensureSettlementManagersExists(editingCommands: Collection<SettlementEditingCommand>) {
        val identifiedInternalCaregivingManagerIds = mutableSetOf<String>()

        for (editingCommand in editingCommands) {
            val settlementManagerId = editingCommand.settlementManagerId.patchingValue ?: continue

            if (identifiedInternalCaregivingManagerIds.contains(settlementManagerId)) {
                continue
            }

            internalCaregivingManagerByIdQueryHandler.ensureInternalCaregivingManagerExists(
                InternalCaregivingManagerByIdQuery(
                    internalCaregivingManagerId = settlementManagerId,
                    subject = editingCommand.subject,
                )
            )

            identifiedInternalCaregivingManagerIds.add(settlementManagerId)
        }
    }

    private fun ensureAllSettlementIdsAreQueried(
        queryingSettlementsIds: Set<String>,
        queriedSettlements: Collection<Settlement>
    ) {
        val queriedSettlementIds = queriedSettlements.map { it.id }.toSet()

        val notQueriedSettlementIds = queryingSettlementsIds subtract queriedSettlementIds

        notQueriedSettlementIds.firstOrNull()?.let {
            throw ReferenceSettlementNotExistsException(
                referenceSettlementId = it
            )
        }
    }

    @EventListener(CaregivingChargeModified::class)
    @Transactional
    fun handleCaregivingChargeModified(event: CaregivingChargeModified) {
        settlementRepository.findByCaregivingRoundId(event.caregivingRoundId).forEach {
            it.handleCaregivingChargeModified(event)
            settlementRepository.save(it)
        }
    }

    private fun getSettlement(query: SettlementByIdQuery): Settlement {
        val settlement = settlementRepository.findByIdOrNull(query.settlementId)
            ?: throw SettlementNotFoundByIdException(query.settlementId)

        SettlementAccessPolicy.check(query.subject, query, settlement)

        return settlement
    }

    @Transactional(readOnly = true)
    override fun getTransactions(
        query: TransactionsBySettlementIdQuery,
        pageable: Pageable
    ): Page<Settlement.TransactionRecord> {
        val settlement = getSettlement(SettlementByIdQuery(query.settlementId, query.subject))

        SettlementAccessPolicy.check(query.subject, query, settlement)

        val sortedTransactions = settlement.transactions.sortedWith(
            compareByDescending<Settlement.TransactionRecord> {
                it.transactionDate
            }.thenByDescending {
                it.enteredDateTime
            }
        )

        return sortedTransactions.page(pageable)
    }

    @Transactional
    override fun recordTransaction(query: SettlementByIdQuery, command: SettlementTransactionRecordingCommand) {
        val settlement = getSettlement(query)
        settlement.recordTransaction(command)

        settlementRepository.save(settlement)
    }

    @Transactional(readOnly = true)
    override fun getSettlement(query: SettlementByCaregivingRoundIdQuery): Settlement {
        val settlement = settlementRepository.findTopByCaregivingRoundId(query.caregivingRoundId)
            ?: throw SettlementNotFoundByCaregivingRoundIdException(query.caregivingRoundId)

        SettlementAccessPolicy.check(query.subject, query, settlement)

        return settlement
    }

    private fun Pageable.withSort(sorting: SettlementsSearchQuery.Sorting?) = this.withSort(orderBy(sorting))

    private fun orderBy(sorting: SettlementsSearchQuery.Sorting?) = when (sorting) {
        SettlementsSearchQuery.Sorting.EXPECTED_SETTLEMENT_DATE_DESC_ACCIDENT_NUMBER_DESC
        -> Sort.by(
            Sort.Order.desc(Settlement::expectedSettlementDate.name),
            Sort.Order.desc(Settlement::accidentNumber.name),
        )
        SettlementsSearchQuery.Sorting.LAST_TRANSACTION_DATE_TIME_DESC
        -> Sort.by(Sort.Order.desc(Settlement::lastTransactionDatetime.name))

        null -> Sort.by(
            Sort.Order.desc(Settlement::expectedSettlementDate.name),
            Sort.Order.desc(Settlement::accidentNumber.name),
        )
    }

    @Transactional
    @EventListener(ReceptionModified::class)
    fun handleReceptionModified(event: ReceptionModified) {
        if (!Settlement.isAffectsToSettlement(event)) {
            return
        }

        settlementRepository.findByReceptionId(event.receptionId).forEach {
            it.handleReceptionModified(event)
        }
    }
}
