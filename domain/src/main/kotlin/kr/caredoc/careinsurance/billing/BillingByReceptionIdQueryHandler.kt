package kr.caredoc.careinsurance.billing

interface BillingByReceptionIdQueryHandler {
    fun getBillingReception(query: BillingByReceptionIdQuery): List<Billing>
}
