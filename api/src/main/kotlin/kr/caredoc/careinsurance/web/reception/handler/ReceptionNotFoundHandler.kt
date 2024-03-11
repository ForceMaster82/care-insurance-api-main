package kr.caredoc.careinsurance.web.reception.handler

import kr.caredoc.careinsurance.reception.ReferenceReceptionNotExistsException
import kr.caredoc.careinsurance.reception.exception.AccidentNumberExistsException
import kr.caredoc.careinsurance.reception.exception.InsuranceNumberExistsException
import kr.caredoc.careinsurance.reception.exception.ReceptionNotFoundByIdException
import kr.caredoc.careinsurance.web.reception.response.EnteredReceptionIdNotRegisteredData
import kr.caredoc.careinsurance.web.response.GeneralErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ReceptionNotFoundHandler {
    @ExceptionHandler(ReceptionNotFoundByIdException::class)
    fun handleReceptionNotFoundByIdException(e: ReceptionNotFoundByIdException) = ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(
            GeneralErrorResponse(
                message = "조회하고자 하는 간병 접수가 존재하지 않습니다.",
                errorType = "RECEPTION_NOT_EXISTS",
                data = EnteredReceptionIdNotRegisteredData(
                    enteredReceptionId = e.receptionId
                ),
            )
        )

    @ExceptionHandler(ReferenceReceptionNotExistsException::class)
    fun handleReferenceReceptionNotExistsException(e: ReferenceReceptionNotExistsException) = ResponseEntity
        .status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(
            GeneralErrorResponse(
                message = "요청에 포함된 간병 접수가 존재하지 않습니다.",
                errorType = "REFERENCE_RECEPTION_NOT_EXISTS",
                data = EnteredReceptionIdNotRegisteredData(
                    enteredReceptionId = e.referenceReceptionId
                ),
            )
        )

    @ExceptionHandler(AccidentNumberExistsException::class)
    fun handleReceptionNotFoundByIdException(e: AccidentNumberExistsException) = ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(
            GeneralErrorResponse(
                message = "사고번호($e.accidentNumber)는 이미 등록되어 있습니다.",
                errorType = "ACCIDENT_NUMBER_EXISTS",
                data = EnteredReceptionIdNotRegisteredData(
                    enteredReceptionId = e.accidentNumber
                ),
            )
        )

    @ExceptionHandler(InsuranceNumberExistsException::class)
    fun handleReceptionNotFoundByIdException(e: InsuranceNumberExistsException) = ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(
            GeneralErrorResponse(
                message = "접수번호($e.insuranceNumber)는 이미 등록되어 있습니다.",
                errorType = "INSURANCE_NUMBER_EXISTS",
                data = EnteredReceptionIdNotRegisteredData(
                    enteredReceptionId = e.insuranceNumber
                ),
            )
        )
}
