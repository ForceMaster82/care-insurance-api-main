package kr.caredoc.careinsurance.billing

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BillingByFilterQueryHandler {
    fun getBillings(query: BillingByFilterQuery, pageRequest: Pageable): Page<Billing>
}
