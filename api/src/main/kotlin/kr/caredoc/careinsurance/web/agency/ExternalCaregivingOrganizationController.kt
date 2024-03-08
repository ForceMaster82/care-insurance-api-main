package kr.caredoc.careinsurance.web.agency

import kr.caredoc.careinsurance.account.AccountInfo
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganization
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationByIdQuery
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationByIdQueryHandler
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationCreationCommand
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationCreationCommandHandler
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationCreationResult
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationEditingCommand
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationEditingCommandHandler
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationNotFoundByIdException
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationType
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationsByFilterQuery
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationsByFilterQueryHandler
import kr.caredoc.careinsurance.file.FileByUrlQuery
import kr.caredoc.careinsurance.file.GeneratingOpenedFileUrlCommand
import kr.caredoc.careinsurance.file.GeneratingOpenedFileUrlCommandHandler
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.web.agency.request.ExternalCaregivingOrganizationCreationRequest
import kr.caredoc.careinsurance.web.agency.request.ExternalCaregivingOrganizationEditingRequest
import kr.caredoc.careinsurance.web.agency.response.DetailExternalCaregivingOrganizationResponse
import kr.caredoc.careinsurance.web.agency.response.EnteredExternalCaregivingOrganizationIdNotRegisteredData
import kr.caredoc.careinsurance.web.agency.response.SimpleExternalCaregivingOrganizationResponse
import kr.caredoc.careinsurance.web.paging.PagingRequest
import kr.caredoc.careinsurance.web.paging.intoPageable
import kr.caredoc.careinsurance.web.paging.intoPagedResponse
import kr.caredoc.careinsurance.web.response.GeneralErrorResponse
import kr.caredoc.careinsurance.web.search.QueryParser
import org.springframework.http.ContentDisposition
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
import java.net.URLEncoder
import java.time.Duration

