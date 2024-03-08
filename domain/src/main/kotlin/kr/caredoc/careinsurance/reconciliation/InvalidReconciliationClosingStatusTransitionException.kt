package kr.caredoc.careinsurance.reconciliation

class InvalidReconciliationClosingStatusTransitionException(
    val currentReconciliationClosingStatus: ClosingStatus,
    val enteredReconciliationClosingStatus: ClosingStatus,
) : RuntimeException("Reconciliation(closingStatus: $currentReconciliationClosingStatus)를 $enteredReconciliationClosingStatus 상태로 진행할 수 없습니다.")
