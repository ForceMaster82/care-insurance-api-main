package kr.caredoc.careinsurance.web.application.handler

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import kr.caredoc.careinsurance.RequiredParameterNotSuppliedException
import kr.caredoc.careinsurance.web.response.GeneralErrorResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class InvalidRequestHandler {
    @ExceptionHandler(MissingKotlinParameterException::class)
    fun handleMissingKotlinParameterException(e: MissingKotlinParameterException) =
        generateRequiredItemsNotSuppliedResponse()

    @ExceptionHandler(RequiredParameterNotSuppliedException::class)
    fun handleRequiredRequestParameterNotSuppliedException(e: RequiredParameterNotSuppliedException) =
        generateRequiredItemsNotSuppliedResponse()

    private fun generateRequiredItemsNotSuppliedResponse() = ResponseEntity.badRequest()
        .body(
            GeneralErrorResponse(
                message = "필수 항목이 입력되지 않았습니다.",
                errorType = "REQUIRED_ITEMS_NOT_SUPPLIED",
                data = Unit
            )
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException) =
        run {
            e.fieldError?.defaultMessage ?: "알 수 없는 요청 검증 실패"
        }.let { errorMessage ->
            ResponseEntity.badRequest()
                .body(
                    GeneralErrorResponse(
                        message = errorMessage,
                        errorType = "REQUEST_VALIDATION_POLICY_VIOLATION",
                        data = Unit
                    )
                )
        }
}
