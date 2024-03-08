package kr.caredoc.careinsurance.reception.caregivingstartmessage

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CaregivingStartMessageSummarySearchQueryHandler {
    fun searchCaregivingStartMessageSummary(query: CaregivingStartMessageSummarySearchQuery, pageRequest: Pageable): Page<CaregivingStartMessageSummary>
}
