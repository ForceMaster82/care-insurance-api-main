package kr.caredoc.careinsurance.file

import java.time.LocalDateTime

data class OpenedUrl(
    val url: String,
    val expiration: LocalDateTime,
)
