package kr.caredoc.careinsurance.reception.caregivingstartmessage

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import kr.caredoc.careinsurance.AggregateRoot
import java.time.LocalDateTime

@Entity
class CaregivingStartMessageSendingHistory(
    id: String,
    val receptionId: String,
    val attemptDateTime: LocalDateTime,
    @Enumerated(EnumType.STRING)
    val result: SendingResult,
    val messageId: String?
) : AggregateRoot(id) {
    enum class SendingResult {
        SENT,
        FAILED,
    }
}
