package kr.caredoc.careinsurance.reconciliation

import com.github.guepardoapps.kulid.ULID
import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationByIdQuery
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationByIdQueryHandler
import kr.caredoc.careinsurance.billing.BillingByCaregivingRoundIdQuery
import kr.caredoc.careinsurance.billing.BillingByCaregivingRoundIdQueryHandler
import kr.caredoc.careinsurance.billing.BillingModified
import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import kr.caredoc.careinsurance.caregiving.CaregivingNotFinishedException
import kr.caredoc.careinsurance.caregiving.CaregivingRound
import kr.caredoc.careinsurance.caregiving.CaregivingRoundByIdQuery
import kr.caredoc.careinsurance.caregiving.CaregivingRoundByIdQueryHandler
import kr.caredoc.careinsurance.caregiving.CaregivingRoundModified
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.SystemUser
import kr.caredoc.careinsurance.security.hash.PepperedHasher
import kr.caredoc.careinsurance.security.personaldata.IncludingPersonalData
import kr.caredoc.careinsurance.settlement.SettlementByCaregivingRoundIdQuery
import kr.caredoc.careinsurance.settlement.SettlementByCaregivingRoundIdQueryHandler
import kr.caredoc.careinsurance.settlement.SettlementProgressingStatus
import kr.caredoc.careinsurance.settlement.SettlementTransactionRecorded
import kr.caredoc.careinsurance.transaction.TransactionType
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.temporal.ChronoUnit

