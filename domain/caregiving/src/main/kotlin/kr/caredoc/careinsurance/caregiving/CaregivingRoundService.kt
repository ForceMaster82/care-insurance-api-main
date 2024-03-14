package kr.caredoc.careinsurance.caregiving

import com.github.guepardoapps.kulid.ULID
import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.RequiredParameterNotSuppliedException
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationByIdQuery
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationByIdQueryHandler
import kr.caredoc.careinsurance.billing.BillingGenerated
import kr.caredoc.careinsurance.billing.BillingModified
import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import kr.caredoc.careinsurance.caregiving.exception.CaregivingChargeEditingDeniedException
import kr.caredoc.careinsurance.caregiving.exception.CaregivingRoundNotFoundByIdException
import kr.caredoc.careinsurance.caregiving.exception.InvalidCaregivingChargeActionableStatusException
import kr.caredoc.careinsurance.caregiving.exception.UnknownCaregivingRoundInfoException
import kr.caredoc.careinsurance.caregiving.state.CancellationReason
import kr.caredoc.careinsurance.caregiving.state.FinishingReason
import kr.caredoc.careinsurance.reception.*
import kr.caredoc.careinsurance.reception.exception.ManagingUserNotFoundException
import kr.caredoc.careinsurance.reconciliation.IssuedType
import kr.caredoc.careinsurance.reconciliation.ReconciliationClosed
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.ReadOneAccess
import kr.caredoc.careinsurance.security.accesscontrol.checkAll
import kr.caredoc.careinsurance.security.encryption.Decryptor
import kr.caredoc.careinsurance.security.personaldata.IncludingPersonalData
import kr.caredoc.careinsurance.settlement.SettlementGenerated
import kr.caredoc.careinsurance.settlement.SettlementModified
import kr.caredoc.careinsurance.settlement.SettlementProgressingStatus
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.StringJoiner

