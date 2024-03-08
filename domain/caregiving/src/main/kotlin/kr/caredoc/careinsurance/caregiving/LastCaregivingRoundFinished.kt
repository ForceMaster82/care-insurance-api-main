package kr.caredoc.careinsurance.caregiving

import java.time.LocalDateTime

data class LastCaregivingRoundFinished(
    val receptionId: String,
    val lastCaregivingRoundId: String,
    val endDateTime: LocalDateTime,
)
