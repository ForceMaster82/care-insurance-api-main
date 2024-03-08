package kr.caredoc.careinsurance.settlement

import kr.caredoc.careinsurance.search.SearchCondition
import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.Subject

data class SettlementsSearchQuery(
    val progressingStatus: SettlementProgressingStatus,
    val expectedSettlementDate: DateRange? = null,
    val transactionDate: DateRange? = null,
    val searchCondition: SearchCondition<SearchingProperty>?,
    val sorting: Sorting,
    val subject: Subject,
) : Action {
    override fun get(attribute: ActionAttribute) = when (attribute) {
        ActionAttribute.ACTION_TYPE -> setOf(ActionType.READ_ALL)
        else -> setOf()
    }

    enum class SearchingProperty {
        ACCIDENT_NUMBER,
        PATIENT_NAME,
        ORGANIZATION_NAME,
    }

    enum class Sorting {
        EXPECTED_SETTLEMENT_DATE_DESC_ACCIDENT_NUMBER_DESC,
        LAST_TRANSACTION_DATE_TIME_DESC,
    }
}