@Service
class ReconciliationService(
    private val reconciliationRepository: ReconciliationRepository,
    private val caregivingRoundByIdQueryHandler: CaregivingRoundByIdQueryHandler,
    private val externalCaregivingOrganizationByIdQueryHandler: ExternalCaregivingOrganizationByIdQueryHandler,
    private val settlementByCaregivingRoundIdQueryHandler: SettlementByCaregivingRoundIdQueryHandler,
    private val billingByCaregivingRoundIdQueryHandler: BillingByCaregivingRoundIdQueryHandler,
    private val patientNameHasher: PepperedHasher,
    private val reconciliationCsvTemplate: ReconciliationCsvTemplate,
) : OpenReconciliationsByFilterQueryHandler,
    ClosedReconciliationsByFilterQueryHandler,
    ReconciliationEditingCommandHandler {

    companion object {
        private val SETTLEMENT_READY_TO_RECONCILE_STATUSES = setOf(SettlementProgressingStatus.COMPLETED)
        private val BILLING_READY_TO_RECONCILE_STATUSES = setOf(
            BillingProgressingStatus.WAITING_DEPOSIT,
            BillingProgressingStatus.OVER_DEPOSIT,
            BillingProgressingStatus.UNDER_DEPOSIT,
            BillingProgressingStatus.COMPLETED_DEPOSIT,
        )
    }

    @Transactional
    override fun getOpenReconciliations(
        query: OpenReconciliationsByFilterQuery,
        pageRequest: Pageable
    ): Page<Reconciliation> {
        ReconciliationAccessPolicy.check(query.subject, query, Object.Empty)
        return when (query.searchCondition?.searchingProperty) {
            OpenReconciliationsByFilterQuery.SearchingProperty.ACCIDENT_NUMBER -> reconciliationRepository.findByIssuedDateBetweenAndClosingStatusAndAccidentNumberLike(
                from = query.from,
                until = query.until,
                closingStatus = ClosingStatus.OPEN,
                accidentNumber = query.searchCondition.keyword,
                pageable = pageRequest,
            )

            OpenReconciliationsByFilterQuery.SearchingProperty.CAREGIVER_NAME -> reconciliationRepository.findBycaregiverNameLike(
                from = query.from,
                until = query.until,
                closingStatus = ClosingStatus.OPEN,
                caregiverName = query.searchCondition.keyword,
                pageable = pageRequest,
            )

            OpenReconciliationsByFilterQuery.SearchingProperty.PATIENT_NAME -> reconciliationRepository.findByIssuedDateBetweenAndClosingStatusAndHashedPatientName(
                from = query.from,
                until = query.until,
                closingStatus = ClosingStatus.OPEN,
                hashedPatientName = patientNameHasher.hashAsHex(query.searchCondition.keyword),
                pageable = pageRequest,
            )

            null -> reconciliationRepository.findByIssuedDateBetweenAndClosingStatus(
                from = query.from,
                until = query.until,
                closingStatus = ClosingStatus.OPEN,
                pageable = pageRequest,
            )
        }
    }

    private fun getOpenReconciliations(
        query: OpenReconciliationsByFilterQuery,
    ): List<Reconciliation> {
        ReconciliationAccessPolicy.check(query.subject, query, Object.Empty)
        return when (query.searchCondition?.searchingProperty) {
            OpenReconciliationsByFilterQuery.SearchingProperty.ACCIDENT_NUMBER -> reconciliationRepository.findByIssuedDateBetweenAndClosingStatusAndAccidentNumberLike(
                from = query.from,
                until = query.until,
                closingStatus = ClosingStatus.OPEN,
                accidentNumber = query.searchCondition.keyword,
            )

            OpenReconciliationsByFilterQuery.SearchingProperty.CAREGIVER_NAME -> reconciliationRepository.findBycaregiverNameLike(
                from = query.from,
                until = query.until,
                closingStatus = ClosingStatus.OPEN,
                caregiverName = query.searchCondition.keyword,
            )

            OpenReconciliationsByFilterQuery.SearchingProperty.PATIENT_NAME -> reconciliationRepository.findByIssuedDateBetweenAndClosingStatusAndHashedPatientName(
                from = query.from,
                until = query.until,
                closingStatus = ClosingStatus.OPEN,
                hashedPatientName = patientNameHasher.hashAsHex(query.searchCondition.keyword),
            )

            null -> reconciliationRepository.findByIssuedDateBetweenAndClosingStatus(
                from = query.from,
                until = query.until,
                closingStatus = ClosingStatus.OPEN,
            )
        }
    }

    @Transactional
    override fun getOpenReconciliationsAsCsv(query: OpenReconciliationsByFilterQuery): String {
        val reconciliations = getOpenReconciliations(query)
        return reconciliationCsvTemplate.generate(reconciliations, query.subject)
    }

    @Transactional
    override fun getClosedReconciliations(
        query: ClosedReconciliationsByFilterQuery,
        pageRequest: Pageable
    ): Page<Reconciliation> {
        ReconciliationAccessPolicy.check(query.subject, query, Object.Empty)
        return reconciliationRepository.findByReconciledYearAndReconciledMonthAndClosingStatusOrderByIdDesc(
            reconciledYear = query.year,
            reconciledMonth = query.month,
            closingStatus = ClosingStatus.CLOSED,
            pageRequest,
        )
    }

    private fun getClosedReconciliations(
        query: ClosedReconciliationsByFilterQuery,
    ): List<Reconciliation> {
        ReconciliationAccessPolicy.check(query.subject, query, Object.Empty)
        return reconciliationRepository.findByReconciledYearAndReconciledMonthAndClosingStatusOrderByIdDesc(
            reconciledYear = query.year,
            reconciledMonth = query.month,
            closingStatus = ClosingStatus.CLOSED,
        )
    }

    @Transactional
    override fun getClosedReconciliationsAsCsv(query: ClosedReconciliationsByFilterQuery): String {
        val reconciliations = getClosedReconciliations(query)
        return reconciliationCsvTemplate.generate(reconciliations, query.subject)
    }

    @EventListener(CaregivingRoundModified::class)
    @Transactional
    fun handleCaregivingRoundModified(@IncludingPersonalData event: CaregivingRoundModified) {
        if (!event.hasReadyToReconcile()) {
            return
        }

        val caregivingRound = getCaregivingRound(event.caregivingRoundId)
        val externalCaregivingOrganization = getCaregivingExternalOrganization(caregivingRound)
        val settlement = settlementByCaregivingRoundIdQueryHandler.getSettlement(
            SettlementByCaregivingRoundIdQuery(
                caregivingRoundId = caregivingRound.id,
                subject = SystemUser,
            )
        )
        val billing = billingByCaregivingRoundIdQueryHandler.getBilling(
            BillingByCaregivingRoundIdQuery(
                caregivingRoundId = caregivingRound.id,
                subject = SystemUser,
            )
        )

        val profit = billing.totalAmount - settlement.totalAmount
        val profitAllocationRatio = externalCaregivingOrganization?.profitAllocationRatio ?: 0.0f
        val caregivingEndDateTime = caregivingRound.endDateTime
            ?: throw CaregivingNotFinishedException(caregivingRound.id)

        val reconciliation = Reconciliation(
            id = ULID.random(),
            receptionId = caregivingRound.receptionInfo.receptionId,
            caregivingRoundId = caregivingRound.id,
            issuedDate = caregivingEndDateTime.toLocalDate(),
            issuedType = IssuedType.FINISH,
            billingAmount = billing.totalAmount,
            settlementAmount = settlement.totalAmount,
            settlementDepositAmount = 0,
            settlementWithdrawalAmount = 0,
            profit = profit,
            distributedProfit = (profit * profitAllocationRatio).toInt(),
            caregiverPhoneNumberWhenIssued = caregivingRound.caregiverInfo?.phoneNumber ?: "",
            actualCaregivingSecondsWhenIssued = caregivingRound.calculateCaregivingPeriodInSeconds().toInt(),
            reconciledYear = caregivingEndDateTime.year,
            reconciledMonth = caregivingEndDateTime.monthValue,
        )

        reconciliationRepository.save(reconciliation)
    }

    private fun CaregivingRoundModified.hasReadyToReconcile(): Boolean {
        val billingReadyStateModification = this.billingProgressingStatus.map { it.hasReadyToReconcile() }
        val settlementReadyStateModification = this.settlementProgressingStatus.map { it.hasReadyToReconcile() }
        val statusChanged = billingReadyStateModification.hasChanged || settlementReadyStateModification.hasChanged

        return statusChanged &&
            billingReadyStateModification.current &&
            settlementReadyStateModification.current
    }

    private fun SettlementProgressingStatus.hasReadyToReconcile(): Boolean =
        SETTLEMENT_READY_TO_RECONCILE_STATUSES.contains(this)

    private fun BillingProgressingStatus.hasReadyToReconcile(): Boolean =
        BILLING_READY_TO_RECONCILE_STATUSES.contains(this)

    @Transactional
    override fun editReconciliations(commands: Collection<Pair<ReconciliationByIdQuery, ReconciliationEditingCommand>>) {
        val editingCommandsById = commands.associate {
            it.first.reconciliationId to it.second
        }
        val targetIds = editingCommandsById.keys
        val reconciliations = reconciliationRepository.findByIdIn(targetIds)
        ensureAllReconciliationFound(targetIds, reconciliations)

        reconciliations.forEach { reconciliation ->
            editingCommandsById[reconciliation.id]?.let { command ->
                reconciliation.edit(command)
            }
        }

        reconciliationRepository.saveAll(reconciliations)
    }

    private fun ensureAllReconciliationFound(
        targetReconciliationIds: Collection<String>,
        foundReconciliations: Collection<Reconciliation>
    ) {
        val notFoundReconciliationIds = targetReconciliationIds.toSet().subtract(
            foundReconciliations.asSequence().map { it.id }.toSet()
        )

        notFoundReconciliationIds.firstOrNull()?.run {
            throw ReferenceReconciliationNotExistsException(this)
        }
    }

    @EventListener(SettlementTransactionRecorded::class)
    @Transactional
    fun handleSettlementTransactionRecorded(event: SettlementTransactionRecorded) {
        if (event.order == 0) {
            // 최초 입금건의 대사는 간병이 마무리되어 대사가 이뤄질때 수행합니다.
            return
        }

        val caregivingRound = getCaregivingRound(event.caregivingRoundId)
        val externalCaregivingOrganization = getCaregivingExternalOrganization(caregivingRound)

        val profitAllocationRatio = externalCaregivingOrganization?.profitAllocationRatio ?: 0.0f

        val reconciliation = when (event.transactionType) {
            TransactionType.DEPOSIT -> Reconciliation(
                id = ULID.random(),
                receptionId = caregivingRound.receptionInfo.receptionId,
                caregivingRoundId = caregivingRound.id,
                issuedDate = Clock.today(),
                issuedType = IssuedType.TRANSACTION,
                billingAmount = 0,
                settlementAmount = 0,
                settlementDepositAmount = event.amount,
                settlementWithdrawalAmount = 0,
                profit = event.amount,
                distributedProfit = (event.amount * profitAllocationRatio).toInt(),
                caregiverPhoneNumberWhenIssued = caregivingRound.caregiverInfo?.phoneNumber ?: "",
                actualCaregivingSecondsWhenIssued = caregivingRound.calculateCaregivingPeriodInSeconds().toInt(),
            )

            TransactionType.WITHDRAWAL -> Reconciliation(
                id = ULID.random(),
                receptionId = caregivingRound.receptionInfo.receptionId,
                caregivingRoundId = caregivingRound.id,
                issuedDate = Clock.today(),
                issuedType = IssuedType.TRANSACTION,
                billingAmount = 0,
                settlementAmount = 0,
                settlementDepositAmount = 0,
                settlementWithdrawalAmount = event.amount,
                profit = -event.amount,
                distributedProfit = -(event.amount * profitAllocationRatio).toInt(),
                caregiverPhoneNumberWhenIssued = caregivingRound.caregiverInfo?.phoneNumber ?: "",
                actualCaregivingSecondsWhenIssued = caregivingRound.calculateCaregivingPeriodInSeconds().toInt(),
            )
        }

        reconciliationRepository.save(reconciliation)
    }

    private fun getCaregivingRound(caregivingRoundId: String) = caregivingRoundByIdQueryHandler.getCaregivingRound(
        CaregivingRoundByIdQuery(
            caregivingRoundId = caregivingRoundId,
            subject = SystemUser,
        ),
    )

    private fun getCaregivingExternalOrganization(caregivingRound: CaregivingRound) =
        caregivingRound.receptionInfo.caregivingManagerInfo.organizationId?.let {
            externalCaregivingOrganizationByIdQueryHandler.getExternalCaregivingOrganization(
                ExternalCaregivingOrganizationByIdQuery(
                    id = it,
                    subject = SystemUser,
                ),
            )
        }

    @EventListener(BillingModified::class)
    @Transactional
    fun handleBillingModified(event: BillingModified) {
        if (event.totalAmount.previous <= 0) {
            // 최초 등록된 경우에는 처리하지 않습니다.
            return
        }
        if (!event.totalAmount.hasChanged) {
            // 금액이 변경되지 않는다면 처리하지 않습니다.
            return
        }

        val caregivingRound = getCaregivingRound(event.caregivingRoundId)
        val externalCaregivingOrganization = getCaregivingExternalOrganization(caregivingRound)
        val profitAllocationRatio = externalCaregivingOrganization?.profitAllocationRatio ?: 0.0f

        val changedAmount = event.totalAmount.current - event.totalAmount.previous

        val reconciliation = Reconciliation(
            id = ULID.random(),
            receptionId = caregivingRound.receptionInfo.receptionId,
            caregivingRoundId = caregivingRound.id,
            issuedDate = Clock.today(),
            issuedType = IssuedType.ADDITIONAL,
            billingAmount = changedAmount,
            settlementAmount = 0,
            settlementDepositAmount = 0,
            settlementWithdrawalAmount = 0,
            profit = changedAmount,
            distributedProfit = (changedAmount * profitAllocationRatio).toInt(),
            caregiverPhoneNumberWhenIssued = caregivingRound.caregiverInfo?.phoneNumber ?: "",
            actualCaregivingSecondsWhenIssued = caregivingRound.calculateCaregivingPeriodInSeconds().toInt(),
        )

        reconciliationRepository.save(reconciliation)
    }

    private fun CaregivingRound.calculateCaregivingPeriodInSeconds(): Long {
        val caregivingStartDateTime = this.startDateTime
            ?: throw CaregivingNotFinishedException(this.id)
        val caregivingEndDateTime = this.endDateTime
            ?: throw CaregivingNotFinishedException(this.id)
        return caregivingStartDateTime.until(caregivingEndDateTime, ChronoUnit.SECONDS)
    }
}
