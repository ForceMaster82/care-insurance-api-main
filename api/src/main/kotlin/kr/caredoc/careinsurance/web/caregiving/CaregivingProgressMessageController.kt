package kr.caredoc.careinsurance.web.caregiving

import kr.caredoc.careinsurance.caregiving.progressmessage.CaregivingProgressMessageSendingCommand
import kr.caredoc.careinsurance.caregiving.progressmessage.CaregivingProgressMessageSendingCommandHandler
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.web.caregiving.request.CaregivingProgressMessageSendingRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/caregiving-progress-messages")
class CaregivingProgressMessageController(
    private val caregivingProgressMessageSendingCommandHandler: CaregivingProgressMessageSendingCommandHandler,
) {
    @PostMapping
    fun sendCaregivingProgressMessages(
        @RequestBody payload: List<CaregivingProgressMessageSendingRequest>,
        subject: Subject,
    ): ResponseEntity<Unit> {
        caregivingProgressMessageSendingCommandHandler.sendCaregivingProgressMessages(
            CaregivingProgressMessageSendingCommand(
                targetCaregivingRoundIds = payload.map { it.caregivingRoundId }.toSet(),
                subject = subject
            )
        )
        return ResponseEntity.noContent().build()
    }
}
