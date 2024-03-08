package kr.caredoc.careinsurance.reception.exception

import kr.caredoc.careinsurance.reception.ReceptionProgressingStatus

class InvalidReceptionProgressingStatusTransitionException(
    val currentProgressingStatus: ReceptionProgressingStatus,
    val enteredProgressingStatus: ReceptionProgressingStatus,
) : RuntimeException("Reception(progressingStatus: $currentProgressingStatus)를 $enteredProgressingStatus 상태로 진행할 수 없습니다.")
