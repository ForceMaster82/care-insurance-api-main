package kr.caredoc.careinsurance.reception.caregivingsatisfactionsurvey

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import kr.caredoc.careinsurance.AggregateRoot
import kr.caredoc.careinsurance.caregiving.LastCaregivingRoundModified
import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import java.time.LocalDate

@Entity
class CaregivingSatisfactionSurveyStatus(
    id: String,
    val receptionId: String,
    val caregivingRoundId: String,
    caregivingRoundEndDate: LocalDate,
) : AggregateRoot(id), Object {
    companion object {
        private fun calculateExpectedSendingDate(caregivingRoundEndDate: LocalDate) = caregivingRoundEndDate.plusDays(1)
    }

    var expectedSendingDate: LocalDate = calculateExpectedSendingDate(caregivingRoundEndDate)
        protected set

    @Enumerated(EnumType.STRING)
    var reservationStatus: ReservationStatus = ReservationStatus.READY
        protected set

    fun handleLastCaregivingRoundModified(event: LastCaregivingRoundModified) {
        event.endDateTime.ifChanged {
            expectedSendingDate = calculateExpectedSendingDate(current.toLocalDate())
        }
    }

    fun markAsReserved(subject: Subject) {
        CaregivingSatisfactionSurveyStatusAccessPolicy.check(subject, SurveyEditingAction, Object.Empty)

        this.reservationStatus = ReservationStatus.RESERVED
    }

    fun markAsFailed(subject: Subject) {
        CaregivingSatisfactionSurveyStatusAccessPolicy.check(subject, SurveyEditingAction, Object.Empty)

        this.reservationStatus = ReservationStatus.FAILED
    }

    private object SurveyEditingAction : Action {
        override fun get(attribute: ActionAttribute) = when (attribute) {
            ActionAttribute.ACTION_TYPE -> setOf(ActionType.MODIFY)
            else -> setOf()
        }
    }
}
