package kr.caredoc.careinsurance.web.reception

import kr.caredoc.careinsurance.reception.caregivingsatisfactionsurvey.CaregivingSatisfactionSurveyReserveCommand
import kr.caredoc.careinsurance.reception.caregivingsatisfactionsurvey.CaregivingSatisfactionSurveyReserveCommandHandler
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.web.reception.request.CaregivingSatisfactionSurveyReserveRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/caregiving-satisfaction-surveys")
class CaregivingSatisfactionSurveyController(
    private val caregivingSatisfactionSurveyReserveCommandHandler: CaregivingSatisfactionSurveyReserveCommandHandler,
) {
    @PostMapping
    fun reserveCaregivingStartMessages(
        @RequestBody payload: List<CaregivingSatisfactionSurveyReserveRequest>,
        subject: Subject,
    ): ResponseEntity<Unit> {
        caregivingSatisfactionSurveyReserveCommandHandler.reserveSatisfactionSurvey(
            CaregivingSatisfactionSurveyReserveCommand(
                targetReceptionIds = payload.map { it.receptionId }.toSet(),
                subject = subject,
            )
        )

        return ResponseEntity.noContent().build()
    }
}
