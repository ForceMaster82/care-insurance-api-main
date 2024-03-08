package kr.caredoc.careinsurance.security.encryption

interface Encryptor {
    fun encrypt(plainText: String): ByteArray
}