@Service
class CaregivingRoundService(
    private val caregivingRoundRepository: CaregivingRoundRepository,
    private val receptionByIdQueryHandler: ReceptionByIdQueryHandler,
    private val caregivingChargeRepository: CaregivingChargeRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val receptionsByIdsQueryHandler: ReceptionsByIdsQueryHandler,
    private val externalCaregivingOrganizationByIdQueryHandler: ExternalCaregivingOrganizationByIdQueryHandler,
    private val decryptor: Decryptor,
) : CaregivingRoundsByFilterQueryHandler,
    CaregivingRoundsByReceptionIdQueryHandler,
    CaregivingRoundByIdQueryHandler,
    CaregivingRoundEditingCommandHandler,
    CaregivingChargeByCaregivingRoundIdQueryHandler,
    CaregivingChargeEditingCommandHandler,
    CaregivingRoundsByIdsQueryHandler {

    @Transactional(readOnly = true)
    override fun getCaregivingRounds(
        @IncludingPersonalData
        query: CaregivingRoundsByFilterQuery,
        pageRequest: Pageable,
    ): Page<CaregivingRound> {
        CaregivingRoundAccessPolicy.check(query.subject, query, Object.Empty)
        val caregivingRounds = try {
            caregivingRoundRepository.searchCaregivingRounds(query.intoSearchCriteria(), pageRequest)
        } catch (e: ManagingUserNotFoundException) {
            return PageImpl(listOf(), pageRequest, 0)
        }
        caregivingRounds.forEach {
            CaregivingRoundAccessPolicy.check(query.subject, CaregivingRoundByIdQuery(it.id, query.subject), it)
        }

        return caregivingRounds
    }

    @Transactional(readOnly = true)
    override fun getCaregivingRoundsAsCsv(
        @IncludingPersonalData
        query: CaregivingRoundsByFilterQuery
    ): String {
        CaregivingRoundAccessPolicy.check(query.subject, query, Object.Empty)
        val caregivingRounds = caregivingRoundRepository.searchCaregivingRounds(query.intoSearchCriteria())

        CaregivingRoundAccessPolicy.checkAll(query.subject, ReadOneAccess, caregivingRounds)
        caregivingRounds.forEach {
            CaregivingRoundAccessPolicy.check(query.subject, CaregivingRoundByIdQuery(it.id, query.subject), it)
        }

        val receptions = receptionsByIdsQueryHandler.getReceptions(
            ReceptionsByIdsQuery(
                caregivingRounds.map { it.receptionInfo.receptionId }.toSet(),
                query.subject,
            )
        ).associateBy { it.id }

        val csvWriter = CaregivingRoundCsvWriter()

        for (caregivingRound in caregivingRounds) {
            val reception = receptions[caregivingRound.receptionInfo.receptionId]
                ?: throw ReferenceReceptionNotExistsException(caregivingRound.receptionInfo.receptionId)
            val patientName = reception.inDecryptionContext(decryptor, query.subject) { decryptPatientName() }
            val managerOrganizationId = reception.caregivingManagerInfo?.organizationId?.let {
                reception.caregivingManagerInfo?.organizationId
            }
            val managerOrganizationName = if (managerOrganizationId != null) {
                externalCaregivingOrganizationByIdQueryHandler.getExternalCaregivingOrganization(
                    ExternalCaregivingOrganizationByIdQuery(
                        managerOrganizationId,
                        query.subject,
                    )
                ).name
            } else {
                "케어닥"
            }

            val hospitalState = reception.accidentInfo.hospitalAndRoomInfo.state.orEmpty()
            val hospitalCity = reception.accidentInfo.hospitalAndRoomInfo.city.orEmpty()
            val hospitalAndRoom = reception.accidentInfo.hospitalAndRoomInfo.hospitalAndRoom
            val hospitalAndRoomInfo = if (hospitalState.isEmpty() && hospitalCity.isEmpty()) {
                hospitalAndRoom
            } else {
                "$hospitalAndRoom ($hospitalState $hospitalCity)"
            }

            csvWriter.append(
                patientName,
                caregivingRound,
                hospitalAndRoomInfo,
                managerOrganizationName,
            )
        }

        return csvWriter.writeCsv()
    }

    private class CaregivingRoundCsvWriter {
        private data class CaregivingRoundCsvRecordData(
            val expectedCaregivingStartDate: String,
            val hospitalAndRoomInfo: String,
            val patientName: String,
            val caregiverName: String,
            val managerOrganizationName: String,
        ) {
            fun writeRecord() = "$expectedCaregivingStartDate,$hospitalAndRoomInfo,$patientName,$caregiverName,$managerOrganizationName"
        }

        private val records: MutableList<CaregivingRoundCsvRecordData> = mutableListOf()

        fun append(
            patientName: String,
            caregivingRound: CaregivingRound,
            hospitalAndRoomInfo: String,
            managerOrganizationName: String,
        ) = records.add(

            CaregivingRoundCsvRecordData(
                expectedCaregivingStartDate = if (caregivingRound.receptionInfo.expectedCaregivingStartDate == null) { "-" } else { caregivingRound.receptionInfo.expectedCaregivingStartDate.toString() },
                hospitalAndRoomInfo = hospitalAndRoomInfo,
                patientName = patientName,
                caregiverName = caregivingRound.caregiverInfo?.name ?: "-",
                managerOrganizationName = managerOrganizationName,
            )
        )

        fun writeCsv(): String {
            val joiner = StringJoiner("\n")

            joiner.add(writeHeader())
            records.map { it.writeRecord() }.forEach { joiner.add(it) }

            return joiner.toString()
        }

        private fun writeHeader() = "간병 예상일자,병실정보,환자명,간병인명,배정담당자 소속"
    }

    private fun CaregivingRoundsByFilterQuery.intoSearchCriteria() =
        CaregivingRoundSearchingRepository.SearchingCriteria(
            caregivingStartDateFrom = this.from,
            caregivingStartDateUntil = this.until,
            organizationId = this.organizationId,
            expectedCaregivingStartDate = this.expectedCaregivingStartDate,
            receptionProgressingStatuses = this.receptionProgressingStatuses,
            caregivingProgressingStatuses = this.caregivingProgressingStatuses,
            settlementProgressingStatuses = this.settlementProgressingStatuses,
            billingProgressingStatuses = this.billingProgressingStatuses,
            insuranceNumberContains = this.getKeyword(
                propertyToExtractionKeyword = CaregivingRoundsByFilterQuery.SearchingProperty.INSURANCE_NUMBER
            ),
            patientName = this.getKeyword(
                propertyToExtractionKeyword = CaregivingRoundsByFilterQuery.SearchingProperty.PATIENT_NAME
            ),
            accidentNumberContains = this.getKeyword(
                propertyToExtractionKeyword = CaregivingRoundsByFilterQuery.SearchingProperty.ACCIDENT_NUMBER
            ),
            receptionReceivedDateFrom = Clock.today().minusDays(200),
            caregiverName = this.getKeyword(
                propertyToExtractionKeyword = CaregivingRoundsByFilterQuery.SearchingProperty.CAREGIVER_NAME
            ),
            hospitalAndRoom = this.getKeyword(
                propertyToExtractionKeyword = CaregivingRoundsByFilterQuery.SearchingProperty.HOSPITAL_AND_ROOM
            ),
            patientPhoneNumberContains = this.getKeyword(
                propertyToExtractionKeyword = CaregivingRoundsByFilterQuery.SearchingProperty.PATIENT_PHONE_NUMBER
            ),
        )

    private fun CaregivingRoundsByFilterQuery.getKeyword(
        propertyToExtractionKeyword: CaregivingRoundsByFilterQuery.SearchingProperty
    ) = if (searchCondition?.searchingProperty == propertyToExtractionKeyword) {
        searchCondition.keyword
    } else {
        null
    }

    @Transactional
    @EventListener(CaregivingManagerAssignedToReception::class)
    fun handleCaregivingManagerAssignedToReception(e: CaregivingManagerAssignedToReception) {
        if (caregivingRoundRepository.existsByReceptionInfoReceptionId(e.receptionId)) {
            return
        }

        val reception = receptionByIdQueryHandler.getReception(
            ReceptionByIdQuery(e.receptionId, e.subject)
        )

        caregivingRoundRepository.save(
            CaregivingRound(
                id = ULID.random(),
                caregivingRoundNumber = 1,
                receptionInfo = CaregivingRound.ReceptionInfo(
                    receptionId = reception.id,
                    insuranceNumber = reception.insuranceInfo.insuranceNumber,
                    accidentNumber = reception.accidentInfo.accidentNumber,
                    maskedPatientName = reception.patientInfo.name.masked,
                    expectedCaregivingStartDate = reception.expectedCaregivingStartDate,
                    receptionProgressingStatus = reception.progressingStatus,
                    caregivingManagerInfo = e.caregivingManagerInfo,
                )
            )
        )
    }

    @Transactional(readOnly = true)
    override fun getReceptionCaregivingRounds(query: CaregivingRoundsByReceptionIdQuery): List<CaregivingRound> {
        val receptionCaregivingRounds =
            caregivingRoundRepository.findByReceptionInfoReceptionIdOrderByCaregivingRoundNumberDesc(
                receptionId = query.receptionId
            )

        CaregivingRoundAccessPolicy.checkAll(query.subject, query, receptionCaregivingRounds)

        return receptionCaregivingRounds
    }

    @Transactional
    override fun editCaregivingRound(
        query: CaregivingRoundByIdQuery,
        @IncludingPersonalData
        command: CaregivingRoundEditingCommand,
    ) {
        val subject = command.subject
        val caregivingRound = getCaregivingRound(query)

        CaregivingRoundAccessPolicy.check(command.subject, command, caregivingRound)

        command.caregiverInfo?.run { caregivingRound.assignCaregiver(this, subject) }

        command.caregivingProgressingStatus
            .compareWith(caregivingRound.caregivingProgressingStatus)
            .ifHavingDifference { _, patching -> caregivingRound.proceedCaregivingInto(patching, command) }

        command.startDateTime?.run { caregivingRound.editCaregivingStartDateTime(this, subject) }
        command.endDateTime?.run { caregivingRound.editCaregivingEndDateTime(command.endDateTime, subject) }
        caregivingRound.updateRemarks(command.remarks, subject)
        caregivingRoundRepository.save(caregivingRound)
    }

    private fun CaregivingRound.proceedCaregivingInto(
        proceedingStatus: CaregivingProgressingStatus,
        command: CaregivingRoundEditingCommand,
    ) {
        val proceedingAction = try {
            ProceedingAction.fromCaregivingProgressingStatus(proceedingStatus)
        } catch (e: UnavailableCaregivingProceedingException) {
            // do nothing
            return
        }

        when (proceedingAction) {
            ProceedingAction.START -> this.startCaregiving(command)
            ProceedingAction.STOP -> this.stop(command)
            ProceedingAction.FINISH -> this.finish(command)
            ProceedingAction.CANCEL -> this.cancel(command)
            ProceedingAction.PENDING -> this.pend(command)
            else -> throw UnavailableCaregivingProceedingException(
                proceedingStatus
            )
        }
    }

    private fun CaregivingRound.pend(command: CaregivingRoundEditingCommand) = this.pend(command.subject)

    private fun CaregivingRound.startCaregiving(
        command: CaregivingRoundEditingCommand,
    ) = this.startCaregiving(
        command.startDateTime ?: throw RequiredParameterNotSuppliedException(),
        command.subject
    )

    private fun CaregivingRound.stop(command: CaregivingRoundEditingCommand) = this.stop(
        command.endDateTime ?: throw RequiredParameterNotSuppliedException(),
        command.subject
    )

    private fun CaregivingRound.finish(command: CaregivingRoundEditingCommand) {
        val finishingResult = this.finish(
            command.endDateTime ?: throw RequiredParameterNotSuppliedException(),
            FinishingReason.fromClosingReasonType(
                command.caregivingRoundClosingReasonType ?: throw RequiredParameterNotSuppliedException()
            ),
            command.subject
        )

        finishingResult.nextRound?.run { caregivingRoundRepository.save(this) }
    }

    private fun CaregivingRound.cancel(command: CaregivingRoundEditingCommand) = this.cancel(
        CancellationReason.fromClosingReasonType(
            command.caregivingRoundClosingReasonType ?: throw RequiredParameterNotSuppliedException()
        ),
        command.caregivingRoundClosingReasonDetail ?: "",
        command.subject
    )

    private enum class ProceedingAction {
        START,
        STOP,
        FINISH,
        CANCEL,
        PENDING,
        REMATCHING,
        RECONCILIATION_COMPLETED;

        companion object {
            fun fromCaregivingProgressingStatus(
                caregivingProgressingStatus: CaregivingProgressingStatus
            ) = when (caregivingProgressingStatus) {
                CaregivingProgressingStatus.NOT_STARTED -> throw UnavailableCaregivingProceedingException(
                    caregivingProgressingStatus
                )

                CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS -> START
                CaregivingProgressingStatus.REMATCHING -> REMATCHING
                CaregivingProgressingStatus.PENDING_REMATCHING -> PENDING
                CaregivingProgressingStatus.CANCELED_WHILE_REMATCHING -> CANCEL
                CaregivingProgressingStatus.COMPLETED_RESTARTING -> STOP
                CaregivingProgressingStatus.COMPLETED -> FINISH
                CaregivingProgressingStatus.COMPLETED_USING_PERSONAL_CAREGIVER -> FINISH
                CaregivingProgressingStatus.RECONCILIATION_COMPLETED -> RECONCILIATION_COMPLETED
            }
        }
    }

    class UnavailableCaregivingProceedingException(
        proceedInto: CaregivingProgressingStatus
    ) : RuntimeException("간병은 ${proceedInto}로 진행될 수 없습니다.")

    @Transactional(readOnly = true)
    override fun getCaregivingRound(query: CaregivingRoundByIdQuery): CaregivingRound {
        val caregivingRound = caregivingRoundRepository.findByIdOrNull(query.caregivingRoundId)
            ?: throw CaregivingRoundNotFoundByIdException(query.caregivingRoundId)

        CaregivingRoundAccessPolicy.check(query.subject, query, caregivingRound)

        return caregivingRound
    }

    @Transactional
    override fun createOrEditCaregivingCharge(
        query: CaregivingRoundByIdQuery,
        command: CaregivingChargeEditingCommand,
    ) {
        val caregivingRound = getCaregivingRound(query)
        CaregivingRoundAccessPolicy.check(command.subject, command, caregivingRound)

        val caregivingCharge =
            caregivingChargeRepository.findByCaregivingRoundInfoCaregivingRoundId(query.caregivingRoundId)?.also {
                if (!it.caregivingChargeConfirmStatus.isEditableStatus) {
                    throw CaregivingChargeEditingDeniedException(it.id)
                }

                it.edit(command)
            } ?: run {
                if (!caregivingRound.caregivingProgressingStatus.isCaregivingChargeActionableStatus) {
                    throw InvalidCaregivingChargeActionableStatusException(caregivingRound.caregivingProgressingStatus)
                }

                command.intoEntity(caregivingRound)
            }

        caregivingChargeRepository.save(caregivingCharge)
    }

    private fun markCaregivingRoundInfo(caregivingRound: CaregivingRound): CaregivingCharge.CaregivingRoundInfo {

        val startDateTime = caregivingRound.startDateTime
        val endDateTime = caregivingRound.endDateTime
        val caregiverInfo = caregivingRound.caregiverInfo
        if (startDateTime == null || endDateTime == null || caregiverInfo == null) {
            throw UnknownCaregivingRoundInfoException(caregivingRound.id)
        }

        return CaregivingCharge.CaregivingRoundInfo(
            caregivingRoundId = caregivingRound.id,
            caregivingRoundNumber = caregivingRound.caregivingRoundNumber,
            startDateTime = startDateTime,
            endDateTime = endDateTime,
            dailyCaregivingCharge = caregiverInfo.dailyCaregivingCharge,
            receptionId = caregivingRound.receptionInfo.receptionId,
        )
    }

    private fun CaregivingChargeEditingCommand.intoEntity(caregivingRound: CaregivingRound) = CaregivingCharge(
        id = ULID.random(),
        caregivingRoundInfo = markCaregivingRoundInfo(caregivingRound),
        additionalHoursCharge = this.additionalHoursCharge,
        mealCost = this.mealCost,
        transportationFee = this.transportationFee,
        holidayCharge = this.holidayCharge,
        caregiverInsuranceFee = this.caregiverInsuranceFee,
        commissionFee = this.commissionFee,
        vacationCharge = this.vacationCharge,
        patientConditionCharge = this.patientConditionCharge,
        covid19TestingCost = this.covid19TestingCost,
        outstandingAmount = this.outstandingAmount,
        additionalCharges = this.additionalCharges,
        isCancelAfterArrived = this.isCancelAfterArrived,
        expectedSettlementDate = this.expectedSettlementDate,
    )

    @Transactional(readOnly = true)
    override fun getCaregivingCharge(query: CaregivingChargeByCaregivingRoundIdQuery): CaregivingCharge {
        val caregivingRound = getCaregivingRound(
            CaregivingRoundByIdQuery(
                caregivingRoundId = query.caregivingRoundId,
                subject = query.subject,
            )
        )
        CaregivingRoundAccessPolicy.check(query.subject, query, caregivingRound)

        return caregivingChargeRepository.findByCaregivingRoundInfoCaregivingRoundId(query.caregivingRoundId)
            ?: throw CaregivingChargeNotEnteredException(query.caregivingRoundId)
    }

    @Transactional
    @EventListener(ReceptionModified::class)
    fun handleReceptionModified(@IncludingPersonalData event: ReceptionModified) {
        val caregivingRounds = caregivingRoundRepository.findByReceptionInfoReceptionIdOrderByCaregivingRoundNumberDesc(
            receptionId = event.receptionId
        )
        val caregivingCharges = caregivingChargeRepository.findByCaregivingRoundInfoCaregivingRoundIdIn(
            caregivingRounds.map { it.id }
        )

        caregivingRounds.forEach { it.handleReceptionModified(event) }
        caregivingCharges.forEach { it.handleReceptionModified(event) }
    }

    @Transactional(readOnly = true)
    override fun getCaregivingRounds(query: CaregivingRoundsByIdsQuery): List<CaregivingRound> {
        CaregivingRoundAccessPolicy.check(query.subject, query, Object.Empty)

        val caregivingRounds = caregivingRoundRepository.findByIdIn(query.caregivingRoundIds)

        CaregivingRoundAccessPolicy.checkAll(query.subject, ReadOneAccess, caregivingRounds)
        ensureAllCaregivingRoundFound(caregivingRounds, query.caregivingRoundIds)

        return caregivingRounds
    }

    private fun ensureAllCaregivingRoundFound(
        foundCaregivingRounds: Collection<CaregivingRound>,
        targetIds: Collection<String>
    ) {
        val notFoundCaregivingRoundIds = targetIds.toSet() subtract foundCaregivingRounds.map { it.id }.toSet()
        notFoundCaregivingRoundIds.firstOrNull()?.let {
            throw CaregivingRoundNotFoundByIdException(it)
        }
    }

    @Transactional
    @EventListener(BillingModified::class)
    fun handleBillingModified(event: BillingModified) {
        caregivingRoundRepository.findByIdOrNull(event.caregivingRoundId)?.apply {
            if (!willBeAffectedBy(event)) {
                return
            }

            handleBillingModified(event)
            caregivingRoundRepository.save(this)
        }
    }

    @Transactional
    @EventListener(SettlementModified::class)
    fun handleSettlementModified(event: SettlementModified) {
        caregivingRoundRepository.findByIdOrNull(event.caregivingRoundId)?.apply {
            if (!willBeAffectedBy(event)) {
                return
            }

            handleSettlementModified(event)
            caregivingRoundRepository.save(this)
        }
    }

    @Transactional
    @EventListener(SettlementGenerated::class)
    fun handleSettlementGenerated(event: SettlementGenerated) {
        caregivingRoundRepository.findByIdOrNull(event.caregivingRoundId)?.handleSettlementGenerated(event)
    }

    @Transactional
    @EventListener(BillingGenerated::class)
    fun handleBillingGenerated(event: BillingGenerated) {
        caregivingRoundRepository.findByIdOrNull(event.caregivingRoundId)?.handleBillingGenerated(event)
    }

    @Transactional
    @EventListener(ReconciliationClosed::class)
    fun handleReconciliationClosed(event: ReconciliationClosed) {
        if (event.issuedType != IssuedType.FINISH) {
            return
        }

        val caregivingRound = getCaregivingRound(
            CaregivingRoundByIdQuery(event.caregivingRoundId, event.subject)
        )
        caregivingRound.handleReconciliationClosed(event)
        caregivingRoundRepository.save(caregivingRound)

        if (caregivingRoundRepository.findByReceptionInfoReceptionIdOrderByCaregivingRoundNumberDesc(
                receptionId = event.receptionId
            ).all { it.caregivingProgressingStatus.isReconciliationCompletedStatus }
        ) {
            eventPublisher.publishEvent(
                AllCaregivingRoundReconciliationCompleted(
                    receptionId = event.receptionId,
                    subject = event.subject,
                )
            )
        }
    }
}
