package kr.caredoc.careinsurance.caregiving

import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import java.time.LocalDate

data class CaregivingChargeEditingCommand(
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
    val additionalCharges: List<CaregivingCharge.AdditionalCharge>,
    val isCancelAfterArrived: Boolean,
    val expectedSettlementDate: LocalDate,
    val caregivingChargeConfirmStatus: CaregivingChargeConfirmStatus,
    val subject: Subject,
) : Action {

    override fun get(attribute: ActionAttribute) = when (attribute) {
        ActionAttribute.ACTION_TYPE -> setOf(ActionType.MODIFY)
        else -> setOf()
    }
}
