package kr.caredoc.careinsurance.web.application.filter

import com.github.guepardoapps.kulid.ULID
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.caredoc.careinsurance.web.application.RequestMetaData
import kr.caredoc.careinsurance.web.application.extractFrom
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class MDCFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        fillMdc(request)
        filterChain.doFilter(request, response)
        clearMdc()
    }

    private fun fillMdc(request: HttpServletRequest) {
        with(RequestMetaData.extractFrom(request)) {
            MDC.put("method", method)
            MDC.put("uri", uri)
            MDC.put("protocol", protocol)
            MDC.put("requestIp", requestIp)
            MDC.put("originIp", originIp)
        }
        MDC.put("span", ULID.random())
    }

    private fun clearMdc() {
        MDC.remove("span")
        MDC.remove("method")
        MDC.remove("uri")
        MDC.remove("protocol")
        MDC.remove("requestIp")
        MDC.remove("originIp")
    }
}
