package kr.caredoc.careinsurance.web.user

import kr.caredoc.careinsurance.intoUtcOffsetDateTime
import kr.caredoc.careinsurance.patch.Patches
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.user.AllInternalCaregivingManagersQueryHandler
import kr.caredoc.careinsurance.user.GetAllInternalCaregivingManagersQuery
import kr.caredoc.careinsurance.user.InternalCaregivingManagerByIdQuery
import kr.caredoc.careinsurance.user.InternalCaregivingManagerByIdQueryHandler
import kr.caredoc.careinsurance.user.InternalCaregivingManagerCreationCommand
import kr.caredoc.careinsurance.user.InternalCaregivingManagerCreationCommandHandler
import kr.caredoc.careinsurance.user.InternalCaregivingManagerCreationResult
import kr.caredoc.careinsurance.user.InternalCaregivingManagerEditingCommand
import kr.caredoc.careinsurance.user.InternalCaregivingManagerEditingCommandHandler
import kr.caredoc.careinsurance.user.InternalCaregivingManagersBySearchConditionQuery
import kr.caredoc.careinsurance.user.InternalCaregivingManagersBySearchConditionQueryHandler
import kr.caredoc.careinsurance.user.UserByIdQuery
import kr.caredoc.careinsurance.user.UserByIdQueryHandler
import kr.caredoc.careinsurance.user.UsersByIdsQuery
import kr.caredoc.careinsurance.user.UsersByIdsQueryHandler
import kr.caredoc.careinsurance.user.exception.AlreadyExistsUserEmailAddressException
import kr.caredoc.careinsurance.user.exception.InternalCaregivingManagerNotFoundByIdException
import kr.caredoc.careinsurance.web.paging.PagedResponse
import kr.caredoc.careinsurance.web.paging.PagingRequest
import kr.caredoc.careinsurance.web.paging.intoPageable
import kr.caredoc.careinsurance.web.paging.intoPagedResponse
import kr.caredoc.careinsurance.web.response.GeneralErrorResponse
import kr.caredoc.careinsurance.web.search.QueryParser
import kr.caredoc.careinsurance.web.user.request.InternalCaregivingManagerCreationRequest
import kr.caredoc.careinsurance.web.user.request.InternalCaregivingManagerEditingRequest
import kr.caredoc.careinsurance.web.user.request.PatchInternalCaregivingManagersRequest
import kr.caredoc.careinsurance.web.user.response.DetailCaregivingResponse
import kr.caredoc.careinsurance.web.user.response.EnteredAlreadyUserEmailAddressData
import kr.caredoc.careinsurance.web.user.response.EnteredInternalCaregivingManagerNotRegisteredData
import kr.caredoc.careinsurance.web.user.response.SimpleCaregivingResponse
import kr.caredoc.careinsurance.withSort
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
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
@RequestMapping("/api/v1/internal-caregiving-managers")
class InternalCaregivingManagerController(
    private val internalCaregivingManagerCreationCommandHandler: InternalCaregivingManagerCreationCommandHandler,
    private val allInternalCaregivingManagersQueryHandler: AllInternalCaregivingManagersQueryHandler,
    private val internalCaregivingManagersBySearchConditionQueryHandler: InternalCaregivingManagersBySearchConditionQueryHandler,
    private val usersByIdsQueryHandler: UsersByIdsQueryHandler,
    private val internalCaregivingManagerByIdQueryHandler: InternalCaregivingManagerByIdQueryHandler,
    private val userByIdQueryHandler: UserByIdQueryHandler,
    private val internalCaregivingManagerEditingCommandHandler: InternalCaregivingManagerEditingCommandHandler,
) {
    private val queryParser = QueryParser(
        mapOf(
            "name" to InternalCaregivingManagersBySearchConditionQuery.SearchingProperty.NAME,
            "email" to InternalCaregivingManagersBySearchConditionQuery.SearchingProperty.EMAIL,
        )
    )

    @GetMapping
    fun getInternalCaregivingManagers(
        pagingRequest: PagingRequest,
        @RequestParam("query", required = false) query: String?,
        subject: Subject,
    ): ResponseEntity<PagedResponse<SimpleCaregivingResponse>> {
        val caregivingManagers = getInternalCaregivingManagers(
            query = query,
            pageRequest = pagingRequest.intoPageable().withSort(Sort.by(Sort.Direction.DESC, "id")),
            subject = subject
        )

        val usersById = usersByIdsQueryHandler.getUsers(
            UsersByIdsQuery(
                userIds = caregivingManagers.map { it.userId }.toSet(),
                subject = subject,
            )
        ).associateBy { it.id }

        return ResponseEntity.ok(
            caregivingManagers.map { manager ->
                manager to usersById[manager.userId]
            }.map { (manager, user) ->
                SimpleCaregivingResponse(
                    id = manager.id,
                    userId = manager.userId,
                    email = user?.emailAddress ?: "",
                    name = manager.name,
                    nickname = manager.nickname,
                    phoneNumber = manager.phoneNumber,
                    lastLoginDateTime = user?.lastLoginDateTime?.intoUtcOffsetDateTime() ?: OffsetDateTime.MIN,
                    suspended = user?.suspended ?: true,
                )
            }.intoPagedResponse()
        )
    }

    private fun getInternalCaregivingManagers(
        query: String?,
        pageRequest: Pageable,
        subject: Subject
    ) = if (query == null) {
        allInternalCaregivingManagersQueryHandler.getInternalCaregivingManagers(
            query = GetAllInternalCaregivingManagersQuery(subject),
            pageRequest = pageRequest,
        )
    } else {
        internalCaregivingManagersBySearchConditionQueryHandler.getInternalCaregivingManagers(
            query = InternalCaregivingManagersBySearchConditionQuery(
                searchCondition = queryParser.parse(query),
                subject = subject,
            ),
            pageRequest = pageRequest,
        )
    }

    @PostMapping
    fun createInternalCaregivingManager(
        @RequestBody payload: InternalCaregivingManagerCreationRequest,
        subject: Subject,
    ) = internalCaregivingManagerCreationCommandHandler.createInternalCaregivingManager(
        payload.toCommand(subject)
    ).toResponse()

    private fun InternalCaregivingManagerCreationResult.toResponse() =
        ResponseEntity.created(this.toLocationHeader()).build<Unit>()

    private fun InternalCaregivingManagerCreationResult.toLocationHeader() =
        ServletUriComponentsBuilder
            .fromCurrentRequest()
            .replacePath("/api/v1/internal-caregiving-managers/${this.createdInternalCaregivingManagerId}")
            .build()
            .toUri()

    private fun InternalCaregivingManagerCreationRequest.toCommand(subject: Subject) =
        InternalCaregivingManagerCreationCommand(
            email = email,
            name = name,
            nickname = nickname,
            phoneNumber = phoneNumber,
            role = role,
            remarks = remarks,
            subject = subject,
        )

    @GetMapping("/{internal-caregiving-manager-id}")
    fun getInternalCaregivingManager(
        @PathVariable("internal-caregiving-manager-id") internalCaregivingManagerId: String,
        subject: Subject,
    ): ResponseEntity<DetailCaregivingResponse> {
        val internalCaregivingManager = internalCaregivingManagerByIdQueryHandler.getInternalCaregivingManager(
            InternalCaregivingManagerByIdQuery(
                internalCaregivingManagerId = internalCaregivingManagerId,
                subject = subject,
            )
        )
        val user = userByIdQueryHandler.getUser(
            UserByIdQuery(
                internalCaregivingManager.userId
            )
        )

        return ResponseEntity.ok(
            DetailCaregivingResponse(
                id = internalCaregivingManager.id,
                userId = internalCaregivingManager.userId,
                email = user.emailAddress,
                name = internalCaregivingManager.name,
                nickname = internalCaregivingManager.nickname,
                phoneNumber = internalCaregivingManager.phoneNumber,
                lastLoginDateTime = user.lastLoginDateTime.intoUtcOffsetDateTime(),
                suspended = user.suspended,
                role = internalCaregivingManager.role,
                remarks = internalCaregivingManager.remarks ?: "",
            )
        )
    }

    @PutMapping("/{internal-caregiving-manager-id}")
    fun editInternalCaregivingManager(
        @PathVariable("internal-caregiving-manager-id") internalCaregivingManagerId: String,
        @RequestBody payload: InternalCaregivingManagerEditingRequest,
        subject: Subject,
    ): ResponseEntity<Unit> {
        editInternalCaregivingManager(
            query = InternalCaregivingManagerByIdQuery(
                internalCaregivingManagerId = internalCaregivingManagerId,
                subject = subject,
            ),
            command = payload.intoEditingCommand(subject),
        )
        return ResponseEntity.noContent().build()
    }

    @PatchMapping
    fun editMultipleInternalCaregivingManagers(
        @RequestBody payload: List<PatchInternalCaregivingManagersRequest>,
        subject: Subject,
    ): ResponseEntity<Unit> {
        payload.forEach {
            editInternalCaregivingManager(
                query = InternalCaregivingManagerByIdQuery(
                    internalCaregivingManagerId = it.id,
                    subject = subject,
                ),
                command = InternalCaregivingManagerEditingCommand(
                    suspended = Patches.ofValue(it.suspended),
                    subject = subject,
                ),
            )
        }
        return ResponseEntity.noContent().build()
    }

    private fun editInternalCaregivingManager(
        query: InternalCaregivingManagerByIdQuery,
        command: InternalCaregivingManagerEditingCommand,
    ) = internalCaregivingManagerEditingCommandHandler.editInternalCaregivingManager(query = query, command = command)

    private fun InternalCaregivingManagerEditingRequest.intoEditingCommand(subject: Subject) = InternalCaregivingManagerEditingCommand(
        name = Patches.ofValue(name),
        nickname = Patches.ofValue(nickname),
        phoneNumber = Patches.ofValue(phoneNumber),
        email = Patches.ofValue(email),
        suspended = Patches.ofValue(suspended),
        role = Patches.ofValue(role),
        remarks = Patches.ofValue(remarks),
        subject = subject,
    )

    @ExceptionHandler(InternalCaregivingManagerNotFoundByIdException::class)
    fun handleInternalCaregivingManagerNotFoundByIdException(e: InternalCaregivingManagerNotFoundByIdException) =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(
                GeneralErrorResponse(
                    message = "내부 간병 관리자를 찾을 수 없습니다.",
                    errorType = "INTERNAL_CAREGIVING_MANAGER_NOT_EXISTS",
                    data = EnteredInternalCaregivingManagerNotRegisteredData(
                        enteredInternalCaregivingManagerId = e.internalCaregivingManagerId,
                    )
                )
            )

    @ExceptionHandler(AlreadyExistsUserEmailAddressException::class)
    fun handleAlreadyExistsUserEmailAddressException(e: AlreadyExistsUserEmailAddressException) =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(
                GeneralErrorResponse(
                    message = "입력한 이메일은 이미 존재하는 이메일입니다.",
                    errorType = "ALREADY_EXISTS_USER_EMAIL_ADDRESS",
                    data = EnteredAlreadyUserEmailAddressData(
                        enteredEmailAddress = e.emailAddress,
                    )
                )
            )
}
