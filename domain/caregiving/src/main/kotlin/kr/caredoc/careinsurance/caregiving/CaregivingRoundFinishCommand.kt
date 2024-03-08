package kr.caredoc.careinsurance.caregiving

import kr.caredoc.careinsurance.patch.Patch
import kr.caredoc.careinsurance.patch.Patches
import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import java.time.LocalDateTime

data class CaregivingRoundFinishCommand(
    val caregivingProgressingStatus: Patch<CaregivingProgressingStatus> = Patches.ofEmpty(),
    val endDateTime: LocalDateTime,
    val remarks: String,
    val caregivingRoundClosingReasonType: ClosingReasonType?,
    val subject: Subject,
) : Action {

    override fun get(attribute: ActionAttribute) = when (attribute) {
        ActionAttribute.ACTION_TYPE -> setOf(ActionType.MODIFY)
        else -> setOf()
    }
}
