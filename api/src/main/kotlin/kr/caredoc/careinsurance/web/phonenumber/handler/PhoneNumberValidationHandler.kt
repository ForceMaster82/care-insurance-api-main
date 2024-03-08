package kr.caredoc.careinsurance.web.phonenumber.handler

import kr.caredoc.careinsurance.phonenumber.InvalidPhoneNumberException
import kr.caredoc.careinsurance.web.phonenumber.response.InvalidPhoneNumberEnteredData
import kr.caredoc.careinsurance.web.response.GeneralErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class PhoneNumberValidationHandler {
    @ExceptionHandler(InvalidPhoneNumberException::class)
    fun handleInvalidPhoneNumberException(e: InvalidPhoneNumberException) = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(
            GeneralErrorResponse(
                message = "잘못된 핸드폰 번호 형식이 입력되었습니다.",
                errorType = "ILLEGAL_PHONE_NUMBER",
                data = InvalidPhoneNumberEnteredData(
                    enteredPhoneNumber = e.enteredPhoneNumber
                ),
            )
        )
}
