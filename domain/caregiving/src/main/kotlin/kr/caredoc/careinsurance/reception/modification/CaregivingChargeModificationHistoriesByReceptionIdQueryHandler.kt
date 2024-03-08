package kr.caredoc.careinsurance.reception.modification

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CaregivingChargeModificationHistoriesByReceptionIdQueryHandler {
    fun getCaregivingChargeModificationHistories(
        query: CaregivingChargeModificationHistoriesByReceptionIdQuery,
        pageRequest: Pageable
    ): Page<CaregivingChargeModificationHistory>
}
