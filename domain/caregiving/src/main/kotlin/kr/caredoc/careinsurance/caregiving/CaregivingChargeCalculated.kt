package kr.caredoc.careinsurance.caregiving

import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.security.accesscontrol.SystemUser
import java.time.LocalDate
import java.time.LocalDateTime

data class CaregivingChargeCalculated(
    val receptionId: String,
    val caregivingRoundId: String,
    val roundNumber: Int,
    val dailyCaregivingCharge: Int,
    val basicAmount: Int,
    val additionalAmount: Int,
    val totalAmount: Int,
    val expectedSettlementDate: LocalDate,
    val isCancelAfterArrived: Boolean,
) {
    val calculatedDateTime: LocalDateTime = Clock.now()
    val subject = SystemUser
}
