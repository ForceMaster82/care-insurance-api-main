package kr.caredoc.careinsurance.user

import kr.caredoc.careinsurance.patch.Patch
import kr.caredoc.careinsurance.patch.Patches
import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.Subject

data class InternalCaregivingManagerEditingCommand(
    val name: Patch<String> = Patches.ofEmpty(),
    val nickname: Patch<String> = Patches.ofEmpty(),
    val phoneNumber: Patch<String> = Patches.ofEmpty(),
    val email: Patch<String> = Patches.ofEmpty(),
    val suspended: Patch<Boolean> = Patches.ofEmpty(),
    val role: Patch<String> = Patches.ofEmpty(),
    val remarks: Patch<String?> = Patches.ofEmpty(),
    val subject: Subject,
) : Action {
    override fun get(attribute: ActionAttribute) = when (attribute) {
        ActionAttribute.ACTION_TYPE -> setOf(ActionType.MODIFY)
        else -> setOf()
    }
}
