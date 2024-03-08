package kr.caredoc.careinsurance.settlement

interface SettlementByCaregivingRoundIdQueryHandler {
    fun getSettlement(query: SettlementByCaregivingRoundIdQuery): Settlement
}
