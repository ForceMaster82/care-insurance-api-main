package kr.caredoc.careinsurance.caregiving.exception

import kr.caredoc.careinsurance.caregiving.CaregivingProgressingStatus

class InvalidCaregivingProgressingStatusTransitionException(
    val currentProgressingStatus: CaregivingProgressingStatus,
    val enteredProgressingStatus: CaregivingProgressingStatus,
) : RuntimeException("Caregiving Round(progressingStatus: $currentProgressingStatus)를 $enteredProgressingStatus 상태로 진행할 수 없습니다.")
