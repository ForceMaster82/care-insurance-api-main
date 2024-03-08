package kr.caredoc.careinsurance.caregiving.exception

class CaregivingRoundNotFoundByIdException(val caregivingRoundId: String) :
    RuntimeException("CaregivingRound($caregivingRoundId)이 존재하지 않습니다.")
