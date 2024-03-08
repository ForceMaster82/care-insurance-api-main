package kr.caredoc.careinsurance.user

import kr.caredoc.careinsurance.search.SearchCondition
import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.Subject

data class ExternalCaregivingManagersByFilterQuery(
    val externalCaregivingOrganizationId: String?,
    val searchQuery: SearchCondition<SearchingProperty>?,
    val subject: Subject,
) : Action {

    override fun get(attribute: ActionAttribute) = when (attribute) {
        ActionAttribute.ACTION_TYPE -> setOf(ActionType.READ_ALL)
        ActionAttribute.SCOPED_ORGANIZATION_ID -> this.externalCaregivingOrganizationId?.let { setOf(it) } ?: setOf()
        else -> setOf()
    }

    enum class SearchingProperty {
        NAME,
        EMAIL,
    }
}
