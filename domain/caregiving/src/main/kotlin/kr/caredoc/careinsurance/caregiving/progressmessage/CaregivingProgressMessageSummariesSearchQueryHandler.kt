package kr.caredoc.careinsurance.caregiving.progressmessage

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CaregivingProgressMessageSummariesSearchQueryHandler {

    fun searchCaregivingProgressMessageSummaries(
        query: CaregivingProgressMessageSummariesSearchQuery,
        pageRequest: Pageable
    ): Page<CaregivingProgressMessageSummary>
}
