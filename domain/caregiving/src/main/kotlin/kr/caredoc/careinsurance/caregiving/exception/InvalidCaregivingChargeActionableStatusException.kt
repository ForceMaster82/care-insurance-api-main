package kr.caredoc.careinsurance.caregiving.exception

import kr.caredoc.careinsurance.caregiving.CaregivingProgressingStatus

class InvalidCaregivingChargeActionableStatusException(
    val currentProgressingStatus: CaregivingProgressingStatus
) : RuntimeException("Caregiving Round(progressingStatus: $currentProgressingStatus) 상태로 간병비 산정을 진행할 수 없습니다.")
