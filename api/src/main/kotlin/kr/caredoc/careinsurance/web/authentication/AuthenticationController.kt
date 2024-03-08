package kr.caredoc.careinsurance.web.authentication

import io.jsonwebtoken.JwtParser
import jakarta.validation.Valid
import kr.caredoc.careinsurance.security.authentication.AuthenticationMethod
import kr.caredoc.careinsurance.user.EmailAuthenticationCodeLoginCredential
import kr.caredoc.careinsurance.user.EmailAuthenticationCodeLoginHandler
import kr.caredoc.careinsurance.user.EmailPasswordLoginCredential
import kr.caredoc.careinsurance.user.EmailPasswordLoginHandler
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerByUserIdQuery
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerByUserIdQueryHandler
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerNotFoundByUserIdException
import kr.caredoc.careinsurance.user.InternalCaregivingManagerByUserIdQuery
import kr.caredoc.careinsurance.user.InternalCaregivingManagerByUserIdQueryHandler
import kr.caredoc.careinsurance.user.InternalCaregivingManagerNotFoundByUserIdException
import kr.caredoc.careinsurance.user.User
import kr.caredoc.careinsurance.user.UserByIdQuery
import kr.caredoc.careinsurance.user.UserByIdQueryHandler
import kr.caredoc.careinsurance.user.exception.UserNotFoundByEmailAddressException
import kr.caredoc.careinsurance.web.application.handler.InvalidRequestHandler
import kr.caredoc.careinsurance.web.application.security.jwt.Claims
import kr.caredoc.careinsurance.web.application.security.jwt.JwtManager
import kr.caredoc.careinsurance.web.application.security.jwt.TokenSet
import kr.caredoc.careinsurance.web.authentication.request.AuthenticationRequest
import kr.caredoc.careinsurance.web.authentication.response.EnteredEmailNotRegisteredResponse
import kr.caredoc.careinsurance.web.authentication.response.JwtResponse
import kr.caredoc.careinsurance.web.autorization.exception.IllegalTokenException
import kr.caredoc.careinsurance.web.response.GeneralErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/authentications")
class AuthenticationController(
    private val emailPasswordLoginHandler: EmailPasswordLoginHandler,
    private val jwtManager: JwtManager,
    private val invalidRequestHandler: InvalidRequestHandler,
    private val authenticationCodeLoginHandler: EmailAuthenticationCodeLoginHandler,
    private val jwtParser: JwtParser,
    private val userByIdQueryHandler: UserByIdQueryHandler,
    private val internalCaregivingManagerByUserIdQueryHandler: InternalCaregivingManagerByUserIdQueryHandler,
    private val externalCaregivingManagerByUserIdQueryHandler: ExternalCaregivingManagerByUserIdQueryHandler,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun getGeneratedAuthenticationTokens(
        @Valid @RequestBody payload: AuthenticationRequest,
    ) = ResponseEntity.ok(handleLogin(payload).toResponse())

    private fun issueTokens(user: User, authenticationMethod: AuthenticationMethod) = jwtManager.issueTokens(
        Claims(
            subjectId = user.id,
            credentialRevision = user.credentialRevision,
            internalCaregivingManagerId = findInternalCaregivingManager(user),
            externalCaregivingManagerIds = findExternalCaregivingManagers(user),
        ),
        authenticationMethod,
    )

    private fun issueTokens(user: User, refreshToken: String) = jwtManager.issueTokens(
        Claims(
            subjectId = user.id,
            credentialRevision = user.credentialRevision,
            internalCaregivingManagerId = findInternalCaregivingManager(user),
            externalCaregivingManagerIds = findExternalCaregivingManagers(user),
        ),
        refreshToken,
    )

    private fun handleLogin(payload: AuthenticationRequest): TokenSet {
        val suppliedCredentialCount = listOf(
            payload.password != null,
            payload.authenticationCode != null,
            payload.refreshToken != null,
        ).count { it }

        if (suppliedCredentialCount > 1) {
            throw CredentialNotSuppliedException()
        }

        if (payload.email != null && payload.password != null) {
            logger.info("이메일/패스워드를 사용한 로그인 시도가 감지됐습니다. email: {}", payload.email)
            return issueTokens(
                emailPasswordLoginHandler.handleLogin(
                    EmailPasswordLoginCredential(
                        emailAddress = payload.email,
                        password = payload.password,
                    )
                ),
                AuthenticationMethod.ID_PW_LOGIN,
            )
        }

        if (payload.email != null && payload.authenticationCode != null) {
            logger.info("이메일/인증코드를 사용한 로그인 시도가 감지됐습니다. email: {}", payload.email)
            return issueTokens(
                authenticationCodeLoginHandler.handleLogin(
                    EmailAuthenticationCodeLoginCredential(
                        emailAddress = payload.email,
                        authenticationCode = payload.authenticationCode,
                    )
                ),
                AuthenticationMethod.TEMPORAL_CODE,
            )
        }

        if (payload.refreshToken != null) {
            logger.info("리프래쉬 토큰을 사용한 로그인 시도가 감지됐습니다.")
            jwtManager.ensureRefreshTokenNotUsed(payload.refreshToken)
            val claims = jwtParser.parseClaimsJws(payload.refreshToken).body

            if (claims["tokenType"] != "refresh") {
                throw IllegalTokenException("리프래쉬 토큰이 아닌 토큰이 갱신에 사용됐습니다.")
            }

            val sub = claims.subject?.toString() ?: throw IllegalTokenException("sub 클레임이 존재하지 않습니다.")

            val user = userByIdQueryHandler.getUser(UserByIdQuery(sub))
            claims["credentialRevision"]?.toString()?.let {
                user.ensureCredentialRevisionMatched(it)
            }

            return issueTokens(user, payload.refreshToken)
        }

        throw CredentialNotSuppliedException()
    }

    private fun TokenSet.toResponse() = JwtResponse(
        accessToken = accessToken,
        refreshToken = refreshToken,
    )

    private fun findInternalCaregivingManager(user: User) = try {
        internalCaregivingManagerByUserIdQueryHandler.getInternalCaregivingManager(
            InternalCaregivingManagerByUserIdQuery(
                userId = user.id,
                subject = user,
            )
        ).id
    } catch (e: InternalCaregivingManagerNotFoundByUserIdException) {
        null
    }

    private fun findExternalCaregivingManagers(user: User) = try {
        listOf(
            externalCaregivingManagerByUserIdQueryHandler.getExternalCaregivingManager(
                ExternalCaregivingManagerByUserIdQuery(
                    userId = user.id,
                    subject = user,
                )
            ).id
        )
    } catch (e: ExternalCaregivingManagerNotFoundByUserIdException) {
        listOf()
    }

    @ExceptionHandler(UserNotFoundByEmailAddressException::class)
    fun handleUserNotFoundByEmailAddressException(e: UserNotFoundByEmailAddressException) =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(
                GeneralErrorResponse(
                    message = "입력한 이메일은 등록되지 않은 이메일입니다.",
                    errorType = "NOT_REGISTERED_EMAIL_ADDRESS",
                    data = EnteredEmailNotRegisteredResponse(
                        enteredEmailAddress = e.enteredEmailAddress
                    ),
                )
            )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<GeneralErrorResponse<Unit>> {
        val fieldError = e.fieldError ?: return invalidRequestHandler.handleMethodArgumentNotValidException(e)
        val defaultMessage = fieldError.defaultMessage
            ?: return invalidRequestHandler.handleMethodArgumentNotValidException(e)

        return when (fieldError.field) {
            AuthenticationRequest::email.name -> "EMAIL_VALIDATION_POLICY_VIOLATION"
            else -> return invalidRequestHandler.handleMethodArgumentNotValidException(e)
        }.let { errorType ->
            ResponseEntity.badRequest()
                .body(
                    GeneralErrorResponse(
                        message = defaultMessage,
                        errorType = errorType,
                        data = Unit,
                    )
                )
        }
    }

    @ExceptionHandler(CredentialNotSuppliedException::class)
    fun handleCredentialNotSuppliedException(e: CredentialNotSuppliedException) = ResponseEntity.badRequest()
        .body(
            GeneralErrorResponse(
                message = "유효한 자격증명이 제공되지 않았습니다.",
                errorType = "CREDENTIAL_NOT_SUPPLIED",
                data = Unit,
            )
        )
}
