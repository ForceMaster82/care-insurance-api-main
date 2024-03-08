package kr.caredoc.careinsurance.settlement

import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.transaction.TransactionType
import java.time.LocalDate

data class SettlementTransactionRecordingCommand(
    val transactionType: TransactionType,
    val amount: Int,
    val transactionDate: LocalDate,
    val transactionSubjectId: String,
    val subject: Subject,
) : Action {
    override fun get(attribute: ActionAttribute) = when (attribute) {
        ActionAttribute.ACTION_TYPE -> setOf(ActionType.MODIFY)
        else -> setOf()
    }
}
