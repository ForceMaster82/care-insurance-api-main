package kr.caredoc.careinsurance.reconciliation

import kr.caredoc.careinsurance.security.accesscontrol.Subject

data class ReconciliationClosed(
    val reconciliationId: String,
    val caregivingRoundId: String,
    val receptionId: String,
    val issuedType: IssuedType,
    val subject: Subject,
)
