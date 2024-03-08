package kr.caredoc.careinsurance.web.settlement.response

import java.time.LocalDate

data class DailyCaregivingRoundSettlementTransactionStatisticsResponse(
    val receptionId: String,
    val caregivingRoundId: String,
    val date: LocalDate,
    val totalDepositAmount: Int,
    val totalWithdrawalAmount: Int,
)
