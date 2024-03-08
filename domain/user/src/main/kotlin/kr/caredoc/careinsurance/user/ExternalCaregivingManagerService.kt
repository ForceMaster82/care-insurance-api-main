package kr.caredoc.careinsurance.user

import com.github.guepardoapps.kulid.ULID
import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.checkAll
import kr.caredoc.careinsurance.security.personaldata.IncludingPersonalData
import kr.caredoc.careinsurance.user.exception.ReferenceExternalCaregivingManagerNotExistsException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ExternalCaregivingManagerService(
    private val externalCaregivingManagerRepository: ExternalCaregivingManagerRepository,
    private val userCreationCommandHandler: UserCreationCommandHandler,
    private val userEditingCommandHandler: UserEditingCommandHandler,
    private val eventPublisher: ApplicationEventPublisher,
) : ExternalCaregivingManagerCreationCommandHandler,
    ExternalCaregivingManagerByUserIdQueryHandler,
    ExternalCaregivingManagerByIdQueryHandler,
    ExternalCaregivingManagerEditCommandHandler,
    ExternalCaregivingManagerByIdsQueryHandler,
    ExternalCaregivingManagersByFilterQueryHandler {
    @Transactional
    override fun createExternalCaregivingManager(@IncludingPersonalData command: ExternalCaregivingManagerCreationCommand): ExternalCaregivingManagerCreationCommandResult {
        ExternalCaregivingManagerAccessPolicy.check(command.subject, command, Object.Empty)

        val user = userCreationCommandHandler.createUser(
            UserCreationCommand(
                name = command.name,
                emailAddressForLogin = command.email,
            )
        )

        val externalCaregivingManager = command.intoEntity(user.createdUserId)

        externalCaregivingManagerRepository.save(externalCaregivingManager)

        eventPublisher.publishEvent(
            ExternalCaregivingManagerGenerated(
                externalCaregivingManagerId = externalCaregivingManager.id,
                userId = externalCaregivingManager.userId,
                grantedDateTime = Clock.now(),
                subject = command.subject,
            )
        )

        return ExternalCaregivingManagerCreationCommandResult(externalCaregivingManager.id)
    }

    @Transactional(readOnly = true)
    override fun getExternalCaregivingManager(query: ExternalCaregivingManagerByUserIdQuery): ExternalCaregivingManager {
        val externalCaregivingManager = externalCaregivingManagerRepository.findByUserId(query.userId)
            ?: throw ExternalCaregivingManagerNotFoundByUserIdException(query.userId)

        ExternalCaregivingManagerAccessPolicy.check(query.subject, query, externalCaregivingManager)
        return externalCaregivingManager
    }

    @Transactional(readOnly = true)
    override fun existExternalCaregivingManager(query: ExternalCaregivingManagerByUserIdQuery): Boolean {
        ExternalCaregivingManagerAccessPolicy.check(query.subject, query, Object.Empty)

        return externalCaregivingManagerRepository.existsByUserId(query.userId)
    }

    private fun ExternalCaregivingManagerCreationCommand.intoEntity(userId: String) = ExternalCaregivingManager(
        id = ULID.random(),
        email = this.email,
        name = this.name,
        externalCaregivingOrganizationId = this.externalCaregivingOrganizationId,
        phoneNumber = this.phoneNumber,
        remarks = this.remarks,
        userId = userId,
    )

    @Transactional(readOnly = true)
    override fun getExternalCaregivingManager(query: ExternalCaregivingManagerByIdQuery): ExternalCaregivingManager {
        val externalCaregivingManager = externalCaregivingManagerRepository.findByIdOrNull(
            query.externalCaregivingManagerId
        ) ?: throw ExternalCaregivingManagerNotExistsException(query.externalCaregivingManagerId)

        ExternalCaregivingManagerAccessPolicy.check(query.subject, query, externalCaregivingManager)
        return externalCaregivingManager
    }

    @Transactional
    override fun editExternalCaregivingManager(
        query: ExternalCaregivingManagerByIdQuery,
        @IncludingPersonalData
        command: ExternalCaregivingManagerEditCommand,
    ) {
        val externalCaregivingManager = getExternalCaregivingManager(query)
        ExternalCaregivingManagerAccessPolicy.check(command.subject, command, externalCaregivingManager)
        this.executeEditExternalCaregivingManager(externalCaregivingManager, command)
    }

    @Transactional
    override fun editExternalCaregivingManagers(
        @IncludingPersonalData
        commandByIds: Map<String, ExternalCaregivingManagerEditCommand>
    ) {
        val subject = commandByIds.values.first().subject
        ExternalCaregivingManagerAccessPolicy.check(subject, commandByIds.values.first(), Object.Empty)
        val externalCaregivingManagerList = getExternalCaregivingManagers(
            ExternalCaregivingManagerByIdsQuery(
                externalCaregivingManagerIds = commandByIds.keys,
                subject = subject,
            ),
        )
        val unableReferenceExternalCaregivingManagerId = commandByIds.keys.minus(
            externalCaregivingManagerList.map { it.id }.toSet()
        ).firstOrNull()

        if (unableReferenceExternalCaregivingManagerId != null) {
            throw ReferenceExternalCaregivingManagerNotExistsException(unableReferenceExternalCaregivingManagerId)
        }

        externalCaregivingManagerList.forEach {
            val command = commandByIds[it.id] ?: return@forEach
            executeEditExternalCaregivingManager(
                it,
                ExternalCaregivingManagerEditCommand(
                    email = command.email,
                    name = command.name,
                    phoneNumber = command.phoneNumber,
                    remarks = command.remarks,
                    suspended = command.suspended,
                    externalCaregivingOrganizationId = command.externalCaregivingOrganizationId,
                    subject = subject,
                )
            )
        }
    }

    @Transactional(readOnly = true)
    override fun getExternalCaregivingManagers(query: ExternalCaregivingManagerByIdsQuery): List<ExternalCaregivingManager> {
        val externalCaregivingManagerList = externalCaregivingManagerRepository.findByIdIn(
            query.externalCaregivingManagerIds
        )

        ExternalCaregivingManagerAccessPolicy.checkAll(query.subject, query, externalCaregivingManagerList)

        return externalCaregivingManagerList
    }

    private fun executeEditExternalCaregivingManager(
        externalCaregivingManager: ExternalCaregivingManager,
        command: ExternalCaregivingManagerEditCommand,
    ) {
        userEditingCommandHandler.editUser(
            UserByIdQuery(
                externalCaregivingManager.userId
            ),
            UserEditingCommand(
                email = command.email,
                name = command.name,
                suspended = command.suspended,
                subject = command.subject,
            )
        )

        externalCaregivingManager.edit(command)
        externalCaregivingManagerRepository.save(externalCaregivingManager)
    }

    @Transactional(readOnly = true)
    override fun getExternalCaregivingManagers(
        @IncludingPersonalData
        query: ExternalCaregivingManagersByFilterQuery,
        pageRequest: Pageable,
    ): Page<ExternalCaregivingManager> {
        ExternalCaregivingManagerAccessPolicy.check(query.subject, query, Object.Empty)

        val externalCaregivingManagers = if (query.isEmpty()) {
            getExternalCaregivingManagers(pageRequest)
        } else {
            val searchingCriteria = when (query.searchQuery?.searchingProperty) {
                ExternalCaregivingManagersByFilterQuery.SearchingProperty.NAME -> ExternalCaregivingManagerSearchingRepository.SearchingCriteria(
                    externalCaregivingOrganizationId = query.externalCaregivingOrganizationId,
                    name = query.searchQuery.keyword,
                )

                ExternalCaregivingManagersByFilterQuery.SearchingProperty.EMAIL -> ExternalCaregivingManagerSearchingRepository.SearchingCriteria(
                    externalCaregivingOrganizationId = query.externalCaregivingOrganizationId,
                    email = query.searchQuery.keyword,
                )

                null -> ExternalCaregivingManagerSearchingRepository.SearchingCriteria(
                    externalCaregivingOrganizationId = query.externalCaregivingOrganizationId,
                )
            }

            externalCaregivingManagerRepository.searchExternalCaregivingManagers(
                searchingCriteria = searchingCriteria,
                pageRequest
            )
        }

        externalCaregivingManagers.forEach {
            ExternalCaregivingManagerAccessPolicy.check(
                query.subject,
                ExternalCaregivingManagerByIdQuery(it.id, query.subject),
                it,
            )
        }

        return externalCaregivingManagers
    }

    private fun getExternalCaregivingManagers(pageRequest: Pageable): Page<ExternalCaregivingManager> {
        val pageWithSortRequest = PageRequest.of(
            pageRequest.pageNumber,
            pageRequest.pageSize,
            Sort.by(Sort.Direction.DESC, "id"),
        )
        return externalCaregivingManagerRepository.findAll(pageWithSortRequest)
    }

    private fun ExternalCaregivingManagersByFilterQuery.isEmpty() = this.searchQuery == null &&
        this.externalCaregivingOrganizationId == null
}
