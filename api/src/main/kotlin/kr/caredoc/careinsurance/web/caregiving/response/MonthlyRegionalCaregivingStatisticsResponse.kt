package kr.caredoc.careinsurance.web.caregiving.response

data class MonthlyRegionalCaregivingStatisticsResponse(
    val year: Int,
    val month: Int,
    val state: String,
    val city: String?,
    val receptionCount: Long,
)
