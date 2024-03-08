package kr.caredoc.careinsurance.reception.caregivingstartmessage

interface CaregivingStartMessageSendingCommandHandler {
    fun sendCaregivingStartMessage(command: CaregivingStartMessageSendingCommand)
}
