package kr.caredoc.careinsurance.caregiving.state

import kr.caredoc.careinsurance.caregiving.CaregiverInfo
import kr.caredoc.careinsurance.caregiving.CaregivingProgressingStatus
import kr.caredoc.careinsurance.caregiving.exception.InvalidCaregivingProgressingStatusTransitionException
import java.time.LocalDateTime

class CompleteState(
    private val caregivingRoundInfo: CaregivingRoundInfo,
    private val caregiverInfo: CaregiverInfo,
    private val startDateTime: LocalDateTime,
    private val endDateTime: LocalDateTime,
    private val reason: FinishingReason
) : CaregivingState {
    private val progressingStatus = when (reason) {
        FinishingReason.FINISHED_USING_PERSONAL_CAREGIVER -> CaregivingProgressingStatus.COMPLETED_USING_PERSONAL_CAREGIVER
        else -> CaregivingProgressingStatus.COMPLETED
    }

    override val stateData: CaregivingStateData
        get() = CaregivingStateData(
            progressingStatus = progressingStatus,
            caregiverInfo = caregiverInfo,
            startDateTime = startDateTime,
            endDateTime = endDateTime,
            closingReasonType = reason.intoClosingReasonType()
        )

    override fun assignCaregiver(caregiverInfo: CaregiverInfo): CaregivingState {
        return CompleteState(
            caregivingRoundInfo,
            caregiverInfo,
            startDateTime,
            endDateTime,
            reason
        )
    }

    override fun start(startDateTime: LocalDateTime): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.COMPLETED,
            CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
        )
    }

    override fun editStartDateTime(startDateTime: LocalDateTime): CaregivingState {
        return CompleteState(
            caregivingRoundInfo,
            caregiverInfo,
            startDateTime,
            endDateTime,
            reason
        )
    }

    override fun editEndDateTime(endDateTime: LocalDateTime): CaregivingState {
        return CompleteState(
            caregivingRoundInfo,
            caregiverInfo,
            startDateTime,
            endDateTime,
            reason
        )
    }

    override fun complete(endDateTime: LocalDateTime, reason: FinishingReason): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            progressingStatus,
            CaregivingProgressingStatus.COMPLETED
        )
    }

    override fun stop(stopDateTime: LocalDateTime): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            progressingStatus,
            CaregivingProgressingStatus.COMPLETED_RESTARTING
        )
    }

    override fun cancel(reason: CancellationReason, detailReason: String): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            progressingStatus,
            CaregivingProgressingStatus.CANCELED_WHILE_REMATCHING
        )
    }

    override fun pend(): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            progressingStatus,
            CaregivingProgressingStatus.PENDING_REMATCHING
        )
    }

    override fun completeReconciliation(): CaregivingState {
        return ReconciliationCompletedState(
            caregivingRoundInfo,
            caregiverInfo,
            startDateTime,
            endDateTime,
            reason
        )
    }
}
