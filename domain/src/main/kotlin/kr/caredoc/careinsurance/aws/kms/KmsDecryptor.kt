package kr.caredoc.careinsurance.aws.kms

import com.amazonaws.encryptionsdk.AwsCrypto
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider
import kr.caredoc.careinsurance.security.encryption.Decryptor

class KmsDecryptor(
    private val crypto: AwsCrypto,
    private val keyProvider: KmsMasterKeyProvider,
) : Decryptor {
    override fun decryptAsString(cipher: ByteArray): String {
        //return crypto.decryptData(keyProvider, cipher).result.toString(charset = Charsets.UTF_8)
        return cipher.toString();
    }
}
