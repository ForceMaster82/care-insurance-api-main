package kr.caredoc.careinsurance.reconciliation.statistics

data class MonthlyReconciliationStatistics(
    val year: Int,
    val month: Int,
    val receptionCount: Int,
    val caregiverCount: Int,
    val totalCaregivingPeriod: Int,
    val totalBillingAmount: Int,
    val totalSettlementAmount: Int,
    val totalSales: Int,
    val totalDistributedProfit: Int,
)
