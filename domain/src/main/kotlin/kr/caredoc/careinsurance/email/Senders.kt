package kr.caredoc.careinsurance.email

class Senders(
    private val infoProfileAddress: String,
) {
    operator fun get(profile: SenderProfile): String = when (profile) {
        SenderProfile.INFO -> infoProfileAddress
    }
}
