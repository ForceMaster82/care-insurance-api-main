package kr.caredoc.careinsurance.email

import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import kr.caredoc.careinsurance.AggregateRoot
import java.time.LocalDateTime

@Entity
class EmailSendingLog protected constructor(
    id: String,
    @Access(AccessType.FIELD)
    val recipientAddress: String,
    val senderAddress: String,
    val senderProfile: String,
    val title: String,
    @Enumerated(EnumType.STRING)
    val result: SendingResult,
    val sentDateTime: LocalDateTime?,
    val reasonForFailure: ReasonForFailure?,
) : AggregateRoot(id) {
    companion object {
        fun ofSent(
            id: String,
            recipientAddress: String,
            senderAddress: String,
            senderProfile: String,
            title: String,
            sentDateTime: LocalDateTime,
        ) = EmailSendingLog(
            id = id,
            recipientAddress = recipientAddress,
            senderAddress = senderAddress,
            senderProfile = senderProfile,
            title = title,
            result = SendingResult.SENT,
            sentDateTime = sentDateTime,
            reasonForFailure = null,
        )

        fun ofFailed(
            id: String,
            recipientAddress: String,
            senderAddress: String,
            senderProfile: String,
            title: String,
            exception: Exception,
        ) = EmailSendingLog(
            id = id,
            recipientAddress = recipientAddress,
            senderAddress = senderAddress,
            senderProfile = senderProfile,
            title = title,
            result = SendingResult.FAILED,
            sentDateTime = null,
            reasonForFailure = ReasonForFailure(
                reasonForFailure = exception.javaClass.name,
                failureMessage = exception.message ?: "",
            ),
        )
    }

    enum class SendingResult {
        SENT,
        FAILED,
    }

    @Embeddable
    data class ReasonForFailure(
        @Access(AccessType.FIELD)
        val reasonForFailure: String,
        val failureMessage: String
    )
}
