package kr.caredoc.careinsurance.settlement

class InvalidSettlementProgressingStatusTransitionException(
    val currentSettlementProgressingStatus: SettlementProgressingStatus,
    val enteredSettlementProgressingStatus: SettlementProgressingStatus,
) : RuntimeException("Settlement(progressingStatus: $currentSettlementProgressingStatus)를 $enteredSettlementProgressingStatus 상태로 진행할 수 없습니다.")
