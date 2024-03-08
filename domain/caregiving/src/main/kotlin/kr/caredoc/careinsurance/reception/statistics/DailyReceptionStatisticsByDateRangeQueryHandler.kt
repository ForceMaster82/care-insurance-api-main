package kr.caredoc.careinsurance.reception.statistics

interface DailyReceptionStatisticsByDateRangeQueryHandler {
    fun getDailyReceptionStatistics(query: DailyReceptionStatisticsByDateRangeQuery): List<DailyReceptionStatistics>

    fun getDailyReceptionStatisticsAsCsv(query: DailyReceptionStatisticsByDateRangeQuery): String
}
