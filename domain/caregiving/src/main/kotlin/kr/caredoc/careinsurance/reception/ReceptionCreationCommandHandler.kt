package kr.caredoc.careinsurance.reception

interface ReceptionCreationCommandHandler {
    fun createReception(command: ReceptionCreationCommand): ReceptionCreationResult
}
