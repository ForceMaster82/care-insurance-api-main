package kr.caredoc.careinsurance.settlement

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface SettlementsSearchQueryHandler {
    fun getSettlements(query: SettlementsSearchQuery, pageRequest: Pageable): Page<Settlement>
    fun getSettlementsAsCsv(query: SettlementsSearchQuery): String

    fun getSettlementsCalculate(query: SettlementsSearchQuery): List<Settlement>
}
