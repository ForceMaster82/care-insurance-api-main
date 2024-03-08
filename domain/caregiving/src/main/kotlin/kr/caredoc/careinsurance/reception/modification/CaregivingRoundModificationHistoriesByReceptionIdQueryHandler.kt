package kr.caredoc.careinsurance.reception.modification

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CaregivingRoundModificationHistoriesByReceptionIdQueryHandler {
    fun getCaregivingRoundModificationHistories(
        query: CaregivingRoundModificationHistoriesByReceptionIdQuery,
        pageRequest: Pageable
    ): Page<CaregivingRoundModificationHistory>
}
