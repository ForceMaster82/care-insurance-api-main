package kr.caredoc.careinsurance.email

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SenderConfig(
    @Value("\${email.senders.info.address}")
    private val infoProfileAddress: String
) {
    @Bean
    fun senders() = Senders(
        infoProfileAddress = infoProfileAddress,
    )
}
