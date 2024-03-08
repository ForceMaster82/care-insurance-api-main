package kr.caredoc.careinsurance.web.reconciliation.response

import kr.caredoc.careinsurance.reconciliation.ClosingStatus

data class EditingReconciliationRequest(
    val id: String,
    val closingStatus: ClosingStatus,
)
