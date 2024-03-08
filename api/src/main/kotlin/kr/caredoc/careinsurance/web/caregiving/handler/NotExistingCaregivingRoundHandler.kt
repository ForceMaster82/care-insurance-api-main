package kr.caredoc.careinsurance.web.caregiving.handler

import kr.caredoc.careinsurance.caregiving.exception.CaregivingRoundNotFoundByIdException
import kr.caredoc.careinsurance.caregiving.exception.ReferenceCaregivingRoundNotExistException
import kr.caredoc.careinsurance.web.caregiving.response.EnteredCaregivingRoundNotRegisteredData
import kr.caredoc.careinsurance.web.response.GeneralErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class NotExistingCaregivingRoundHandler {
    @ExceptionHandler(ReferenceCaregivingRoundNotExistException::class)
    fun handleReferenceCaregivingRoundNotExistException(e: ReferenceCaregivingRoundNotExistException) = ResponseEntity
        .status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(
            GeneralErrorResponse(
                message = "조회하고자 하는 간병 정보가 존재하지 않습니다.",
                errorType = "REFERENCE_CAREGIVING_ROUND_NOT_EXISTS",
                data = EnteredCaregivingRoundNotRegisteredData(
                    enteredCaregivingRoundId = e.referenceCaregivingRoundId
                ),
            )
        )

    @ExceptionHandler(CaregivingRoundNotFoundByIdException::class)
    fun handleCaregivingRoundNotFoundByIdException(e: CaregivingRoundNotFoundByIdException) = ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(
            GeneralErrorResponse(
                message = "조회하고자 하는 간병 회차 정보가 존재하지 않습니다.",
                errorType = "CAREGIVING_ROUND_NOT_EXISTS",
                data = EnteredCaregivingRoundNotRegisteredData(
                    enteredCaregivingRoundId = e.caregivingRoundId
                ),
            )
        )
}
