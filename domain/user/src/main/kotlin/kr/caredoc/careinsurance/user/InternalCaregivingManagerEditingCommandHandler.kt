package kr.caredoc.careinsurance.user

interface InternalCaregivingManagerEditingCommandHandler {
    fun editInternalCaregivingManager(query: InternalCaregivingManagerByIdQuery, command: InternalCaregivingManagerEditingCommand)
}
