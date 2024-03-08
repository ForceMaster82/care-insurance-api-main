package kr.caredoc.careinsurance.bizcallapi

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Configuration
class BizcallApiConfig(
    @Value("\${bizcall-api.host}")
    private val host: String,
    @Value("\${bizcall-api.apiKey}")
    private val apiKey: String
) {
    fun sendBoxWebClient(): WebClient {
        val sslContext = SslContextBuilder
            .forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .build()
        val httpClient = HttpClient.create().secure { it.sslContext(sslContext) }.wiretap(true)

        val strategies = ExchangeStrategies.builder()
            .codecs { it.defaultCodecs().jackson2JsonEncoder(jsonEncoder()) }.build()

        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(strategies)
            .baseUrl("$host/v1.0")
            .defaultHeaders {
                it.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                it.set("bizcall-api-key", apiKey)
            }
            .build()
    }

    fun webClient(): WebClient {
        val strategies = ExchangeStrategies.builder()
            .codecs { it.defaultCodecs().jackson2JsonEncoder(jsonEncoder()) }.build()

        return WebClient.builder()
            .exchangeStrategies(strategies)
            .baseUrl("$host/v1.0")
            .defaultHeader("bizcall-api-key", apiKey)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build()
    }

    @Bean
    @Primary
    @Profile("local", "dev")
    fun sendBoxBizcallApiService() = BizcallApiService(sendBoxWebClient())

    @Bean
    @Profile("prod", "stage")
    fun bizcallApiService() = BizcallApiService(webClient())

    fun jsonEncoder() = Jackson2JsonEncoder(
        ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL),
        MediaType.APPLICATION_JSON
    )
}
