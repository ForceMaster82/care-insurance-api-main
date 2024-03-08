package kr.caredoc.careinsurance.web.caregiving.handler

import kr.caredoc.careinsurance.caregiving.CaregivingChargeNotEnteredException
import kr.caredoc.careinsurance.web.caregiving.response.EnteredCaregivingRoundNotRegisteredData
import kr.caredoc.careinsurance.web.response.GeneralErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class NotExistingCaregivingChargeHandler {
    @ExceptionHandler(CaregivingChargeNotEnteredException::class)
    fun handleCaregivingChargeNotEnteredException(e: CaregivingChargeNotEnteredException) = ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(
            GeneralErrorResponse(
                message = "산정된 간병비가 존재하지 않습니다.",
                errorType = "CAREGIVING_CHARGE_NOT_ENTERED",
                data = EnteredCaregivingRoundNotRegisteredData(
                    enteredCaregivingRoundId = e.enteredCaregivingRoundId
                ),
            )
        )
}
