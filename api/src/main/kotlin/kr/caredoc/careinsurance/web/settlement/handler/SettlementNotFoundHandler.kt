package kr.caredoc.careinsurance.web.settlement.handler

import kr.caredoc.careinsurance.settlement.ReferenceSettlementNotExistsException
import kr.caredoc.careinsurance.settlement.SettlementNotFoundByIdException
import kr.caredoc.careinsurance.web.response.GeneralErrorResponse
import kr.caredoc.careinsurance.web.settlement.response.EnteredSettlementNotRegisteredData
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class SettlementNotFoundHandler {
    @ExceptionHandler(ReferenceSettlementNotExistsException::class)
    fun handleReferenceSettlementNotExistsException(e: ReferenceSettlementNotExistsException) = ResponseEntity
        .unprocessableEntity()
        .body(
            GeneralErrorResponse(
                message = "요청에 포함된 정산이 존재하지 않습니다.",
                errorType = "REFERENCE_SETTLEMENT_NOT_EXISTS",
                data = EnteredSettlementNotRegisteredData(
                    enteredSettlementId = e.referenceSettlementId,
                )
            )
        )

    @ExceptionHandler(SettlementNotFoundByIdException::class)
    fun handleSettlementNotFoundByIdException(e: SettlementNotFoundByIdException) = ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(
            GeneralErrorResponse(
                message = "조회하고자 하는 정산이 존재하지 않습니다.",
                errorType = "SETTLEMENT_NOT_EXISTS",
                data = EnteredSettlementNotRegisteredData(
                    enteredSettlementId = e.settlementId,
                )
            )
        )
}
