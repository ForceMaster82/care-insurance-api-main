package kr.caredoc.careinsurance.web.agency

import jakarta.validation.Valid
import kr.caredoc.careinsurance.intoUtcOffsetDateTime
import kr.caredoc.careinsurance.patch.Patches
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerByIdQuery
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerByIdQueryHandler
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerCreationCommand
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerCreationCommandHandler
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerEditCommand
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerEditCommandHandler
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerNotExistsException
import kr.caredoc.careinsurance.user.ExternalCaregivingManagersByFilterQuery
import kr.caredoc.careinsurance.user.ExternalCaregivingManagersByFilterQueryHandler
import kr.caredoc.careinsurance.user.UserByIdQuery
import kr.caredoc.careinsurance.user.UserByIdQueryHandler
import kr.caredoc.careinsurance.user.UsersByIdsQuery
import kr.caredoc.careinsurance.user.UsersByIdsQueryHandler
import kr.caredoc.careinsurance.user.exception.ReferenceExternalCaregivingManagerNotExistsException
import kr.caredoc.careinsurance.web.agency.request.ExternalCaregivingManagerCreationRequest
import kr.caredoc.careinsurance.web.agency.request.ExternalCaregivingManagerEditRequest
import kr.caredoc.careinsurance.web.agency.request.PatchExternalCaregivingManagerRequest
import kr.caredoc.careinsurance.web.agency.response.DetailExternalCaregivingManagerResponse
import kr.caredoc.careinsurance.web.agency.response.EnteredExternalCaregivingManagerNotExists
import kr.caredoc.careinsurance.web.agency.response.ExternalCaregivingManagerResponse
import kr.caredoc.careinsurance.web.paging.PagedResponse
import kr.caredoc.careinsurance.web.paging.PagingRequest
import kr.caredoc.careinsurance.web.paging.intoPageable
import kr.caredoc.careinsurance.web.paging.intoPagedResponse
import kr.caredoc.careinsurance.web.response.GeneralErrorResponse
import kr.caredoc.careinsurance.web.search.QueryParser
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/v1/external-caregiving-managers")
class ExternalCaregivingManagerController(
    private val externalCaregivingManagerCreationCommandHandler: ExternalCaregivingManagerCreationCommandHandler,
    private val externalCaregivingManagerByIdQueryHandler: ExternalCaregivingManagerByIdQueryHandler,
    private val userIdQueryHandler: UserByIdQueryHandler,
    private val externalCaregivingManagerEditCommandHandler: ExternalCaregivingManagerEditCommandHandler,
    private val externalCaregivingManagersByFilterQueryHandler: ExternalCaregivingManagersByFilterQueryHandler,
    private val usersByIdsQueryHandler: UsersByIdsQueryHandler,
) {
    private val queryParser = QueryParser(
        mapOf(
            "name" to ExternalCaregivingManagersByFilterQuery.SearchingProperty.NAME,
            "email" to ExternalCaregivingManagersByFilterQuery.SearchingProperty.EMAIL,
        )
    )

    @PostMapping
    fun createExternalCaregivingManager(
        @Valid @RequestBody payload: ExternalCaregivingManagerCreationRequest,
        subject: Subject
    ): ResponseEntity<Unit> {
        val result = externalCaregivingManagerCreationCommandHandler.createExternalCaregivingManager(
            ExternalCaregivingManagerCreationCommand(
                externalCaregivingOrganizationId = payload.externalCaregivingOrganizationId,
                email = payload.email,
                name = payload.name,
                phoneNumber = payload.phoneNumber,
                remarks = payload.remarks,
                subject = subject,
            )
        )

        val uri = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .replacePath("/api/v1/external-caregiving-managers/${result.externalCaregivingManagerId}")
            .build()
            .toUri()
        return ResponseEntity.created(uri).build()
    }

    @PatchMapping
    fun editSuspendedExternalCaregivingManager(
        @Valid @RequestBody payload: List<PatchExternalCaregivingManagerRequest>,
        subject: Subject,
    ): ResponseEntity<Unit> {
        externalCaregivingManagerEditCommandHandler.editExternalCaregivingManagers(
            payload.associate {
                it.id to ExternalCaregivingManagerEditCommand(
                    suspended = Patches.ofValue(it.suspended),
                    subject = subject,
                )
            }
        )

        return ResponseEntity.noContent().build()
    }

    @GetMapping
    fun getExternalCaregivingManagers(
        pagingRequest: PagingRequest,
        @RequestParam(
            "external-caregiving-organization-id",
            required = false
        ) externalCaregivingOrganizationId: String?,
        @RequestParam("query", required = false) query: String?,
        subject: Subject
    ): ResponseEntity<PagedResponse<ExternalCaregivingManagerResponse>> {
        val externalCaregivingManagers = externalCaregivingManagersByFilterQueryHandler.getExternalCaregivingManagers(
            ExternalCaregivingManagersByFilterQuery(
                externalCaregivingOrganizationId = externalCaregivingOrganizationId,
                searchQuery = query?.let { queryParser.parse(it) },
                subject = subject
            ),
            pageRequest = pagingRequest.intoPageable(),
        )

        val userMap = usersByIdsQueryHandler.getUsers(
            UsersByIdsQuery(
                userIds = externalCaregivingManagers.map { it.userId }.toList(),
                subject = subject,
            )
        ).associateBy { it.id }

        return ResponseEntity.ok(
            externalCaregivingManagers.map {
                it to userMap[it.userId]
            }.map { (externalCaregivingManager, user) ->
                ExternalCaregivingManagerResponse(
                    id = externalCaregivingManager.id,
                    externalCaregivingOrganizationId = externalCaregivingManager.externalCaregivingOrganizationId,
                    email = externalCaregivingManager.email,
                    name = externalCaregivingManager.name,
                    lastLoginDateTime = user?.lastLoginDateTime?.intoUtcOffsetDateTime() ?: OffsetDateTime.MIN,
                    suspended = user?.suspended ?: true,
                )
            }.intoPagedResponse()
        )
    }

    @GetMapping("/{external-caregiving-manager-id}")
    fun getExternalCaregivingManager(
        @PathVariable("external-caregiving-manager-id") externalCaregivingManagerId: String,
        subject: Subject,
    ): ResponseEntity<DetailExternalCaregivingManagerResponse> {
        val externalCaregivingManager = externalCaregivingManagerByIdQueryHandler.getExternalCaregivingManager(
            ExternalCaregivingManagerByIdQuery(
                externalCaregivingManagerId = externalCaregivingManagerId,
                subject = subject,
            )
        )
        val user = userIdQueryHandler.getUser(UserByIdQuery(userId = externalCaregivingManager.userId))

        return ResponseEntity.ok(
            DetailExternalCaregivingManagerResponse(
                id = externalCaregivingManager.id,
                email = externalCaregivingManager.email,
                name = externalCaregivingManager.name,
                phoneNumber = externalCaregivingManager.phoneNumber,
                remarks = externalCaregivingManager.remarks,
                lastLoginDateTime = user.lastLoginDateTime.intoUtcOffsetDateTime(),
                suspended = user.suspended,
                externalCaregivingOrganizationId = externalCaregivingManager.externalCaregivingOrganizationId
            )
        )
    }

    @PutMapping("/{external-caregiving-manager-id}")
    fun editExternalCaregivingManager(
        @PathVariable("external-caregiving-manager-id") externalCaregivingManagerId: String,
        @Valid @RequestBody payload: ExternalCaregivingManagerEditRequest,
        subject: Subject,
    ): ResponseEntity<Unit> {
        externalCaregivingManagerEditCommandHandler.editExternalCaregivingManager(
            ExternalCaregivingManagerByIdQuery(
                externalCaregivingManagerId = externalCaregivingManagerId,
                subject = subject,
            ),
            ExternalCaregivingManagerEditCommand(
                email = Patches.ofValue(payload.email),
                name = Patches.ofValue(payload.name),
                phoneNumber = Patches.ofValue(payload.phoneNumber),
                remarks = Patches.ofValue(payload.remarks),
                suspended = Patches.ofValue(payload.suspended),
                externalCaregivingOrganizationId = Patches.ofValue(payload.externalCaregivingOrganizationId),
                subject = subject,
            )
        )

        return ResponseEntity.noContent().build()
    }

    @ExceptionHandler(ExternalCaregivingManagerNotExistsException::class)
    private fun handleExternalCaregivingManagerNotExists(e: ExternalCaregivingManagerNotExistsException) =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(
                GeneralErrorResponse(
                    message = "조회하고자 하는 외부 제휴사(협회) 계정이 존재하지 않습니다.",
                    errorType = "EXTERNAL_CAREGIVING_MANAGER_NOT_EXISTS",
                    data = EnteredExternalCaregivingManagerNotExists(
                        enteredExternalCaregivingManagerId = e.externalCaregivingManagerId
                    )
                )
            )

    @ExceptionHandler(ReferenceExternalCaregivingManagerNotExistsException::class)
    private fun handleReferenceExternalCaregivingManagerNotExists(e: ReferenceExternalCaregivingManagerNotExistsException) =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(
                GeneralErrorResponse(
                    message = "요청에 포함된 외부 제휴사(협회) 계정이 존재하지 않습니다.",
                    errorType = "REFERENCE_EXTERNAL_CAREGIVING_MANAGER_NOT_EXISTS",
                    data = EnteredExternalCaregivingManagerNotExists(
                        enteredExternalCaregivingManagerId = e.enteredExternalCaregivingManagerId
                    )
                )
            )
}
