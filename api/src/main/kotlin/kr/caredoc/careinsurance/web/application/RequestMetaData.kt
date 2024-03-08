package kr.caredoc.careinsurance.web.application

import java.time.LocalDateTime

data class RequestMetaData(
    val method: String,
    val uri: String,
    val protocol: String,
    val requestIp: String,
    val originIp: String,
    val arrivedAt: LocalDateTime,
) {
    companion object

    val isHealthCheckRequest = uri.startsWith("/actuator/health")
}
