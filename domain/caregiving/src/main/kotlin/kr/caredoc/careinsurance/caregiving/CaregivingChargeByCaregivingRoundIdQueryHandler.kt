package kr.caredoc.careinsurance.caregiving

interface CaregivingChargeByCaregivingRoundIdQueryHandler {
    fun getCaregivingCharge(query: CaregivingChargeByCaregivingRoundIdQuery): CaregivingCharge
}
