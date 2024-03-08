package kr.caredoc.careinsurance.web.application.sentry

import io.sentry.SamplingContext
import jakarta.servlet.http.HttpServletRequest

class HttpMethodBlacklistFilter(
    private val blacklistedHttpMethods: Set<String>,
) : SamplingFilter {
    override fun filter(context: SamplingContext): Boolean {
        val servletRequest = context.customSamplingContext?.get("request") as? HttpServletRequest
            ?: return true
        return !blacklistedHttpMethods.contains(servletRequest.method)
    }
}
