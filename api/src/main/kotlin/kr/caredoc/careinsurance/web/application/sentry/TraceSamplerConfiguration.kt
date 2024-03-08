package kr.caredoc.careinsurance.web.application.sentry

import io.sentry.SentryOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["sentry.traces-sample-rate"])
class TraceSamplerConfiguration(
    @Value("\${sentry.traces-sample-rate}")
    private val samplingRate: String,
    @Value("\${sentry.http-method-blacklist:#{null}}")
    private val methodBlackList: Set<String>?,
) {
    @Bean
    fun tracesSamplerCallback(): SentryOptions.TracesSamplerCallback {
        return FilteredSamplerCallback(
            samplingRate.toDouble(),
            listOfNotNull(
                methodBlackList?.let { HttpMethodBlacklistFilter(it) },
                HttpPathBlacklistFilter(setOf("/actuator/health", "/**", "/actuator/health/readiness", "/actuator/health/liveness"))
            ),
        )
    }
}
