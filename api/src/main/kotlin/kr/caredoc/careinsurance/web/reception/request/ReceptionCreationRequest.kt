package kr.caredoc.careinsurance.web.reception.request

import kr.caredoc.careinsurance.patient.Sex
import kr.caredoc.careinsurance.reception.ClaimType
import kr.caredoc.careinsurance.reception.Reception
import java.time.LocalDate
import java.time.OffsetDateTime

data class ReceptionCreationRequest(
    val insuranceInfo: InsuranceInfo,
    val patientInfo: PatientInfo,
    val accidentInfo: AccidentInfo,
    val insuranceManagerInfo: InsuranceManagerInfo,
    val registerManagerInfo: RegisterManagerInfo,
    val receivedDateTime: OffsetDateTime,
    val desiredCaregivingStartDate: LocalDate,
    val urgency: Reception.Urgency,
    val desiredCaregivingPeriod: String?,
    val notifyCaregivingProgress: Boolean,
    val additionalRequests: String,
) {
    data class InsuranceInfo(
        val insuranceNumber: String,
        val subscriptionDate: LocalDate,
        val coverageId: String,
        val caregivingLimitPeriod: Int,
    )

    data class PatientInfo(
        val name: String,
        val nickname: String?,
        val age: Int,
        val sex: Sex,
        val primaryContact: PatientContact,
        val secondaryContact: PatientContact?,
    )

    data class PatientContact(
        val phoneNumber: String,
        val relationshipWithPatient: String,
    )

    data class AccidentInfo(
        val accidentNumber: String,
        val accidentDateTime: OffsetDateTime,
        val claimType: ClaimType,
        val patientDescription: String,
        val admissionDateTime: OffsetDateTime,
        val hospitalRoomInfo: HospitalAndRoomInfo,
    )

    data class HospitalAndRoomInfo(
        val state: String?,
        val city: String?,
        val hospitalAndRoom: String,
    )

    data class InsuranceManagerInfo(
        val branchName: String,
        val receptionistName: String,
        val phoneNumber: String?,
    )

    data class RegisterManagerInfo(
        val managingUserId: String,
    )
}
