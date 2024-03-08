package kr.caredoc.careinsurance.file

import org.springframework.http.ContentDisposition
import java.time.Duration

data class GeneratingOpenedFileUrlCommand(
    val duration: Duration,
    val contentDisposition: ContentDisposition? = null,
)
