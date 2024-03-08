package kr.caredoc.careinsurance.externalapi

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class ExternalApiConfig(
    @Value("\${external-api.host}")
    host: String,
) {
    val client = WebClient.create(host)

    @Bean
    fun externalApiAlimtalkService() = ExternalApiAlimtalkService(client)
}
