package kr.caredoc.careinsurance.web.user

import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.regions.Regions
import com.amazonaws.services.secretsmanager.AWSSecretsManager
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult
import com.amazonaws.services.secretsmanager.model.InvalidParameterException
import com.amazonaws.services.secretsmanager.model.InvalidRequestException
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kr.caredoc.careinsurance.RequiredParameterNotSuppliedException
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganization
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationByIdQuery
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationByIdQueryHandler
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationNotFoundByIdException
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationType
import kr.caredoc.careinsurance.intoUtcOffsetDateTime
import kr.caredoc.careinsurance.reception.OrganizationType
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerByUserIdQuery
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerByUserIdQueryHandler
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerNotFoundByUserIdException
import kr.caredoc.careinsurance.user.InternalCaregivingManagerByUserIdQuery
import kr.caredoc.careinsurance.user.InternalCaregivingManagerByUserIdQueryHandler
import kr.caredoc.careinsurance.user.InternalCaregivingManagerNotFoundByUserIdException
import kr.caredoc.careinsurance.user.TemporalAuthenticationCodeIssuer
import kr.caredoc.careinsurance.user.UserByEmailQuery
import kr.caredoc.careinsurance.user.UserByEmailQueryHandler
import kr.caredoc.careinsurance.user.UserByIdQuery
import kr.caredoc.careinsurance.user.UserByIdQueryHandler
import kr.caredoc.careinsurance.user.UserEditingCommand
import kr.caredoc.careinsurance.user.UserEditingCommandHandler
import kr.caredoc.careinsurance.user.UserPasswordResetCommand
import kr.caredoc.careinsurance.user.UserPasswordResetCommandHandler
import kr.caredoc.careinsurance.user.exception.UserNotFoundByEmailAddressException
import kr.caredoc.careinsurance.user.exception.UserNotFoundByIdException
import kr.caredoc.careinsurance.web.response.GeneralErrorResponse
import kr.caredoc.careinsurance.web.user.request.UserPasswordEditingRequest
import kr.caredoc.careinsurance.web.user.response.DetailUserResponse
import kr.caredoc.careinsurance.web.user.response.EnteredUserIdNotRegisteredData
import kr.caredoc.careinsurance.web.user.response.SimpleUserResponse
import org.springframework.beans.factory.annotation.Value
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
import java.nio.ByteBuffer

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userByIdQueryHandler: UserByIdQueryHandler,
    private val internalCaregivingManagerByUserIdQueryHandler: InternalCaregivingManagerByUserIdQueryHandler,
    private val externalCaregivingManagerByUserIdQueryHandler: ExternalCaregivingManagerByUserIdQueryHandler,
    private val externalCaregivingOrganizationByIdQueryHandler: ExternalCaregivingOrganizationByIdQueryHandler,
    private val userEditingCommandHandler: UserEditingCommandHandler,
    private val userPasswordResetCommandHandler: UserPasswordResetCommandHandler,
    private val temporalAuthenticationCodeIssuer: TemporalAuthenticationCodeIssuer,
    private val userByEmailQueryHandler: UserByEmailQueryHandler,
) {
    val internalUserCaregivingManager = DetailUserResponse.UserOrganization(
        organizationType = OrganizationType.INTERNAL,
        id = null,
    )

    @GetMapping
    fun getUsers(
        @RequestParam("email") email: String,
    ): ResponseEntity<List<SimpleUserResponse>> {
        val users = try {
            listOf(
                userByEmailQueryHandler.getUser(
                    UserByEmailQuery(
                        email = email,
                    )
                )
            )
        } catch (e: UserNotFoundByEmailAddressException) {
            listOf()
        }

        return ResponseEntity.ok(users.map { SimpleUserResponse(id = it.id) })
    }

    @GetMapping("/{user-id}")
    fun getUser(
        @PathVariable("user-id") userId: String,
        subject: Subject,
    ): ResponseEntity<DetailUserResponse> {
        val user = userByIdQueryHandler.getUser(UserByIdQuery(userId))
        val hasInternalCaregivingManagerRole = hasInternalCaregivingManagerRole(userId, subject)
        val externalCaregivingOrganization = findUserExternalOrganization(userId, subject)

        val response = DetailUserResponse(
            id = user.id,
            name = user.name,
            organizations = listOfNotNull(
                if (hasInternalCaregivingManagerRole) {
                    internalUserCaregivingManager
                } else {
                    null
                },
                externalCaregivingOrganization?.intoUserOrganization()
            ),
            lastLoginDateTime = user.lastLoginDateTime.intoUtcOffsetDateTime(),
        )

        return ResponseEntity.ok().body(response)
    }

    private fun ExternalCaregivingOrganization.intoUserOrganization() = DetailUserResponse.UserOrganization(
        organizationType = externalCaregivingOrganizationType.intoOrganizationType(),
        id = id,
    )

    private fun ExternalCaregivingOrganizationType.intoOrganizationType() = when (this) {
        ExternalCaregivingOrganizationType.ORGANIZATION -> OrganizationType.ORGANIZATION
        ExternalCaregivingOrganizationType.AFFILIATED -> OrganizationType.AFFILIATED
    }

    private fun findUserExternalOrganization(userId: String, subject: Subject) = try {
        externalCaregivingManagerByUserIdQueryHandler.getExternalCaregivingManager(
            ExternalCaregivingManagerByUserIdQuery(userId, subject)
        )
    } catch (e: ExternalCaregivingManagerNotFoundByUserIdException) {
        null
    }?.let {
        try {
            externalCaregivingOrganizationByIdQueryHandler.getExternalCaregivingOrganization(
                ExternalCaregivingOrganizationByIdQuery(it.externalCaregivingOrganizationId, subject)
            )
        } catch (e: ExternalCaregivingOrganizationNotFoundByIdException) {
            null
        }
    }

    fun hasInternalCaregivingManagerRole(userId: String, subject: Subject) = try {
        internalCaregivingManagerByUserIdQueryHandler.getInternalCaregivingManager(
            InternalCaregivingManagerByUserIdQuery(
                userId = userId,
                subject = subject,
            )
        )

        true
    } catch (e: InternalCaregivingManagerNotFoundByUserIdException) {
        false
    }

    @PutMapping("/{user-id}/password")
    fun editUserPassword(
        @PathVariable("user-id") userId: String,
        @RequestBody payload: UserPasswordEditingRequest,
        subject: Subject,
    ): ResponseEntity<Unit> {
        if (payload.currentPassword == null) {
            userPasswordResetCommandHandler.resetPassword(
                query = UserByIdQuery(userId),
                command = UserPasswordResetCommand(subject),
            )
        } else {
            userEditingCommandHandler.editUser(
                query = UserByIdQuery(userId),
                command = UserEditingCommand(
                    passwordModification = UserEditingCommand.PasswordModification(
                        currentPassword = payload.currentPassword,
                        newPassword = payload.password ?: throw RequiredParameterNotSuppliedException(),
                    ),
                    subject = subject
                )
            )
        }

        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{user-id}/authentication-code")
    fun issueAuthenticationCode(
        @PathVariable("user-id") userId: String,
    ): ResponseEntity<Unit> {
        temporalAuthenticationCodeIssuer.issueTemporalAuthenticationCode(
            UserByIdQuery(
                userId = userId,
            )
        )
        return ResponseEntity.noContent().build()
    }

    @ExceptionHandler(UserNotFoundByIdException::class)
    fun handleUserNotFoundByIdException(e: UserNotFoundByIdException) = ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            GeneralErrorResponse(
                message = "조회하고자 하는 사용자가 존재하지 않습니다.",
                errorType = "USER_NOT_EXISTS",
                data = EnteredUserIdNotRegisteredData(
                    enteredUserId = e.userId
                )
            )
        )
}
