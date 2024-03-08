package kr.caredoc.careinsurance.web.autorization.handler

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.security.SignatureException
import kr.caredoc.careinsurance.security.authentication.CredentialExpiredException
import kr.caredoc.careinsurance.user.exception.UserSuspendedException
import kr.caredoc.careinsurance.web.application.security.jwt.RefreshTokenAlreadyUsed
import kr.caredoc.careinsurance.web.autorization.exception.AuthorizationHeaderNotPresentException
import kr.caredoc.careinsurance.web.autorization.exception.IllegalTokenException
import kr.caredoc.careinsurance.web.autorization.response.SuspendedUserData
import kr.caredoc.careinsurance.web.response.GeneralErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class NotAuthorizedRequestHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(UserSuspendedException::class)
    fun handleUserSuspendedException(e: UserSuspendedException) =
        ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(
                GeneralErrorResponse(
                    message = "사용이 중지된 사용자입니다.",
                    errorType = "USER_SUSPENDED",
                    data = SuspendedUserData(
                        userId = e.userId
                    )
                )
            )

    @ExceptionHandler(AuthorizationHeaderNotPresentException::class)
    fun handleAuthorizationHeaderNotPresentException(e: AuthorizationHeaderNotPresentException) =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(
                GeneralErrorResponse(
                    message = "크레덴셜이 제공되지 않았습니다.",
                    errorType = "CREDENTIAL_NOT_SUPPLIED",
                    data = Unit,
                )
            )

    @ExceptionHandler(IllegalTokenException::class)
    fun handleIllegalTokenException(e: IllegalTokenException) = generateIllegalTokenResponse()

    @ExceptionHandler(SignatureException::class)
    fun handleIllegalTokenException(e: SignatureException) = generateIllegalTokenResponse()

    fun generateIllegalTokenResponse() = ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(
            GeneralErrorResponse(
                message = "토큰 형식이 잘못되었습니다.",
                errorType = "ILLEGAL_TOKEN_SUPPLIED",
                data = Unit,
            )
        )

    @ExceptionHandler(ExpiredJwtException::class)
    fun handleExpiredJwtException(e: ExpiredJwtException) =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(
                GeneralErrorResponse(
                    message = "토큰이 만료되었습니다.",
                    errorType = "TOKEN_EXPIRED",
                    data = Unit,
                )
            )

    @ExceptionHandler(CredentialExpiredException::class)
    fun handleCredentialExpiredException(e: CredentialExpiredException): ResponseEntity<GeneralErrorResponse<Unit>> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(
                GeneralErrorResponse(
                    message = "비밀번호가 만료됐습니다. 비밀번호 변경이 필요합니다.",
                    errorType = "PASSWORD_CHANGE_REQUIRED",
                    data = Unit,
                )
            )
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedHandler(e: AccessDeniedException): ResponseEntity<GeneralErrorResponse<Unit>> {
        logger.warn("불충분한 권한을 가진 접근이 감지되었습니다.", e)

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(
                GeneralErrorResponse(
                    message = "권한이 불충분합니다.",
                    errorType = "NOT_AUTHORIZED",
                    data = Unit,
                )
            )
    }

    @ExceptionHandler(RefreshTokenAlreadyUsed::class)
    fun handleRefreshTokenAlreadyUsed(e: RefreshTokenAlreadyUsed) =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(
                GeneralErrorResponse(
                    message = "이미 사용된 RefreshToken입니다.",
                    errorType = "REFRESH_TOKEN_ALREADY_USED",
                    data = Unit,
                )
            )
}
