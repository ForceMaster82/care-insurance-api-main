package kr.caredoc.careinsurance.caregiving.state

import kr.caredoc.careinsurance.caregiving.CaregiverInfo
import kr.caredoc.careinsurance.caregiving.CaregivingProgressingStatus
import kr.caredoc.careinsurance.caregiving.exception.InvalidCaregivingProgressingStatusTransitionException
import java.time.LocalDateTime

class StoppedState(
    private val caregivingRoundInfo: CaregivingRoundInfo,
    private val caregiverInfo: CaregiverInfo,
    private val startDateTime: LocalDateTime,
    private val stoppedAt: LocalDateTime,
) : CaregivingState {
    override val stateData: CaregivingStateData
        get() = CaregivingStateData(
            progressingStatus = CaregivingProgressingStatus.COMPLETED_RESTARTING,
            caregiverInfo = caregiverInfo,
            startDateTime = startDateTime,
            endDateTime = stoppedAt
        )

    override fun assignCaregiver(caregiverInfo: CaregiverInfo): CaregivingState {
        return StoppedState(
            caregivingRoundInfo,
            caregiverInfo,
            startDateTime,
            stoppedAt,
        )
    }

    override fun start(startDateTime: LocalDateTime): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.COMPLETED_RESTARTING,
            CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
        )
    }

    override fun editStartDateTime(startDateTime: LocalDateTime): CaregivingState {
        return StoppedState(
            caregivingRoundInfo,
            caregiverInfo,
            startDateTime,
            stoppedAt,
        )
    }

    override fun editEndDateTime(endDateTime: LocalDateTime): CaregivingState {
        return StoppedState(
            caregivingRoundInfo = caregivingRoundInfo,
            caregiverInfo = caregiverInfo,
            startDateTime = startDateTime,
            stoppedAt = endDateTime,
        )
    }

    override fun complete(endDateTime: LocalDateTime, reason: FinishingReason): CaregivingState {
        return CompleteState(
            caregivingRoundInfo = caregivingRoundInfo,
            caregiverInfo = caregiverInfo,
            startDateTime = startDateTime,
            endDateTime = endDateTime,
            reason = reason
        )
    }

    override fun stop(stopDateTime: LocalDateTime): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.COMPLETED_RESTARTING,
            CaregivingProgressingStatus.COMPLETED_RESTARTING
        )
    }

    override fun cancel(reason: CancellationReason, detailReason: String): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.COMPLETED_RESTARTING,
            CaregivingProgressingStatus.CANCELED_WHILE_REMATCHING
        )
    }

    override fun pend(): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.COMPLETED_RESTARTING,
            CaregivingProgressingStatus.PENDING_REMATCHING
        )
    }

    override fun completeReconciliation(): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.COMPLETED_RESTARTING,
            CaregivingProgressingStatus.RECONCILIATION_COMPLETED
        )
    }
}
