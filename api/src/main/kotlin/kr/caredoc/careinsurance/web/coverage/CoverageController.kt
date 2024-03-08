package kr.caredoc.careinsurance.web.coverage

import kr.caredoc.careinsurance.coverage.AllCoveragesQuery
import kr.caredoc.careinsurance.coverage.AllCoveragesQueryHandler
import kr.caredoc.careinsurance.coverage.AnnualCoverageDuplicatedException
import kr.caredoc.careinsurance.coverage.Coverage
import kr.caredoc.careinsurance.coverage.CoverageByIdQuery
import kr.caredoc.careinsurance.coverage.CoverageByIdQueryHandler
import kr.caredoc.careinsurance.coverage.CoverageCreationCommand
import kr.caredoc.careinsurance.coverage.CoverageCreationCommandHandler
import kr.caredoc.careinsurance.coverage.CoverageCreationResult
import kr.caredoc.careinsurance.coverage.CoverageEditingCommand
import kr.caredoc.careinsurance.coverage.CoverageEditingCommandHandler
import kr.caredoc.careinsurance.coverage.CoverageNameDuplicatedException
import kr.caredoc.careinsurance.coverage.CoverageNotFoundByIdException
import kr.caredoc.careinsurance.coverage.CoveragesBySearchConditionQuery
import kr.caredoc.careinsurance.coverage.CoveragesBySearchConditionQueryHandler
import kr.caredoc.careinsurance.coverage.IllegalRenewalTypeEnteredException
import kr.caredoc.careinsurance.coverage.SubscriptionYearDuplicatedException
import kr.caredoc.careinsurance.intoUtcOffsetDateTime
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.web.coverage.request.CoverageCreationRequest
import kr.caredoc.careinsurance.web.coverage.request.CoverageEditingRequest
import kr.caredoc.careinsurance.web.coverage.response.DetailCoverageResponse
import kr.caredoc.careinsurance.web.coverage.response.DuplicatedAccidentYearData
import kr.caredoc.careinsurance.web.coverage.response.IllegalRenewalTypeEnteredData
import kr.caredoc.careinsurance.web.coverage.response.NotExistingCoverageIdData
import kr.caredoc.careinsurance.web.coverage.response.SimpleCoverageResponse
import kr.caredoc.careinsurance.web.paging.PagingRequest
import kr.caredoc.careinsurance.web.paging.intoPageable
import kr.caredoc.careinsurance.web.paging.intoPagedResponse
import kr.caredoc.careinsurance.web.response.GeneralErrorResponse
import kr.caredoc.careinsurance.web.search.QueryParser
import kr.caredoc.careinsurance.withSort
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
@RequestMapping("/api/v1/coverages")
class CoverageController(
    private val allCoveragesQueryHandler: AllCoveragesQueryHandler,
    private val coverageByIdQueryHandler: CoverageByIdQueryHandler,
    private val coverageCreationCommandHandler: CoverageCreationCommandHandler,
    private val coverageEditingCommandHandler: CoverageEditingCommandHandler,
    private val coveragesBySearchConditionQueryHandler: CoveragesBySearchConditionQueryHandler,
) {
    private val queryParser = QueryParser(
        mapOf(
            "name" to CoveragesBySearchConditionQuery.SearchingProperty.NAME,
        )
    )

    @GetMapping
    fun getCoverages(
        @RequestParam("query") query: String?,
        pagingRequest: PagingRequest,
        subject: Subject,
    ) = ResponseEntity.ok(
        if (query == null) {
            allCoveragesQueryHandler.getCoverages(
                AllCoveragesQuery(subject),
                pagingRequest.intoPageable().withSort(Sort.by(Sort.Direction.DESC, "id")),
            )
        } else {
            coveragesBySearchConditionQueryHandler.getCoverages(
                CoveragesBySearchConditionQuery(
                    searchCondition = queryParser.parse(query),
                    subject = subject,
                ),
                pagingRequest.intoPageable().withSort(Sort.by(Sort.Direction.DESC, "id")),
            )
        }.map { it.intoSimpleResponse() }
            .intoPagedResponse()
    )

    @GetMapping("/{coverage-id}")
    fun getCoverage(
        @PathVariable("coverage-id") coverageId: String,
        subject: Subject,
    ) = coverageByIdQueryHandler.getCoverage(
        CoverageByIdQuery(
            coverageId = coverageId,
            subject = subject,
        ),
    ) { coverage ->
        coverage.intoDetailResponse()
    }.let { coverageResponse ->
        ResponseEntity.ok(coverageResponse)
    }

    private fun Coverage.intoDetailResponse() = DetailCoverageResponse(
        id = this.id,
        name = this.name,
        targetSubscriptionYear = this.targetSubscriptionYear,
        renewalType = this.renewalType,
        annualCoveredCaregivingCharges = this.annualCoveredCaregivingCharges.intoResponseDataSet(),
        lastModifiedDateTime = this.lastModifiedDateTime.intoUtcOffsetDateTime(),
    )

    private fun Collection<Coverage.AnnualCoveredCaregivingCharge>.intoResponseDataSet() =
        this.map { it.intoResponseDataSet() }.sortedByDescending { it.targetAccidentYear }

    private fun Coverage.AnnualCoveredCaregivingCharge.intoResponseDataSet() =
        DetailCoverageResponse.AnnualCoveredCaregivingCharge(
            targetAccidentYear = targetAccidentYear,
            caregivingCharge = caregivingCharge,
        )

    private fun Coverage.intoSimpleResponse() = SimpleCoverageResponse(
        id = this.id,
        name = this.name,
        targetSubscriptionYear = this.targetSubscriptionYear,
        renewalType = this.renewalType,
        lastModifiedDateTime = this.lastModifiedDateTime.intoUtcOffsetDateTime(),
    )

    @PostMapping
    fun createCoverage(
        @RequestBody payload: CoverageCreationRequest,
        subject: Subject,
    ) = coverageCreationCommandHandler.createCoverage(
        payload.intoCommand(subject)
    ).intoResponse()

    private fun CoverageCreationResult.intoResponse() =
        ResponseEntity.created(this.toLocationHeader()).build<Unit>()

    private fun CoverageCreationResult.toLocationHeader() =
        ServletUriComponentsBuilder
            .fromCurrentRequest()
            .replacePath("/api/v1/coverages/${this.createdCoverageId}")
            .build()
            .toUri()

    private fun CoverageCreationRequest.intoCommand(subject: Subject) =
        CoverageCreationCommand(
            name = name,
            targetSubscriptionYear = targetSubscriptionYear,
            renewalType = renewalType,
            annualCoveredCaregivingCharges = annualCoveredCaregivingCharges.intoCommandData(),
            subject = subject,
        )

    private fun Collection<CoverageCreationRequest.AnnualCoveredCaregivingCharge>.intoCommandData() =
        this.map { it.intoCommandData() }

    private fun CoverageCreationRequest.AnnualCoveredCaregivingCharge.intoCommandData() =
        Coverage.AnnualCoveredCaregivingCharge(
            targetAccidentYear = targetAccidentYear,
            caregivingCharge = caregivingCharge,
        )

    @ExceptionHandler(IllegalRenewalTypeEnteredException::class)
    fun handleIllegalRenewalTypeEnteredException(e: IllegalRenewalTypeEnteredException) =
        ResponseEntity.badRequest()
            .body(
                GeneralErrorResponse(
                    message = "허용되지 않은 갱신 구분입니다.",
                    errorType = "ILLEGAL_RENEWAL_TYPE",
                    data = IllegalRenewalTypeEnteredData(
                        enteredRenewalType = e.enteredRenewalType,
                    )
                )
            )

    @PutMapping("/{coverage-id}")
    fun editCoverage(
        @PathVariable("coverage-id") coverageId: String,
        @RequestBody payload: CoverageEditingRequest,
        subject: Subject,
    ): ResponseEntity<Unit> {
        coverageEditingCommandHandler.editCoverage(
            payload.intoCommand(
                coverageId = coverageId,
                subject = subject
            )
        )
        return ResponseEntity.noContent().build()
    }

    private fun CoverageEditingRequest.intoCommand(coverageId: String, subject: Subject) = CoverageEditingCommand(
        coverageId = coverageId,
        name = name,
        targetSubscriptionYear = targetSubscriptionYear,
        annualCoveredCaregivingCharges = annualCoveredCaregivingCharges.intoCommandDataSet(),
        subject = subject,
    )

    private fun Collection<CoverageEditingRequest.AnnualCoveredCaregivingCharge>.intoCommandDataSet() =
        this.map { it.intoCommandDataSet() }

    private fun CoverageEditingRequest.AnnualCoveredCaregivingCharge.intoCommandDataSet() =
        Coverage.AnnualCoveredCaregivingCharge(
            targetAccidentYear = targetAccidentYear,
            caregivingCharge = caregivingCharge,
        )

    @ExceptionHandler(AnnualCoverageDuplicatedException::class)
    fun handleAnnualCoverageDuplicated(e: AnnualCoverageDuplicatedException) =
        ResponseEntity.badRequest()
            .body(
                GeneralErrorResponse(
                    message = "중복된 기준일자가 존재합니다.",
                    errorType = "DUPLICATED_ACCIDENT_YEAR",
                    data = DuplicatedAccidentYearData(
                        duplicatedAccidentYear = e.duplicatedYears,
                    )
                )
            )

    @ExceptionHandler(CoverageNotFoundByIdException::class)
    fun handleCoverageNotFoundByIdException(e: CoverageNotFoundByIdException) =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(
                GeneralErrorResponse(
                    message = "가입 담보를 찾을 수 없습니다.",
                    errorType = "COVERAGE_NOT_EXISTS",
                    data = NotExistingCoverageIdData(
                        enteredCoverageId = e.coverageId,
                    )
                )
            )

    @ExceptionHandler(SubscriptionYearDuplicatedException::class)
    fun handleSubscriptionYearDuplicated(e: SubscriptionYearDuplicatedException) =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(
                GeneralErrorResponse(
                    message = "동일한 연도에 이미 가입 담보가 등록되어 있습니다.",
                    errorType = "ACCIDENT_YEAR_ALREADY_REGISTERED",
                    data = Unit,
                )
            )

    @ExceptionHandler(CoverageNameDuplicatedException::class)
    fun handleCoverageNameDuplicated(e: CoverageNameDuplicatedException) =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(
                GeneralErrorResponse(
                    message = "동일한 이름을 가입 담보가 이미 등록되어 있습니다.",
                    errorType = "NAME_ALREADY_REGISTERED",
                    data = Unit,
                )
            )
}
