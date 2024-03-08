package kr.caredoc.careinsurance.web.billing.response

import kr.caredoc.careinsurance.billing.BillingProgressingStatus

data class InvalidBillingProgressingStatusEnteredData(
    val currentBillingProgressingStatus: BillingProgressingStatus,
    val enteredBillingProgressingStatus: BillingProgressingStatus,
)
