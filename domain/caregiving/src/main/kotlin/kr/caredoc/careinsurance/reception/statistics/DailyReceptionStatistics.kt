package kr.caredoc.careinsurance.reception.statistics

import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Entity
import kr.caredoc.careinsurance.AggregateRoot
import kr.caredoc.careinsurance.billing.BillingModified
import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import kr.caredoc.careinsurance.billing.BillingTransactionRecorded
import kr.caredoc.careinsurance.reception.Reception
import kr.caredoc.careinsurance.reception.ReceptionModified
import kr.caredoc.careinsurance.reception.ReceptionProgressingStatus
import kr.caredoc.careinsurance.reception.ReceptionReceived
import kr.caredoc.careinsurance.then
import kr.caredoc.careinsurance.transaction.TransactionType
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
class DailyReceptionStatistics(
    id: String,
    @Access(AccessType.FIELD)
    val receivedDate: LocalDate,
) : AggregateRoot(id) {
    companion object {
        val STARTED_RECEPTION_STATUSES = setOf(
            ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS,
            ReceptionProgressingStatus.COMPLETED,
        )

        val REQUESTED_BILLING_PROGRESSING_STATUSES = setOf(
            BillingProgressingStatus.WAITING_DEPOSIT,
            BillingProgressingStatus.OVER_DEPOSIT,
            BillingProgressingStatus.UNDER_DEPOSIT,
            BillingProgressingStatus.COMPLETED_DEPOSIT,
        )
    }

    var receptionCount: Int = 0
        protected set
    var canceledReceptionCount: Int = 0
        protected set
    var canceledByPersonalCaregiverReceptionCount: Int = 0
        protected set
    var canceledByMedicalRequestReceptionCount: Int = 0
        protected set
    var requestedBillingCount: Int = 0
        protected set
    var requestedBillingAmount: Int = 0
        protected set
    var depositCount: Int = 0
        protected set
    var depositAmount: Int = 0
        protected set
    var withdrawalCount: Int = 0
        protected set
    var withdrawalAmount: Int = 0
        protected set
    var sameDayAssignmentReceptionCount: Int = 0
        protected set
    var startedSameDayAssignmentReceptionCount: Int = 0
        protected set
    var shortTermReceptionCount: Int = 0
        protected set
    var startedShortTermReceptionCount: Int = 0
        protected set

    fun handleReceptionReceived(event: ReceptionReceived) {
        receptionCount += 1

        event.isSameDayAssignment().then {
            sameDayAssignmentReceptionCount += 1
        }

        event.isShortTerm().then {
            shortTermReceptionCount += 1
        }
    }

    fun handleReceptionModified(event: ReceptionModified) {
        event.progressingStatus.ifChanged {
            when (current) {
                ReceptionProgressingStatus.CANCELED_BY_MEDICAL_REQUEST -> canceledByMedicalRequestReceptionCount += 1
                ReceptionProgressingStatus.CANCELED_BY_PERSONAL_CAREGIVER -> canceledByPersonalCaregiverReceptionCount += 1
                else -> Unit
            }
        }

        val sameDayAssignmentModification = event.desiredCaregivingStartDate.map {
            isSameDayAssignment(event.urgency, event.receivedDateTime, it)
        }
        sameDayAssignmentModification.ifChanged {
            when (current) {
                true -> sameDayAssignmentReceptionCount += 1
                false -> sameDayAssignmentReceptionCount -= 1
            }
        }

        event.progressingStatus.map { it.isCancellationStatus }.ifChanged {
            canceledReceptionCount += 1

            if (sameDayAssignmentModification.current) {
                sameDayAssignmentReceptionCount -= 1
            }

            if (event.periodType.current == Reception.PeriodType.SHORT) {
                shortTermReceptionCount -= 1
            }
        }

        event.periodType.ifChanged {
            when (current) {
                Reception.PeriodType.SHORT -> shortTermReceptionCount += 1
                else -> shortTermReceptionCount -= 1
            }
        }

        event.periodType.ifChanged {
            when (current) {
                Reception.PeriodType.SHORT -> {
                    if (event.progressingStatus.previous == ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS &&
                        event.progressingStatus.current == ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                    ) {
                        startedShortTermReceptionCount += 1
                    }

                    if (event.progressingStatus.previous == ReceptionProgressingStatus.COMPLETED &&
                        event.progressingStatus.current == ReceptionProgressingStatus.COMPLETED
                    ) {
                        startedShortTermReceptionCount += 1
                    }
                }
                else -> {
                    if (event.progressingStatus.previous == event.progressingStatus.current &&
                        event.progressingStatus.current == ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                    ) {
                        startedShortTermReceptionCount -= 1
                    }

                    if (event.progressingStatus.previous == event.progressingStatus.current &&
                        event.progressingStatus.current == ReceptionProgressingStatus.COMPLETED
                    ) {
                        startedShortTermReceptionCount -= 1
                    }
                }
            }
        }

        event.progressingStatus.map { it.isStarted }.ifChanged {
            sameDayAssignmentModification.current.then {
                startedSameDayAssignmentReceptionCount += 1
            }
            if (event.periodType.current == Reception.PeriodType.SHORT) {
                startedShortTermReceptionCount += 1
            }
        }
    }

    private fun ReceptionReceived.isSameDayAssignment() = isSameDayAssignment(
        this.urgency,
        this.receivedDateTime,
        this.desiredCaregivingStartDate,
    )

    private fun isSameDayAssignment(
        urgency: Reception.Urgency,
        receivedDateTime: LocalDateTime,
        desiredCaregivingStartDate: LocalDate
    ) = urgency == Reception.Urgency.URGENT && receivedDateTime.toLocalDate() == desiredCaregivingStartDate

    private fun ReceptionReceived.isShortTerm() = this.periodType == Reception.PeriodType.SHORT

    private val ReceptionProgressingStatus.isStarted: Boolean
        get() = STARTED_RECEPTION_STATUSES.contains(this)

    fun handleBillingModified(event: BillingModified) {
        if (!event.progressingStatus.current.isRequested()) {
            // 요청되지 않은 청구는 통계로 집계되지 않습니다.
            return
        }

        event.progressingStatus.map {
            it.isRequested()
        }.ifChanged {
            requestedBillingCount += 1
            requestedBillingAmount += event.totalAmount.current
        }.orElse {
            requestedBillingAmount += event.totalAmount.current - event.totalAmount.previous
        }
    }

    private fun BillingProgressingStatus.isRequested() = REQUESTED_BILLING_PROGRESSING_STATUSES.contains(this)

    fun handleBillingTransactionRecorded(event: BillingTransactionRecorded) {
        when (event.transactionType) {
            TransactionType.DEPOSIT -> {
                depositCount += 1
                depositAmount += event.amount
            }

            TransactionType.WITHDRAWAL -> {
                withdrawalCount += 1
                withdrawalAmount += event.amount
            }
        }
    }
}
