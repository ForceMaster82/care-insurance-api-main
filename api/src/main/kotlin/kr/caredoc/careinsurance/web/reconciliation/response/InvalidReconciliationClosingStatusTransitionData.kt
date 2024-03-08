package kr.caredoc.careinsurance.web.reconciliation.response

import kr.caredoc.careinsurance.reconciliation.ClosingStatus

class InvalidReconciliationClosingStatusTransitionData(
    val currentReconciliationClosingStatus: ClosingStatus,
    val enteredReconciliationClosingStatus: ClosingStatus,
)
