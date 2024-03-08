package kr.caredoc.careinsurance.web.caregiving.response

import java.time.LocalDateTime

data class IllegalCaregivingPeriodEnteredData(
    val targetCaregivingRoundId: String,
    val enteredStartDateTime: LocalDateTime,
)
