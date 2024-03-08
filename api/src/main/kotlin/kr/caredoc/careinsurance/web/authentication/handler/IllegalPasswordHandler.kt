package kr.caredoc.careinsurance.web.authentication.handler

import kr.caredoc.careinsurance.security.password.IllegalPasswordException
import kr.caredoc.careinsurance.web.response.GeneralErrorResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class IllegalPasswordHandler {
    @ExceptionHandler(IllegalPasswordException::class)
    fun handleIllegalPasswordException(e: IllegalPasswordException) = ResponseEntity.badRequest()
        .body(
            GeneralErrorResponse(
                message = "비밀번호는 영문 대/소문자, 숫자, 특수문자(!@#$%^&*-_)를 조합하여 8~20자 이내로 입력해야 합니다.",
                errorType = "PASSWORD_VALIDATION_POLICY_VIOLATION",
                data = Unit,
            )
        )
}
