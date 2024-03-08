package kr.caredoc.careinsurance.settlement

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface TransactionsBySettlementIdQueryHandler {
    fun getTransactions(query: TransactionsBySettlementIdQuery, pageable: Pageable): Page<Settlement.TransactionRecord>
}
