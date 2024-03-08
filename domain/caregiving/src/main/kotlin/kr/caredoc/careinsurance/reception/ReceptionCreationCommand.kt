package kr.caredoc.careinsurance.reception

import kr.caredoc.careinsurance.insurance.InsuranceInfo
import kr.caredoc.careinsurance.insurance.InsuranceManagerInfo
import kr.caredoc.careinsurance.patient.PatientInfo
import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import java.time.LocalDate
import java.time.LocalDateTime

data class ReceptionCreationCommand(
    val insuranceInfo: InsuranceInfo,
    val patientInfo: PatientInfo,
    val accidentInfo: AccidentInfo,
    val insuranceManagerInfo: InsuranceManagerInfo,
    val registerManagerInfo: RegisterManagerInfo,
    val receivedDateTime: LocalDateTime,
    val desiredCaregivingStartDate: LocalDate,
    val urgency: Reception.Urgency,
    val desiredCaregivingPeriod: Int?,
    val additionalRequests: String,
    val notifyCaregivingProgress: Boolean,
    val subject: Subject,
) : Action {
    override fun get(attribute: ActionAttribute) = when (attribute) {
        ActionAttribute.ACTION_TYPE -> setOf(ActionType.CREATE)
        else -> setOf()
    }
}
