package kr.caredoc.careinsurance.web.caregiving.response

import kr.caredoc.careinsurance.caregiving.CaregivingChargeConfirmStatus
import java.time.LocalDate
import java.time.OffsetDateTime

data class DetailCaregivingChargeResponse(
    val id: String,
    val caregivingRoundInfo: CaregivingRoundInfo,
    val additionalHoursCharge: Int,
    val mealCost: Int,
    val transportationFee: Int,
    val holidayCharge: Int,
    val caregiverInsuranceFee: Int,
    val commissionFee: Int,
    val vacationCharge: Int,
    val patientConditionCharge: Int,
    val covid19TestingCost: Int,
    val outstandingAmount: Int,
    val additionalCharges: List<AdditionalCharge>,
    val isCancelAfterArrived: Boolean,
    val caregivingChargeConfirmStatus: CaregivingChargeConfirmStatus,
    val basicAmount: Int,
    val additionalAmount: Int,
    val totalAmount: Int,
    val expectedSettlementDate: LocalDate,
) {
    data class CaregivingRoundInfo(
        val caregivingRoundId: String,
        val caregivingRoundNumber: Int,
        val startDateTime: OffsetDateTime,
        val endDateTime: OffsetDateTime,
        val dailyCaregivingCharge: Int,
        val receptionId: String,
    )

    data class AdditionalCharge(
        val name: String,
        val amount: Int,
    )
}
