package kr.caredoc.careinsurance.settlement

import java.time.LocalDate

data class DateRange(
    val from: LocalDate,
    val until: LocalDate,
)
