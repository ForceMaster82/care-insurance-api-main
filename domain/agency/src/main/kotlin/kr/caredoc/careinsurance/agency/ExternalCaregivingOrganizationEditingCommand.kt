package kr.caredoc.careinsurance.agency

import kr.caredoc.careinsurance.account.AccountInfo
import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.Subject

data class ExternalCaregivingOrganizationEditingCommand(
    val externalCaregivingOrganizationId: String,
    val name: String,
    val externalCaregivingOrganizationType: ExternalCaregivingOrganizationType,
    val address: String,
    val contractName: String,
    val phoneNumber: String,
    val profitAllocationRatio: Float,
    val accountInfo: AccountInfo,
    val subject: Subject,
) : Action {
    override fun get(attribute: ActionAttribute) = when (attribute) {
        ActionAttribute.ACTION_TYPE -> setOf(ActionType.MODIFY)
        else -> setOf()
    }
}
