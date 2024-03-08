package kr.caredoc.careinsurance.billing.statistics

import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Entity
import kr.caredoc.careinsurance.AggregateRoot
import kr.caredoc.careinsurance.billing.BillingTransactionRecorded
import kr.caredoc.careinsurance.transaction.TransactionType
import java.time.LocalDate

@Entity
class DailyBillingTransactionStatistics(
    id: String,
    @Access(AccessType.FIELD)
    val date: LocalDate,
) : AggregateRoot(id) {
    var totalDepositAmount: Int = 0
        protected set
    var totalWithdrawalAmount: Int = 0
        protected set

    fun handleBillingTransactionRecorded(event: BillingTransactionRecorded) {
        when (event.transactionType) {
            TransactionType.DEPOSIT -> totalDepositAmount += event.amount
            TransactionType.WITHDRAWAL -> totalWithdrawalAmount += event.amount
        }
    }
}
