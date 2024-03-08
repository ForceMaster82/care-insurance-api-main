package kr.caredoc.careinsurance.caregiving.exception

import kr.caredoc.careinsurance.caregiving.CaregivingChargeConfirmStatus

class InvalidCaregivingChargeConfirmStatusTransitionException(
    val currentCaregivingChargeConfirmStatus: CaregivingChargeConfirmStatus,
    val enteredCaregivingChargeConfirmStatus: CaregivingChargeConfirmStatus,
) : RuntimeException("Caregiving Charge(confirmStatus: $currentCaregivingChargeConfirmStatus)를 $enteredCaregivingChargeConfirmStatus 상태로 진행할 수 없습니다.")
