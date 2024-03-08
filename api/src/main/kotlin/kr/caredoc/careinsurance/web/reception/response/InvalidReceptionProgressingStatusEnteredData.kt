package kr.caredoc.careinsurance.web.reception.response

import kr.caredoc.careinsurance.reception.ReceptionProgressingStatus

data class InvalidReceptionProgressingStatusEnteredData(
    val currentReceptionProgressingStatus: ReceptionProgressingStatus,
    val enteredReceptionProgressingStatus: ReceptionProgressingStatus,
)
