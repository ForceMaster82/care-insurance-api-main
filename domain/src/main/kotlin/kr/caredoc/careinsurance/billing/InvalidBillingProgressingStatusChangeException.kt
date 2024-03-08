package kr.caredoc.careinsurance.billing

class InvalidBillingProgressingStatusChangeException(
    val currentBillingProgressingStatus: BillingProgressingStatus,
    val enteredBillingProgressingStatus: BillingProgressingStatus,
) : RuntimeException("[Billing] $currentBillingProgressingStatus 에서 $enteredBillingProgressingStatus 로 변경할 수 없습니다.")
