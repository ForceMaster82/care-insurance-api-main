package kr.caredoc.careinsurance.caregiving.state

import kr.caredoc.careinsurance.caregiving.CaregiverInfo
import kr.caredoc.careinsurance.caregiving.CaregivingProgressingStatus
import kr.caredoc.careinsurance.caregiving.exception.InvalidCaregivingProgressingStatusTransitionException
import java.time.LocalDateTime

class ReconciliationCompletedState(
    private val caregivingRoundInfo: CaregivingRoundInfo,
    private val caregiverInfo: CaregiverInfo,
    private val startDateTime: LocalDateTime,
    private val endDateTime: LocalDateTime,
    private val reason: FinishingReason
) : CaregivingState {
    override val stateData: CaregivingStateData
        get() = CaregivingStateData(
            progressingStatus = CaregivingProgressingStatus.RECONCILIATION_COMPLETED,
            caregiverInfo = caregiverInfo,
            startDateTime = startDateTime,
            endDateTime = endDateTime,
            closingReasonType = reason.intoClosingReasonType()
        )

    override fun assignCaregiver(caregiverInfo: CaregiverInfo): CaregivingState {
        return ReconciliationCompletedState(
            caregivingRoundInfo,
            caregiverInfo,
            startDateTime,
            endDateTime,
            reason
        )
    }

    override fun start(startDateTime: LocalDateTime): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.RECONCILIATION_COMPLETED,
            CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
        )
    }

    override fun editStartDateTime(startDateTime: LocalDateTime): CaregivingState {
        return ReconciliationCompletedState(
            caregivingRoundInfo,
            caregiverInfo,
            startDateTime,
            endDateTime,
            reason
        )
    }

    override fun editEndDateTime(endDateTime: LocalDateTime): CaregivingState {
        return ReconciliationCompletedState(
            caregivingRoundInfo,
            caregiverInfo,
            startDateTime,
            endDateTime,
            reason
        )
    }

    override fun complete(endDateTime: LocalDateTime, reason: FinishingReason): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.RECONCILIATION_COMPLETED,
            CaregivingProgressingStatus.COMPLETED
        )
    }

    override fun stop(stopDateTime: LocalDateTime): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.RECONCILIATION_COMPLETED,
            CaregivingProgressingStatus.COMPLETED_RESTARTING
        )
    }

    override fun cancel(reason: CancellationReason, detailReason: String): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.RECONCILIATION_COMPLETED,
            CaregivingProgressingStatus.CANCELED_WHILE_REMATCHING
        )
    }

    override fun pend(): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.RECONCILIATION_COMPLETED,
            CaregivingProgressingStatus.PENDING_REMATCHING
        )
    }

    override fun completeReconciliation(): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.RECONCILIATION_COMPLETED,
            CaregivingProgressingStatus.RECONCILIATION_COMPLETED
        )
    }
}
