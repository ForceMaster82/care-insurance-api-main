package kr.caredoc.careinsurance.web.authentication.handler

import kr.caredoc.careinsurance.user.exception.CredentialNotMatchedException
import kr.caredoc.careinsurance.web.response.GeneralErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class AuthenticationFailedHandler {
    @ExceptionHandler(CredentialNotMatchedException::class)
    fun handleCredentialNotMatchedException(e: CredentialNotMatchedException) =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(
                GeneralErrorResponse(
                    message = "잘못된 로그인 정보입니다.",
                    errorType = "WRONG_CREDENTIAL",
                    data = Unit,
                )
            )
}
