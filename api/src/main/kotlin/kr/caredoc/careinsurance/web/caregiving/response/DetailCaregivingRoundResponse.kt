package kr.caredoc.careinsurance.web.caregiving.response

import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import kr.caredoc.careinsurance.caregiving.CaregivingProgressingStatus
import kr.caredoc.careinsurance.caregiving.ClosingReasonType
import kr.caredoc.careinsurance.patient.Sex
import kr.caredoc.careinsurance.reception.AccidentInfo
import kr.caredoc.careinsurance.reception.OrganizationType
import kr.caredoc.careinsurance.reception.ReceptionProgressingStatus
import kr.caredoc.careinsurance.settlement.SettlementProgressingStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

data class DetailCaregivingRoundResponse(
    val id: String,
    val caregivingRoundNumber: Number,
    val caregivingProgressingStatus: CaregivingProgressingStatus,
    val billingProgressingStatus: BillingProgressingStatus,
    val settlementProgressingStatus: SettlementProgressingStatus,
    val startDateTime: OffsetDateTime?,
    val endDateTime: OffsetDateTime?,
    val cancelDateTime: OffsetDateTime?,
    val caregivingRoundClosingReasonType: ClosingReasonType?,
    val caregivingRoundClosingReasonDetail: String?,
    val caregiverInfo: CaregiverInfo?,
    val receptionInfo: ReceptionInfo,
    val remarks: String,
) {
    data class CaregiverInfo(
        val caregiverOrganizationId: String?,
        val name: String,
        val sex: Sex?,
        val birthDate: String?,
        val phoneNumber: String?,
        val insured: Boolean,
        val dailyCaregivingCharge: Int,
        val commissionFee: Int?,
        val accountInfo: AccountInfo,
    )

    data class AccountInfo(
        val bank: String?,
        val accountHolder: String?,
        val accountNumber: String?,
    )

    data class ReceptionInfo(
            val receptionId: String,
            val insuranceNumber: String,
            val accidentNumber: String,
            val patientName: String,
            val patientNickName: String?,
            val patientAge: Int,
            val patientSex: Sex,
            val patientDescription: String,
            val patientPrimaryPhoneNumber: String,
            val hospitalAndRoom: String,
            val receivedDateTime: LocalDateTime,
            val expectedCaregivingStartDate: LocalDate?,
            val receptionProgressingStatus: ReceptionProgressingStatus,
            val caregivingManagerInfo: CaregivingManagerInfo,
    )

    data class CaregivingManagerInfo(
        val organizationType: OrganizationType,
        val organizationId: String?,
        val managingUserId: String,
    )
}
