package kr.caredoc.careinsurance.caregiving.state

import kr.caredoc.careinsurance.caregiving.CaregiverInfo
import java.time.LocalDateTime

interface CaregivingState {
    val stateData: CaregivingStateData

    fun assignCaregiver(caregiverInfo: CaregiverInfo): CaregivingState

    fun start(startDateTime: LocalDateTime): CaregivingState

    fun editStartDateTime(startDateTime: LocalDateTime): CaregivingState

    fun editEndDateTime(endDateTime: LocalDateTime): CaregivingState

    fun complete(endDateTime: LocalDateTime, reason: FinishingReason): CaregivingState

    fun stop(stopDateTime: LocalDateTime): CaregivingState

    fun cancel(reason: CancellationReason, detailReason: String): CaregivingState

    fun pend(): CaregivingState

    fun completeReconciliation(): CaregivingState
}
