package kr.caredoc.careinsurance.web.settlement

import kr.caredoc.careinsurance.reception.ReceptionsByIdsQuery
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQueryHandler
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.settlement.SettlementsByReceptionIdQuery
import kr.caredoc.careinsurance.settlement.SettlementsByReceptionIdQueryHandler
import kr.caredoc.careinsurance.web.settlement.response.SettlementResponse
import kr.caredoc.careinsurance.web.settlement.response.SettlementResponseConverter
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/receptions/{reception-id}/settlements")
class SettlementsByReceptionController(
    private val settlementsByReceptionIdQueryHandler: SettlementsByReceptionIdQueryHandler,
    private val receptionsByIdsQueryHandler: ReceptionsByIdsQueryHandler,
) {
    @GetMapping
    fun getSettlementsInReception(
        @PathVariable("reception-id") receptionId: String,
        subject: Subject,
    ): ResponseEntity<List<SettlementResponse>> {
        val settlements = settlementsByReceptionIdQueryHandler.getSettlements(
            SettlementsByReceptionIdQuery(
                receptionId = receptionId,
                subject = subject,
            )
        )
        val receptions = receptionsByIdsQueryHandler.getReceptions(
            ReceptionsByIdsQuery(
                settlements.map { it.receptionId },
                subject,
            )
        ).associateBy { it.id }

        return ResponseEntity.ok(SettlementResponseConverter.intoSettlementResponses(receptions, settlements))
    }
}
