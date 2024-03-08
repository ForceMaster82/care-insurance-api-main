package kr.caredoc.careinsurance.settlement

interface SettlementEditingCommandHandler {
    fun editSettlements(commands: Collection<Pair<SettlementByIdQuery, SettlementEditingCommand>>)
}
