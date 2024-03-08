package kr.caredoc.careinsurance.billing

import kr.caredoc.careinsurance.security.accesscontrol.SystemUser
import java.time.LocalDateTime

data class BillingGenerated(
    val caregivingRoundId: String,
    val progressingStatus: BillingProgressingStatus,
    val billingId: String,
    val billingAmount: Int,
    val issuedDateTime: LocalDateTime,
) {
    val subject = SystemUser
}
