package kr.caredoc.careinsurance.web.settlement.request

import kr.caredoc.careinsurance.settlement.SettlementProgressingStatus

data class IdentifiedSettlementEditingRequest(
    val id: String,
    val progressingStatus: SettlementProgressingStatus,
    val settlementManagerId: String,
)
