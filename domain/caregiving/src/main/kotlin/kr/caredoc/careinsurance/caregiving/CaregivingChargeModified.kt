package kr.caredoc.careinsurance.caregiving

import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import java.time.LocalDate
import java.time.LocalDateTime

data class CaregivingChargeModified(
    val receptionId: String,
    val caregivingRoundId: String,
    val caregivingRoundNumber: Int,
    val basicAmount: Int,
    val additionalAmount: Int,
    val totalAmount: Int,
    val additionalHoursCharge: Modification<Int>,
    val mealCost: Modification<Int>,
    val transportationFee: Modification<Int>,
    val holidayCharge: Modification<Int>,
    val caregiverInsuranceFee: Modification<Int>,
    val commissionFee: Modification<Int>,
    val vacationCharge: Modification<Int>,
    val patientConditionCharge: Modification<Int>,
    val covid19TestingCost: Modification<Int>,
    val additionalCharges: Modification<List<CaregivingCharge.AdditionalCharge>>,
    val outstandingAmount: Modification<Int>,
    val expectedSettlementDate: Modification<LocalDate>,
    val isCancelAfterArrived: Modification<Boolean>,
    val confirmStatus: CaregivingChargeConfirmStatus,
    val editingSubject: Subject,
) {
    val calculatedDateTime: LocalDateTime = Clock.now()
}
