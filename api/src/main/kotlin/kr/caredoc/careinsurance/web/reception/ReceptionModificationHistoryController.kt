package kr.caredoc.careinsurance.web.reception

import kr.caredoc.careinsurance.intoUtcOffsetDateTime
import kr.caredoc.careinsurance.reception.history.ReceptionModificationHistoriesByReceptionIdQuery
import kr.caredoc.careinsurance.reception.history.ReceptionModificationHistoriesByReceptionIdQueryHandler
import kr.caredoc.careinsurance.reception.history.ReceptionModificationHistory
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.web.modification.DynamicTypedValueFormatter
import kr.caredoc.careinsurance.web.paging.PagedResponse
import kr.caredoc.careinsurance.web.paging.PagingRequest
import kr.caredoc.careinsurance.web.paging.intoPageable
import kr.caredoc.careinsurance.web.paging.intoPagedResponse
import kr.caredoc.careinsurance.web.reception.response.ReceptionModificationHistoryResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/receptions/{reception-id}/reception-modification-history")
class ReceptionModificationHistoryController(
    private val receptionModificationHistoriesByReceptionIdQueryHandler: ReceptionModificationHistoriesByReceptionIdQueryHandler
) {

    @GetMapping
    fun getReceptionModificationHistory(
        @PathVariable("reception-id") receptionId: String,
        pagingRequest: PagingRequest,
        subject: Subject,
    ): ResponseEntity<PagedResponse<ReceptionModificationHistoryResponse>> {
        val modificationHistories =
            receptionModificationHistoriesByReceptionIdQueryHandler.getReceptionModificationHistories(
                ReceptionModificationHistoriesByReceptionIdQuery(
                    receptionId = receptionId,
                    subject = subject,
                ),
                pageRequest = pagingRequest.intoPageable(),
            )

        return ResponseEntity.ok(modificationHistories.map { it.intoResponse() }.intoPagedResponse())
    }

    private fun ReceptionModificationHistory.intoResponse() = ReceptionModificationHistoryResponse(
        modifiedProperty = modifiedProperty,
        previous = previous?.let { DynamicTypedValueFormatter.format(it, modifiedProperty.type) },
        modified = modified?.let { DynamicTypedValueFormatter.format(it, modifiedProperty.type) },
        modifierId = modifierId,
        modifiedDateTime = modifiedDateTime.intoUtcOffsetDateTime(),
    )
}
