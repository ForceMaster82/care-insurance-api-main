package kr.caredoc.careinsurance.caregiving

interface CaregivingRoundsByReceptionIdQueryHandler {
    fun getReceptionCaregivingRounds(query: CaregivingRoundsByReceptionIdQuery): List<CaregivingRound>
}
