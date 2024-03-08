package kr.caredoc.careinsurance.settlement

interface SettlementTransactionRecordingCommandHandler {
    fun recordTransaction(query: SettlementByIdQuery, command: SettlementTransactionRecordingCommand)
}
