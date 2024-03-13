package kr.caredoc.careinsurance.web.billing

import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.billing.Billing
import kr.caredoc.careinsurance.billing.BillingByFilterQuery
import kr.caredoc.careinsurance.billing.BillingByFilterQueryHandler
import kr.caredoc.careinsurance.billing.BillingByIdQuery
import kr.caredoc.careinsurance.billing.BillingByIdQueryHandler
import kr.caredoc.careinsurance.billing.BillingNotExistsException
import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import kr.caredoc.careinsurance.billing.BillingTransactionRecordingCommand
import kr.caredoc.careinsurance.billing.BillingTransactionRecordingCommandHandler
import kr.caredoc.careinsurance.billing.DownloadCertificateCommand
import kr.caredoc.careinsurance.billing.DownloadCertificateCommandHandler
import kr.caredoc.careinsurance.billing.InvalidBillingProgressingStatusChangeException
import kr.caredoc.careinsurance.intoUtcOffsetDateTime
import kr.caredoc.careinsurance.page
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQuery
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQueryHandler
import kr.caredoc.careinsurance.reception.ReferenceReceptionNotExistsException
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.web.billing.request.BillingTransactionCreationRequest
import kr.caredoc.careinsurance.web.billing.response.BillingResponse
import kr.caredoc.careinsurance.web.billing.response.BillingTransactionResponse
import kr.caredoc.careinsurance.web.billing.response.DetailBillingResponse
import kr.caredoc.careinsurance.web.billing.response.EnteredBillingNotExists
import kr.caredoc.careinsurance.web.billing.response.InvalidBillingProgressingStatusEnteredData
import kr.caredoc.careinsurance.web.paging.PagedResponse
import kr.caredoc.careinsurance.web.paging.PagingRequest
import kr.caredoc.careinsurance.web.paging.intoPageable
import kr.caredoc.careinsurance.web.paging.intoPagedResponse
import kr.caredoc.careinsurance.web.response.GeneralErrorResponse
import kr.caredoc.careinsurance.web.search.QueryParser
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/v1/billings")
class BillingController(
    private val downloadCertificateCommandHandler: DownloadCertificateCommandHandler,
    private val billingByIdQueryHandler: BillingByIdQueryHandler,
    private val billingTransactionRecordingCommandHandler: BillingTransactionRecordingCommandHandler,
    private val billingByFilterQueryHandler: BillingByFilterQueryHandler,
    private val receptionsByIdsQueryHandler: ReceptionsByIdsQueryHandler,
) {
    private val queryParser = QueryParser(
        mapOf(
            "accidentNumber" to BillingByFilterQuery.SearchingProperty.ACCIDENT_NUMBER,
            "patientName" to BillingByFilterQuery.SearchingProperty.PATIENT_NAME,
            "caregiverName" to BillingByFilterQuery.SearchingProperty.CAREGIVER_NAME,
        )
    )

    @GetMapping("/{billing-id}/certificate")
    fun getCertificate(
        @PathVariable("billing-id") billingId: String,
        subject: Subject,
    ): ResponseEntity<ByteArray> {
        val billing = billingByIdQueryHandler.getBilling(BillingByIdQuery(billingId, subject))
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                    .filename(URLEncoder.encode(generateCertificateFileName(billing), "UTF-8").replace("+", "%20"))
                    .build()
                    .toString()
            )
            .body(
                downloadCertificateCommandHandler.downloadCertification(
                    DownloadCertificateCommand(
                        billingId = billingId,
                        subject = subject,
                    )
                )
            )
    }

    private fun generateCertificateFileName(billing: Billing): String {
        val accidentNumber = billing.receptionInfo.accidentNumber
        val roundNumber = billing.caregivingRoundInfo.roundNumber
        val formattedToday = Clock.today().format(DateTimeFormatter.BASIC_ISO_DATE)

        return "[청구관리]${accidentNumber}_${roundNumber}_$formattedToday.jpg"
    }

    @GetMapping("/{billing-id}")
    fun getBilling(
        @PathVariable("billing-id") billingId: String,
        subject: Subject,
    ): ResponseEntity<DetailBillingResponse> {
        val billing = billingByIdQueryHandler.getBilling(
            BillingByIdQuery(
                billingId = billingId,
                subject = subject,
            )
        )

        return ResponseEntity.ok().body(
            DetailBillingResponse(
                accidentNumber = billing.receptionInfo.accidentNumber,
                subscriptionDate = billing.receptionInfo.subscriptionDate,
                receptionId = billing.receptionInfo.receptionId,
                roundNumber = billing.caregivingRoundInfo.roundNumber,
                startDateTime = billing.caregivingRoundInfo.startDateTime.intoUtcOffsetDateTime(),
                endDateTime = billing.caregivingRoundInfo.endDateTime.intoUtcOffsetDateTime(),
                basicAmounts = billing.basicAmounts,
                additionalHours = billing.additionalHours,
                additionalAmount = billing.additionalAmount,
                totalAmount = billing.totalAmount,
                totalDepositAmount = billing.totalDepositAmount,
                totalWithdrawalAmount = billing.totalWithdrawalAmount,
            )
        )
    }

    @PostMapping("/{billing-id}/transactions")
    fun createBillingTransactions(
        @PathVariable("billing-id") billingId: String,
        @RequestBody payload: BillingTransactionCreationRequest,
        subject: Subject,
    ): ResponseEntity<Unit> {
        billingTransactionRecordingCommandHandler.recordTransaction(
            BillingByIdQuery(
                billingId = billingId,
                subject = subject,
            ),
            BillingTransactionRecordingCommand(
                transactionType = payload.transactionType,
                amount = payload.amount,
                transactionDate = payload.transactionDate,
                transactionSubjectId = payload.transactionSubjectId,
                subject = subject,
            )
        )
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{billing-id}/transactions")
    fun getBillingTransactions(
        @PathVariable("billing-id") billingId: String,
        pagingRequest: PagingRequest,
        subject: Subject,
    ): ResponseEntity<PagedResponse<BillingTransactionResponse>> {
        val billing = billingByIdQueryHandler.getBilling(
            BillingByIdQuery(
                billingId = billingId,
                subject = subject,
            )
        )

        val sortedTransactions = billing.transactions.sortedWith(
            compareByDescending<Billing.TransactionRecord> {
                it.transactionDate
            }.thenByDescending {
                it.enteredDateTime
            }
        )

        return ResponseEntity.ok(
            sortedTransactions.page(pagingRequest.intoPageable()).map {
                it.intoResponse()
            }.intoPagedResponse()
        )
    }

    private fun Billing.TransactionRecord.intoResponse() = BillingTransactionResponse(
        transactionType = transactionType,
        amount = amount,
        transactionDate = transactionDate,
        enteredDateTime = enteredDateTime.intoUtcOffsetDateTime(),
        transactionSubjectId = transactionSubjectId,
    )

    @GetMapping
    fun getBillings(
        pagingRequest: PagingRequest,
        @RequestParam(
            "progressing-status",
            required = false,
            defaultValue = "",
        )
        progressingStatus: Set<BillingProgressingStatus>,
        @RequestParam("used-period-from", required = false) usedPeriodFrom: LocalDate?,
        @RequestParam("used-period-until", required = false) usedPeriodUntil: LocalDate?,
        @RequestParam("billing-date-from", required = false) billingDateFrom: LocalDate?,
        @RequestParam("billing-date-until", required = false) billingDateUntil: LocalDate?,
        @RequestParam("transaction-date-from", required = false) transactionDateFrom: LocalDate?,
        @RequestParam("transaction-date-until", required = false) transactionDateUntil: LocalDate?,
        @RequestParam("sort", required = false) sorting: BillingByFilterQuery.Sorting?,
        @RequestParam("query", required = false) query: String?,
        subject: Subject
    ): ResponseEntity<PagedResponse<BillingResponse>> {
        val billings = billingByFilterQueryHandler.getBillings(
            BillingByFilterQuery(
                progressingStatus = progressingStatus,
                usedPeriodFrom = usedPeriodFrom,
                usedPeriodUntil = usedPeriodUntil,
                billingDateFrom = billingDateFrom,
                billingDateUntil = billingDateUntil,
                transactionDateFrom = transactionDateFrom,
                transactionDateUntil = transactionDateUntil,
                searchQuery = query?.let { queryParser.parse(query) },
                sorting = sorting,
                subject = subject,
            ),
            pageRequest = pagingRequest.intoPageable()
        )

        val receptions = receptionsByIdsQueryHandler.getReceptions(
            ReceptionsByIdsQuery(
                receptionIds = billings.content.map { it.receptionInfo.receptionId },
                subject = subject,
            )
        ).associateBy { it.id }

        return ResponseEntity.ok(
            billings.map {
                val reception = receptions[it.receptionInfo.receptionId]
                    ?: throw ReferenceReceptionNotExistsException(it.receptionInfo.receptionId)
                BillingResponse(
                    id = it.id,
                    receptionId = it.receptionInfo.receptionId,
                    accidentNumber = reception.accidentInfo.accidentNumber,
                    patientName = reception.patientInfo.name.masked,
                    roundNumber = it.caregivingRoundInfo.roundNumber,
                    startDateTime = it.caregivingRoundInfo.startDateTime.intoUtcOffsetDateTime(),
                    endDateTime = it.caregivingRoundInfo.endDateTime.intoUtcOffsetDateTime(),
                    actualUsagePeriod = convertIntoDateAndTime(
                        it.caregivingRoundInfo.startDateTime,
                        it.caregivingRoundInfo.endDateTime
                    ),
                    billingDate = it.billingDate,
                    totalAmount = it.totalAmount,
                    totalDepositAmount = it.totalDepositAmount,
                    totalWithdrawalAmount = it.totalWithdrawalAmount,
                    transactionDate = it.lastTransactionDate,
                    billingProgressingStatus = it.billingProgressingStatus,
                )
            }.intoPagedResponse()
        )
    }

    private fun convertIntoDateAndTime(startDateTime: LocalDateTime, endDateTime: LocalDateTime): String {
        val result = StringBuilder()
        val date = Duration.between(startDateTime, endDateTime).toDays()
        val time = Duration.between(startDateTime, endDateTime).toHoursPart()
        if (date > 0) {
            result.append("${date}일")
        }
        if (time > 0) {
            result.append(" ${time}시간")
        }
        return result.toString()
    }

    @ExceptionHandler(BillingNotExistsException::class)
    private fun handleBillingNotExists(e: BillingNotExistsException) =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(
                GeneralErrorResponse(
                    message = "조회하고자 하는 청구가 존재하지 않습니다.",
                    errorType = "BILLING_NOT_EXISTS",
                    data = EnteredBillingNotExists(
                        enteredBillingId = e.billingId
                    )
                )
            )

    @ExceptionHandler(InvalidBillingProgressingStatusChangeException::class)
    private fun handleInvalidBillingProgressingStatusChange(e: InvalidBillingProgressingStatusChangeException) =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(
                GeneralErrorResponse(
                    message = "지정한 상태로 청구 상태를 변경할 수 없습니다.",
                    errorType = "INVALID_BILLING_STATE_TRANSITION",
                    data = InvalidBillingProgressingStatusEnteredData(
                        currentBillingProgressingStatus = e.currentBillingProgressingStatus,
                        enteredBillingProgressingStatus = e.enteredBillingProgressingStatus,
                    ),
                )
            )
}
