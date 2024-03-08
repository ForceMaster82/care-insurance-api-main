package kr.caredoc.careinsurance.caregiving.state

import kr.caredoc.careinsurance.caregiving.CaregiverInfo
import kr.caredoc.careinsurance.caregiving.CaregivingNotFinishedException
import kr.caredoc.careinsurance.caregiving.CaregivingProgressingStatus
import kr.caredoc.careinsurance.caregiving.exception.CaregiverNotAssignedException
import kr.caredoc.careinsurance.caregiving.exception.CaregivingNotStartedException
import kr.caredoc.careinsurance.caregiving.exception.InvalidCaregivingProgressingStatusTransitionException
import java.time.LocalDateTime

class InitialState private constructor(
    private val caregivingRoundInfo: CaregivingRoundInfo,
) : CaregivingState {
    constructor(
        caregivingRoundInfo: CaregivingRoundInfo,
        caregiverInfo: CaregiverInfo?
    ) : this (caregivingRoundInfo) {
        this.caregiverInfo = caregiverInfo
    }
    override val stateData: CaregivingStateData
        get() = CaregivingStateData(
            progressingStatus = CaregivingProgressingStatus.NOT_STARTED,
            caregiverInfo = caregiverInfo,
        )

    private var caregiverInfo: CaregiverInfo? = null

    override fun assignCaregiver(caregiverInfo: CaregiverInfo): CaregivingState {
        this.caregiverInfo = caregiverInfo

        return if (caregivingRoundInfo.caregivingRoundNumber == 1) {
            this
        } else {
            RematchingState(
                caregivingRoundInfo,
                caregiverInfo
            )
        }
    }

    override fun start(startDateTime: LocalDateTime): CaregivingState {
        return InProgressState(
            caregivingRoundInfo,
            caregiverInfo ?: throw CaregiverNotAssignedException(caregivingRoundInfo.caregivingRoundId),
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
            CaregivingProgressingStatus.NOT_STARTED,
            CaregivingProgressingStatus.COMPLETED
        )
    }

    override fun stop(stopDateTime: LocalDateTime): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.NOT_STARTED,
            CaregivingProgressingStatus.COMPLETED_RESTARTING
        )
    }

    override fun cancel(reason: CancellationReason, detailReason: String): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.NOT_STARTED,
            CaregivingProgressingStatus.CANCELED_WHILE_REMATCHING
        )
    }

    override fun pend(): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.NOT_STARTED,
            CaregivingProgressingStatus.PENDING_REMATCHING
        )
    }

    override fun completeReconciliation(): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.NOT_STARTED,
            CaregivingProgressingStatus.RECONCILIATION_COMPLETED
        )
    }
}
