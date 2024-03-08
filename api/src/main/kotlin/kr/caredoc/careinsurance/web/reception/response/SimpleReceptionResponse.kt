package kr.caredoc.careinsurance.web.reception.response

import kr.caredoc.careinsurance.patient.Sex
import kr.caredoc.careinsurance.reception.OrganizationType
import kr.caredoc.careinsurance.reception.Reception
import kr.caredoc.careinsurance.reception.ReceptionProgressingStatus
import java.time.LocalDate
import java.time.OffsetDateTime

data class SimpleReceptionResponse(
    val id: String,
    val insuranceInfo: InsuranceInfo,
    val patientInfo: PatientInfo,
    val accidentInfo: AccidentInfo,
    val caregivingManagerInfo: CaregivingManagerInfo?,
    val progressingStatus: ReceptionProgressingStatus,
    val desiredCaregivingStartDate: LocalDate,
    val urgency: Reception.Urgency,
    val desiredCaregivingPeriod: Int?,
    val periodType: Reception.PeriodType,
    val receivedDateTime: OffsetDateTime,
) {
    data class InsuranceInfo(
        val insuranceNumber: String,
        val coverageId: String,
    )

    data class PatientInfo(
        val name: String,
        val age: Int,
        val sex: Sex,
        val primaryContact: Contact,
    )

    data class Contact(
        val phoneNumber: String,
        val relationshipWithPatient: String,
    )

    data class AccidentInfo(
        val accidentNumber: String,
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
