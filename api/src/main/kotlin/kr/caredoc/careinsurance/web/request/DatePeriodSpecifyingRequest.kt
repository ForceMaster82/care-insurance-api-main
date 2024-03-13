package kr.caredoc.careinsurance.web.request

import java.time.LocalDate

data class DatePeriodSpecifyingRequest(
    val from: LocalDate?,
    val until: LocalDate?,
)
