package kr.caredoc.careinsurance.web.caregiving.response

import kr.caredoc.careinsurance.caregiving.CaregivingChargeConfirmStatus

data class InvalidCaregivingChargeConfirmStatusEnteredData(
    val currentCaregivingChargeConfirmStatus: CaregivingChargeConfirmStatus,
    val enteredCaregivingChargeConfirmStatus: CaregivingChargeConfirmStatus,
)
