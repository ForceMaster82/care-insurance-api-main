package kr.caredoc.careinsurance.billing

import kr.caredoc.careinsurance.search.SearchCondition
import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import java.time.LocalDate

class BillingByFilterQuery(
    val progressingStatus: Set<BillingProgressingStatus>,
    val usedPeriodFrom: LocalDate? = null,
    val usedPeriodUntil: LocalDate? = null,
    val billingDateFrom: LocalDate? = null,
    val billingDateUntil: LocalDate? = null,
    val transactionDateFrom: LocalDate? = null,
    val transactionDateUntil: LocalDate? = null,
    val searchQuery: SearchCondition<SearchingProperty>?,
    val sorting: Sorting?,
    val subject: Subject,
) : Action {
    override fun get(attribute: ActionAttribute) = when (attribute) {
        ActionAttribute.ACTION_TYPE -> setOf(ActionType.READ_ALL)
        else -> setOf()
    }

    enum class SearchingProperty {
        PATIENT_NAME,
        ACCIDENT_NUMBER,
        CAREGIVER_NAME,
    }

    enum class Sorting {
        ID_DESC,
        BILLING_DATE_ASC,
        TRANSACTION_DATE_DESC,
    }
}
