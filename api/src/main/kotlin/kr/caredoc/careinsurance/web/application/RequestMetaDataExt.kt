package kr.caredoc.careinsurance.web.application

import jakarta.servlet.http.HttpServletRequest
import kr.caredoc.careinsurance.Clock

fun RequestMetaData.Companion.extractFrom(request: HttpServletRequest): RequestMetaData {
    return RequestMetaData(
        method = request.method,
        protocol = request.protocol,
        uri = request.requestURI,
        requestIp = request.remoteAddr,
        originIp = extractOriginIp(request),
        arrivedAt = Clock.now(),
    )
}
