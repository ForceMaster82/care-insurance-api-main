package kr.caredoc.careinsurance.caregiving.progressmessage

import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import kr.caredoc.careinsurance.AggregateRoot
import kr.caredoc.careinsurance.caregiving.CaregivingProgressingStatus
import kr.caredoc.careinsurance.caregiving.CaregivingRoundModified
import kr.caredoc.careinsurance.message.SendingStatus
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
class CaregivingProgressMessageSummary private constructor(
    id: String,
    @Access(AccessType.FIELD)
    val caregivingRoundId: String,
    val caregivingRoundNumber: Int,
    startDateTime: LocalDateTime,
    expectedSendingDate: LocalDate,
    val receptionId: String,
) : AggregateRoot(id) {
    fun willBeAffectedBy(event: CaregivingRoundModified): Boolean {
        return event.caregivingProgressingStatus.hasChanged && event.caregivingProgressingStatus.current != caregivingProgressingStatus
    }

    companion object {

        fun calculateExpectedSendingDate(caregivingRoundNumber: Int, startDateTime: LocalDateTime): LocalDate {
            return if (caregivingRoundNumber == 1) {
                startDateTime.plusDays(5).toLocalDate()
            } else {
                startDateTime.plusDays(10).toLocalDate()
            }
        }
    }

    var startDateTime = startDateTime
        protected set

    @Enumerated(EnumType.STRING)
    var caregivingProgressingStatus: CaregivingProgressingStatus = CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
        protected set

    var expectedSendingDate = expectedSendingDate
        protected set

    @Enumerated(EnumType.STRING)
    var sendingStatus: SendingStatus = SendingStatus.READY
        protected set

    var sentDate: LocalDate? = null
        protected set

    fun handleCaregivingRoundModified(event: CaregivingRoundModified) {
        event.startDateTime.ifChanged {
            val startDateTime = event.startDateTime.current
            if (startDateTime != null) {
                expectedSendingDate = calculateExpectedSendingDate(event.caregivingRoundNumber, startDateTime)
            }
        }
        event.caregivingProgressingStatus.ifChanged {
            caregivingProgressingStatus = event.caregivingProgressingStatus.current
        }
    }

    constructor(
        id: String,
        caregivingRoundId: String,
        caregivingRoundNumber: Int,
        startDateTime: LocalDateTime,
        receptionId: String,
    ) : this (
        id = id,
        caregivingRoundId = caregivingRoundId,
        caregivingRoundNumber = caregivingRoundNumber,
        startDateTime = startDateTime,
        expectedSendingDate = calculateExpectedSendingDate(caregivingRoundNumber, startDateTime),
        receptionId = receptionId,
    )

    fun updateSendingResult(sendingStatus: SendingStatus, sentDateTime: LocalDateTime?) {
        this.sendingStatus = sendingStatus
        if (sendingStatus == SendingStatus.SENT && sentDateTime != null) {
            this.sentDate = sentDateTime.toLocalDate()
        }
    }

    fun handleCaregivingModified(event: CaregivingRoundModified) {
        event.caregivingProgressingStatus.ifChanged { caregivingProgressingStatus = current }
    }
}
