package kr.caredoc.careinsurance.caregiving.progressmessage

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CaregivingProgressMessageSummariesByFilterQueryHandler {

    fun getCaregivingProgressMessageSummaries(
        query: CaregivingProgressMessageSummariesByFilterQuery,
        pageRequest: Pageable
    ): Page<CaregivingProgressMessageSummary>
}
