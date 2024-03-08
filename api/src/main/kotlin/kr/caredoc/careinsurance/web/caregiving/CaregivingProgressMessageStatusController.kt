package kr.caredoc.careinsurance.web.caregiving

import kr.caredoc.careinsurance.caregiving.progressmessage.CaregivingProgressMessageSummariesByFilterQuery
import kr.caredoc.careinsurance.caregiving.progressmessage.CaregivingProgressMessageSummariesByFilterQueryHandler
import kr.caredoc.careinsurance.caregiving.progressmessage.CaregivingProgressMessageSummariesSearchQuery
import kr.caredoc.careinsurance.caregiving.progressmessage.CaregivingProgressMessageSummariesSearchQueryHandler
import kr.caredoc.careinsurance.caregiving.progressmessage.CaregivingProgressMessageSummary
import kr.caredoc.careinsurance.caregiving.progressmessage.CaregivingProgressMessageSummaryFilter
import kr.caredoc.careinsurance.message.SendingStatus
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.web.caregiving.response.CaregivingProgressMessageSummaryResponse
import kr.caredoc.careinsurance.web.paging.PagedResponse
import kr.caredoc.careinsurance.web.paging.PagingRequest
import kr.caredoc.careinsurance.web.paging.intoPageable
import kr.caredoc.careinsurance.web.paging.intoPagedResponse
import kr.caredoc.careinsurance.web.search.QueryParser
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@Controller
@RequestMapping("/api/v1/caregiving-progress-message-statuses")
class CaregivingProgressMessageStatusController(
    private val caregivingProgressMessageSummariesByFilterQueryHandler: CaregivingProgressMessageSummariesByFilterQueryHandler,
    private val caregivingProgressMessageSummariesSearchQueryHandler: CaregivingProgressMessageSummariesSearchQueryHandler,
) {
    private val queryParser = QueryParser(
        mapOf(
            "accidentNumber" to CaregivingProgressMessageSummariesSearchQuery.SearchingProperty.ACCIDENT_NUMBER,
            "patientName" to CaregivingProgressMessageSummariesSearchQuery.SearchingProperty.PATIENT_NAME,
        )
    )

    @GetMapping
    fun getCaregivingProgressMessageSummaries(
        pagingRequest: PagingRequest,
        @RequestParam("date", required = true) enteredDate: LocalDate,
        @RequestParam("sending-status", required = false) sendingStatus: SendingStatus?,
        @RequestParam("query", required = false) query: String?,
        subject: Subject,
    ): ResponseEntity<PagedResponse<CaregivingProgressMessageSummaryResponse>> {
        val filter = CaregivingProgressMessageSummaryFilter(
            date = enteredDate,
            sendingStatus = sendingStatus,
        )
        val caregivingProgressMessageSummaries = if (query == null) {
            caregivingProgressMessageSummariesByFilterQueryHandler.getCaregivingProgressMessageSummaries(
                CaregivingProgressMessageSummariesByFilterQuery(
                    filter = filter,
                    subject = subject,
                ),
                pageRequest = pagingRequest.intoPageable(),
            )
        } else {
            caregivingProgressMessageSummariesSearchQueryHandler.searchCaregivingProgressMessageSummaries(
                CaregivingProgressMessageSummariesSearchQuery(
                    filter = filter,
                    searchCondition = queryParser.parse(query),
                    subject = subject,
                ),
                pageRequest = pagingRequest.intoPageable(),
            )
        }

        return ResponseEntity.ok(
            caregivingProgressMessageSummaries.map { it.intoResponse() }.intoPagedResponse()

        )
    }

    fun CaregivingProgressMessageSummary.intoResponse() = CaregivingProgressMessageSummaryResponse(
        receptionId = this.receptionId,
        caregivingRoundId = this.caregivingRoundId,
        sendingStatus = this.sendingStatus,
        sentDate = this.sentDate,
    )
}
