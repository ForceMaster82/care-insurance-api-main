package kr.caredoc.careinsurance.web.settlement.response

import kr.caredoc.careinsurance.settlement.SettlementProgressingStatus

data class InvalidSettlementProgressingStatusTransitionData(
    val currentSettlementProgressingStatus: SettlementProgressingStatus,
    val enteredSettlementProgressingStatus: SettlementProgressingStatus,
)
