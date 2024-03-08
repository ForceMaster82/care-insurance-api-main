package kr.caredoc.careinsurance.caregiving

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CaregivingRoundsByFilterQueryHandler {
    fun getCaregivingRounds(query: CaregivingRoundsByFilterQuery, pageRequest: Pageable): Page<CaregivingRound>

    fun getCaregivingRoundsAsCsv(query: CaregivingRoundsByFilterQuery): String
}
