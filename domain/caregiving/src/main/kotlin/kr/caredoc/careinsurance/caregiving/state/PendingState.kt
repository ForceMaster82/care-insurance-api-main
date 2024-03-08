package kr.caredoc.careinsurance.caregiving.state

import kr.caredoc.careinsurance.caregiving.CaregiverInfo
import kr.caredoc.careinsurance.caregiving.CaregivingNotFinishedException
import kr.caredoc.careinsurance.caregiving.CaregivingProgressingStatus
import kr.caredoc.careinsurance.caregiving.exception.CaregivingNotStartedException
import kr.caredoc.careinsurance.caregiving.exception.InvalidCaregivingProgressingStatusTransitionException
import java.time.LocalDateTime

class PendingState(
    private val caregivingRoundInfo: CaregivingRoundInfo,
    private val caregiverInfo: CaregiverInfo
) : CaregivingState {
    override val stateData: CaregivingStateData
        get() = CaregivingStateData(
            progressingStatus = CaregivingProgressingStatus.PENDING_REMATCHING,
            caregiverInfo = caregiverInfo,
        )

    override fun assignCaregiver(caregiverInfo: CaregiverInfo): CaregivingState {
        return RematchingState(
            caregivingRoundInfo,
            caregiverInfo
        )
    }

    override fun start(startDateTime: LocalDateTime): CaregivingState {
        return InProgressState(
            caregivingRoundInfo,
            caregiverInfo,
            startDateTime
        )
    }

    override fun editStartDateTime(startDateTime: LocalDateTime): CaregivingState {
        throw CaregivingNotStartedException(caregivingRoundInfo.caregivingRoundId)
    }

    override fun editEndDateTime(endDateTime: LocalDateTime): CaregivingState {
        throw CaregivingNotFinishedException(caregivingRoundInfo.caregivingRoundId)
    }

    override fun complete(endDateTime: LocalDateTime, reason: FinishingReason): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.PENDING_REMATCHING,
            CaregivingProgressingStatus.COMPLETED
        )
    }

    override fun stop(stopDateTime: LocalDateTime): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.PENDING_REMATCHING,
            CaregivingProgressingStatus.COMPLETED_RESTARTING
        )
    }

    override fun cancel(reason: CancellationReason, detailReason: String): CaregivingState {
        return CanceledState(
            caregivingRoundInfo = caregivingRoundInfo,
            caregiverInfo = caregiverInfo,
            reason = reason,
            detailReason = detailReason
        )
    }

    override fun pend(): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.PENDING_REMATCHING,
            CaregivingProgressingStatus.PENDING_REMATCHING
        )
    }

    override fun completeReconciliation(): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.PENDING_REMATCHING,
            CaregivingProgressingStatus.RECONCILIATION_COMPLETED
        )
    }
}
