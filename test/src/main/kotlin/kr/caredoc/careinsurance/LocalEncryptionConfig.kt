package kr.caredoc.careinsurance

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LocalEncryptionConfig {
    @Bean
    fun decryptor() = LocalEncryption.LocalDecryptor
}
