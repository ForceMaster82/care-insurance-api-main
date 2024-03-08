package kr.caredoc.careinsurance.reception.caregivingsatisfactionsurvey

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CaregivingSatisfactionSurveyStatusSearchQueryHandler {
    fun searchCaregivingSatisfactionSurveyStatus(
        query: CaregivingSatisfactionSurveyStatusSearchQuery,
        pageRequest: Pageable,
    ): Page<CaregivingSatisfactionSurveyStatus>
}
