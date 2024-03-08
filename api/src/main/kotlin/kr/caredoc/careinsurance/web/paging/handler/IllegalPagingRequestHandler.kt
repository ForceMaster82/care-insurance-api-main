package kr.caredoc.careinsurance.web.paging.handler

import kr.caredoc.careinsurance.web.paging.exception.IllegalPagingRequestException
import kr.caredoc.careinsurance.web.paging.response.IllegalPagingRequestData
import kr.caredoc.careinsurance.web.response.GeneralErrorResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class IllegalPagingRequestHandler {
    @ExceptionHandler(IllegalPagingRequestException::class)
    fun handleIllegalPagingRequestException(e: IllegalPagingRequestException) =
        ResponseEntity.badRequest()
            .body(
                GeneralErrorResponse(
                    message = "잘못된 페이지 요청입니다.",
                    errorType = "ILLEGAL_PAGE_REQUEST",
                    data = IllegalPagingRequestData(
                        enteredPageNumber = e.enteredPageNumber,
                        enteredPageSize = e.enteredPageSize,
                    )
                )
            )
}
