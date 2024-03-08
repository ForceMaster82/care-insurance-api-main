package kr.caredoc.careinsurance.bizcall

data class BizcallRecipient(
    val mdn: String,
    val taskInfo: Map<String, String>?
)
