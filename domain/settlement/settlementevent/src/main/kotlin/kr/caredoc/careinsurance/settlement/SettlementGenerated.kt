package kr.caredoc.careinsurance.settlement

data class SettlementGenerated(
    val caregivingRoundId: String,
    val progressingStatus: SettlementProgressingStatus,
    val settlementId: String,
    val totalAmount: Int,
)
