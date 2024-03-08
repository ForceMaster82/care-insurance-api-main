package kr.caredoc.careinsurance.web.caregiving.request

import kr.caredoc.careinsurance.caregiving.CaregivingChargeConfirmStatus
import java.time.LocalDate

data class CaregivingChargeEditingRequest(
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
    val expectedSettlementDate: LocalDate,
    val caregivingChargeConfirmStatus: CaregivingChargeConfirmStatus,
) {
    data class AdditionalCharge(
        val name: String,
        val amount: Int,
    )
}
