package kr.caredoc.careinsurance.reconciliation

interface ReconciliationByIdQueryHandler {
    fun getReconciliation(query: ReconciliationByIdQuery): Reconciliation
}
