package kr.caredoc.careinsurance.settlement

import jakarta.persistence.CollectionTable
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Transient
import kr.caredoc.careinsurance.AggregateRoot
import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.caregiving.CaregivingChargeConfirmStatus
import kr.caredoc.careinsurance.caregiving.CaregivingChargeModified
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.reception.CaregivingManagerInfo
import kr.caredoc.careinsurance.reception.ReceptionModified
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.ObjectAttribute
import kr.caredoc.careinsurance.transaction.TransactionType
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
class Settlement(
    id: String,
    val receptionId: String,
    val caregivingRoundId: String,
    val caregivingRoundNumber: Int,
    accidentNumber: String,
    dailyCaregivingCharge: Int,
    basicAmount: Int,
    additionalAmount: Int,
    totalAmount: Int,
    lastCalculationDateTime: LocalDateTime,
    expectedSettlementDate: LocalDate,
    protected var caregivingManagerInfo: CaregivingManagerInfo? = null
) : AggregateRoot(id), Object {

    companion object {
        fun isAffectsToSettlement(event: ReceptionModified): Boolean {
            return event.accidentInfo.map { it.accidentNumber }.hasChanged
        }
    }

    @Enumerated(EnumType.STRING)
    var progressingStatus = SettlementProgressingStatus.CONFIRMED
        protected set
    var settlementCompletionDateTime: LocalDateTime? = null
        protected set
    var settlementManagerId: String? = null
        protected set
    var dailyCaregivingCharge = dailyCaregivingCharge
        protected set
    var basicAmount = basicAmount
        protected set
    var additionalAmount = additionalAmount
        protected set
    var totalAmount = totalAmount
        protected set
    var lastCalculationDateTime = lastCalculationDateTime
        protected set
    var expectedSettlementDate = expectedSettlementDate
        protected set
    var accidentNumber = accidentNumber
        protected set
    var totalDepositAmount: Int = 0
        protected set
    var totalWithdrawalAmount: Int = 0
        protected set
    var lastTransactionDatetime: LocalDateTime? = null
        protected set

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "settlement_transaction")
    protected val internalTransactions = mutableListOf<TransactionRecord>()

    @get:Transient
    val transactions: List<TransactionRecord>
        get() = internalTransactions.toList()

    @Embeddable
    data class TransactionRecord(
        @Enumerated(EnumType.STRING)
        val transactionType: TransactionType,
        val amount: Int,
        val transactionDate: LocalDate,
        val enteredDateTime: LocalDateTime,
        val transactionSubjectId: String,
    )

    fun edit(command: SettlementEditingCommand) = trackModification {
        command.progressingStatus.compareWith(this.progressingStatus)
            .ifHavingDifference { currentStatus, patchingStatus ->
                if (currentStatus == patchingStatus) {
                    return@ifHavingDifference
                }

                when (patchingStatus) {
                    SettlementProgressingStatus.COMPLETED -> {
                        val enteredSettlementManagerId = command.settlementManagerId
                            .expectValue("정산을 완료하기 위해서는 정산 담당자를 반드시 입력해야 합니다.")
                        progressIntoCompleted(enteredSettlementManagerId)
                    }

                    SettlementProgressingStatus.WAITING -> progressIntoWaiting()

                    else -> Unit
                }
            }
    }

    private fun ensureTransitionableInto(progressingStatus: SettlementProgressingStatus) {
        if (!this.progressingStatus.isTransitionableTo(progressingStatus)) {
            throw InvalidSettlementProgressingStatusTransitionException(this.progressingStatus, progressingStatus)
        }
    }

    private fun progressIntoWaiting() {
        ensureTransitionableInto(SettlementProgressingStatus.WAITING)

        this.progressingStatus = SettlementProgressingStatus.WAITING
    }

    private fun progressIntoCompleted(enteredSettlementManagerId: String) {
        ensureTransitionableInto(SettlementProgressingStatus.COMPLETED)

        val now = Clock.now()
        this.progressingStatus = SettlementProgressingStatus.COMPLETED
        this.settlementCompletionDateTime = now
        this.settlementManagerId = enteredSettlementManagerId
        addWithdrawalTransaction(this.totalAmount, now.toLocalDate(), enteredSettlementManagerId, now)
    }

    private fun addWithdrawalTransaction(
        amount: Int,
        transactionDate: LocalDate,
        transactionSubjectId: String,
        now: LocalDateTime
    ) {
        val transactionRecord = TransactionRecord(
            transactionType = TransactionType.WITHDRAWAL,
            amount = amount,
            transactionDate = transactionDate,
            enteredDateTime = now,
            transactionSubjectId = transactionSubjectId,
        )
        this.internalTransactions.add(
            transactionRecord
        )
        this.totalWithdrawalAmount += amount
        this.lastTransactionDatetime = now

        registerTransactionRecorded(transactionRecord)
    }

    private fun addDepositTransaction(
        amount: Int,
        transactionDate: LocalDate,
        transactionSubjectId: String,
        now: LocalDateTime
    ) {
        val transactionRecord = TransactionRecord(
            transactionType = TransactionType.DEPOSIT,
            amount = amount,
            transactionDate = transactionDate,
            enteredDateTime = now,
            transactionSubjectId = transactionSubjectId,
        )
        this.internalTransactions.add(
            transactionRecord
        )
        this.totalDepositAmount += amount
        this.lastTransactionDatetime = now

        registerTransactionRecorded(transactionRecord)
    }

    private fun registerTransactionRecorded(transaction: TransactionRecord) {
        registerEvent(
            SettlementTransactionRecorded(
                receptionId = this.receptionId,
                caregivingRoundId = this.caregivingRoundId,
                settlementId = this.id,
                transactionDate = transaction.transactionDate,
                transactionType = transaction.transactionType,
                amount = transaction.amount,
                enteredDateTime = transaction.enteredDateTime,
                order = this.transactions.size - 1,
                progressingStatus = progressingStatus,
                totalAmount = totalAmount,
                totalDepositAmount = totalDepositAmount,
                totalWithdrawalAmount = totalWithdrawalAmount,
            )
        )
    }

    fun handleCaregivingChargeModified(event: CaregivingChargeModified) = trackModification {
        basicAmount = event.basicAmount
        additionalAmount = event.additionalAmount
        totalAmount = event.totalAmount
        expectedSettlementDate = event.expectedSettlementDate.current
        lastCalculationDateTime = event.calculatedDateTime

        if (event.confirmStatus == CaregivingChargeConfirmStatus.CONFIRMED) {
            progressIntoWaiting()
        }
    }

    fun recordTransaction(command: SettlementTransactionRecordingCommand) {
        SettlementAccessPolicy.check(command.subject, command, this)

        val now = Clock.now()
        when (command.transactionType) {
            TransactionType.DEPOSIT -> addDepositTransaction(
                amount = command.amount,
                transactionDate = command.transactionDate,
                transactionSubjectId = command.transactionSubjectId,
                now = now,
            )

            TransactionType.WITHDRAWAL -> addWithdrawalTransaction(
                amount = command.amount,
                transactionDate = command.transactionDate,
                transactionSubjectId = command.transactionSubjectId,
                now = now,
            )
        }
    }

    private fun trackModification(block: () -> Unit) {
        val tracker = ModificationTracker()

        block()

        val modification = tracker.getModification()

        if (modification.hasChanged) {
            updateModifiedEvent(modification)
        }
    }

    private fun updateModifiedEvent(modification: Modification<TrackedData>) {
        updateEvent(
            SettlementModified::class,
            SettlementModified(
                caregivingRoundId = caregivingRoundId,
                progressingStatus = modification.map { it.progressingStatus },
                settlementId = id,
                totalAmount = totalAmount,
                totalDepositAmount = totalDepositAmount,
                totalWithdrawalAmount = totalWithdrawalAmount,
            )
        )
    }

    private inner class ModificationTracker {
        private val previous = generateTrackedData()

        private fun generateTrackedData() = TrackedData(
            progressingStatus = progressingStatus,
        )

        fun getModification() = Modification(previous, generateTrackedData())
    }

    private data class TrackedData(
        val progressingStatus: SettlementProgressingStatus,
    )

    fun handleReceptionModified(event: ReceptionModified) {
        event.accidentInfo.map { it.accidentNumber }.ifChanged {
            accidentNumber = current
        }
        event.caregivingManagerInfo.ifChanged {
            caregivingManagerInfo = current
        }
    }

    init {
        registerEvent(
            SettlementGenerated(
                caregivingRoundId = caregivingRoundId,
                progressingStatus = progressingStatus,
                settlementId = id,
                totalAmount = totalAmount
            )
        )
    }

    override fun get(attribute: ObjectAttribute): Set<String> = when (attribute) {
        ObjectAttribute.ASSIGNED_ORGANIZATION_ID -> this.caregivingManagerInfo?.organizationId?.let {
            setOf(it)
        } ?: setOf()

        else -> setOf()
    }
}
