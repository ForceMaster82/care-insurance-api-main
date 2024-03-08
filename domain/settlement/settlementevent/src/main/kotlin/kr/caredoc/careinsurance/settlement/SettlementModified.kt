package kr.caredoc.careinsurance.settlement

import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.security.accesscontrol.SystemUser

data class SettlementModified(
    val caregivingRoundId: String,
    val progressingStatus: Modification<SettlementProgressingStatus>,
    val settlementId: String,
    val totalAmount: Int,
    val totalDepositAmount: Int,
    val totalWithdrawalAmount: Int,
) {
    val subject = SystemUser
}
