package kr.caredoc.careinsurance.web.reconciliation

import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.RequiredParameterNotSuppliedException
import kr.caredoc.careinsurance.reconciliation.ClosedReconciliationsByFilterQuery
import kr.caredoc.careinsurance.reconciliation.ClosedReconciliationsByFilterQueryHandler
import kr.caredoc.careinsurance.reconciliation.ClosingStatus
import kr.caredoc.careinsurance.reconciliation.InvalidReconciliationClosingStatusTransitionException
import kr.caredoc.careinsurance.reconciliation.OpenReconciliationsByFilterQuery
import kr.caredoc.careinsurance.reconciliation.OpenReconciliationsByFilterQueryHandler
import kr.caredoc.careinsurance.reconciliation.Reconciliation
import kr.caredoc.careinsurance.reconciliation.ReconciliationByIdQuery
import kr.caredoc.careinsurance.reconciliation.ReconciliationEditingCommand
import kr.caredoc.careinsurance.reconciliation.ReconciliationEditingCommandHandler
import kr.caredoc.careinsurance.reconciliation.ReferenceReconciliationNotExistsException
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.web.paging.PagedResponse
import kr.caredoc.careinsurance.web.paging.PagingRequest
import kr.caredoc.careinsurance.web.paging.intoPageable
import kr.caredoc.careinsurance.web.paging.intoPagedResponse
import kr.caredoc.careinsurance.web.reconciliation.response.EditingReconciliationRequest
import kr.caredoc.careinsurance.web.reconciliation.response.EnteredReconciliationNotRegisteredData
import kr.caredoc.careinsurance.web.reconciliation.response.InvalidReconciliationClosingStatusTransitionData
import kr.caredoc.careinsurance.web.reconciliation.response.ReconciliationResponse
import kr.caredoc.careinsurance.web.response.GeneralErrorResponse
import kr.caredoc.careinsurance.web.search.QueryParser
import org.springframework.data.domain.Page
import org.springframework.http.ContentDisposition
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
@RequestMapping("/api/v1/reconciliations")
class ReconciliationController(
    private val openReconciliationsByFilterQueryHandler: OpenReconciliationsByFilterQueryHandler,
    private val closedReconciliationsByFilterQueryHandler: ClosedReconciliationsByFilterQueryHandler,
    private val reconciliationEditingCommandHandler: ReconciliationEditingCommandHandler,
) {
    private val queryParser = QueryParser(
        mapOf(
            "accidentNumber" to OpenReconciliationsByFilterQuery.SearchingProperty.ACCIDENT_NUMBER,
            "patientName" to OpenReconciliationsByFilterQuery.SearchingProperty.PATIENT_NAME,
        )
    )

    @GetMapping(headers = ["Accept!=text/csv"])
    fun getReconciliations(
        @RequestParam("closing-status") closingStatus: ClosingStatus,
        @RequestParam("issued-at-from") issuedAtFrom: LocalDate?,
        @RequestParam("issued-at-until") issuedAtUntil: LocalDate?,
        @RequestParam("query") query: String?,
        @RequestParam("reconciled-year") year: Int?,
        @RequestParam("reconciled-month") month: Int?,
        pagingRequest: PagingRequest,
        subject: Subject,
    ): ResponseEntity<PagedResponse<ReconciliationResponse>> {
        val reconciliations = when (closingStatus) {
            ClosingStatus.OPEN -> getOpenReconciliations(
                issuedAtFrom = assertNotNull(issuedAtFrom),
                issuedAtUntil = assertNotNull(issuedAtUntil),
                query = query,
                pagingRequest = pagingRequest,
                subject = subject,
            )

            ClosingStatus.CLOSED -> getClosedReconciliations(
                year = assertNotNull(year),
                month = assertNotNull(month),
                pagingRequest = pagingRequest,
                subject = subject,
            )
        }

        return ResponseEntity.ok(reconciliations.map { it.intoResponse() }.intoPagedResponse())
    }

    @GetMapping(headers = ["Accept=text/csv"])
    fun getReconciliationsAsCsv(
        @RequestParam("closing-status") closingStatus: ClosingStatus,
        @RequestParam("issued-at-from") issuedAtFrom: LocalDate?,
        @RequestParam("issued-at-until") issuedAtUntil: LocalDate?,
        @RequestParam("query") query: String?,
        @RequestParam("reconciled-year") year: Int?,
        @RequestParam("reconciled-month") month: Int?,
        subject: Subject,
    ): ResponseEntity<ByteArray> {
        val reconciliations = when (closingStatus) {
            ClosingStatus.OPEN -> getOpenReconciliationsAsCsv(
                issuedAtFrom = assertNotNull(issuedAtFrom),
                issuedAtUntil = assertNotNull(issuedAtUntil),
                query = query,
                subject = subject,
            )

            ClosingStatus.CLOSED -> getClosedReconciliationsAsCsv(
                year = assertNotNull(year),
                month = assertNotNull(month),
                subject = subject,
            )
        }

        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(generateReconciliationsCsvFileName()).build().toString(),
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
                    reconciliations.toByteArray(Charsets.UTF_8)
            )
    }

    private fun <T> assertNotNull(queryParam: T?): T {
        return queryParam ?: throw RequiredParameterNotSuppliedException()
    }

    private fun Reconciliation.intoResponse() = ReconciliationResponse(
        id = id,
        closingStatus = closingStatus,
        receptionId = receptionId,
        caregivingRoundId = caregivingRoundId,
        billingAmount = billingAmount,
        settlementAmount = settlementAmount,
        settlementDepositAmount = settlementDepositAmount,
        settlementWithdrawalAmount = settlementWithdrawalAmount,
        issuedType = issuedType,
        profit = profit,
        distributedProfit = distributedProfit,
    )

    private fun getOpenReconciliations(
        issuedAtFrom: LocalDate,
        issuedAtUntil: LocalDate,
        query: String?,
        pagingRequest: PagingRequest,
        subject: Subject,
    ): Page<Reconciliation> {
        return openReconciliationsByFilterQueryHandler.getOpenReconciliations(
            query = OpenReconciliationsByFilterQuery(
                from = issuedAtFrom,
                until = issuedAtUntil,
                searchCondition = query?.let { queryParser.parse(it) },
                subject = subject,
            ),
            pageRequest = pagingRequest.intoPageable(),
        )
    }

    private fun getOpenReconciliationsAsCsv(
        issuedAtFrom: LocalDate,
        issuedAtUntil: LocalDate,
        query: String?,
        subject: Subject,
    ): String {
        return openReconciliationsByFilterQueryHandler.getOpenReconciliationsAsCsv(
            query = OpenReconciliationsByFilterQuery(
                from = issuedAtFrom,
                until = issuedAtUntil,
                searchCondition = query?.let { queryParser.parse(it) },
                subject = subject,
            ),
        )
    }

    private fun getClosedReconciliations(
        year: Int,
        month: Int,
        pagingRequest: PagingRequest,
        subject: Subject,
    ): Page<Reconciliation> {
        return closedReconciliationsByFilterQueryHandler.getClosedReconciliations(
            query = ClosedReconciliationsByFilterQuery(
                year = year,
                month = month,
                subject = subject,
            ),
            pageRequest = pagingRequest.intoPageable(),
        )
    }

    private fun getClosedReconciliationsAsCsv(
        year: Int,
        month: Int,
        subject: Subject,
    ): String {
        return closedReconciliationsByFilterQueryHandler.getClosedReconciliationsAsCsv(
            query = ClosedReconciliationsByFilterQuery(
                year = year,
                month = month,
                subject = subject,
            ),
        )
    }

    private fun generateReconciliationsCsvFileName(): String {
        val dateString = Clock.today().format(DateTimeFormatter.BASIC_ISO_DATE)
        return URLEncoder.encode("정산대사현황_$dateString.csv", "UTF-8")
    }

    @PatchMapping
    fun editReconciliations(
        @RequestBody payload: List<EditingReconciliationRequest>,
        subject: Subject,
    ): ResponseEntity<Unit> {
        reconciliationEditingCommandHandler.editReconciliations(
            payload.map {
                ReconciliationByIdQuery(
                    reconciliationId = it.id,
                    subject = subject
                ) to ReconciliationEditingCommand(
                    closingStatus = it.closingStatus,
                    subject = subject,
                )
            }
        )

        return ResponseEntity.noContent().build()
    }

    @ExceptionHandler(ReferenceReconciliationNotExistsException::class)
    fun handleReferenceReconciliationNotExistsException(e: ReferenceReconciliationNotExistsException) = ResponseEntity
        .unprocessableEntity()
        .body(
            GeneralErrorResponse(
                message = "요청에 포함된 대사가 존재하지 않습니다.",
                errorType = "REFERENCE_RECONCILIATION_NOT_EXISTS",
                data = EnteredReconciliationNotRegisteredData(
                    enteredReconciliationId = e.referenceReconciliationId
                )
            )
        )

    @ExceptionHandler(InvalidReconciliationClosingStatusTransitionException::class)
    fun handleInvalidReconciliationClosingStatusTransitionException(e: InvalidReconciliationClosingStatusTransitionException) =
        ResponseEntity
            .unprocessableEntity()
            .body(
                GeneralErrorResponse(
                    message = "지정한 상태로 대사 마감 상태를 변경할 수 없습니다.",
                    errorType = "INVALID_RECONCILIATION_CLOSING_STATUS_TRANSITION",
                    data = InvalidReconciliationClosingStatusTransitionData(
                        currentReconciliationClosingStatus = e.currentReconciliationClosingStatus,
                        enteredReconciliationClosingStatus = e.enteredReconciliationClosingStatus,
                    )
                )
            )
}
