package kr.caredoc.careinsurance.user

interface InternalCaregivingManagerCreationCommandHandler {
    fun createInternalCaregivingManager(command: InternalCaregivingManagerCreationCommand): InternalCaregivingManagerCreationResult
}
