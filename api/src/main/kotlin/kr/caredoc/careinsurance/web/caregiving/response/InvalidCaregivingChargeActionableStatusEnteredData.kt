package kr.caredoc.careinsurance.web.caregiving.response

import kr.caredoc.careinsurance.caregiving.CaregivingProgressingStatus

data class InvalidCaregivingChargeActionableStatusEnteredData(
    val currentCaregivingProgressingStatus: CaregivingProgressingStatus,
)
