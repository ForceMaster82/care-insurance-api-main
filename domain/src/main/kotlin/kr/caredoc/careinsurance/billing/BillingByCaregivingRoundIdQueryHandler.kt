package kr.caredoc.careinsurance.billing

interface BillingByCaregivingRoundIdQueryHandler {
    fun getBilling(query: BillingByCaregivingRoundIdQuery): Billing
}
