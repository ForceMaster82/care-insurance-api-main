package kr.caredoc.careinsurance.web.reconciliation.response

data class MonthlyReconciliationStatisticsResponse(
    val year: Int,
    val month: Int,
    val receptionCount: Int,
    val caregiverCount: Int,
    val totalCaregivingPeriod: Int,
    val totalBillingAmount: Int,
    val totalSettlementAmount: Int,
    val totalProfit: Int,
    val totalDistributedProfit: Int,
)
