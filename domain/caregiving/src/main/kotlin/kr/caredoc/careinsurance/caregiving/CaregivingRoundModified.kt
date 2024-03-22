package kr.caredoc.careinsurance.caregiving

import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.settlement.SettlementProgressingStatus
import java.time.LocalDate
import java.time.LocalDateTime

data class CaregivingRoundModified(
    val caregivingRoundId: String,
    val caregivingRoundNumber: Int,
    val receptionId: String,
    val settlementProgressingStatus: Modification<SettlementProgressingStatus>,
    val billingProgressingStatus: Modification<BillingProgressingStatus>,
    val caregivingProgressingStatus: Modification<CaregivingProgressingStatus>,
    val caregiverInfo: Modification<CaregiverInfo?>,
    val startDateTime: Modification<LocalDateTime?>,
    val endDateTime: Modification<LocalDateTime?>,
    val remarks: Modification<String>,
    val expectedSettlementDate: Modification<LocalDate?>,
    val cause: Cause,
    val editingSubject: Subject,
) {
    enum class Cause {
        DIRECT_EDIT,
        ETC,
    }

    val modifiedDateTime: LocalDateTime = Clock.now()
}
