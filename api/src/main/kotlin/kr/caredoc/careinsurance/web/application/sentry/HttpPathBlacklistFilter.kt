package kr.caredoc.careinsurance.web.application.sentry

import io.sentry.SamplingContext
import jakarta.servlet.http.HttpServletRequest

class HttpPathBlacklistFilter(
    private val blacklistedHttpPaths: Set<String>,
) : SamplingFilter {
    override fun filter(context: SamplingContext): Boolean {
        val servletRequest = context.customSamplingContext?.get("request") as? HttpServletRequest
            ?: return true
        return !blacklistedHttpPaths.contains(servletRequest.requestURI)
    }
}
