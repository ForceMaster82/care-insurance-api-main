package kr.caredoc.careinsurance.web.application

import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.context.request.NativeWebRequest

private val FORWARDED_HEADER_IP_PATTERN = Regex("""for=(?>"\[)?([.\w:]+)(?>]")?+""")
private val X_FORWARDED_FOR_HEADER_IP_PATTERN = Regex("""([\w.]+)""")

fun extractOriginIp(request: HttpServletRequest): String {
    request.getHeader("Forwarded")?.let {
        extractOriginIpFromForwardedHeader(it)
    }?.let {
        return it
    }

    request.getHeader("X-Forwarded-For")?.let {
        extractOriginIpFromXForwardedForHeader(it)
    }?.let {
        return it
    }

    return request.remoteAddr
}

fun extractOriginIp(request: NativeWebRequest): String? {
    request.getHeader("Forwarded")?.let {
        extractOriginIpFromForwardedHeader(it)
    }?.let {
        return it
    }

    request.getHeader("X-Forwarded-For")?.let {
        extractOriginIpFromXForwardedForHeader(it)
    }?.let {
        return it
    }

    return (request as? HttpServletRequest)?.remoteAddr
}

fun extractOriginIpFromForwardedHeader(forwardedHeaderValue: String): String? {
    return FORWARDED_HEADER_IP_PATTERN.find(forwardedHeaderValue)?.let {
        it.groupValues[1]
    }
}

fun extractOriginIpFromXForwardedForHeader(xForwardedForHeaderValue: String): String? {
    return X_FORWARDED_FOR_HEADER_IP_PATTERN.find(xForwardedForHeaderValue)?.let {
        return it.groupValues[1]
    }
}
