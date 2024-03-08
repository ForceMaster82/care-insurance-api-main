package kr.caredoc.careinsurance.security.encryption

interface Decryptor {
    fun decryptAsString(cipher: ByteArray): String
}
