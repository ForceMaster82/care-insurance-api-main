package kr.caredoc.careinsurance.settlement

import kr.caredoc.careinsurance.transaction.TransactionType
import java.time.LocalDate
import java.time.LocalDateTime

data class SettlementTransactionRecorded(
    val receptionId: String,
    val caregivingRoundId: String,
    val settlementId: String,
    val transactionDate: LocalDate,
    val transactionType: TransactionType,
    val amount: Int,
    val enteredDateTime: LocalDateTime,
    val order: Int,
    val progressingStatus: SettlementProgressingStatus,
    val totalAmount: Int,
    val totalDepositAmount: Int,
    val totalWithdrawalAmount: Int,
)
