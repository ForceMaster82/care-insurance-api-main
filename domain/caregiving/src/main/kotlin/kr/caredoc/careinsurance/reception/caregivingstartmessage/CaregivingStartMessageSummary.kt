package kr.caredoc.careinsurance.reception.caregivingstartmessage

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import kr.caredoc.careinsurance.AggregateRoot
import kr.caredoc.careinsurance.caregiving.CaregivingRoundModified
import kr.caredoc.careinsurance.message.SendingStatus
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
class CaregivingStartMessageSummary(
    id: String,
    val receptionId: String,
    val firstCaregivingRoundId: String,
    caregivingRoundStartDate: LocalDate,
) : AggregateRoot(id) {
    companion object {
        private fun calcSendingDate(caregivingRoundStartDate: LocalDate) = caregivingRoundStartDate.plusDays(1)
    }

    @Enumerated(EnumType.STRING)
    var sendingStatus: SendingStatus = SendingStatus.READY
        protected set
    var sentDate: LocalDate? = null
        protected set
    var expectedSendingDate: LocalDate = calcSendingDate(caregivingRoundStartDate)
        protected set

    fun handleCaregivingRoundModified(event: CaregivingRoundModified) {
        event.startDateTime.current?.toLocalDate()?.let {
            this.expectedSendingDate = calcSendingDate(it)
        }
    }

    fun updateSendingResult(sendingStatus: SendingStatus, sentDateTime: LocalDateTime?) {
        this.sendingStatus = sendingStatus
        if (sendingStatus == SendingStatus.SENT && sentDateTime != null) {
            this.sentDate = sentDateTime.toLocalDate()
        }
    }
}
