package kr.caredoc.careinsurance.web.reception.response

import kr.caredoc.careinsurance.patient.Sex
import kr.caredoc.careinsurance.reception.ClaimType
import kr.caredoc.careinsurance.reception.OrganizationType
import kr.caredoc.careinsurance.reception.Reception
import kr.caredoc.careinsurance.reception.ReceptionProgressingStatus
import java.time.LocalDate
import java.time.OffsetDateTime

data class DetailReceptionResponse(
    val id: String,
    val insuranceInfo: InsuranceInfo,
    val patientInfo: PatientInfo,
    val accidentInfo: AccidentInfo,
    val insuranceManagerInfo: InsuranceManagerInfo,
    val caregivingManagerInfo: CaregivingManagerInfo?,
    val registerManagerInfo: RegisterManagerInfo,
    val desiredCaregivingStartDate: LocalDate,
    val urgency: Reception.Urgency,
    val desiredCaregivingPeriod: Int?,
    val additionalRequests: String,
    val expectedCaregivingStartDate: LocalDate?,
    val expectedCaregivingLimitDate: LocalDate,
    val progressingStatus: ReceptionProgressingStatus,
    val periodType: Reception.PeriodType,
    val notifyCaregivingProgress: Boolean,
    val reasonForCancellation: String?,
    val canceledDateTime: OffsetDateTime?,
    val receivedDateTime: OffsetDateTime,
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
        val weight: Int?,
        val height: Int?,
        val primaryContact: Contact,
        val secondaryContact: Contact?,
    )

    data class Contact(
        val phoneNumber: String,
        val relationshipWithPatient: String,
    )

    data class AccidentInfo(
        val accidentNumber: String,
        val accidentDateTime: OffsetDateTime,
        val claimType: ClaimType,
        val patientDescription: String,
        val admissionDateTime: OffsetDateTime,
        val hospitalRoomInfo: HospitalRoomInfo,
    )

    data class InsuranceManagerInfo(
        val branchName: String,
        val receptionistName: String,
        val phoneNumber: String?,
    )

    data class CaregivingManagerInfo(
        val organizationType: OrganizationType,
        val organizationId: String?,
        val managingUserId: String
    )

    data class RegisterManagerInfo(
        val managingUserId: String,
    )

    data class HospitalRoomInfo(
        val state: String?,
        val city: String?,
        val hospitalAndRoom: String
    )
}
