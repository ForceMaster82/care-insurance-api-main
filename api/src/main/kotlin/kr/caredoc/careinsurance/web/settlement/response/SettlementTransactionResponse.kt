package kr.caredoc.careinsurance.web.settlement.response

import kr.caredoc.careinsurance.transaction.TransactionType
import java.time.LocalDate
import java.time.OffsetDateTime

data class SettlementTransactionResponse(
    val transactionType: TransactionType,
    val amount: Int,
    val transactionDate: LocalDate,
    val enteredDateTime: OffsetDateTime,
    val transactionSubjectId: String,
)
