package kr.caredoc.careinsurance.web.billing.response

import java.time.LocalDate

data class DailyBillingTransactionStatisticsResponse(
    val date: LocalDate,
    val totalDepositAmount: Int,
    val totalWithdrawalAmount: Int,
)
