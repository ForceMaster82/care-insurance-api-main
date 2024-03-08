package kr.caredoc.careinsurance.web.settlement.response

import kr.caredoc.careinsurance.settlement.SettlementProgressingStatus
import java.time.LocalDate
import java.time.OffsetDateTime

data class SettlementResponse(
    val id: String,
    val receptionId: String,
    val caregivingRoundId: String,
    val accidentNumber: String,
    val caregivingRoundNumber: Int,
    val progressingStatus: SettlementProgressingStatus,
    val patientName: String,
    val dailyCaregivingCharge: Int,
    val basicAmount: Int,
    val additionalAmount: Int,
    val totalAmount: Int,
    val lastCalculationDateTime: OffsetDateTime,
    val expectedSettlementDate: LocalDate,
    val totalDepositAmount: Int,
    val totalWithdrawalAmount: Int,
    val lastTransactionDateTime: OffsetDateTime?,
    val settlementCompletionDateTime: OffsetDateTime?,
    val settlementManagerId: String?,
)
