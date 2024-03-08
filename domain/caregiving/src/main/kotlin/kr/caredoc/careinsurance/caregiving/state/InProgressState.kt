package kr.caredoc.careinsurance.caregiving.state

import kr.caredoc.careinsurance.caregiving.CaregiverInfo
import kr.caredoc.careinsurance.caregiving.CaregivingNotFinishedException
import kr.caredoc.careinsurance.caregiving.CaregivingProgressingStatus
import kr.caredoc.careinsurance.caregiving.exception.InvalidCaregivingProgressingStatusTransitionException
import java.time.LocalDateTime

class InProgressState(
    private val caregivingRoundInfo: CaregivingRoundInfo,
    private val caregiverInfo: CaregiverInfo,
    private val startDateTime: LocalDateTime
) : CaregivingState {
    override val stateData: CaregivingStateData
        get() = CaregivingStateData(
            progressingStatus = CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS,
            caregiverInfo = caregiverInfo,
            startDateTime = startDateTime
        )

    override fun assignCaregiver(caregiverInfo: CaregiverInfo): CaregivingState {
        return InProgressState(
            caregivingRoundInfo,
            caregiverInfo,
            startDateTime
        )
    }

    override fun start(startDateTime: LocalDateTime): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS,
            CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
        )
    }

    override fun editStartDateTime(startDateTime: LocalDateTime): CaregivingState {
        return InProgressState(
            caregivingRoundInfo,
            caregiverInfo,
            startDateTime
        )
    }

    override fun editEndDateTime(endDateTime: LocalDateTime): CaregivingState {
        throw CaregivingNotFinishedException(caregivingRoundInfo.caregivingRoundId)
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
        return StoppedState(
            caregivingRoundInfo,
            caregiverInfo,
            startDateTime,
            stopDateTime
        )
    }

    override fun cancel(reason: CancellationReason, detailReason: String): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS,
            CaregivingProgressingStatus.CANCELED_WHILE_REMATCHING
        )
    }

    override fun pend(): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS,
            CaregivingProgressingStatus.PENDING_REMATCHING
        )
    }

    override fun completeReconciliation(): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS,
            CaregivingProgressingStatus.RECONCILIATION_COMPLETED
        )
    }
}
