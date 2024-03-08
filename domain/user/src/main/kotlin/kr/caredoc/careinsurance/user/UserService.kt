package kr.caredoc.careinsurance.user

import com.github.guepardoapps.kulid.ULID
import kr.caredoc.careinsurance.email.EmailSender
import kr.caredoc.careinsurance.security.IncludingSecuredData
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.checkAll
import kr.caredoc.careinsurance.security.password.PasswordPolicy
import kr.caredoc.careinsurance.security.personaldata.IncludingPersonalData
import kr.caredoc.careinsurance.user.exception.AlreadyExistsUserEmailAddressException
import kr.caredoc.careinsurance.user.exception.UserNotFoundByEmailAddressException
import kr.caredoc.careinsurance.user.exception.UserNotFoundByIdException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userPasswordGuidanceTemplate: NewUserPasswordGuidanceEmailTemplate,
    private val temporalPasswordGuidanceEmailTemplate: TemporalPasswordGuidanceEmailTemplate,
    private val temporalAuthenticationCodeGuidanceEmailTemplate: TemporalAuthenticationCodeGuidanceEmailTemplate,
    private val emailSender: EmailSender,
) : EmailPasswordLoginHandler,
    UserCreationCommandHandler,
    UserByIdQueryHandler,
    UsersByNameKeywordQueryHandler,
    UsersByEmailKeywordQueryHandler,
    UsersByIdsQueryHandler,
    UserEditingCommandHandler,
    UserPasswordResetCommandHandler,
    TemporalAuthenticationCodeIssuer,
    EmailAuthenticationCodeLoginHandler,
    UserByEmailQueryHandler {
    @Transactional
    override fun handleLogin(@IncludingSecuredData credential: EmailPasswordLoginCredential) =
        getUserByEmailAddress(credential.emailAddress).also {
            it.ensureUserActivated()
            it.login(credential.password)
        }

    @Transactional(readOnly = true)
    override fun getUser(query: UserByIdQuery) = userRepository.findByIdOrNull(query.userId)
        ?: throw UserNotFoundByIdException(query.userId)

    @Transactional
    override fun createUser(@IncludingPersonalData command: UserCreationCommand): UserCreationResult {
        ensureEmailAddressNotOccupied(command.emailAddressForLogin)
        val password = PasswordPolicy.generateRandomPassword()
        val newUser = command.intoEntity(password)

        userRepository.save(newUser)

        val passwordGuidanceEmail = userPasswordGuidanceTemplate.generate(
            NewUserPasswordGuidanceEmailTemplate.TemplateData(
                userName = newUser.name,
                userEmail = newUser.emailAddress,
                rawPassword = password,
            )
        )

        emailSender.send(passwordGuidanceEmail)

        return UserCreationResult(newUser.id)
    }

    private fun ensureEmailAddressNotOccupied(emailAddress: String) {
        if (userRepository.existsByCredentialEmailAddress(emailAddress)) {
            throw AlreadyExistsUserEmailAddressException(emailAddress)
        }
    }

    private fun UserCreationCommand.intoEntity(password: String) = User(
        id = ULID.random(),
        name = name,
        credential = User.EmailPasswordCredential(
            emailAddress = emailAddressForLogin,
            password = password,
        ),
    )

    @Transactional(readOnly = true)
    override fun getUsers(@IncludingPersonalData query: UsersByNameKeywordQuery): List<User> {
        UserAccessPolicy.check(query.subject, query, Object.Empty)
        return userRepository.findByNameContains(query.nameKeyword)
    }

    @Transactional(readOnly = true)
    override fun getUsers(@IncludingPersonalData query: UsersByEmailKeywordQuery): List<User> {
        UserAccessPolicy.check(query.subject, query, Object.Empty)
        return userRepository.findByCredentialEmailAddressContains(query.emailKeyword)
    }

    @Transactional(readOnly = true)
    override fun getUsers(query: UsersByIdsQuery): List<User> {
        val users = userRepository.findByIdIn(query.userIds)

        UserAccessPolicy.checkAll(query.subject, query, users)

        return users
    }

    @Transactional
    override fun editUser(
        query: UserByIdQuery,
        @IncludingSecuredData
        @IncludingPersonalData
        command: UserEditingCommand,
    ) {
        val user = getUser(query)

        UserAccessPolicy.check(command.subject, command, user)

        command.email.compareWith(user.emailAddress).ifHavingDifference { _, patchingEmail ->
            ensureEmailAddressNotOccupied(patchingEmail)
        }

        user.edit(command)
        userRepository.save(user)
    }

    @Transactional
    override fun editUsers(
        @IncludingSecuredData
        @IncludingPersonalData
        commandsById: Map<String, UserEditingCommand>
    ) {
        if (commandsById.isEmpty()) {
            return
        }

        UserAccessPolicy.check(commandsById.values.first().subject, commandsById.values.first(), Object.Empty)

        val users = getUsers(UsersByIdsQuery(commandsById.keys, commandsById.values.first().subject))

        for (user in users) {
            commandsById[user.id]?.let { user.edit(it) }
        }
    }

    @Transactional
    override fun resetPassword(query: UserByIdQuery, command: UserPasswordResetCommand) {
        val user = getUser(query)
        val result = user.resetPassword(command)

        UserAccessPolicy.check(command.subject, command, user)

        val passwordGuidanceEmail = temporalPasswordGuidanceEmailTemplate.generate(
            TemporalPasswordGuidanceEmailTemplate.TemplateData(
                userName = user.name,
                userEmail = user.emailAddress,
                temporalRawPassword = result.newPassword,
            )
        )

        emailSender.send(passwordGuidanceEmail)
    }

    @Transactional
    override fun issueTemporalAuthenticationCode(query: UserByIdQuery) {
        val user = getUser(query)

        val issuingResult = user.issueTemporalAuthenticationCode()

        emailSender.send(
            temporalAuthenticationCodeGuidanceEmailTemplate.generate(
                TemporalAuthenticationCodeGuidanceEmailTemplate.TemplateData(
                    userEmail = user.emailAddress,
                    userName = user.name,
                    authenticationCode = issuingResult.generatedAuthenticationCode,
                )
            )
        )
    }

    @Transactional
    override fun handleLogin(
        @IncludingSecuredData
        @IncludingPersonalData
        credential: EmailAuthenticationCodeLoginCredential,
    ): User {
        val user = getUserByEmailAddress(credential.emailAddress)
        user.authenticateUsingCode(credential.authenticationCode)

        return user
    }

    private fun getUserByEmailAddress(emailAddress: String) =
        userRepository.findByCredentialEmailAddress(emailAddress)
            ?: throw UserNotFoundByEmailAddressException(emailAddress)

    @Transactional(readOnly = true)
    override fun getUser(query: UserByEmailQuery): User {
        return userRepository.findTopByCredentialEmailAddress(query.email)
            ?: throw UserNotFoundByEmailAddressException(query.email)
    }
}
