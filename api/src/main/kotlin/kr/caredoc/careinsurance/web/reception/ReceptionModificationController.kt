package kr.caredoc.careinsurance.web.reception

import kr.caredoc.careinsurance.intoUtcOffsetDateTime
import kr.caredoc.careinsurance.reception.history.ReceptionModificationSummary
import kr.caredoc.careinsurance.reception.history.ReceptionModificationSummaryByReceptionIdQuery
import kr.caredoc.careinsurance.reception.history.ReceptionModificationSummaryByReceptionIdQueryHandler
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.web.reception.response.ReceptionModificationResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v1/receptions/{reception-id}/reception-modification")
class ReceptionModificationController(
    private val receptionModificationSummaryByReceptionIdQueryHandler: ReceptionModificationSummaryByReceptionIdQueryHandler
) {
    @GetMapping
    fun getReceptionModification(
        @PathVariable("reception-id") receptionId: String,
        subject: Subject,
    ): ResponseEntity<ReceptionModificationResponse> {
        val receptionModificationSummary =
            receptionModificationSummaryByReceptionIdQueryHandler.getReceptionModificationSummary(
                ReceptionModificationSummaryByReceptionIdQuery(
                    receptionId = receptionId,
                    subject = subject
                )
            )
        return ResponseEntity.ok(receptionModificationSummary.intoResponse())
    }

    private fun ReceptionModificationSummary.intoResponse() = ReceptionModificationResponse(
        lastModifiedDateTime = lastModifiedDateTime?.intoUtcOffsetDateTime(),
        lastModifierId = lastModifierId,
        modificationCount = modificationCount,
    )
}
