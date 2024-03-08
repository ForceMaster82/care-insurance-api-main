package kr.caredoc.careinsurance.web.reception

import kr.caredoc.careinsurance.intoUtcOffsetDateTime
import kr.caredoc.careinsurance.reception.modification.CaregivingChargeModificationHistoriesByReceptionIdQuery
import kr.caredoc.careinsurance.reception.modification.CaregivingChargeModificationHistoriesByReceptionIdQueryHandler
import kr.caredoc.careinsurance.reception.modification.CaregivingChargeModificationHistory
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.web.modification.DynamicTypedValueFormatter
import kr.caredoc.careinsurance.web.paging.PagedResponse
import kr.caredoc.careinsurance.web.paging.PagingRequest
import kr.caredoc.careinsurance.web.paging.intoPageable
import kr.caredoc.careinsurance.web.paging.intoPagedResponse
import kr.caredoc.careinsurance.web.reception.response.CaregivingChargeModificationHistoryResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/receptions/{reception-id}/caregiving-charge-modification-history")
class CaregivingChargeModificationHistoryController(
    private val caregivingChargeModificationHistoriesByReceptionIdQueryHandler: CaregivingChargeModificationHistoriesByReceptionIdQueryHandler,
) {
    @GetMapping
    fun getCaregivingChargeModificationHistory(
        @PathVariable("reception-id") receptionId: String,
        pagingRequest: PagingRequest,
        subject: Subject,
    ): ResponseEntity<PagedResponse<CaregivingChargeModificationHistoryResponse>> {
        val modificationHistories =
            caregivingChargeModificationHistoriesByReceptionIdQueryHandler.getCaregivingChargeModificationHistories(
                CaregivingChargeModificationHistoriesByReceptionIdQuery(
                    receptionId = receptionId,
                    subject = subject,
                ),
                pageRequest = pagingRequest.intoPageable(),
            )

        return ResponseEntity.ok(modificationHistories.map { it.intoResponse() }.intoPagedResponse())
    }

    private fun CaregivingChargeModificationHistory.intoResponse() = CaregivingChargeModificationHistoryResponse(
        caregivingRoundNumber = caregivingRoundNumber,
        modifiedProperty = modifiedProperty,
        previous = previous?.let { DynamicTypedValueFormatter.format(it, modifiedProperty.type) },
        modified = modified?.let { DynamicTypedValueFormatter.format(it, modifiedProperty.type) },
        modifierId = modifierId,
        modifiedDateTime = modifiedDateTime.intoUtcOffsetDateTime(),
    )
}
