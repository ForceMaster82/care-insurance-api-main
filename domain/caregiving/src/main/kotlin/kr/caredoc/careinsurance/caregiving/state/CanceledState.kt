package kr.caredoc.careinsurance.caregiving.state

import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.caregiving.CaregiverInfo
import kr.caredoc.careinsurance.caregiving.CaregivingNotFinishedException
import kr.caredoc.careinsurance.caregiving.CaregivingProgressingStatus
import kr.caredoc.careinsurance.caregiving.exception.CaregivingNotStartedException
import kr.caredoc.careinsurance.caregiving.exception.InvalidCaregivingProgressingStatusTransitionException
import java.time.LocalDateTime

class CanceledState private constructor(
    private val caregivingRoundInfo: CaregivingRoundInfo,
    private val caregiverInfo: CaregiverInfo,
    private val reason: CancellationReason,
    private val detailReason: String,
    private val canceledDateTime: LocalDateTime
) : CaregivingState {
    constructor(
        caregivingRoundInfo: CaregivingRoundInfo,
        caregiverInfo: CaregiverInfo,
        reason: CancellationReason,
        detailReason: String
    ) : this(
        caregivingRoundInfo,
        caregiverInfo,
        reason,
        detailReason,
        Clock.now()
    )

    override val stateData: CaregivingStateData
        get() = CaregivingStateData(
            progressingStatus = CaregivingProgressingStatus.CANCELED_WHILE_REMATCHING,
            caregiverInfo = caregiverInfo,
            closingReasonType = reason.intoClosingReasonType(),
            detailClosingReason = detailReason,
            canceledDateTime = canceledDateTime
        )

    override fun assignCaregiver(caregiverInfo: CaregiverInfo): CaregivingState {
        return CanceledState(
            caregivingRoundInfo = caregivingRoundInfo,
            caregiverInfo = caregiverInfo,
            reason = reason,
            detailReason = detailReason,
            canceledDateTime = canceledDateTime
        )
    }

    override fun start(startDateTime: LocalDateTime): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.CANCELED_WHILE_REMATCHING,
            CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
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
            CaregivingProgressingStatus.CANCELED_WHILE_REMATCHING,
            CaregivingProgressingStatus.COMPLETED
        )
    }

    override fun stop(stopDateTime: LocalDateTime): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.CANCELED_WHILE_REMATCHING,
            CaregivingProgressingStatus.PENDING_REMATCHING
        )
    }

    override fun cancel(reason: CancellationReason, detailReason: String): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.CANCELED_WHILE_REMATCHING,
            CaregivingProgressingStatus.CANCELED_WHILE_REMATCHING
        )
    }

    override fun pend(): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.CANCELED_WHILE_REMATCHING,
            CaregivingProgressingStatus.PENDING_REMATCHING
        )
    }

    override fun completeReconciliation(): CaregivingState {
        throw InvalidCaregivingProgressingStatusTransitionException(
            CaregivingProgressingStatus.CANCELED_WHILE_REMATCHING,
            CaregivingProgressingStatus.RECONCILIATION_COMPLETED
        )
    }
}
