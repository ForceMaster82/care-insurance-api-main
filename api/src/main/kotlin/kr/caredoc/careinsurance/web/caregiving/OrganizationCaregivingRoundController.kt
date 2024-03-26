package kr.caredoc.careinsurance.web.caregiving

import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import kr.caredoc.careinsurance.caregiving.CaregivingProgressingStatus
import kr.caredoc.careinsurance.caregiving.CaregivingRoundsByFilterQuery
import kr.caredoc.careinsurance.caregiving.CaregivingRoundsByFilterQueryHandler
import kr.caredoc.careinsurance.reception.ReceptionProgressingStatus
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQuery
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQueryHandler
import kr.caredoc.careinsurance.reception.ReferenceReceptionNotExistsException
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.settlement.SettlementProgressingStatus
import kr.caredoc.careinsurance.web.caregiving.response.CaregivingRoundResponseConverter
import kr.caredoc.careinsurance.web.caregiving.response.SimpleCaregivingRoundResponse
import kr.caredoc.careinsurance.web.paging.PagedResponse
import kr.caredoc.careinsurance.web.paging.PagingRequest
import kr.caredoc.careinsurance.web.paging.intoPageable
import kr.caredoc.careinsurance.web.paging.intoPagedResponse
import kr.caredoc.careinsurance.web.search.QueryParser
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/v1/organizations/{organization-id}/caregiving-rounds")
class OrganizationCaregivingRoundController(
    private val caregivingRoundsByFilterQueryHandler: CaregivingRoundsByFilterQueryHandler,
    private val receptionsByIdsQueryHandler: ReceptionsByIdsQueryHandler,
) {
    private val queryParser = QueryParser(
        mapOf(
            "accidentNumber" to CaregivingRoundsByFilterQuery.SearchingProperty.ACCIDENT_NUMBER,
            "insuranceNumber" to CaregivingRoundsByFilterQuery.SearchingProperty.INSURANCE_NUMBER,
            "patientName" to CaregivingRoundsByFilterQuery.SearchingProperty.PATIENT_NAME,
        )
    )

    @GetMapping(headers = ["Accept!=text/csv"])
    fun getOrganizationCaregivingRounds(
        pagingRequest: PagingRequest,
        @PathVariable("organization-id") organizationId: String?,
        @RequestParam("from", required = false) startDate: LocalDate?,
        @RequestParam("until", required = false) endDate: LocalDate?,
        @RequestParam("expected-caregiving-start-date", required = false) expectedDate: LocalDate?,
        @RequestParam(
            "reception-progressing-status",
            defaultValue = ""
        ) receptionStatuses: Set<ReceptionProgressingStatus>,
        @RequestParam(
            "caregiving-progressing-status",
            defaultValue = ""
        ) caregivingStatuses: Set<CaregivingProgressingStatus>,
        @RequestParam(
            "settlement-progressing-status",
            defaultValue = ""
        ) settlementStatuses: Set<SettlementProgressingStatus>,
        @RequestParam("billing-progressing-status", defaultValue = "") billingStatuses: Set<BillingProgressingStatus>,
        @RequestParam("query", required = false) query: String?,
        @RequestParam("notify", required = false) notifyCaregivingProgress: Boolean?,
        @RequestParam("expected_settlement_date", required = true) expectedSettlementDate: LocalDate?,
        subject: Subject,
    ): ResponseEntity<PagedResponse<SimpleCaregivingRoundResponse>> {
        val caregivingRounds = caregivingRoundsByFilterQueryHandler.getCaregivingRounds(
            CaregivingRoundsByFilterQuery(
                from = startDate,
                until = endDate,
                organizationId = organizationId,
                expectedCaregivingStartDate = expectedDate,
                receptionProgressingStatuses = receptionStatuses,
                caregivingProgressingStatuses = caregivingStatuses,
                settlementProgressingStatuses = settlementStatuses,
                billingProgressingStatuses = billingStatuses,
                searchCondition = query?.let { queryParser.parse(it) },
                subject = subject,
                notifyCaregivingProgress = notifyCaregivingProgress,
                expectedSettlementDate = expectedSettlementDate,
            ),
            pageRequest = pagingRequest.intoPageable(),
        )

        if (caregivingRounds.isEmpty) {
            return ResponseEntity.ok(PagedResponse.empty(pagingRequest.pageNumber))
        }

        val receptions = receptionsByIdsQueryHandler.getReceptions(
            ReceptionsByIdsQuery(
                receptionIds = caregivingRounds.content.map { it.receptionInfo.receptionId },
                subject = subject,
            )
        ).associateBy { it.id }

        return ResponseEntity.ok(
            caregivingRounds.map {
                val reception = receptions[it.receptionInfo.receptionId]
                    ?: throw ReferenceReceptionNotExistsException(it.receptionInfo.receptionId)
                CaregivingRoundResponseConverter.intoSimpleResponse(reception, it)
            }.intoPagedResponse()
        )
    }

    @GetMapping(headers = ["Accept=text/csv"])
    fun getOrganizationCaregivingRoundsAsCsv(
        @PathVariable("organization-id") organizationId: String?,
        @RequestParam("from", required = false) startDate: LocalDate?,
        @RequestParam("until", required = false) endDate: LocalDate?,
        @RequestParam("expected-caregiving-start-date", required = true) expectedDate: LocalDate,
        @RequestParam(
            "reception-progressing-status",
            defaultValue = ""
        ) receptionStatuses: Set<ReceptionProgressingStatus>,
        @RequestParam(
            "caregiving-progressing-status",
            defaultValue = ""
        ) caregivingStatuses: Set<CaregivingProgressingStatus>,
        @RequestParam(
            "settlement-progressing-status",
            defaultValue = ""
        ) settlementStatuses: Set<SettlementProgressingStatus>,
        @RequestParam("billing-progressing-status", defaultValue = "") billingStatuses: Set<BillingProgressingStatus>,
        @RequestParam("query", required = false) query: String?,
        @RequestParam("notify", required = false) notifyCaregivingProgress: Boolean?,
        @RequestParam("expected_settlement_date", required = true) expectedSettlementDate: LocalDate?,
        subject: Subject,
    ): ResponseEntity<ByteArray> {
        val caregivingRounds = caregivingRoundsByFilterQueryHandler.getCaregivingRoundsAsCsv(
            CaregivingRoundsByFilterQuery(
                from = startDate,
                until = endDate,
                organizationId = organizationId,
                expectedCaregivingStartDate = expectedDate,
                receptionProgressingStatuses = receptionStatuses,
                caregivingProgressingStatuses = caregivingStatuses,
                settlementProgressingStatuses = settlementStatuses,
                billingProgressingStatuses = billingStatuses,
                searchCondition = query?.let { queryParser.parse(it) },
                subject = subject,
                notifyCaregivingProgress = notifyCaregivingProgress,
                expectedSettlementDate = expectedSettlementDate
            )
        )

        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                generateCaregivingRoundsCsvContentDispositionHeader(expectedDate),
            )
            .header(
                HttpHeaders.CONTENT_ENCODING,
                "UTF-8"
            )
            .header(
                HttpHeaders.CONTENT_TYPE,
                "text/csv; charset=UTF-8"
            )
            .body(
                byteArrayOf(239.toByte(), 187.toByte(), 191.toByte()) +
                    caregivingRounds.toByteArray(Charsets.UTF_8)
            )
    }

    fun generateCaregivingRoundsCsvContentDispositionHeader(date: LocalDate): String {
        return "attachment; filename=\"${generateCaregivingRoundsCsvFileName(date)}\""
    }

    fun generateCaregivingRoundsCsvFileName(date: LocalDate): String {
        val dateString = date.format(DateTimeFormatter.BASIC_ISO_DATE)
        return URLEncoder.encode("[간병관리]$dateString.csv", "UTF-8")
    }
}
