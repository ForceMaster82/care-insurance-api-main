package kr.caredoc.careinsurance.email

data class Email(
    val title: String,
    val content: String,
    val recipient: String,
    val senderProfile: SenderProfile,
)
