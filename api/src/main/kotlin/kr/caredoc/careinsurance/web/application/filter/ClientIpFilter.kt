package kr.caredoc.careinsurance.web.application.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.caredoc.careinsurance.web.application.extractOriginIp
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class ClientIpFilter(
    @Value("\${security.allowed-client-ip-addresses:}")
    private val allowedClientIpAddresses: Set<String>,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (allowedClientIpAddresses.isEmpty() || allowedClientIpAddresses.contains(extractOriginIp(request))) {
            filterChain.doFilter(request, response)
            return
        }

        logger.warn("request arrived from not-allowed ip address. client ip: ${extractOriginIp(request)}")

        response.status = HttpStatus.FORBIDDEN.value()
    }
}
