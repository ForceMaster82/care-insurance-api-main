package kr.caredoc.careinsurance.file

import java.io.InputStream

data class FileSavingCommand(
    val bucketName: String,
    val path: String,
    val fileStream: InputStream,
    val contentLength: Long,
    val mime: String,
)
