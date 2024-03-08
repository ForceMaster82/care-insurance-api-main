package kr.caredoc.careinsurance.billing

import kr.caredoc.careinsurance.security.accesscontrol.SystemUser
import kr.caredoc.careinsurance.transaction.TransactionType
import java.time.LocalDate
import java.time.LocalDateTime

data class BillingTransactionRecorded(
    val receptionId: String,
    val caregivingRoundId: String,
    val billingId: String,
    val progressingStatus: BillingProgressingStatus,
    val totalAmount: Int,
    val totalDepositAmount: Int,
    val totalWithdrawalAmount: Int,
    val transactionDate: LocalDate,
    val transactionType: TransactionType,
    val amount: Int,
    val enteredDateTime: LocalDateTime,
) {
    val subject = SystemUser
}
