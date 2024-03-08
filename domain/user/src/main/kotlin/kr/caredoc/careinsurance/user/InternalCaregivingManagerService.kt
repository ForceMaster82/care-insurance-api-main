package kr.caredoc.careinsurance.user

import com.github.guepardoapps.kulid.ULID
import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.personaldata.IncludingPersonalData
import kr.caredoc.careinsurance.user.exception.InternalCaregivingManagerNotFoundByIdException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InternalCaregivingManagerService(
    private val internalCaregivingManagerRepository: InternalCaregivingManagerRepository,
    private val userCreationCommandHandler: UserCreationCommandHandler,
    private val usersByEmailKeywordQueryHandler: UsersByEmailKeywordQueryHandler,
    private val userEditingCommandHandler: UserEditingCommandHandler,
    private val eventPublisher: ApplicationEventPublisher,
) : InternalCaregivingManagerCreationCommandHandler,
    InternalCaregivingManagerByUserIdQueryHandler,
    AllInternalCaregivingManagersQueryHandler,
    InternalCaregivingManagersBySearchConditionQueryHandler,
    InternalCaregivingManagerByIdQueryHandler,
    InternalCaregivingManagerEditingCommandHandler {

    @Transactional
    override fun createInternalCaregivingManager(@IncludingPersonalData command: InternalCaregivingManagerCreationCommand): InternalCaregivingManagerCreationResult {
        InternalCaregivingManagerAccessPolicy.check(command.subject, command, Object.Empty)
        val newInternalCaregivingManager = command.intoEntity(
            createUser(
                command.name,
                command.email,
            )
        )
        internalCaregivingManagerRepository.save(newInternalCaregivingManager)

        eventPublisher.publishEvent(
            InternalCaregivingManagerGenerated(
                internalCaregivingManagerId = newInternalCaregivingManager.id,
                userId = newInternalCaregivingManager.userId,
                grantedDateTime = Clock.now(),
                subject = command.subject,
            )
        )

        return InternalCaregivingManagerCreationResult(newInternalCaregivingManager.id)
    }

    private fun createUser(name: String, emailAddress: String): String {
        return userCreationCommandHandler.createUser(
            UserCreationCommand(
                name,
                emailAddress,
            )
        ).createdUserId
    }

    private fun InternalCaregivingManagerCreationCommand.intoEntity(createdUserId: String): InternalCaregivingManager {
        return InternalCaregivingManager(
            id = ULID.random(),
            userId = createdUserId,
            name = this.name,
            nickname = this.nickname,
            phoneNumber = this.phoneNumber,
            role = this.role,
            remarks = this.remarks,
        )
    }

    @Transactional(readOnly = true)
    override fun getInternalCaregivingManager(query: InternalCaregivingManagerByUserIdQuery): InternalCaregivingManager {
        val internalCaregivingManager = internalCaregivingManagerRepository.findByUserId(query.userId)
            ?: throw InternalCaregivingManagerNotFoundByUserIdException(query.userId)

        InternalCaregivingManagerAccessPolicy.check(query.subject, query, internalCaregivingManager)

        return internalCaregivingManager
    }

    @Transactional(readOnly = true)
    override fun existInternalCaregivingManager(query: InternalCaregivingManagerByUserIdQuery): Boolean {
        InternalCaregivingManagerAccessPolicy.check(query.subject, query, Object.Empty)

        return internalCaregivingManagerRepository.existsByUserId(query.userId)
    }

    @Transactional(readOnly = true)
    override fun getInternalCaregivingManagers(
        query: GetAllInternalCaregivingManagersQuery,
        pageRequest: Pageable,
    ): Page<InternalCaregivingManager> {
        InternalCaregivingManagerAccessPolicy.check(query.subject, query, Object.Empty)

        return internalCaregivingManagerRepository.findAll(pageRequest)
    }

    @Transactional(readOnly = true)
    override fun getInternalCaregivingManagers(
        @IncludingPersonalData
        query: InternalCaregivingManagersBySearchConditionQuery,
        pageRequest: Pageable
    ): Page<InternalCaregivingManager> {
        InternalCaregivingManagerAccessPolicy.check(query.subject, query, Object.Empty)

        return when (query.searchCondition.searchingProperty) {
            InternalCaregivingManagersBySearchConditionQuery.SearchingProperty.NAME -> {
                internalCaregivingManagerRepository.findByNameContains(query.searchCondition.keyword, pageRequest)
            }

            InternalCaregivingManagersBySearchConditionQuery.SearchingProperty.EMAIL -> {
                val users = usersByEmailKeywordQueryHandler.getUsers(
                    UsersByEmailKeywordQuery(
                        emailKeyword = query.searchCondition.keyword,
                        subject = query.subject
                    )
                )
                internalCaregivingManagerRepository.findByUserIdIn(users.map { it.id }.toSet(), pageRequest)
            }
        }
    }

    @Transactional(readOnly = true)
    override fun getInternalCaregivingManager(query: InternalCaregivingManagerByIdQuery): InternalCaregivingManager {
        val internalCaregivingManager = internalCaregivingManagerRepository.findByIdOrNull(
            query.internalCaregivingManagerId
        ) ?: throw InternalCaregivingManagerNotFoundByIdException(
            query.internalCaregivingManagerId
        )

        InternalCaregivingManagerAccessPolicy.check(query.subject, query, internalCaregivingManager)

        return internalCaregivingManager
    }

    @Transactional(readOnly = true)
    override fun ensureInternalCaregivingManagerExists(query: InternalCaregivingManagerByIdQuery) {
        getInternalCaregivingManager(query)
    }

    @Transactional
    override fun editInternalCaregivingManager(
        query: InternalCaregivingManagerByIdQuery,
        @IncludingPersonalData
        command: InternalCaregivingManagerEditingCommand,
    ) {
        val internalCaregivingManager = getInternalCaregivingManager(query)
        InternalCaregivingManagerAccessPolicy.check(command.subject, command, internalCaregivingManager)

        userEditingCommandHandler.editUser(
            UserByIdQuery(internalCaregivingManager.userId),
            UserEditingCommand(
                email = command.email,
                name = command.name,
                suspended = command.suspended,
                subject = command.subject,
            ),
        )

        internalCaregivingManager.edit(command)
        internalCaregivingManagerRepository.save(internalCaregivingManager)
    }
}
