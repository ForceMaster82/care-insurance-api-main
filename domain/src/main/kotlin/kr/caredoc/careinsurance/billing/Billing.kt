package kr.caredoc.careinsurance.billing

import jakarta.persistence.*
import kr.caredoc.careinsurance.AggregateRoot
import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.caregiving.CaregivingChargeModified
import kr.caredoc.careinsurance.caregiving.CaregivingRoundModified
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.reception.CaregivingManagerInfo
import kr.caredoc.careinsurance.reception.ReceptionModified
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.ObjectAttribute
import kr.caredoc.careinsurance.transaction.TransactionType
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.jvm.Transient

@Entity
class Billing private constructor(
    id: String,
    receptionInfo: ReceptionInfo,
    @Embedded
    val caregivingRoundInfo: CaregivingRoundInfo,
    billingProgressingStatus: BillingProgressingStatus,
    isCancelAfterArrived: Boolean,
    protected var caregivingManagerInfo: CaregivingManagerInfo?,
) : AggregateRoot(id), Object {
    companion object {
        fun isAffectsToBilling(event: ReceptionModified): Boolean {
            return event.accidentInfo.map { it.accidentNumber }.hasChanged ||
                event.insuranceInfo.map { it.subscriptionDate }.hasChanged
        }
    }

    @Embedded
    var receptionInfo = receptionInfo
        protected set

    @Enumerated(EnumType.STRING)
    var billingProgressingStatus: BillingProgressingStatus = billingProgressingStatus
        protected set

    var billingDate: LocalDate? = null
        protected set

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "billing_basic_amounts")
    val basicAmounts = mutableListOf<BasicAmount>()

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "billing_transaction_record")
    @Fetch(FetchMode.SUBSELECT)
    val transactions = mutableListOf<TransactionRecord>()

    var additionalHours: Int = 0
        protected set

    var additionalAmount: Int = 0
        protected set

    var totalAmount: Int = 0
        protected set

    var totalDepositAmount: Int = 0
        protected set
    var totalWithdrawalAmount: Int = 0
        protected set
    var lastTransactionDate: LocalDate? = null
        protected set

    var isCancelAfterArrived: Boolean = isCancelAfterArrived
        protected set

    @Column(insertable=false, updatable=false)
    var caregivingRoundId: String? = ""
        protected set


    @Embeddable
    data class ReceptionInfo(
        @Access(AccessType.FIELD)
        val receptionId: String,
        val accidentNumber: String,
        val subscriptionDate: LocalDate,
    )

    @Embeddable
    data class CaregivingRoundInfo(
        @Access(AccessType.FIELD)
        val caregivingRoundId: String,
        val roundNumber: Int,
        var startDateTime: LocalDateTime,
        var endDateTime: LocalDateTime,
    ) {
        fun willBeAffectedBy(event: CaregivingRoundModified): Boolean {
            return event.startDateTime.current != this.startDateTime || event.endDateTime.current != this.endDateTime
        }
    }

    @Embeddable
    data class BasicAmount(
        @Access(AccessType.FIELD)
        val targetAccidentYear: Int,
        val dailyCaregivingCharge: Int,
        val caregivingDays: Int,
        val totalAmount: Int,
    )

    @Embeddable
    data class TransactionRecord(
        @Enumerated(EnumType.STRING)
        val transactionType: TransactionType,
        val amount: Int,
        val transactionDate: LocalDate,
        val enteredDateTime: LocalDateTime,
        val transactionSubjectId: String,
    )

    constructor(
        id: String,
        receptionInfo: ReceptionInfo,
        caregivingRoundInfo: CaregivingRoundInfo,
        billingProgressingStatus: BillingProgressingStatus,
        coverageInfo: CoverageInfo,
        isCancelAfterArrived: Boolean,
        caregivingManagerInfo: CaregivingManagerInfo? = null,
    ) : this(
        id,
        receptionInfo,
        caregivingRoundInfo,
        billingProgressingStatus,
        isCancelAfterArrived,
        caregivingManagerInfo,
    ) {
        calculateBillingAmount(coverageInfo)
    }

    fun willBeAffectedBy(event: CaregivingRoundModified): Boolean {
        return caregivingRoundInfo.willBeAffectedBy(event)
    }

    fun handleCaregivingRoundModified(event: CaregivingRoundModified, coverageInfo: CoverageInfo) {
        val startDateTime = event.startDateTime.current
        val endDateTime = event.endDateTime.current

        if (startDateTime != null && endDateTime != null) {
            updateCaregivingPeriod(startDateTime, endDateTime)
        }

        calculateBillingAmount(coverageInfo)
    }

    fun handleCaregivingChargeModified(event: CaregivingChargeModified, coverageInfo: CoverageInfo) {
        editIsCancelAfterArrived(event.isCancelAfterArrived.current)

        calculateBillingAmount(coverageInfo)
    }

    private fun calculateBillingAmount(coverageInfo: CoverageInfo) = trackModification {
        calculateBasicAmounts(coverageInfo)
        calculateAdditionalHours()
        calculateAdditionalAmount()
        calculateTotalAmount()
    }

    private fun editIsCancelAfterArrived(isCancelAfterArrived: Boolean) {
        this.isCancelAfterArrived = isCancelAfterArrived
    }

    private fun updateCaregivingPeriod(startDateTime: LocalDateTime, endDateTime: LocalDateTime) {
        caregivingRoundInfo.startDateTime = startDateTime
        caregivingRoundInfo.endDateTime = endDateTime
    }

    fun waitDeposit() {
        processStartedBillingDate()
        processBillingProgressingStatus(BillingProgressingStatus.WAITING_DEPOSIT)
    }

    private fun processBillingProgressingStatus(status: BillingProgressingStatus) = trackModification {
        if (billingProgressingStatus == status) {
            return@trackModification
        }

        when (status) {
            BillingProgressingStatus.WAITING_DEPOSIT -> progressToWaitingDeposit()
            BillingProgressingStatus.OVER_DEPOSIT -> progressToOverDeposit()
            BillingProgressingStatus.UNDER_DEPOSIT -> progressToUnderDeposit()
            BillingProgressingStatus.COMPLETED_DEPOSIT -> progressToCompletedDeposit()
            else -> Unit
        }
    }

    private fun ensureTransitionableInto(progressingStatus: BillingProgressingStatus) {
        if (!this.billingProgressingStatus.isTransitionableTo(progressingStatus)) {
            throw InvalidBillingProgressingStatusChangeException(this.billingProgressingStatus, progressingStatus)
        }
    }

    private fun progressToWaitingDeposit() {
        ensureTransitionableInto(BillingProgressingStatus.WAITING_DEPOSIT)

        billingProgressingStatus = BillingProgressingStatus.WAITING_DEPOSIT
    }

    private fun progressToOverDeposit() {
        ensureTransitionableInto(BillingProgressingStatus.OVER_DEPOSIT)

        billingProgressingStatus = BillingProgressingStatus.OVER_DEPOSIT
    }

    private fun progressToUnderDeposit() {
        ensureTransitionableInto(BillingProgressingStatus.UNDER_DEPOSIT)

        billingProgressingStatus = BillingProgressingStatus.UNDER_DEPOSIT
    }

    private fun progressToCompletedDeposit() {
        ensureTransitionableInto(BillingProgressingStatus.COMPLETED_DEPOSIT)

        billingProgressingStatus = BillingProgressingStatus.COMPLETED_DEPOSIT
    }

    private fun updateBillingProgressingStatus() {
        val transactionAmount = totalDepositAmount - totalWithdrawalAmount
        if (totalAmount > transactionAmount) {
            processBillingProgressingStatus(BillingProgressingStatus.UNDER_DEPOSIT)
        } else if (totalAmount < transactionAmount) {
            processBillingProgressingStatus(BillingProgressingStatus.OVER_DEPOSIT)
        } else if (totalAmount == transactionAmount) {
            processBillingProgressingStatus(BillingProgressingStatus.COMPLETED_DEPOSIT)
        }
    }

    fun recordTransaction(command: BillingTransactionRecordingCommand) {
        BillingAccessPolicy.check(command.subject, command, this)

        when (command.transactionType) {
            TransactionType.DEPOSIT -> addDepositTransaction(
                amount = command.amount,
                transactionDate = command.transactionDate,
                transactionSubjectId = command.transactionSubjectId,
            )

            TransactionType.WITHDRAWAL -> addWithdrawalTransaction(
                amount = command.amount,
                transactionDate = command.transactionDate,
                transactionSubjectId = command.transactionSubjectId,
            )
        }

        updateBillingProgressingStatus()
    }

    private fun addDepositTransaction(
        amount: Int,
        transactionDate: LocalDate,
        transactionSubjectId: String,
    ) {
        val transactionRecord = TransactionRecord(
            transactionType = TransactionType.DEPOSIT,
            amount = amount,
            transactionDate = transactionDate,
            enteredDateTime = Clock.now(),
            transactionSubjectId = transactionSubjectId,
        )
        transactions.add(transactionRecord)

        totalDepositAmount += transactionRecord.amount
        lastTransactionDate = Clock.today()

        registerTransactionRecord(transactionRecord)
    }

    private fun addWithdrawalTransaction(
        amount: Int,
        transactionDate: LocalDate,
        transactionSubjectId: String,
    ) {
        val transactionRecord = TransactionRecord(
            transactionType = TransactionType.WITHDRAWAL,
            amount = amount,
            transactionDate = transactionDate,
            enteredDateTime = Clock.now(),
            transactionSubjectId = transactionSubjectId,
        )
        transactions.add(transactionRecord)

        totalWithdrawalAmount += transactionRecord.amount
        lastTransactionDate = Clock.today()

        registerTransactionRecord(transactionRecord)
    }

    private fun processStartedBillingDate() {
        if (billingDate == null) {
            this.billingDate = Clock.today()
        }
    }

    private fun registerTransactionRecord(transactionRecord: TransactionRecord) {
        registerEvent(
            BillingTransactionRecorded(
                receptionId = receptionInfo.receptionId,
                caregivingRoundId = caregivingRoundInfo.caregivingRoundId,
                billingId = id,
                progressingStatus = billingProgressingStatus,
                totalAmount = totalAmount,
                totalDepositAmount = totalDepositAmount,
                totalWithdrawalAmount = totalWithdrawalAmount,
                transactionType = transactionRecord.transactionType,
                amount = transactionRecord.amount,
                transactionDate = transactionRecord.transactionDate,
                enteredDateTime = transactionRecord.enteredDateTime,
            )
        )
    }

    private inner class ModificationTracker {
        private val previous = generateTrackData(this@Billing)

        fun getModification(): BillingModified? {
            val current = generateTrackData(this@Billing)
            if (previous == current) {
                return null
            }

            return generateModificationData(current)
        }

        private fun generateTrackData(status: Billing) = TrackedData(
            billingProgressingStatus = status.billingProgressingStatus,
            totalAmount = status.totalAmount,
            totalDepositAmount = status.totalDepositAmount,
            totalWithdrawalAmount = status.totalWithdrawalAmount,
        )

        private fun generateModificationData(status: TrackedData) = BillingModified(
            caregivingRoundId = caregivingRoundInfo.caregivingRoundId,
            receptionId = receptionInfo.receptionId,
            progressingStatus = Modification(previous.billingProgressingStatus, status.billingProgressingStatus),
            totalAmount = Modification(previous.totalAmount, status.totalAmount),
            billingId = id,
            modifiedDateTime = Clock.now(),
            totalDepositAmount = Modification(previous.totalDepositAmount, status.totalDepositAmount),
            totalWithdrawalAmount = Modification(previous.totalWithdrawalAmount, status.totalWithdrawalAmount),
        )
    }

    @Transient
    private var modificationTracker: ModificationTracker? = null

    private fun initModificationTracking() {
        if (this.modificationTracker == null) {
            this.modificationTracker = ModificationTracker()
        }
    }

    private fun updateModifiedEvent() {
        val modifiedEvent = modificationTracker?.getModification()
        this.updateEvent(BillingModified::class, modifiedEvent)
    }

    private fun trackModification(block: () -> Unit) {
        initModificationTracking()
        block()
        updateModifiedEvent()
    }

    private data class TrackedData(
        val billingProgressingStatus: BillingProgressingStatus,
        val totalAmount: Int,
        val totalDepositAmount: Int,
        val totalWithdrawalAmount: Int,
    )

    private fun calculateBasicAmounts(coverageInfo: CoverageInfo) {
        basicAmounts.clear()

        basicAmounts.addAll(
            availableAnnualCoveredCaregivingCharges(coverageInfo).map {
                val annualCaregivingChargePeriod = AnnualCaregivingChargePeriod(
                    year = it.targetAccidentYear,
                    renewalType = coverageInfo.renewalType
                )

                val caregivingDays = getValidCaregivingDays().filter { date ->
                    annualCaregivingChargePeriod.includes(date)
                }.size

                BasicAmount(
                    targetAccidentYear = it.targetAccidentYear,
                    dailyCaregivingCharge = it.caregivingCharge,
                    caregivingDays = caregivingDays,
                    totalAmount = it.caregivingCharge * caregivingDays
                )
            }
        )
    }

    private fun availableAnnualCoveredCaregivingCharges(coverageInfo: CoverageInfo): List<CoverageInfo.AnnualCoveredCaregivingCharge> {
        val annualCaregivingCharges = coverageInfo.annualCoveredCaregivingCharges.filter {
            val annualCaregivingChargePeriod = AnnualCaregivingChargePeriod(
                year = it.targetAccidentYear,
                renewalType = coverageInfo.renewalType
            )

            annualCaregivingChargePeriod.includes(caregivingRoundInfo.startDateTime.toLocalDate()) ||
                annualCaregivingChargePeriod.includes(caregivingRoundInfo.endDateTime.toLocalDate().minusDays(1))
        }

        return if (!isCancelAfterArrived) {
            annualCaregivingCharges
        } else {
            listOf(annualCaregivingCharges.last())
        }
    }

    private fun getValidCaregivingDays() = if (!isCancelAfterArrived) {
        val days =
            Duration.between(caregivingRoundInfo.startDateTime, caregivingRoundInfo.endDateTime).toDaysPart().toInt()
        caregivingRoundInfo.startDateTime.toLocalDate()
            .datesUntil(caregivingRoundInfo.endDateTime.toLocalDate())
            .toList().take(days)
    } else {
        listOf(caregivingRoundInfo.endDateTime.toLocalDate())
    }

    private fun calculateAdditionalHours() {
        additionalHours = if (!isCancelAfterArrived) {
            Duration.between(caregivingRoundInfo.startDateTime, caregivingRoundInfo.endDateTime).toHoursPart()
        } else {
            0
        }
    }

    private fun calculateAdditionalAmount() {
        additionalAmount = if (!isCancelAfterArrived) {
            if (additionalHours < 4) {
                additionalHours * 20000
            } else {
                basicAmounts.last().dailyCaregivingCharge
            }
        } else {
            0
        }
    }

    private fun calculateTotalAmount() {
        totalAmount = if (!isCancelAfterArrived) {
            basicAmounts.sumOf { it.totalAmount } + additionalAmount
        } else {
            basicAmounts.last().dailyCaregivingCharge
        }
    }

    inner class AnnualCaregivingChargePeriod(
        year: Int,
        renewalType: CoverageInfo.RenewalType,
    ) {
        private val baseDate: LocalDate = LocalDate.of(
            year,
            receptionInfo.subscriptionDate.monthValue,
            receptionInfo.subscriptionDate.dayOfMonth
        )
        private val availableDays = when (renewalType) {
            CoverageInfo.RenewalType.TEN_YEAR -> 364
            CoverageInfo.RenewalType.THREE_YEAR -> 364 * 3 + 2
        }

        fun includes(targetDate: LocalDate): Boolean {
            val distance = Duration.between(baseDate.atStartOfDay(), targetDate.atStartOfDay()).toDays()
            return distance in 0..availableDays
        }
    }

    fun handleReceptionModified(event: ReceptionModified) {
        if (!isAffectsToBilling(event)) {
            return
        }

        this.receptionInfo = ReceptionInfo(
            receptionId = receptionInfo.receptionId,
            accidentNumber = event.accidentInfo.current.accidentNumber,
            subscriptionDate = event.insuranceInfo.current.subscriptionDate,
        )

        event.caregivingManagerInfo.ifChanged {
            this@Billing.caregivingManagerInfo = current
        }
    }

    init {
        registerEvent(
            BillingGenerated(
                caregivingRoundId = caregivingRoundInfo.caregivingRoundId,
                progressingStatus = billingProgressingStatus,
                billingId = id,
                billingAmount = totalAmount,
                issuedDateTime = Clock.now(),
            )
        )
    }

    override fun get(attribute: ObjectAttribute) = when (attribute) {
        ObjectAttribute.ASSIGNED_ORGANIZATION_ID -> this.caregivingManagerInfo?.organizationId?.let {
            setOf(it)
        } ?: setOf()

        else -> setOf()
    }
}
