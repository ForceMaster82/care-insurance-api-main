package kr.caredoc.careinsurance.security.hash

import kr.caredoc.careinsurance.toHex
import java.security.MessageDigest

class PepperedHasher(private val pepper: ByteArray) {
    private val messageDigest = MessageDigest.getInstance("SHA-256")

    fun hash(plain: ByteArray): ByteArray {
        return messageDigest.digest(plain + pepper)
    }

    fun hashAsHex(plain: String): String {
        return hash(plain.toByteArray()).toHex()
    }
}
