package kr.caredoc.careinsurance.web.user.handler

import kr.caredoc.careinsurance.user.ReferenceInternalCaregivingManagerNotExistsException
import kr.caredoc.careinsurance.web.response.GeneralErrorResponse
import kr.caredoc.careinsurance.web.user.response.EnteredInternalCaregivingManagerNotRegisteredData
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class InternalCaregivingManagerNotFoundHandler {
    @ExceptionHandler(ReferenceInternalCaregivingManagerNotExistsException::class)
    fun handleReferenceInternalCaregivingManagerNotExistsException(e: ReferenceInternalCaregivingManagerNotExistsException) =
        ResponseEntity
            .unprocessableEntity()
            .body(
                GeneralErrorResponse(
                    message = "요청에 포함된 내부 간병 관리자가 존재하지 않습니다.",
                    errorType = "REFERENCE_INTERNAL_CAREGIVING_MANAGER_NOT_EXISTS",
                    data = EnteredInternalCaregivingManagerNotRegisteredData(
                        enteredInternalCaregivingManagerId = e.referenceInternalCaregivingManagerId,
                    )
                )
            )
}
