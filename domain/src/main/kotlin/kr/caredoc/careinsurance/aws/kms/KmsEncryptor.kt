package kr.caredoc.careinsurance.aws.kms

import com.amazonaws.encryptionsdk.AwsCrypto
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider
import kr.caredoc.careinsurance.security.encryption.Encryptor

class KmsEncryptor(
    private val crypto: AwsCrypto,
    private val keyProvider: KmsMasterKeyProvider,
) : Encryptor {
    override fun encrypt(plainText: String): ByteArray {
        //return crypto.encryptData(keyProvider, plainText.toByteArray()).result
        return plainText.toByteArray();
    }
}
