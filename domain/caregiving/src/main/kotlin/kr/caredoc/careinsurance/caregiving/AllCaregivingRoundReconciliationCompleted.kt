package kr.caredoc.careinsurance.caregiving

import kr.caredoc.careinsurance.security.accesscontrol.Subject

data class AllCaregivingRoundReconciliationCompleted(
    val receptionId: String,
    val subject: Subject,
)
