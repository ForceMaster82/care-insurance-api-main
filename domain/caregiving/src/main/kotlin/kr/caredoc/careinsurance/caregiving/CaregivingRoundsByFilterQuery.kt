package kr.caredoc.careinsurance.caregiving

import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import kr.caredoc.careinsurance.reception.ReceptionProgressingStatus
import kr.caredoc.careinsurance.search.SearchCondition
import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.settlement.SettlementProgressingStatus
import java.time.LocalDate

data class CaregivingRoundsByFilterQuery(
    val from: LocalDate?,
    val until: LocalDate?,
    val organizationId: String?,
    val expectedCaregivingStartDate: LocalDate?,
    val receptionProgressingStatuses: Set<ReceptionProgressingStatus>,
    val caregivingProgressingStatuses: Set<CaregivingProgressingStatus>,
    val settlementProgressingStatuses: Set<SettlementProgressingStatus>,
    val billingProgressingStatuses: Set<BillingProgressingStatus>,
    val searchCondition: SearchCondition<SearchingProperty>?,
    val subject: Subject,
) : Action {

    override fun get(attribute: ActionAttribute) = when (attribute) {
        ActionAttribute.ACTION_TYPE -> setOf(ActionType.READ_ALL)
        ActionAttribute.SCOPED_ORGANIZATION_ID -> this.organizationId?.let { setOf(it) } ?: setOf()
        else -> setOf()
    }

    enum class SearchingProperty {
        ACCIDENT_NUMBER,
        INSURANCE_NUMBER,
        PATIENT_NAME,
        CAREGIVER_NAME,
    }
}
