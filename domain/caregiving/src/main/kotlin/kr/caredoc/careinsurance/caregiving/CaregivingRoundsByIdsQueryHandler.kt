package kr.caredoc.careinsurance.caregiving

interface CaregivingRoundsByIdsQueryHandler {
    fun getCaregivingRounds(query: CaregivingRoundsByIdsQuery): List<CaregivingRound>
}
