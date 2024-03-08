package kr.caredoc.careinsurance.agency

import kr.caredoc.careinsurance.search.SearchCondition
import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.Subject

data class ExternalCaregivingOrganizationsByFilterQuery(
    val searchCondition: SearchCondition<SearchingProperty>?,
    val organizationType: ExternalCaregivingOrganizationType?,
    val subject: Subject,
) : Action {
    override fun get(attribute: ActionAttribute) = when (attribute) {
        ActionAttribute.ACTION_TYPE -> setOf(ActionType.READ_ALL)
        else -> setOf()
    }

    enum class SearchingProperty {
        EXTERNAL_CAREGIVING_ORGANIZATION_NAME,
    }
}
