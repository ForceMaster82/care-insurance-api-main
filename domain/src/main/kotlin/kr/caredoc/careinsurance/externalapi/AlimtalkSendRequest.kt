package kr.caredoc.careinsurance.externalapi

data class AlimtalkSendRequest(
    val receivePhoneNumber: String,
    val templateCode: String,
    val messageReplacements: List<Replacement>,
    val buttonReplacements: List<Replacement>,
) {
    data class Replacement(
        val from: String,
        val to: String,
    )
}
