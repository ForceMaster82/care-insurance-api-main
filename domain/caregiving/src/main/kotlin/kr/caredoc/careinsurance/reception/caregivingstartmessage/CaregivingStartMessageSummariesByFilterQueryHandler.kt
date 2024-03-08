package kr.caredoc.careinsurance.reception.caregivingstartmessage

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CaregivingStartMessageSummariesByFilterQueryHandler {
    fun getCaregivingStartMessageSummaries(
        query: CaregivingStartMessageSummariesByFilterQuery,
        pageRequest: Pageable,
    ): Page<CaregivingStartMessageSummary>
}
