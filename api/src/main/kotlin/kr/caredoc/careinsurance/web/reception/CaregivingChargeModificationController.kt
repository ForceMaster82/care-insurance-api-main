package kr.caredoc.careinsurance.web.reception

import kr.caredoc.careinsurance.intoUtcOffsetDateTime
import kr.caredoc.careinsurance.reception.modification.CaregivingChargeModificationSummary
import kr.caredoc.careinsurance.reception.modification.CaregivingChargeModificationSummaryByReceptionIdQuery
import kr.caredoc.careinsurance.reception.modification.CaregivingChargeModificationSummaryByReceptionIdQueryHandler
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.web.reception.response.CaregivingChargeModificationResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/receptions/{reception-id}/caregiving-charge-modification")
class CaregivingChargeModificationController(
    private val caregivingChargeModificationSummaryByReceptionIdQueryHandler: CaregivingChargeModificationSummaryByReceptionIdQueryHandler,
) {
    @GetMapping
    fun getCaregivingChargeModification(
        @PathVariable("reception-id") receptionId: String,
        subject: Subject,
    ): ResponseEntity<CaregivingChargeModificationResponse> {
        val modificationSummary =
            caregivingChargeModificationSummaryByReceptionIdQueryHandler.getCaregivingChargeModificationSummary(
                CaregivingChargeModificationSummaryByReceptionIdQuery(
                    receptionId = receptionId,
                    subject = subject,
                )
            )

        return ResponseEntity.ok(modificationSummary.intoResponse())
    }

    private fun CaregivingChargeModificationSummary.intoResponse() = CaregivingChargeModificationResponse(
        lastModifiedDateTime = lastModifiedDateTime?.intoUtcOffsetDateTime(),
        lastModifierId = lastModifierId,
        modificationCount = modificationCount,
    )
}
