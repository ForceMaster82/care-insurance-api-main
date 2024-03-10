package kr.caredoc.careinsurance.settlement.statistics

import kr.caredoc.careinsurance.search.SearchCondition
import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import java.time.LocalDate

data class DailySettlementTransactionStatisticsByDateQuery(
    val date: LocalDate,
    val subject: Subject,
    val searchCondition: SearchCondition<SearchingProperty>?,
) : Action {
    override fun get(attribute: ActionAttribute) = when (attribute) {
        ActionAttribute.ACTION_TYPE -> setOf(ActionType.READ_ONE)
        else -> setOf()
    }

    enum class SearchingProperty {
        PATIENT_NAME,
    }
}
