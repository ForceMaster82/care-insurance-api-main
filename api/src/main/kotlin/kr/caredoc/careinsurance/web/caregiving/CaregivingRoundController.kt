package kr.caredoc.careinsurance.web.caregiving

import kr.caredoc.careinsurance.account.AccountInfo
import kr.caredoc.careinsurance.atLocalTimeZone
import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import kr.caredoc.careinsurance.caregiving.CaregiverInfo
import kr.caredoc.careinsurance.caregiving.CaregivingCharge
import kr.caredoc.careinsurance.caregiving.CaregivingChargeByCaregivingRoundIdQuery
import kr.caredoc.careinsurance.caregiving.CaregivingChargeByCaregivingRoundIdQueryHandler
import kr.caredoc.careinsurance.caregiving.CaregivingChargeEditingCommand
import kr.caredoc.careinsurance.caregiving.CaregivingChargeEditingCommandHandler
import kr.caredoc.careinsurance.caregiving.CaregivingProgressingStatus
import kr.caredoc.careinsurance.caregiving.CaregivingRoundByIdQuery
import kr.caredoc.careinsurance.caregiving.CaregivingRoundByIdQueryHandler
import kr.caredoc.careinsurance.caregiving.CaregivingRoundEditingCommand
import kr.caredoc.careinsurance.caregiving.CaregivingRoundEditingCommandHandler
import kr.caredoc.careinsurance.caregiving.CaregivingRoundsByFilterQuery
import kr.caredoc.careinsurance.caregiving.CaregivingRoundsByFilterQueryHandler
import kr.caredoc.careinsurance.caregiving.IllegalCaregivingPeriodEnteredException
import kr.caredoc.careinsurance.caregiving.exception.CaregivingAdditionalChargeNameDuplicatedException
import kr.caredoc.careinsurance.caregiving.exception.CaregivingChargeEditingDeniedException
import kr.caredoc.careinsurance.caregiving.exception.InvalidCaregivingChargeActionableStatusException
import kr.caredoc.careinsurance.caregiving.exception.InvalidCaregivingChargeConfirmStatusTransitionException
import kr.caredoc.careinsurance.caregiving.exception.InvalidCaregivingProgressingStatusTransitionException
import kr.caredoc.careinsurance.intoUtcOffsetDateTime
import kr.caredoc.careinsurance.patch.Patches
import kr.caredoc.careinsurance.reception.ReceptionByIdQuery
import kr.caredoc.careinsurance.reception.ReceptionByIdQueryHandler
import kr.caredoc.careinsurance.reception.ReceptionProgressingStatus
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQuery
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQueryHandler
import kr.caredoc.careinsurance.reception.ReferenceReceptionNotExistsException
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.settlement.SettlementProgressingStatus
import kr.caredoc.careinsurance.web.caregiving.request.CaregivingChargeEditingRequest
import kr.caredoc.careinsurance.web.caregiving.request.CaregivingRoundEditingRequest
import kr.caredoc.careinsurance.web.caregiving.response.CaregivingRoundResponseConverter
import kr.caredoc.careinsurance.web.caregiving.response.DeniedEditingCaregivingChargeData
import kr.caredoc.careinsurance.web.caregiving.response.DetailCaregivingChargeResponse
import kr.caredoc.careinsurance.web.caregiving.response.DetailCaregivingRoundResponse
import kr.caredoc.careinsurance.web.caregiving.response.DuplicatedAdditionalChargeNameData
import kr.caredoc.careinsurance.web.caregiving.response.DuplicatedCaregivingChargeData
import kr.caredoc.careinsurance.web.caregiving.response.IllegalCaregivingPeriodEnteredData
import kr.caredoc.careinsurance.web.caregiving.response.InvalidCaregivingChargeActionableStatusEnteredData
import kr.caredoc.careinsurance.web.caregiving.response.InvalidCaregivingProgressingStatusEnteredData
import kr.caredoc.careinsurance.web.caregiving.response.SimpleCaregivingRoundResponse
import kr.caredoc.careinsurance.web.paging.PagedResponse
import kr.caredoc.careinsurance.web.paging.PagingRequest
import kr.caredoc.careinsurance.web.paging.intoPageable
import kr.caredoc.careinsurance.web.paging.intoPagedResponse
import kr.caredoc.careinsurance.web.response.GeneralErrorResponse
import kr.caredoc.careinsurance.web.search.QueryParser
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/v1/caregiving-rounds")
class CaregivingRoundController(
    private val caregivingRoundsByFilterQueryHandler: CaregivingRoundsByFilterQueryHandler,
    private val caregivingRoundEditingCommandHandler: CaregivingRoundEditingCommandHandler,
    private val caregivingChargeEditingCommandHandler: CaregivingChargeEditingCommandHandler,
    private val caregivingChargeByCaregivingRoundIdQueryHandler: CaregivingChargeByCaregivingRoundIdQueryHandler,
    private val caregivingRoundByIdQueryHandler: CaregivingRoundByIdQueryHandler,
    private val receptionByIdQueryHandler: ReceptionByIdQueryHandler,
    private val receptionsByIdsQueryHandler: ReceptionsByIdsQueryHandler,
) {
    private val queryParser = QueryParser(
        mapOf(
            "accidentNumber" to CaregivingRoundsByFilterQuery.SearchingProperty.ACCIDENT_NUMBER,
            "insuranceNumber" to CaregivingRoundsByFilterQuery.SearchingProperty.INSURANCE_NUMBER,
            "patientName" to CaregivingRoundsByFilterQuery.SearchingProperty.PATIENT_NAME,
            "caregiverName" to CaregivingRoundsByFilterQuery.SearchingProperty.CAREGIVER_NAME,
        )
    )

    @GetMapping(headers = ["Accept!=text/csv"])
    fun getCaregivingRounds(
        pagingRequest: PagingRequest,
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
        subject: Subject,
    ): ResponseEntity<PagedResponse<SimpleCaregivingRoundResponse>> {
        val caregivingRounds = caregivingRoundsByFilterQueryHandler.getCaregivingRounds(
            CaregivingRoundsByFilterQuery(
                from = startDate,
                until = endDate,
                organizationId = null,
                expectedCaregivingStartDate = expectedDate,
                receptionProgressingStatuses = receptionStatuses,
                caregivingProgressingStatuses = caregivingStatuses,
                settlementProgressingStatuses = settlementStatuses,
                billingProgressingStatuses = billingStatuses,
                searchCondition = query?.let { queryParser.parse(it) },
                subject = subject,
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
    fun getCaregivingRoundsAsCsv(
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
        subject: Subject,
    ): ResponseEntity<ByteArray> {
        val caregivingRounds = caregivingRoundsByFilterQueryHandler.getCaregivingRoundsAsCsv(
            CaregivingRoundsByFilterQuery(
                from = startDate,
                until = endDate,
                organizationId = null,
                expectedCaregivingStartDate = expectedDate,
                receptionProgressingStatuses = receptionStatuses,
                caregivingProgressingStatuses = caregivingStatuses,
                settlementProgressingStatuses = settlementStatuses,
                billingProgressingStatuses = billingStatuses,
                searchCondition = query?.let { queryParser.parse(it) },
                subject = subject,
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

    @PutMapping("/{caregiving-round-id}")
    fun editCaregivingRound(
        @PathVariable("caregiving-round-id") caregivingRoundId: String,
        @RequestBody payload: CaregivingRoundEditingRequest,
        subject: Subject,
    ): ResponseEntity<Unit> {
        caregivingRoundEditingCommandHandler.editCaregivingRound(
            CaregivingRoundByIdQuery(
                caregivingRoundId = caregivingRoundId,
                subject = subject,
            ),
            payload.intoCommand(subject),
        )
        return ResponseEntity.noContent().build()
    }

    private fun CaregivingRoundEditingRequest.intoCommand(subject: Subject) = CaregivingRoundEditingCommand(
        caregivingProgressingStatus = Patches.ofValue(this.caregivingProgressingStatus),
        startDateTime = this.startDateTime?.atLocalTimeZone(),
        endDateTime = this.endDateTime?.atLocalTimeZone(),
        caregivingRoundClosingReasonType = this.caregivingRoundClosingReasonType,
        caregivingRoundClosingReasonDetail = this.caregivingRoundClosingReasonDetail,
        caregiverInfo = this.caregiverInfo?.let {
            CaregiverInfo(
                caregiverOrganizationId = it.caregiverOrganizationId,
                name = it.name,
                sex = it.sex,
                birthDate = it.birthDate,
                insured = it.insured,
                phoneNumber = it.phoneNumber,
                dailyCaregivingCharge = it.dailyCaregivingCharge,
                commissionFee = it.commissionFee,
                accountInfo = AccountInfo(
                    bank = this.caregiverInfo.accountInfo.bank,
                    accountNumber = this.caregiverInfo.accountInfo.accountNumber,
                    accountHolder = this.caregiverInfo.accountInfo.accountHolder,
                )
            )
        },
        remarks = this.remarks,
        subject = subject,
    )

    @PutMapping("/{caregiving-round-id}/caregiving-charge")
    fun createAndEditCaregivingCharge(
        @PathVariable("caregiving-round-id") caregivingRoundId: String,
        @RequestBody payload: CaregivingChargeEditingRequest,
        subject: Subject,
    ): ResponseEntity<Unit> {
        try {
            caregivingChargeEditingCommandHandler.createOrEditCaregivingCharge(
                CaregivingRoundByIdQuery(
                    caregivingRoundId = caregivingRoundId,
                    subject = subject,
                ),
                payload.intoCommand(subject),
            )
        } catch (e: DataIntegrityViolationException) {
            throw CaregivingChargeDuplicatedException(caregivingRoundId)
        }
        return ResponseEntity.noContent().build()
    }

    private fun CaregivingChargeEditingRequest.intoCommand(subject: Subject) =
        CaregivingChargeEditingCommand(
            additionalHoursCharge = this.additionalHoursCharge,
            mealCost = this.mealCost,
            transportationFee = this.transportationFee,
            holidayCharge = this.holidayCharge,
            caregiverInsuranceFee = this.caregiverInsuranceFee,
            commissionFee = this.commissionFee,
            vacationCharge = this.vacationCharge,
            patientConditionCharge = this.patientConditionCharge,
            covid19TestingCost = this.covid19TestingCost,
            outstandingAmount = this.outstandingAmount,
            additionalCharges = this.additionalCharges.intoCommandData(),
            isCancelAfterArrived = this.isCancelAfterArrived,
            expectedSettlementDate = this.expectedSettlementDate,
            caregivingChargeConfirmStatus = this.caregivingChargeConfirmStatus,
            subject = subject,
        )

    private fun Collection<CaregivingChargeEditingRequest.AdditionalCharge>.intoCommandData() =
        this.map { it.intoCommandData() }

    private fun CaregivingChargeEditingRequest.AdditionalCharge.intoCommandData() =
        CaregivingCharge.AdditionalCharge(
            name = name,
            amount = amount,
        )

    @GetMapping("/{caregiving-round-id}/caregiving-charge")
    fun getCaregivingCharge(
        @PathVariable("caregiving-round-id") caregivingRoundId: String,
        subject: Subject,
    ) = ResponseEntity.ok(
        caregivingChargeByCaregivingRoundIdQueryHandler.getCaregivingCharge(
            CaregivingChargeByCaregivingRoundIdQuery(
                caregivingRoundId = caregivingRoundId,
                subject = subject,
            ),
        ).intoDetailResponse()
    )

    private fun CaregivingCharge.intoDetailResponse() = DetailCaregivingChargeResponse(
        id = this.id,
        caregivingRoundInfo = this.caregivingRoundInfo.let { caregivingRoundInfo ->
            DetailCaregivingChargeResponse.CaregivingRoundInfo(
                caregivingRoundId = caregivingRoundInfo.caregivingRoundId,
                caregivingRoundNumber = caregivingRoundInfo.caregivingRoundNumber,
                startDateTime = caregivingRoundInfo.startDateTime.intoUtcOffsetDateTime(),
                endDateTime = caregivingRoundInfo.endDateTime.intoUtcOffsetDateTime(),
                dailyCaregivingCharge = caregivingRoundInfo.dailyCaregivingCharge,
                receptionId = caregivingRoundInfo.receptionId,
            )
        },
        additionalHoursCharge = this.additionalHoursCharge,
        mealCost = this.mealCost,
        transportationFee = this.transportationFee,
        holidayCharge = this.holidayCharge,
        caregiverInsuranceFee = this.caregiverInsuranceFee,
        commissionFee = this.commissionFee,
        vacationCharge = this.vacationCharge,
        patientConditionCharge = this.patientConditionCharge,
        covid19TestingCost = this.covid19TestingCost,
        outstandingAmount = this.outstandingAmount,
        additionalCharges = this.additionalCharges.intoResponseDataSet(),
        isCancelAfterArrived = this.isCancelAfterArrived,
        caregivingChargeConfirmStatus = this.caregivingChargeConfirmStatus,
        basicAmount = this.basicAmount,
        additionalAmount = this.additionalAmount,
        totalAmount = this.totalAmount,
        expectedSettlementDate = this.expectedSettlementDate,
    )

    private fun Collection<CaregivingCharge.AdditionalCharge>.intoResponseDataSet() =
        this.map { it.intoResponseDataSet() }

    private fun CaregivingCharge.AdditionalCharge.intoResponseDataSet() =
        DetailCaregivingChargeResponse.AdditionalCharge(
            name = name,
            amount = amount,
        )

    @GetMapping("/{caregiving-round-id}")
    fun getCaregivingRound(
        @PathVariable("caregiving-round-id") caregivingRoundId: String,
        subject: Subject,
    ): ResponseEntity<DetailCaregivingRoundResponse> {
        val caregivingRound = caregivingRoundByIdQueryHandler.getCaregivingRound(
            CaregivingRoundByIdQuery(
                caregivingRoundId = caregivingRoundId,
                subject = subject,
            )
        )

        val reception = receptionByIdQueryHandler.getReception(
            ReceptionByIdQuery(
                caregivingRound.receptionInfo.receptionId,
                subject,
            )
        )

        return ResponseEntity.ok(CaregivingRoundResponseConverter.intoDetailResponse(reception, caregivingRound))
    }

    @ExceptionHandler(CaregivingChargeDuplicatedException::class)
    fun handleCaregivingChargeDuplicatedException(e: CaregivingChargeDuplicatedException) =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(
                GeneralErrorResponse(
                    message = "중복된 간병비 산정이 존재합니다.",
                    errorType = "DUPLICATE_CAREGIVING_CHARGE",
                    data = DuplicatedCaregivingChargeData(
                        caregivingRoundId = e.caregivingRoundId
                    ),
                )
            )

    @ExceptionHandler(InvalidCaregivingProgressingStatusTransitionException::class)
    fun handleInvalidCaregivingProgressingStatusTransitionException(e: InvalidCaregivingProgressingStatusTransitionException) =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(
                GeneralErrorResponse(
                    message = "지정한 상태로 간병를 변경할 수 없습니다.",
                    errorType = "INVALID_CAREGIVING_STATE_TRANSITION",
                    data = InvalidCaregivingProgressingStatusEnteredData(
                        currentCaregivingProgressingStatus = e.currentProgressingStatus,
                        enteredCaregivingProgressingStatus = e.enteredProgressingStatus,
                    ),
                )
            )

    @ExceptionHandler(InvalidCaregivingChargeActionableStatusException::class)
    fun handleInvalidCaregivingChargeActionableStatusException(e: InvalidCaregivingChargeActionableStatusException) =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(
                GeneralErrorResponse(
                    message = "해당 간병 회차의 간병비 산정의 등록 및 수정을 처리할 수 없는 상태입니다.",
                    errorType = "INVALID_CAREGIVING_CHARGE_ACTIONABLE_STATUS",
                    data = InvalidCaregivingChargeActionableStatusEnteredData(
                        currentCaregivingProgressingStatus = e.currentProgressingStatus
                    ),
                )
            )

    @ExceptionHandler(InvalidCaregivingChargeConfirmStatusTransitionException::class)
    fun handleInvalidCaregivingChargeConfirmStatusTransitionException(e: InvalidCaregivingChargeConfirmStatusTransitionException) =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(
                GeneralErrorResponse(
                    message = "지정한 상태로 간병비 확정 상태를 변경할 수 없습니다.",
                    errorType = "INVALID_CAREGIVING_CHARGE_CONFIRM_STATUS_TRANSITION",
                    data = InvalidCaregivingChargeConfirmStatusTransitionException(
                        currentCaregivingChargeConfirmStatus = e.currentCaregivingChargeConfirmStatus,
                        enteredCaregivingChargeConfirmStatus = e.enteredCaregivingChargeConfirmStatus,
                    ),
                )
            )

    @ExceptionHandler(CaregivingChargeEditingDeniedException::class)
    fun handleCaregivingChargeEditingDeniedException(e: CaregivingChargeEditingDeniedException) =
        ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(
                GeneralErrorResponse(
                    message = "간병비 산정은 확정된 상태에서는 수정 처리를 할 수 없습니다.",
                    errorType = "DENIED_EDITING_CAREGIVING_CHARGE",
                    data = DeniedEditingCaregivingChargeData(
                        caregivingChargeId = e.caregivingChargeId
                    ),
                )
            )

    @ExceptionHandler(CaregivingAdditionalChargeNameDuplicatedException::class)
    fun handleCaregivingAdditionalChargeNameDuplicated(e: CaregivingAdditionalChargeNameDuplicatedException) =
        ResponseEntity.badRequest()
            .body(
                GeneralErrorResponse(
                    message = "중복된 간병비 기타 비용 계정과목이 존재합니다.",
                    errorType = "DUPLICATED_ADDITIONAL_CHARGE_NAME",
                    data = DuplicatedAdditionalChargeNameData(
                        duplicatedAdditionalChargeName = e.duplicatedAdditionalChargeNames,
                    ),
                )
            )

    @ExceptionHandler(IllegalCaregivingPeriodEnteredException::class)
    fun handleIllegalCaregivingPeriodEnteredException(e: IllegalCaregivingPeriodEnteredException) =
        ResponseEntity.badRequest()
            .body(
                GeneralErrorResponse(
                    message = "간병 시작 일시는 현 시점이나 종료 일시보다 미래일 수 없습니다.",
                    errorType = "ILLEGAL_CAREGIVING_START_DATE",
                    data = IllegalCaregivingPeriodEnteredData(
                        targetCaregivingRoundId = e.targetCaregivingRoundId,
                        enteredStartDateTime = e.enteredStartDateTime,
                    ),
                )
            )
}
