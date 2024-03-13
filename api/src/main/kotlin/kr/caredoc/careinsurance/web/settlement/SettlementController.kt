package kr.caredoc.careinsurance.web.settlement

import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.patch.Patches
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQuery
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQueryHandler
import kr.caredoc.careinsurance.reception.ReferenceReceptionNotExistsException
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.settlement.DateRange
import kr.caredoc.careinsurance.settlement.InvalidSettlementProgressingStatusTransitionException
import kr.caredoc.careinsurance.settlement.ReferenceDatePeriodNoDataException
import kr.caredoc.careinsurance.settlement.SettlementByIdQuery
import kr.caredoc.careinsurance.settlement.SettlementEditingCommand
import kr.caredoc.careinsurance.settlement.SettlementEditingCommandHandler
import kr.caredoc.careinsurance.settlement.SettlementProgressingStatus
import kr.caredoc.careinsurance.settlement.SettlementsSearchQuery
import kr.caredoc.careinsurance.settlement.SettlementsSearchQueryHandler
import kr.caredoc.careinsurance.web.paging.PagedResponse
import kr.caredoc.careinsurance.web.paging.PagingRequest
import kr.caredoc.careinsurance.web.paging.intoPageable
import kr.caredoc.careinsurance.web.paging.intoPagedResponse
import kr.caredoc.careinsurance.web.response.GeneralErrorResponse
import kr.caredoc.careinsurance.web.search.QueryParser
import kr.caredoc.careinsurance.web.settlement.request.IdentifiedSettlementEditingRequest
import kr.caredoc.careinsurance.web.settlement.response.InvalidSettlementProgressingStatusTransitionData
import kr.caredoc.careinsurance.web.settlement.response.SettlementResponse
import kr.caredoc.careinsurance.web.settlement.response.SettlementResponseConverter
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/v1/settlements")
class SettlementController(
    private val settlementsSearchQueryHandler: SettlementsSearchQueryHandler,
    private val settlementEditingCommandHandler: SettlementEditingCommandHandler,
    private val receptionsByIdsQueryHandler: ReceptionsByIdsQueryHandler,
) {
    private val queryParser = QueryParser(
        mapOf(
            "accidentNumber" to SettlementsSearchQuery.SearchingProperty.ACCIDENT_NUMBER,
            "patientName" to SettlementsSearchQuery.SearchingProperty.PATIENT_NAME,
            "organizationName" to SettlementsSearchQuery.SearchingProperty.ORGANIZATION_NAME,
            "caregiverName" to SettlementsSearchQuery.SearchingProperty.CAREGIVER_NAME,
        )
    )

    @GetMapping(headers = ["Accept!=text/csv"])
    fun getSettlements(
        pagingRequest: PagingRequest,
        @RequestParam("from") expectedSettlementDateFrom: LocalDate?,
        @RequestParam("until") expectedSettlementDateUntil: LocalDate?,
        @RequestParam("transaction-date-from") transactionDateFrom: LocalDate?,
        @RequestParam("transaction-date-until") transactionDateUntil: LocalDate?,
        @RequestParam("progressing-status") progressingStatus: SettlementProgressingStatus,
        @RequestParam("query") query: String?,
        @RequestParam("sort", defaultValue = "EXPECTED_SETTLEMENT_DATE_DESC_ACCIDENT_NUMBER_DESC") sort: SettlementsSearchQuery.Sorting,
        subject: Subject,
    ): ResponseEntity<PagedResponse<SettlementResponse>> {
        val expectedSettlementDateRange =
            if (expectedSettlementDateFrom != null && expectedSettlementDateUntil != null) {
                DateRange(
                    from = expectedSettlementDateFrom,
                    until = expectedSettlementDateUntil,
                )
            } else {
                null
            }
        val transactionDateRange = if (transactionDateFrom != null && transactionDateUntil != null) {
            DateRange(
                from = transactionDateFrom,
                until = transactionDateUntil,
            )
        } else {
            null
        }
        if (expectedSettlementDateRange == null && transactionDateRange == null) {
            throw ReferenceDatePeriodNoDataException()
        }

        val settlements = settlementsSearchQueryHandler.getSettlements(
            query = SettlementsSearchQuery(
                progressingStatus = progressingStatus,
                expectedSettlementDate = expectedSettlementDateRange,
                transactionDate = transactionDateRange,
                searchCondition = query?.let { queryParser.parse(it) },
                sorting = sort,
                subject = subject,
            ),
            pageRequest = pagingRequest.intoPageable(),
        )

        val receptions = receptionsByIdsQueryHandler.getReceptions(
            ReceptionsByIdsQuery(
                settlements.content.map { it.receptionId },
                subject,
            )
        ).associateBy { it.id }

        return ResponseEntity.ok(
            settlements.map {
                val reception = receptions[it.receptionId]
                    ?: throw ReferenceReceptionNotExistsException(it.receptionId)
                SettlementResponseConverter.intoSettlementResponse(reception, it)
            }.intoPagedResponse()
        )
    }

    @GetMapping(headers = ["Accept=text/csv"])
    fun getSettlementsAsCsv(
        @RequestParam("from") expectedSettlementDateFrom: LocalDate?,
        @RequestParam("until") expectedSettlementDateUntil: LocalDate?,
        @RequestParam("progressing-status") progressingStatus: SettlementProgressingStatus,
        @RequestParam("query") query: String?,
        @RequestParam("sort", defaultValue = "EXPECTED_SETTLEMENT_DATE_DESC_ACCIDENT_NUMBER_DESC") sort: SettlementsSearchQuery.Sorting,
        subject: Subject,
    ): ResponseEntity<ByteArray> {
        val expectedSettlementDateRange =
            if (expectedSettlementDateFrom != null && expectedSettlementDateUntil != null) {
                DateRange(
                    from = expectedSettlementDateFrom,
                    until = expectedSettlementDateUntil,
                )
            } else {
                null
            }
        if (expectedSettlementDateRange == null) {
            throw ReferenceDatePeriodNoDataException()
        }
        val settlements = settlementsSearchQueryHandler.getSettlementsAsCsv(
            query = SettlementsSearchQuery(
                progressingStatus = progressingStatus,
                expectedSettlementDate = expectedSettlementDateRange,
                searchCondition = query?.let { queryParser.parse(it) },
                sorting = sort,
                subject = subject,
            ),
        )

        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                generateSettlementsCsvContentDispositionHeader(Clock.today()),
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
                    settlements.toByteArray(Charsets.UTF_8)
            )
    }

    fun generateSettlementsCsvContentDispositionHeader(date: LocalDate): String {
        return "attachment; filename=\"${generateSettlementsCsvFileName(date)}\""
    }

    fun generateSettlementsCsvFileName(date: LocalDate): String {
        val dateString = date.format(DateTimeFormatter.BASIC_ISO_DATE)
        return URLEncoder.encode("[정산관리]$dateString.csv", "UTF-8")
    }

    @PatchMapping
    fun editSettlements(
        @RequestBody payload: List<IdentifiedSettlementEditingRequest>,
        subject: Subject,
    ): ResponseEntity<Unit> {
        settlementEditingCommandHandler.editSettlements(
            payload.map {
                SettlementByIdQuery(
                    settlementId = it.id,
                    subject = subject,
                ) to SettlementEditingCommand(
                    progressingStatus = Patches.ofValue(it.progressingStatus),
                    settlementManagerId = Patches.ofValue(it.settlementManagerId),
                    subject = subject,
                )
            }
        )
        return ResponseEntity.noContent().build()
    }

    @ExceptionHandler(InvalidSettlementProgressingStatusTransitionException::class)
    fun handleInvalidSettlementProgressingStatusTransitionException(e: InvalidSettlementProgressingStatusTransitionException) =
        ResponseEntity
            .unprocessableEntity()
            .body(
                GeneralErrorResponse(
                    message = "지정한 상태로 정산 진행 상태 변경할 수 없습니다.",
                    errorType = "INVALID_SETTLEMENT_PROGRESSING_STATUS_TRANSITION",
                    data = InvalidSettlementProgressingStatusTransitionData(
                        currentSettlementProgressingStatus = e.currentSettlementProgressingStatus,
                        enteredSettlementProgressingStatus = e.enteredSettlementProgressingStatus,
                    )
                )
            )

    @ExceptionHandler(ReferenceDatePeriodNoDataException::class)
    fun handleInvalidReferenceDatePeriodNoDataException(e: ReferenceDatePeriodNoDataException) =
        ResponseEntity
            .unprocessableEntity()
            .body(
                GeneralErrorResponse(
                    message = "요청한 조건에 날짜 기간 정보가 없습니다.",
                    errorType = "REFERENCE_DATE_PERIOD_NO_DATA_EXCEPTION",
                    data = Unit
                )
            )
}
