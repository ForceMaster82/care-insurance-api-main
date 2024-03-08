package kr.caredoc.careinsurance.caregiving.exception

class CaregiverNotAssignedException(caregivingRoundId: String) :
    RuntimeException("CaregivingRound($caregivingRoundId)에 간병인이 할당되어있지 않습니다.")
