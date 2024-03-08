package kr.caredoc.careinsurance.settlement

interface SettlementsByReceptionIdQueryHandler {
    fun getSettlements(query: SettlementsByReceptionIdQuery): List<Settlement>
}
