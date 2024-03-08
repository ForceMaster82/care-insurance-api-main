package kr.caredoc.careinsurance

import kr.caredoc.careinsurance.security.encryption.Decryptor
import kr.caredoc.careinsurance.security.encryption.Encryptor
import kr.caredoc.careinsurance.security.hash.PepperedHasher
import kr.caredoc.careinsurance.security.personaldata.PatientInfoEncryptor
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

object LocalEncryption {
    val keySpec = SecretKeySpec(Random.nextBytes(32), "AES")
    val patientNameHasher = PepperedHasher(Random.nextBytes(32))
    val patientInfoEncryptor = PatientInfoEncryptor(LocalEncryptor, patientNameHasher)

    object LocalEncryptor : Encryptor {
        override fun encrypt(plainText: String): ByteArray {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
            val iv = Random.nextBytes(16)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(iv))
            return iv + cipher.doFinal(plainText.toByteArray())
        }
    }

    object LocalDecryptor : Decryptor {
        override fun decryptAsString(cipher: ByteArray): String {
            val iv = cipher.slice(0 until 16).toByteArray()
            val encrypted = cipher.slice(16 until cipher.size).toByteArray()
            val cipherInstance = Cipher.getInstance("AES/CBC/PKCS5PADDING")
            cipherInstance.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(iv))
            return cipherInstance.doFinal(encrypted).toString(Charsets.UTF_8)
        }
    }
}
