package kr.caredoc.careinsurance.reconciliation

class ReferenceReconciliationNotExistsException(
    val referenceReconciliationId: String,
    override val cause: Throwable? = null,
) : RuntimeException("참조하고자 하는 Reconciliation($referenceReconciliationId)가 존재하지 않습니다.")
