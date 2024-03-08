package kr.caredoc.careinsurance.reception

import java.time.LocalDate
import java.time.LocalDateTime

data class ReceptionReceived(
    val receptionId: String,
    val receivedDateTime: LocalDateTime,
    val desiredCaregivingStartDate: LocalDate,
    val urgency: Reception.Urgency,
    val periodType: Reception.PeriodType,
)
