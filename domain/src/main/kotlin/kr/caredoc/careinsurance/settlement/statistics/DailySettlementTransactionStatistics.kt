package kr.caredoc.careinsurance.settlement.statistics

import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Entity
import kr.caredoc.careinsurance.AggregateRoot
import kr.caredoc.careinsurance.settlement.SettlementTransactionRecorded
import kr.caredoc.careinsurance.transaction.TransactionType
import java.time.LocalDate

@Entity
class DailySettlementTransactionStatistics(
    id: String,
    @Access(AccessType.FIELD)
    val date: LocalDate,
) : AggregateRoot(id) {
    var totalDepositAmount: Int = 0
        protected set
    var totalWithdrawalAmount: Int = 0
        protected set

    fun handleSettlementTransactionRecorded(event: SettlementTransactionRecorded) {
        when (event.transactionType) {
            TransactionType.DEPOSIT -> totalDepositAmount += event.amount
            TransactionType.WITHDRAWAL -> totalWithdrawalAmount += event.amount
        }
    }
}
