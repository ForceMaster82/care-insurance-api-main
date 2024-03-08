package kr.caredoc.careinsurance.reception

interface ReceptionApplicationCreationCommandHandler {
    fun createReceptionApplication(receptionApplicationCreationCommand: ReceptionApplicationCreationCommand)
}
