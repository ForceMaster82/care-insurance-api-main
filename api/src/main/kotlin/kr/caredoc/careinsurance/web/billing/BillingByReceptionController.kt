package kr.caredoc.careinsurance.web.billing

import kr.caredoc.careinsurance.billing.Billing
import kr.caredoc.careinsurance.billing.BillingByReceptionIdQuery
import kr.caredoc.careinsurance.billing.BillingByReceptionIdQueryHandler
import kr.caredoc.careinsurance.intoUtcOffsetDateTime
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.web.billing.response.BillingReceptionResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/receptions/{reception-id}/billings")
class BillingByReceptionController(
    private val billingByReceptionIdQueryHandler: BillingByReceptionIdQueryHandler,
) {

    @GetMapping
    fun getReceptionBillings(
        @PathVariable("reception-id") receptionId: String,
        subject: Subject,
    ): ResponseEntity<List<BillingReceptionResponse>> {
        val billings = billingByReceptionIdQueryHandler.getBillingReception(
            BillingByReceptionIdQuery(
                receptionId = receptionId,
                subject = subject,
            )
        )
        return ResponseEntity
            .ok()
            .body(
                billings.map {
                    it.intoResponse()
                }
            )
    }

    private fun Billing.intoResponse() = BillingReceptionResponse(
        id = id,
        caregivingRoundId = caregivingRoundInfo.caregivingRoundId,
        caregivingRoundNumber = caregivingRoundInfo.roundNumber,
        billingProgressingStatus = billingProgressingStatus,
        startDateTime = caregivingRoundInfo.startDateTime.intoUtcOffsetDateTime(),
        endDateTime = caregivingRoundInfo.endDateTime.intoUtcOffsetDateTime(),
        billingDate = billingDate,
        basicAmount = basicAmounts.sumOf { it.totalAmount },
        additionalAmount = additionalAmount,
        totalAmount = totalAmount,
    )
}
