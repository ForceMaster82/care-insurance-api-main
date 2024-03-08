package kr.caredoc.careinsurance.web.reception

import kr.caredoc.careinsurance.reception.caregivingstartmessage.CaregivingStartMessageSendingCommand
import kr.caredoc.careinsurance.reception.caregivingstartmessage.CaregivingStartMessageSendingCommandHandler
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.web.reception.request.CaregivingStartMessageSendingRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/caregiving-start-messages")
class CaregivingStartMessageController(
    private val caregivingStartMessageSendingCommandHandler: CaregivingStartMessageSendingCommandHandler,
) {
    @PostMapping
    fun sendCaregivingStartMessages(
        @RequestBody payload: List<CaregivingStartMessageSendingRequest>,
        subject: Subject,
    ): ResponseEntity<Unit> {
        caregivingStartMessageSendingCommandHandler.sendCaregivingStartMessage(
            CaregivingStartMessageSendingCommand(
                targetReceptionIds = payload.map { it.receptionId }.toSet(),
                subject = subject,
            )
        )

        return ResponseEntity.noContent().build()
    }
}
