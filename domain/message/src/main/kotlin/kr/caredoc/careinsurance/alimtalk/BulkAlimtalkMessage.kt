package kr.caredoc.careinsurance.alimtalk

data class BulkAlimtalkMessage(
    val templateCode: String,
    val messageParameters: List<MessageParameter>
) {
    data class MessageParameter(
        val id: String,
        val recipient: String,
        val templateData: List<Pair<String, String>>,
    )
}
