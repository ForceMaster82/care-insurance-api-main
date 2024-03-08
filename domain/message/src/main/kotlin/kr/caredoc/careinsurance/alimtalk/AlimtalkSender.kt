package kr.caredoc.careinsurance.alimtalk

interface AlimtalkSender {
    fun send(message: BulkAlimtalkMessage): List<SendingResultOfMessage>
}
