package kr.caredoc.careinsurance.reception

import kr.caredoc.careinsurance.search.SearchCondition
import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import java.time.LocalDate

data class ReceptionsByFilterQuery(
    val from: LocalDate,
    val until: LocalDate,
    val urgency: Reception.Urgency?,
    val periodType: Reception.PeriodType?,
    val caregivingManagerAssigned: Boolean?,
    val organizationType: OrganizationType?,
    val progressingStatuses: Set<ReceptionProgressingStatus>,
    val searchCondition: SearchCondition<SearchingProperty>?,
    val subject: Subject,
) : Action {
    override fun get(attribute: ActionAttribute) = when (attribute) {
        ActionAttribute.ACTION_TYPE -> setOf(ActionType.READ_ALL)
        else -> setOf()
    }

    enum class SearchingProperty {
        INSURANCE_NUMBER,
        PATIENT_NAME,
        CAREGIVING_MANAGER_NAME,
        PATIENT_PHONE_NUMBER,
        ACCIDENT_NUMBER,
        CAREGIVER_NAME,
    }
}
