package kr.caredoc.careinsurance.settlement

class SettlementNotFoundByCaregivingRoundIdException(val caregivingRoundId: String) :
    RuntimeException("Settlement(caregivingRoundId: $caregivingRoundId)가 존재하지 않습니다.")
