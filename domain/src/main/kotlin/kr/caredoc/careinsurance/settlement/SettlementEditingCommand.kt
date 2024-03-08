package kr.caredoc.careinsurance.settlement

import kr.caredoc.careinsurance.patch.Patch
import kr.caredoc.careinsurance.patch.Patches
import kr.caredoc.careinsurance.security.accesscontrol.Subject

data class SettlementEditingCommand(
    val progressingStatus: Patch<SettlementProgressingStatus> = Patches.ofEmpty(),
    val settlementManagerId: Patch<String> = Patches.ofEmpty(),
    val subject: Subject,
)
