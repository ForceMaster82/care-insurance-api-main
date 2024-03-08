package kr.caredoc.careinsurance.billing

interface BillingTransactionRecordingCommandHandler {
    fun recordTransaction(query: BillingByIdQuery, command: BillingTransactionRecordingCommand)
}
