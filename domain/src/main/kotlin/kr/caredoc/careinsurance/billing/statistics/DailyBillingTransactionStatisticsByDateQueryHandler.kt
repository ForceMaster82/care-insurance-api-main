package kr.caredoc.careinsurance.billing.statistics

interface DailyBillingTransactionStatisticsByDateQueryHandler {
    fun getDailyBillingTransactionStatistics(query: DailyBillingTransactionStatisticsByDateQuery): DailyBillingTransactionStatistics?
}
