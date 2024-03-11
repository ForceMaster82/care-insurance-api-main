package kr.caredoc.careinsurance.reception

import kr.caredoc.careinsurance.insurance.InsuranceInfo
import kr.caredoc.careinsurance.patient.PatientInfo
import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import java.time.LocalDate

data class ReceptionEditingCommand(
    val insuranceInfo: InsuranceInfo,
    val patientInfo: PatientInfo,
    val accidentInfo: AccidentInfo,
    val caregivingManagerInfo: CaregivingManagerInfo?,
    val desiredCaregivingStartDate: LocalDate,
    val desiredCaregivingPeriod: String?,
    val additionalRequests: String,
    val expectedCaregivingLimitDate: LocalDate,
    val progressingStatus: ReceptionProgressingStatus,
    val reasonForCancellation: String?,
    val notifyCaregivingProgress: Boolean,
    val expectedCaregivingStartDate: LocalDate?,
    val subject: Subject,
) : Action {
    override fun get(attribute: ActionAttribute) = when (attribute) {
        ActionAttribute.ACTION_TYPE -> setOf(ActionType.MODIFY)
        ActionAttribute.PROCEED_INTO -> setOf(progressingStatus.toString())
        ActionAttribute.INTERNAL_ONLY_MODIFICATIONS -> generateInternalOnlyModifiableProperties()
        else -> setOf()
    }

    private fun generateInternalOnlyModifiableProperties(): Set<String> = sequenceOf(
        "INSURANCE_NUMBER" to this.insuranceInfo.insuranceNumber,
        "SUBSCRIPTION_DATE" to this.insuranceInfo.subscriptionDate,
        "COVERAGE_ID" to this.insuranceInfo.coverageId,
        "CAREGIVING_LIMIT_PERIOD" to this.insuranceInfo.caregivingLimitPeriod,
        "PATIENT_NAME" to this.patientInfo.name,
        "PATIENT_AGE" to this.patientInfo.age,
        "PATIENT_SEX" to this.patientInfo.sex,
        "PATIENT_PRIMARY_PHONE_NUMBER" to this.patientInfo.primaryContact.phoneNumber,
        "PATIENT_PRIMARY_CONTACT_OWNER" to this.patientInfo.primaryContact.relationshipWithPatient,
        "PATIENT_SECONDARY_PHONE_NUMBER" to this.patientInfo.secondaryContact?.phoneNumber,
        "PATIENT_SECONDARY_CONTACT_OWNER" to this.patientInfo.secondaryContact?.relationshipWithPatient,
        "ACCIDENT_NUMBER" to this.accidentInfo.accidentNumber,
        "ACCIDENT_DATE_TIME" to this.accidentInfo.accidentDateTime,
        "CLAIM_TYPE" to this.accidentInfo.claimType,
        "CAREGIVING_ORGANIZATION_TYPE" to this.caregivingManagerInfo?.organizationType,
        "CAREGIVING_ORGANIZATION_ID" to this.caregivingManagerInfo?.organizationId,
        "CAREGIVING_MANAGING_USER_ID" to this.caregivingManagerInfo?.managingUserId,
        "EXPECTED_CAREGIVING_LIMIT_DATE" to this.expectedCaregivingLimitDate,
    ).map {
        "${it.first}:${it.second}"
    }.toSet()
}
