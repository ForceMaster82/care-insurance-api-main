package kr.caredoc.careinsurance.aws.kms

import com.amazonaws.encryptionsdk.AwsCrypto
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KmsConfig(
    @Value("\${security.encryption.kms.key-arn}")
    private val keyArn: String
) {
    @Bean
    fun kmsDecryptor(
        awsCrypto: AwsCrypto,
        keyProvider: KmsMasterKeyProvider,
    ) = KmsDecryptor(awsCrypto, keyProvider)

    @Bean
    fun kmsEncryptor(
        awsCrypto: AwsCrypto,
        keyProvider: KmsMasterKeyProvider,
    ) = KmsEncryptor(awsCrypto, keyProvider)

    @Bean
    fun keyProvider(): KmsMasterKeyProvider = KmsMasterKeyProvider.builder()
        .buildStrict(keyArn)

    @Bean
    fun awsCrypto(): AwsCrypto = AwsCrypto.standard()
}
