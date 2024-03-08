package kr.caredoc.careinsurance.user

interface ExternalCaregivingManagerEditCommandHandler {
    fun editExternalCaregivingManager(query: ExternalCaregivingManagerByIdQuery, command: ExternalCaregivingManagerEditCommand)
    fun editExternalCaregivingManagers(commandByIds: Map<String, ExternalCaregivingManagerEditCommand>)
}
