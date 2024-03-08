package kr.caredoc.careinsurance.caregiving

interface CaregivingRoundByIdQueryHandler {
    fun getCaregivingRound(query: CaregivingRoundByIdQuery): CaregivingRound
}
