package kr.caredoc.careinsurance.caregiving.exception

class CaregivingRoundStartDateTimeNoDataException(val caregivingRoundId: String) :
    RuntimeException("CaregivingRound($caregivingRoundId)의 시작일시 정보가 없습니다.")
