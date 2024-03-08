package kr.caredoc.careinsurance.web.application.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.caredoc.careinsurance.web.application.RequestMetaData
import kr.caredoc.careinsurance.web.application.extractFrom
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.time.LocalDateTime

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class LoggingFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestMetaData = RequestMetaData.extractFrom(request)
        logRequest(requestMetaData)

        filterChain.doFilter(request, response)

        logResponse(response, requestMetaData)
    }

    private fun logRequest(requestMetaData: RequestMetaData) {
        with(requestMetaData) {
            if (isHealthCheckRequest) {
                return
            }

            logger.info("$protocol $method $uri from $requestIp $originIp")
        }
    }

    private fun logResponse(response: HttpServletResponse, requestMetaData: RequestMetaData) {
        with(requestMetaData) {
            if (isHealthCheckRequest && response.status.is2XX()) {
                return
            }

            val elapsedTime = calcElapsedTimeAsMillis(this)
            val responseStatus = HttpStatus.resolve(response.status)?.let {
                "${it.value()} ${it.name}"
            } ?: "??? Unknown"

            logger.info("$protocol $method $uri responses $responseStatus took ${elapsedTime}ms")
        }
    }

    private fun Int.is2XX(): Boolean = this in 200..299

    private fun calcElapsedTimeAsMillis(requestMetaData: RequestMetaData): Long {
        val now = LocalDateTime.now()

        return Duration.between(requestMetaData.arrivedAt, now).toMillis()
    }
}
