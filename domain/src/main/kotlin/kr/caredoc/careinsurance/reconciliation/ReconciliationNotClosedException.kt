package kr.caredoc.careinsurance.reconciliation

class ReconciliationNotClosedException(reconciliationId: String) :
    RuntimeException("Reconciliation($reconciliationId)는 아직 마감되지 않았습니다.")
