package kr.caredoc.careinsurance.reconciliation

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface OpenReconciliationsByFilterQueryHandler {
    fun getOpenReconciliations(query: OpenReconciliationsByFilterQuery, pageRequest: Pageable): Page<Reconciliation>

    fun getOpenReconciliationsAsCsv(query: OpenReconciliationsByFilterQuery): String
}
