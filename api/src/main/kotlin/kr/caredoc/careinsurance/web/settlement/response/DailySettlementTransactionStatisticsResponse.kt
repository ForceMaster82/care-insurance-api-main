package kr.caredoc.careinsurance.web.settlement.response

import java.time.LocalDate

data class DailySettlementTransactionStatisticsResponse(
    val date: LocalDate,
    val totalDepositAmount: Int,
    val totalWithdrawalAmount: Int,
)
