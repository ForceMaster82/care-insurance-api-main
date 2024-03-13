package kr.caredoc.careinsurance.web.reception

import kr.caredoc.careinsurance.message.SendingStatus
import kr.caredoc.careinsurance.reception.caregivingstartmessage.CaregivingStartMessageSummariesByFilterQuery
import kr.caredoc.careinsurance.reception.caregivingstartmessage.CaregivingStartMessageSummariesByFilterQueryHandler
import kr.caredoc.careinsurance.reception.caregivingstartmessage.CaregivingStartMessageSummary
import kr.caredoc.careinsurance.reception.caregivingstartmessage.CaregivingStartMessageSummaryFilter
import kr.caredoc.careinsurance.reception.caregivingstartmessage.CaregivingStartMessageSummarySearchQuery
import kr.caredoc.careinsurance.reception.caregivingstartmessage.CaregivingStartMessageSummarySearchQueryHandler
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.web.paging.PagedResponse
import kr.caredoc.careinsurance.web.paging.PagingRequest
import kr.caredoc.careinsurance.web.paging.intoPageable
import kr.caredoc.careinsurance.web.paging.intoPagedResponse
import kr.caredoc.careinsurance.web.reception.response.CaregivingStartMessageResponse
import kr.caredoc.careinsurance.web.search.QueryParser
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/caregiving-start-message-statuses")
class CaregivingStartMessageStatusController(
    private val caregivingStartMessageSummariesByFilterQueryHandler: CaregivingStartMessageSummariesByFilterQueryHandler,
    private val caregivingStartMessageSummarySearchQueryHandler: CaregivingStartMessageSummarySearchQueryHandler,
) {
    private val queryParser = QueryParser(
        mapOf(
            "accidentNumber" to CaregivingStartMessageSummarySearchQuery.SearchingProperty.ACCIDENT_NUMBER,
            "patientName" to CaregivingStartMessageSummarySearchQuery.SearchingProperty.PATIENT_NAME,
            "caregiverName" to CaregivingStartMessageSummarySearchQuery.SearchingProperty.CAREGIVER_NAME,
        )
    )

    @GetMapping
    fun getCaregivingStartMessageStatuses(
        @RequestParam("date") date: LocalDate,
        @RequestParam("sending-status", required = false) sendingStatus: SendingStatus?,
        @RequestParam("query") query: String?,
        pagingRequest: PagingRequest,
        subject: Subject,
    ): ResponseEntity<PagedResponse<CaregivingStartMessageResponse>> {
        val filter = CaregivingStartMessageSummaryFilter(
            date = date,
            sendingStatus = sendingStatus,
        )
        val caregivingStartMessageSummaries = if (query == null) {
            caregivingStartMessageSummariesByFilterQueryHandler.getCaregivingStartMessageSummaries(
                CaregivingStartMessageSummariesByFilterQuery(
                    filter = filter,
                    subject = subject,
                ),
                pageRequest = pagingRequest.intoPageable(),
            )
        } else {
            caregivingStartMessageSummarySearchQueryHandler.searchCaregivingStartMessageSummary(
                CaregivingStartMessageSummarySearchQuery(
                    filter = filter,
                    searchCondition = queryParser.parse(query),
                    subject = subject,
                ),
                pageRequest = pagingRequest.intoPageable(),
            )
        }

        return ResponseEntity.ok(
            caregivingStartMessageSummaries.map { it.intoResponse() }.intoPagedResponse()
        )
    }

    fun CaregivingStartMessageSummary.intoResponse() = CaregivingStartMessageResponse(
        receptionId = receptionId,
        firstCaregivingRoundId = firstCaregivingRoundId,
        sendingStatus = sendingStatus,
        sentDate = sentDate,
    )
}
