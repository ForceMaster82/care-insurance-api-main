package kr.caredoc.careinsurance.web.user.handler

import kr.caredoc.careinsurance.user.exception.AlreadyExistsUserEmailAddressException
import kr.caredoc.careinsurance.web.agency.response.EnteredAlreadyExistsExternalCaregivingManagerEmailData
import kr.caredoc.careinsurance.web.response.GeneralErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class UserPropertyConflictHandler {
    @ExceptionHandler(AlreadyExistsUserEmailAddressException::class)
    private fun handleAlreadyExistsUserEmailAddressException(e: AlreadyExistsUserEmailAddressException) =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(
                GeneralErrorResponse(
                    message = "입력한 이메일은 이미 존재하는 이메일입니다.",
                    errorType = "ALREADY_EXISTS_EXTERNAL_CAREGIVING_MANGER_EMAIL",
                    data = EnteredAlreadyExistsExternalCaregivingManagerEmailData(
                        enteredEmail = e.emailAddress
                    )
                )
            )
}
