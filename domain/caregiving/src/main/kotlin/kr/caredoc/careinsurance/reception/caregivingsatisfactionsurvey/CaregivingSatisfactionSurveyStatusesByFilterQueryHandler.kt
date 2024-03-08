package kr.caredoc.careinsurance.reception.caregivingsatisfactionsurvey

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CaregivingSatisfactionSurveyStatusesByFilterQueryHandler {
    fun getCaregivingSatisfactionSurveyStatuses(
        query: CaregivingSatisfactionSurveyStatusesByFilterQuery,
        pageRequest: Pageable,
    ): Page<CaregivingSatisfactionSurveyStatus>
}
