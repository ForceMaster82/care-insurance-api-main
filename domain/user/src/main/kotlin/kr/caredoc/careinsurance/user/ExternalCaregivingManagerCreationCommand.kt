package kr.caredoc.careinsurance.user

import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.Subject

data class ExternalCaregivingManagerCreationCommand(
    val externalCaregivingOrganizationId: String,
    val email: String,
    val name: String,
    val phoneNumber: String,
    val remarks: String? = null,
    val subject: Subject,
) : Action {
    override fun get(attribute: ActionAttribute) = when (attribute) {
        ActionAttribute.ACTION_TYPE -> setOf(ActionType.CREATE)
        else -> setOf()
    }
}
