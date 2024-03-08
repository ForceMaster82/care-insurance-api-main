package kr.caredoc.careinsurance.billing

interface BillingByIdQueryHandler {
    fun getBilling(query: BillingByIdQuery): Billing
}
