package kr.caredoc.careinsurance.reconciliation

import kr.caredoc.careinsurance.security.accesscontrol.Subject

data class ReconciliationByIdQuery(
    val reconciliationId: String,
    val subject: Subject,
)
