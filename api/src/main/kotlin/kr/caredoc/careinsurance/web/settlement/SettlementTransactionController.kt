package kr.caredoc.careinsurance.web.settlement

import kr.caredoc.careinsurance.intoUtcOffsetDateTime
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.settlement.Settlement
import kr.caredoc.careinsurance.settlement.SettlementByIdQuery
import kr.caredoc.careinsurance.settlement.SettlementTransactionRecordingCommand
import kr.caredoc.careinsurance.settlement.SettlementTransactionRecordingCommandHandler
import kr.caredoc.careinsurance.settlement.TransactionsBySettlementIdQuery
import kr.caredoc.careinsurance.settlement.TransactionsBySettlementIdQueryHandler
import kr.caredoc.careinsurance.web.paging.PagedResponse
import kr.caredoc.careinsurance.web.paging.PagingRequest
import kr.caredoc.careinsurance.web.paging.intoPageable
import kr.caredoc.careinsurance.web.paging.intoPagedResponse
import kr.caredoc.careinsurance.web.settlement.request.SettlementTransactionRecordingRequest
import kr.caredoc.careinsurance.web.settlement.response.SettlementTransactionResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/settlements/{settlement-id}/transactions")
class SettlementTransactionController(
    private val transactionsBySettlementIdQueryHandler: TransactionsBySettlementIdQueryHandler,
    private val settlementTransactionRecordingCommandHandler: SettlementTransactionRecordingCommandHandler
) {
    @GetMapping
    fun getSettlementTransactions(
        @PathVariable("settlement-id") settlementId: String,
        pagingRequest: PagingRequest,
        subject: Subject,
    ): ResponseEntity<PagedResponse<SettlementTransactionResponse>> {
        val transactions = transactionsBySettlementIdQueryHandler.getTransactions(
            TransactionsBySettlementIdQuery(
                settlementId = settlementId,
                subject = subject,
            ),
            pageable = pagingRequest.intoPageable()
        )

        return ResponseEntity.ok(
            transactions.map {
                it.intoResponse()
            }.intoPagedResponse()
        )
    }

    private fun Settlement.TransactionRecord.intoResponse() = SettlementTransactionResponse(
        transactionType = transactionType,
        amount = amount,
        transactionDate = transactionDate,
        enteredDateTime = enteredDateTime.intoUtcOffsetDateTime(),
        transactionSubjectId = transactionSubjectId,
    )

    @PostMapping
    fun recordSettlementTransaction(
        @PathVariable("settlement-id") settlementId: String,
        @RequestBody payload: SettlementTransactionRecordingRequest,
        subject: Subject,
    ): ResponseEntity<Unit> {
        settlementTransactionRecordingCommandHandler.recordTransaction(
            SettlementByIdQuery(
                settlementId = settlementId,
                subject = subject,
            ),
            payload.intoCommand(subject),
        )

        return ResponseEntity.noContent().build()
    }

    private fun SettlementTransactionRecordingRequest.intoCommand(
        subject: Subject,
    ) = SettlementTransactionRecordingCommand(
        transactionType = transactionType,
        amount = amount,
        transactionDate = transactionDate,
        transactionSubjectId = transactionSubjectId,
        subject = subject,
    )
}
