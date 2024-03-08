package kr.caredoc.careinsurance.reconciliation

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ClosedReconciliationsByFilterQueryHandler {
    fun getClosedReconciliations(
        query: ClosedReconciliationsByFilterQuery,
        pageRequest: Pageable,
    ): Page<Reconciliation>

    fun getClosedReconciliationsAsCsv(
        query: ClosedReconciliationsByFilterQuery,
    ): String
}
