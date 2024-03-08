package kr.caredoc.careinsurance.web.autorization

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.io.DecodingException
import io.sentry.Sentry
import io.sentry.SentryOptions
import io.sentry.protocol.User.JsonKeys
import kr.caredoc.careinsurance.security.accesscontrol.CombinedSubject
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.security.accesscontrol.SubjectAttribute
import kr.caredoc.careinsurance.security.authentication.AuthenticationMethod
import kr.caredoc.careinsurance.user.ExternalCaregivingManager
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerByUserIdQuery
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerByUserIdQueryHandler
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerNotFoundByUserIdException
import kr.caredoc.careinsurance.user.InternalCaregivingManager
import kr.caredoc.careinsurance.user.InternalCaregivingManagerByUserIdQuery
import kr.caredoc.careinsurance.user.InternalCaregivingManagerByUserIdQueryHandler
import kr.caredoc.careinsurance.user.InternalCaregivingManagerNotFoundByUserIdException
import kr.caredoc.careinsurance.user.User
import kr.caredoc.careinsurance.user.UserByIdQuery
import kr.caredoc.careinsurance.user.UserByIdQueryHandler
import kr.caredoc.careinsurance.user.exception.UserNotFoundByIdException
import kr.caredoc.careinsurance.web.application.extractOriginIp
import kr.caredoc.careinsurance.web.autorization.exception.AuthorizationHeaderNotPresentException
import kr.caredoc.careinsurance.web.autorization.exception.ClaimedUserNotExistsException
import kr.caredoc.careinsurance.web.autorization.exception.IllegalTokenException
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class UserSubjectArgumentResolver(
    private val userByIdQueryHandler: UserByIdQueryHandler,
    private val internalCaregivingManagerByUserIdQueryHandler: InternalCaregivingManagerByUserIdQueryHandler,
    private val externalCaregivingManagerByUserIdQueryHandler: ExternalCaregivingManagerByUserIdQueryHandler,
    private val jwtParser: JwtParser,
) : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        val parameterType = parameter.parameterType
        return parameterType == Subject::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Subject? {
        val authorizationHeader = webRequest.getHeader(HttpHeaders.AUTHORIZATION)
        if (!parameter.isOptional && authorizationHeader == null) {
            throw AuthorizationHeaderNotPresentException()
        }
        authorizationHeader ?: return null

        val claims = parseClaims(authorizationHeader)
        val user = extractUser(claims)
        user.ensureCredentialRevisionMatched(claims)

        val internalCaregivingManager = extractInternalCaregivingManager(user)
        val externalCaregivingManager = extractExternalCaregivingManager(user)
        val authenticationMethod = claims.extractAuthenticationMethod()?.let {
            setOf(it)
        } ?: setOf()

        val subject = CombinedSubject(
            listOfNotNull(
                user,
                internalCaregivingManager,
                externalCaregivingManager,
                extractOriginIp(webRequest)?.let {
                    Subject.fromAttributes(SubjectAttribute.CLIENT_IP to setOf(it))
                },
                Subject.fromAttributes(
                    SubjectAttribute.AUTHENTICATION_METHOD to authenticationMethod
                )
            )
        )

        updateSentryUser(subject)

        return subject
    }

    private fun extractUser(claims: Claims): User {

        ensureTokenTypeIsAccessToken(claims)
        val userId = extractSubClaim(claims)

        return getUser(userId)
    }

    private fun extractInternalCaregivingManager(user: User): InternalCaregivingManager? {
        return try {
            internalCaregivingManagerByUserIdQueryHandler.getInternalCaregivingManager(
                InternalCaregivingManagerByUserIdQuery(
                    userId = user.id,
                    subject = user,
                )
            )
        } catch (e: InternalCaregivingManagerNotFoundByUserIdException) {
            null
        }
    }

    private fun extractExternalCaregivingManager(user: User): ExternalCaregivingManager? {
        return try {
            externalCaregivingManagerByUserIdQueryHandler.getExternalCaregivingManager(
                ExternalCaregivingManagerByUserIdQuery(
                    userId = user.id,
                    subject = user,
                )
            )
        } catch (e: ExternalCaregivingManagerNotFoundByUserIdException) {
            null
        }
    }

    private fun ensureTokenTypeIsAccessToken(claims: Claims) {
        val tokenType = claims["tokenType"]?.toString() ?: throw IllegalTokenException("tokenType 클레임이 존재하지 않습니다.")
        if (tokenType != "access") {
            throw IllegalTokenException("엑세스 토큰이 아닌 토큰이 인증에 사용됐습니다.")
        }
    }

    private fun extractSubClaim(claims: Claims): String {
        return claims.subject?.toString() ?: throw IllegalTokenException("sub 클레임이 존재하지 않습니다.")
    }

    private fun getUser(userId: String): User {
        try {
            return userByIdQueryHandler.getUser(
                UserByIdQuery(
                    userId = userId,
                )
            )
        } catch (e: UserNotFoundByIdException) {
            throw ClaimedUserNotExistsException(e.userId)
        }
    }

    private fun parseClaims(authorizationHeader: String): Claims {
        val token = authorizationHeader.removePrefix("Bearer ")
        try {
            return jwtParser.parseClaimsJws(token).body
        } catch (e: DecodingException) {
            throw IllegalTokenException(e.message ?: "Decoding failed", e)
        }
    }

    private fun updateSentryUser(subject: Subject) {
        val userMap = mapOf(
            JsonKeys.ID to subject[SubjectAttribute.USER_ID].firstOrNull(),
        )
        val user = io.sentry.protocol.User.fromMap(
            userMap,
            SentryOptions(),
        )
        Sentry.setUser(user)
    }

    private fun User.ensureCredentialRevisionMatched(claims: Claims) {
        val credentialRevision = claims["credentialRevision"]?.toString()

        credentialRevision?.let {
            this.ensureCredentialRevisionMatched(credentialRevision)
        }
    }

    private fun Claims.extractAuthenticationMethod() = this["authenticationMethod"]?.toString()?.also {
        AuthenticationMethod.ensureParsable(it)
    }
}
