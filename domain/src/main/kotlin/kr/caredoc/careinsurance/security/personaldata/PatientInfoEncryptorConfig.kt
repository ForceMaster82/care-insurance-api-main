package kr.caredoc.careinsurance.security.personaldata

import kr.caredoc.careinsurance.security.encryption.Encryptor
import kr.caredoc.careinsurance.security.hash.PepperedHasher
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.Base64

@Configuration
class PatientInfoEncryptorConfig(
    @Value("\${security.encryption.patient-name-pepper}")
    private val patientNameHashPepper: String
) {
    @Bean
    fun patientInfoEncryptor(
        encryptor: Encryptor,
        patientNameHasher: PepperedHasher,
    ) = PatientInfoEncryptor(encryptor, patientNameHasher)

    @Bean
    fun patientNameHasher() = PepperedHasher(Base64.getDecoder().decode(patientNameHashPepper))
}
