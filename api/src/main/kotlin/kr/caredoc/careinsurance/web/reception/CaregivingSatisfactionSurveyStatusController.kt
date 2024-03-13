package kr.caredoc.careinsurance.web.reception

import kr.caredoc.careinsurance.reception.caregivingsatisfactionsurvey.CaregivingSatisfactionSurveyStatus
import kr.caredoc.careinsurance.reception.caregivingsatisfactionsurvey.CaregivingSatisfactionSurveyStatusFilter
import kr.caredoc.careinsurance.reception.caregivingsatisfactionsurvey.CaregivingSatisfactionSurveyStatusSearchQuery
import kr.caredoc.careinsurance.reception.caregivingsatisfactionsurvey.CaregivingSatisfactionSurveyStatusSearchQueryHandler
import kr.caredoc.careinsurance.reception.caregivingsatisfactionsurvey.CaregivingSatisfactionSurveyStatusesByFilterQuery
import kr.caredoc.careinsurance.reception.caregivingsatisfactionsurvey.CaregivingSatisfactionSurveyStatusesByFilterQueryHandler
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.web.paging.PagedResponse
import kr.caredoc.careinsurance.web.paging.PagingRequest
import kr.caredoc.careinsurance.web.paging.intoPageable
import kr.caredoc.careinsurance.web.paging.intoPagedResponse
import kr.caredoc.careinsurance.web.reception.response.CaregivingSatisfactionSurveyStatusResponse
import kr.caredoc.careinsurance.web.search.QueryParser
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/caregiving-satisfaction-survey-statuses")
class CaregivingSatisfactionSurveyStatusController(
    private val caregivingSatisfactionSurveyStatusesByFilterQueryHandler: CaregivingSatisfactionSurveyStatusesByFilterQueryHandler,
    private val caregivingSatisfactionSurveyStatusSearchQueryHandler: CaregivingSatisfactionSurveyStatusSearchQueryHandler,
) {
    val queryParser = QueryParser(
        mapOf(
            "patientName" to CaregivingSatisfactionSurveyStatusSearchQuery.SearchingProperty.PATIENT_NAME,
            "accidentNumber" to CaregivingSatisfactionSurveyStatusSearchQuery.SearchingProperty.ACCIDENT_NUMBER,
            "caregiverName" to CaregivingSatisfactionSurveyStatusSearchQuery.SearchingProperty.CAREGIVER_NAME,
        )
    )

    @GetMapping
    fun getCaregivingSatisfactionSurveyStatuses(
        pagingRequest: PagingRequest,
        @RequestParam("date") date: LocalDate,
        @RequestParam("query", required = false) query: String?,
        subject: Subject,
    ): ResponseEntity<PagedResponse<CaregivingSatisfactionSurveyStatusResponse>> {
        return ResponseEntity.ok(
            if (query == null) {
                caregivingSatisfactionSurveyStatusesByFilterQueryHandler.getCaregivingSatisfactionSurveyStatuses(
                    CaregivingSatisfactionSurveyStatusesByFilterQuery(
                        filter = CaregivingSatisfactionSurveyStatusFilter(
                            date = date,
                        ),
                        subject = subject,
                    ),
                    pagingRequest.intoPageable(),
                )
            } else {
                caregivingSatisfactionSurveyStatusSearchQueryHandler.searchCaregivingSatisfactionSurveyStatus(
                    CaregivingSatisfactionSurveyStatusSearchQuery(
                        filter = CaregivingSatisfactionSurveyStatusFilter(
                            date = date,
                        ),
                        searchCondition = queryParser.parse(query),
                        subject = subject,
                    ),
                    pagingRequest.intoPageable(),
                )
            }.map { it.intoResponse() }.intoPagedResponse()
        )
    }

    private fun CaregivingSatisfactionSurveyStatus.intoResponse() = CaregivingSatisfactionSurveyStatusResponse(
        receptionId = receptionId,
        lastCaregivingRoundId = caregivingRoundId,
        reservationStatus = reservationStatus,
    )
}
