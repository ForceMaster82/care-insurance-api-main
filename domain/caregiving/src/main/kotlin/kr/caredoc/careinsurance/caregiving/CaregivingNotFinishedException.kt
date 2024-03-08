package kr.caredoc.careinsurance.caregiving

class CaregivingNotFinishedException(caregivingRoundId: String) :
    RuntimeException("CaregivingRound($caregivingRoundId)는 아직 종료되지 않았습니다.")
