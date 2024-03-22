package kr.caredoc.careinsurance.web.caregiving.request

import kr.caredoc.careinsurance.caregiving.CaregivingProgressingStatus
import kr.caredoc.careinsurance.caregiving.ClosingReasonType
import kr.caredoc.careinsurance.patient.Sex
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

data class CaregivingRoundEditingRequest(
        val caregivingProgressingStatus: CaregivingProgressingStatus,
        val startDateTime: OffsetDateTime?,
        val endDateTime: OffsetDateTime?,
        val caregivingRoundClosingReasonType: ClosingReasonType?,
        val caregivingRoundClosingReasonDetail: String?,
        val caregiverInfo: CaregiverInfo? = null,
        val remarks: String,
        val expectedSettlementDate: LocalDate?,
) {
    data class CaregiverInfo(
        val caregiverOrganizationId: String?,
        val name: String,
        val sex: Sex,
        val birthDate: String,
        val insured: Boolean,
        val phoneNumber: String,
        val dailyCaregivingCharge: Int,
        val commissionFee: Int,
        val accountInfo: AccountInfo,
    )

    data class AccountInfo(
        val bank: String?,
        val accountNumber: String?,
        val accountHolder: String?,
    )
}
