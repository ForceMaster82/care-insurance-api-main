package kr.caredoc.careinsurance.caregiving

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface MonthlyRegionalCaregivingStatisticsByFilterQueryHandler {
    fun getMonthlyRegionalCaregivingStatistics(
        query: MonthlyRegionalCaregivingStatisticsByFilterQuery,
        pageRequest: Pageable
    ): Page<MonthlyRegionalCaregivingStatistics>

    fun getMonthlyRegionalCaregivingStatisticsAsCsv(query: MonthlyRegionalCaregivingStatisticsByFilterQuery): String
}
