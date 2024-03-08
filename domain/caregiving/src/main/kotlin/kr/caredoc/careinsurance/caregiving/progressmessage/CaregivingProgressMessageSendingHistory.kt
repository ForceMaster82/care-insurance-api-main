package kr.caredoc.careinsurance.caregiving.progressmessage

import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import kr.caredoc.careinsurance.AggregateRoot
import java.time.LocalDateTime

@Entity
class CaregivingProgressMessageSendingHistory(
    id: String,
    @Access(AccessType.FIELD)
    val caregivingRoundId: String,
    val attemptDateTime: LocalDateTime,
    @Enumerated(EnumType.STRING)
    val result: SendingResult,
    val messageId: String?,
) : AggregateRoot(id) {
    enum class SendingResult {
        SENT,
        FAILED,
    }
}
