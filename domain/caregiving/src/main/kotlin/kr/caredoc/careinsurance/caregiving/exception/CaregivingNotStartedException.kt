package kr.caredoc.careinsurance.caregiving.exception

class CaregivingNotStartedException(caregivingRoundId: String) :
    RuntimeException("CaregivingRound($caregivingRoundId)가 아직 시작되지 않았습니다.")
