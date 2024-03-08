package kr.caredoc.careinsurance.web.billing.response

import java.time.LocalDate

data class DailyCaregivingRoundBillingTransactionStatisticsResponse(
    val receptionId: String,
    val caregivingRoundId: String,
    val date: LocalDate,
    val totalDepositAmount: Int,
    val totalWithdrawalAmount: Int,
)
