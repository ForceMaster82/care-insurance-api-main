package kr.caredoc.careinsurance.web.billing.request

import kr.caredoc.careinsurance.transaction.TransactionType
import java.time.LocalDate

data class BillingTransactionCreationRequest(
    val transactionType: TransactionType,
    val amount: Int,
    val transactionDate: LocalDate,
    val transactionSubjectId: String,
)
