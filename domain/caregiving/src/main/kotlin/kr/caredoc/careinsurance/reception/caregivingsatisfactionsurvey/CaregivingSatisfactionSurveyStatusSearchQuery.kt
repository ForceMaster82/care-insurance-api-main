package kr.caredoc.careinsurance.reception.caregivingsatisfactionsurvey

import kr.caredoc.careinsurance.search.SearchCondition
import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.Subject

data class CaregivingSatisfactionSurveyStatusSearchQuery(
    val filter: CaregivingSatisfactionSurveyStatusFilter,
    val searchCondition: SearchCondition<SearchingProperty>,
    val subject: Subject,
) : Action {
    override fun get(attribute: ActionAttribute) = when (attribute) {
        ActionAttribute.ACTION_TYPE -> setOf(ActionType.READ_ALL)
        else -> setOf()
    }

    enum class SearchingProperty {
        ACCIDENT_NUMBER,
        PATIENT_NAME,
    }
}
