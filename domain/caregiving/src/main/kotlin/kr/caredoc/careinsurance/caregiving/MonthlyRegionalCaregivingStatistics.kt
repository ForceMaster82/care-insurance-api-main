package kr.caredoc.careinsurance.caregiving

data class MonthlyRegionalCaregivingStatistics(
    val state: String,
    val city: String?,
    val receptionCount: Long,
)
