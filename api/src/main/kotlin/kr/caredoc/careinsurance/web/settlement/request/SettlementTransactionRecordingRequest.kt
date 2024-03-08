package kr.caredoc.careinsurance.web.settlement.request

import kr.caredoc.careinsurance.transaction.TransactionType
import java.time.LocalDate

data class SettlementTransactionRecordingRequest(
    val transactionType: TransactionType,
    val amount: Int,
    val transactionDate: LocalDate,
    val transactionSubjectId: String,
)
