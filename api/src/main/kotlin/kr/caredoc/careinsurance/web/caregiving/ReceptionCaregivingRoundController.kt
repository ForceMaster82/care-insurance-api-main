package kr.caredoc.careinsurance.web.caregiving

import kr.caredoc.careinsurance.caregiving.CaregivingRoundsByReceptionIdQuery
import kr.caredoc.careinsurance.caregiving.CaregivingRoundsByReceptionIdQueryHandler
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQuery
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQueryHandler
import kr.caredoc.careinsurance.reception.ReferenceReceptionNotExistsException
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.web.caregiving.response.CaregivingRoundResponseConverter
import kr.caredoc.careinsurance.web.caregiving.response.DetailCaregivingRoundResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/receptions/{reception-id}/caregiving-rounds")
class ReceptionCaregivingRoundController(
    private val caregivingRoundsByReceptionIdQueryHandler: CaregivingRoundsByReceptionIdQueryHandler,
    private val receptionsByIdsQueryHandler: ReceptionsByIdsQueryHandler,
) {

    @GetMapping
    fun getReceptionCaregivingRounds(
        @PathVariable("reception-id") receptionId: String,
        subject: Subject,
    ): ResponseEntity<List<DetailCaregivingRoundResponse>> {
        val caregivingRounds = caregivingRoundsByReceptionIdQueryHandler.getReceptionCaregivingRounds(
            CaregivingRoundsByReceptionIdQuery(
                receptionId = receptionId,
                subject = subject,
            )
        )

        val receptions = receptionsByIdsQueryHandler.getReceptions(
            ReceptionsByIdsQuery(
                receptionIds = caregivingRounds.map { it.receptionInfo.receptionId },
                subject = subject,
            )
        ).associateBy { it.id }

        return ResponseEntity.ok(
            caregivingRounds.map {
                val reception = receptions[it.receptionInfo.receptionId]
                    ?: throw ReferenceReceptionNotExistsException(it.receptionInfo.receptionId)
                CaregivingRoundResponseConverter.intoDetailResponse(reception, it)
            }
        )
    }
}
