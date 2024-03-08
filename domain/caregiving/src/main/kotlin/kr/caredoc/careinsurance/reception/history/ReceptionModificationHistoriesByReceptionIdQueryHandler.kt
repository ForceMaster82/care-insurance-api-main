package kr.caredoc.careinsurance.reception.history

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ReceptionModificationHistoriesByReceptionIdQueryHandler {

    fun getReceptionModificationHistories(
        query: ReceptionModificationHistoriesByReceptionIdQuery,
        pageRequest: Pageable
    ): Page<ReceptionModificationHistory>
}
