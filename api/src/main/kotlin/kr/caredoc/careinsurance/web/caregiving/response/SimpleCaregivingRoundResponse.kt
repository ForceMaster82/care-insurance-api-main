package kr.caredoc.careinsurance.web.caregiving.response

import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import kr.caredoc.careinsurance.caregiving.CaregivingProgressingStatus
import kr.caredoc.careinsurance.reception.OrganizationType
import kr.caredoc.careinsurance.reception.ReceptionProgressingStatus
import kr.caredoc.careinsurance.settlement.SettlementProgressingStatus
import java.time.LocalDate
import java.time.OffsetDateTime

data class SimpleCaregivingRoundResponse(
    val id: String,
    val caregivingRoundNumber: Int,
    val caregiverName: String?,
    val caregivingProgressingStatus: CaregivingProgressingStatus,
    val settlementProgressingStatus: SettlementProgressingStatus,
    val billingProgressingStatus: BillingProgressingStatus,
    val startDateTime: OffsetDateTime?,
    val endDateTime: OffsetDateTime?,
    val receptionInfo: ReceptionInfo,
) {
    data class ReceptionInfo(
        val receptionId: String,
        val insuranceNumber: String,
        val accidentNumber: String,
        val patientName: String,
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
