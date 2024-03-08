package kr.caredoc.careinsurance.web.reception.request

import kr.caredoc.careinsurance.patient.Sex
import kr.caredoc.careinsurance.reception.ClaimType
import kr.caredoc.careinsurance.reception.OrganizationType
import kr.caredoc.careinsurance.reception.ReceptionProgressingStatus
import java.time.LocalDate
import java.time.OffsetDateTime

data class ReceptionEditingRequest(
    val insuranceInfo: InsuranceInfo,
    val patientInfo: PatientInfo,
    val accidentInfo: AccidentInfo,
    val desiredCaregivingStartDate: LocalDate,
    val desiredCaregivingPeriod: Int?,
    val additionalRequests: String,
    val caregivingManagerInfo: CaregivingManagerInfo?,
    val expectedCaregivingLimitDate: LocalDate,
    val progressingStatus: ReceptionProgressingStatus,
    val reasonForCancellation: String?,
    val notifyCaregivingProgress: Boolean,
    val expectedCaregivingStartDate: LocalDate?,
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
        val height: Int?,
        val weight: Int?,
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
        val hospitalRoomInfo: HospitalAndRoomInfo,
    )

    data class HospitalAndRoomInfo(
        val state: String?,
        val city: String?,
        val hospitalAndRoom: String,
    )

    data class CaregivingManagerInfo(
        val organizationType: OrganizationType,
        val organizationId: String?,
        val managingUserId: String,
    )
}
