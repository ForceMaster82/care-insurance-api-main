package kr.caredoc.careinsurance.caregiving.progressmessage

interface CaregivingProgressMessageSendingCommandHandler {
    fun sendCaregivingProgressMessages(command: CaregivingProgressMessageSendingCommand)
}
