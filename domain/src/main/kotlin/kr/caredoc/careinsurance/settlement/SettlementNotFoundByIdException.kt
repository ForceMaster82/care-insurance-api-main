package kr.caredoc.careinsurance.settlement

class SettlementNotFoundByIdException(val settlementId: String) : RuntimeException("Settlement($settlementId)가 존재하지 않습니다.")