@RestController
@RequestMapping("/api/v1/external-caregiving-organizations")
class ExternalCaregivingOrganizationController(
    private val externalCaregivingOrganizationCreationCommandHandler: ExternalCaregivingOrganizationCreationCommandHandler,
    private val externalCaregivingOrganizationsByFilterQueryHandler: ExternalCaregivingOrganizationsByFilterQueryHandler,
    private val externalCaregivingOrganizationByIdQueryHandler: ExternalCaregivingOrganizationByIdQueryHandler,
    private val externalCaregivingOrganizationEditingCommandHandler: ExternalCaregivingOrganizationEditingCommandHandler,
    private val generatingOpenedFileUrlCommandHandler: GeneratingOpenedFileUrlCommandHandler,
) {
    private val queryParser = QueryParser(
        mapOf(
            "externalCaregivingOrganizationName" to ExternalCaregivingOrganizationsByFilterQuery.SearchingProperty
                .EXTERNAL_CAREGIVING_ORGANIZATION_NAME,
        )
    )

    @GetMapping
    fun getExternalCaregivingOrganizations(
        pagingRequest: PagingRequest,
        @RequestParam("query", required = false) query: String?,
        @RequestParam("external-caregiving-organization-type", required = false)
        organizationType: ExternalCaregivingOrganizationType?,
        subject: Subject
    ) = ResponseEntity.ok(
        externalCaregivingOrganizationsByFilterQueryHandler.getExternalCaregivingOrganizations(
            ExternalCaregivingOrganizationsByFilterQuery(
                searchCondition = query?.let { queryParser.parse(it) },
                organizationType = organizationType,
                subject = subject,
            ),
            pageRequest = pagingRequest.intoPageable(),
        ).map {
            it.intoSimpleResponse()
        }.intoPagedResponse()
    )

    @GetMapping("/{external-caregiving-organization-id}")
    fun getExternalCaregivingOrganization(
        @PathVariable("external-caregiving-organization-id") externalCaregivingOrganizationId: String,
        subject: Subject,
    ) = ResponseEntity.ok(
        externalCaregivingOrganizationByIdQueryHandler.getExternalCaregivingOrganization(
            ExternalCaregivingOrganizationByIdQuery(
                id = externalCaregivingOrganizationId,
                subject = subject,
            )
        ).intoDetailResponse()
    )

    fun ExternalCaregivingOrganization.intoDetailResponse() = DetailExternalCaregivingOrganizationResponse(
        id = this.id,
        name = this.name,
        externalCaregivingOrganizationType = this.externalCaregivingOrganizationType,
        address = this.address,
        contractName = this.contractName,
        phoneNumber = this.phoneNumber,
        profitAllocationRatio = this.profitAllocationRatio,
        businessLicenseFileName = this.businessLicenseFileName,
        businessLicenseFileUrl = generateOpenedBusinessLicenseDownloadUrl(this),
        accountInfo = this.accountInfo?.let { accountInfo ->
            DetailExternalCaregivingOrganizationResponse.AccountInfo(
                bank = accountInfo.bank,
                accountNumber = accountInfo.accountNumber,
                accountHolder = accountInfo.accountHolder,
            )
        } ?: DetailExternalCaregivingOrganizationResponse.AccountInfo(
            bank = null,
            accountNumber = null,
            accountHolder = null,
        )
    )

    private fun generateOpenedBusinessLicenseDownloadUrl(organization: ExternalCaregivingOrganization): String? {
        val businessLicenseFileName = organization.businessLicenseFileName ?: return null
        val businessLicenseFileUrl = organization.businessLicenseFileUrl ?: return null

        return generateOpenedAttachmentUrl(businessLicenseFileUrl, businessLicenseFileName)
    }

    private fun generateOpenedAttachmentUrl(url: String, fileName: String) =
        generatingOpenedFileUrlCommandHandler.generateOpenedUrl(
            query = FileByUrlQuery(url),
            command = GeneratingOpenedFileUrlCommand(
                duration = Duration.ofSeconds(30),
                contentDisposition = ContentDisposition.attachment()
                    .filename(
                        URLEncoder.encode(fileName, "UTF-8").replace("+", "%20")
                    ).build()
            ),
        )?.url

    fun ExternalCaregivingOrganization.intoSimpleResponse() = SimpleExternalCaregivingOrganizationResponse(
        id = this.id,
        name = this.name,
        externalCaregivingOrganizationType = this.externalCaregivingOrganizationType,
    )

    @PostMapping
    fun createExternalCaregivingOrganization(
        @RequestBody payload: ExternalCaregivingOrganizationCreationRequest,
        subject: Subject
    ) = externalCaregivingOrganizationCreationCommandHandler.createExternalCaregivingOrganization(
        payload.intoCommand(subject)
    ).intoResponse()

    private fun ExternalCaregivingOrganizationCreationResult.intoResponse() =
        ResponseEntity.created(this.intoLocationHeader()).build<Unit>()

    private fun ExternalCaregivingOrganizationCreationResult.intoLocationHeader() =
        ServletUriComponentsBuilder
            .fromCurrentRequest()
            .replacePath("/api/v1/external-caregiving-organizations/${this.createdExternalCaregivingOrganizationId}")
            .build()
            .toUri()

    private fun ExternalCaregivingOrganizationCreationRequest.intoCommand(subject: Subject) =
        ExternalCaregivingOrganizationCreationCommand(
            name = name,
            externalCaregivingOrganizationType = externalCaregivingOrganizationType,
            address = address,
            contractName = contractName,
            phoneNumber = phoneNumber,
            profitAllocationRatio = profitAllocationRatio,
            accountInfo = accountInfo.intoCommandData(),
            subject = subject,
        )

    private fun ExternalCaregivingOrganizationCreationRequest.AccountInfo.intoCommandData() =
        AccountInfo(
            bank = bank,
            accountNumber = accountNumber,
            accountHolder = accountHolder,
        )

    @ExceptionHandler(ExternalCaregivingOrganizationNotFoundByIdException::class)
    fun handleExternalCaregivingOrganizationNotFoundByIdException(e: ExternalCaregivingOrganizationNotFoundByIdException) =
        ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                GeneralErrorResponse(
                    message = "조회하고자 하는 외부 간병 협회가 존재하지 않습니다.",
                    errorType = "EXTERNAL_CAREGIVING_ORGANIZATION_NOT_EXISTS",
                    data = EnteredExternalCaregivingOrganizationIdNotRegisteredData(
                        enteredExternalCaregivingOrganizationId = e.externalCaregivingOrganizationId
                    ),
                )
            )

    @PutMapping("/{external-caregiving-organization-id}")
    fun editExternalCaregivingOrganization(
        @PathVariable("external-caregiving-organization-id") externalCaregivingOrganizationId: String,
        @RequestBody payload: ExternalCaregivingOrganizationEditingRequest,
        subject: Subject,
    ): ResponseEntity<Unit> {
        externalCaregivingOrganizationEditingCommandHandler.editExternalCaregivingOrganization(
            payload.intoCommand(
                externalCaregivingOrganizationId = externalCaregivingOrganizationId,
                subject = subject,
            )
        )
        return ResponseEntity.noContent().build()
    }

    private fun ExternalCaregivingOrganizationEditingRequest.intoCommand(
        externalCaregivingOrganizationId: String,
        subject: Subject
    ) = ExternalCaregivingOrganizationEditingCommand(
        externalCaregivingOrganizationId = externalCaregivingOrganizationId,
        name = name,
        externalCaregivingOrganizationType = externalCaregivingOrganizationType,
        address = address,
        contractName = contractName,
        phoneNumber = phoneNumber,
        profitAllocationRatio = profitAllocationRatio,
        accountInfo = accountInfo.intoCommandDataSet(),
        subject = subject,
    )

    private fun ExternalCaregivingOrganizationEditingRequest.AccountInfo.intoCommandDataSet() =
        AccountInfo(
            bank = bank,
            accountNumber = accountNumber,
            accountHolder = accountHolder,
        )
}
