package kr.caredoc.careinsurance.reconciliation.statistics

data class ReconciliationFinancialSummations(
    val totalBillingAmount: Long,
    val totalSettlementAmount: Long,
    val totalSales: Long,
    val totalDistributedProfit: Long,
)
