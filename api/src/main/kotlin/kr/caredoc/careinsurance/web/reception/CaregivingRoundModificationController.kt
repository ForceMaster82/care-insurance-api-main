package kr.caredoc.careinsurance.web.reception

import kr.caredoc.careinsurance.intoUtcOffsetDateTime
import kr.caredoc.careinsurance.reception.modification.CaregivingRoundModificationSummary
import kr.caredoc.careinsurance.reception.modification.CaregivingRoundModificationSummaryByReceptionIdQuery
import kr.caredoc.careinsurance.reception.modification.CaregivingRoundModificationSummaryByReceptionIdQueryHandler
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.web.reception.response.CaregivingRoundModificationResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/receptions/{reception-id}/caregiving-round-modification")
class CaregivingRoundModificationController(
    private val caregivingRoundModificationSummaryByReceptionIdQueryHandler: CaregivingRoundModificationSummaryByReceptionIdQueryHandler,
) {
    @GetMapping
    fun getCaregivingRoundModification(
        @PathVariable("reception-id") receptionId: String,
        subject: Subject,
    ): ResponseEntity<CaregivingRoundModificationResponse> {
        val modificationSummary =
            caregivingRoundModificationSummaryByReceptionIdQueryHandler.getCaregivingRoundModificationSummary(
                CaregivingRoundModificationSummaryByReceptionIdQuery(
                    receptionId = receptionId,
                    subject = subject,
                )
            )

        return ResponseEntity.ok(modificationSummary.intoResponse())
    }

    private fun CaregivingRoundModificationSummary.intoResponse() = CaregivingRoundModificationResponse(
        lastModifiedDateTime = lastModifiedDateTime?.intoUtcOffsetDateTime(),
        lastModifierId = lastModifierId,
        modificationCount = modificationCount,
    )
}
