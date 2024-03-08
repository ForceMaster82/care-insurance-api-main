package kr.caredoc.careinsurance.web.caregiving.response

import kr.caredoc.careinsurance.caregiving.CaregivingProgressingStatus

data class InvalidCaregivingProgressingStatusEnteredData(
    val currentCaregivingProgressingStatus: CaregivingProgressingStatus,
    val enteredCaregivingProgressingStatus: CaregivingProgressingStatus,
)
