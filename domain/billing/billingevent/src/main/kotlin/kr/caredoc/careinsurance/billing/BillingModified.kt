package kr.caredoc.careinsurance.billing

import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.security.accesscontrol.SystemUser
import java.time.LocalDateTime

data class BillingModified(
    val receptionId: String,
    val caregivingRoundId: String,
    val progressingStatus: Modification<BillingProgressingStatus>,
    val totalAmount: Modification<Int>,
    val totalDepositAmount: Modification<Int>,
    val totalWithdrawalAmount: Modification<Int>,
    val billingId: String,
    val modifiedDateTime: LocalDateTime,
) {
    val subject = SystemUser
}
