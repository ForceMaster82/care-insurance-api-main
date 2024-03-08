package kr.caredoc.careinsurance.web.search.handler

import kr.caredoc.careinsurance.web.response.GeneralErrorResponse
import kr.caredoc.careinsurance.web.search.IllegalSearchQueryException
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class IllegalSearchQueryHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(IllegalSearchQueryException::class)
    fun handleIllegalSearchQueryException(e: IllegalSearchQueryException): ResponseEntity<GeneralErrorResponse<Unit>> {
        logger.info("잘못된 검색 쿼리가 입력됐습니다. query: ${e.enteredSearchQuery}", e)

        return ResponseEntity.badRequest()
            .body(
                GeneralErrorResponse(
                    message = "해석할 수 없는 검색 조건입니다.",
                    errorType = "ILLEGAL_SEARCH_QUERY",
                    data = Unit,
                )
            )
    }
}
