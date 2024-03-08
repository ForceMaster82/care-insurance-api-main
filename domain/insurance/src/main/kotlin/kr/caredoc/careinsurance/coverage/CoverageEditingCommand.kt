package kr.caredoc.careinsurance.coverage

import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.Subject

data class CoverageEditingCommand(
    val coverageId: String,
    val name: String,
    val targetSubscriptionYear: Int,
    val annualCoveredCaregivingCharges: List<Coverage.AnnualCoveredCaregivingCharge>,
    val subject: Subject,
) : Action {
    override fun get(attribute: ActionAttribute) = when (attribute) {
        ActionAttribute.ACTION_TYPE -> setOf(ActionType.MODIFY)
        else -> setOf()
    }
}
