package kr.caredoc.careinsurance.aws.s3

import com.github.guepardoapps.kulid.ULID
import kr.caredoc.careinsurance.applyIf
import kr.caredoc.careinsurance.file.FileByUrlQuery
import kr.caredoc.careinsurance.file.FileByUrlQueryHandler
import kr.caredoc.careinsurance.file.FileMeta
import kr.caredoc.careinsurance.file.FileMetaRepository
import kr.caredoc.careinsurance.file.FileSavingCommand
import kr.caredoc.careinsurance.file.FileSavingCommandHandler
import kr.caredoc.careinsurance.file.FileSavingResult
import kr.caredoc.careinsurance.file.GeneratingOpenedFileUrlCommand
import kr.caredoc.careinsurance.file.GeneratingOpenedFileUrlCommandHandler
import kr.caredoc.careinsurance.file.OpenedUrl
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetUrlRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class S3FileService(
    val amazonS3: S3Client,
    val s3PreSigner: S3Presigner,
    val fileMetaRepository: FileMetaRepository
) : FileSavingCommandHandler,
    FileByUrlQueryHandler,
    GeneratingOpenedFileUrlCommandHandler {
    @Transactional
    override fun saveFile(command: FileSavingCommand): FileSavingResult {
        val fileId = ULID.random()

        val fileMeta = FileMeta(
            id = fileId,
            bucket = command.bucketName,
            path = command.path,
        )
        val putObjectRequest = PutObjectRequest.builder()
            .bucket(command.bucketName)
            .key(command.path)
            .contentType(command.mime)
            .contentLength(command.contentLength)
            .build()

        amazonS3.putObject(
            putObjectRequest,
            RequestBody.fromInputStream(command.fileStream, command.contentLength)
        )

        val url = amazonS3.utilities().getUrl(
            GetUrlRequest.builder()
                .bucket(command.bucketName)
                .key(fileId)
                .build()
        ).toString()

        fileMeta.markAsUploaded(url, command.mime, command.contentLength)

        fileMetaRepository.save(fileMeta)

        return FileSavingResult(
            savedFileId = fileId,
            savedFileUrl = url,
        )
    }

    private fun getFile(query: FileByUrlQuery): FileMeta? {
        return fileMetaRepository.findByUrlAndStatus(query.url, FileMeta.Status.UPLOADED)
    }

    @Transactional
    override fun deleteFile(query: FileByUrlQuery) {
        val fileMeta = getFile(query) ?: return

        fileMeta.scheduleDelete()

        fileMetaRepository.save(fileMeta)
    }

    @Transactional
    override fun generateOpenedUrl(query: FileByUrlQuery, command: GeneratingOpenedFileUrlCommand): OpenedUrl? {
        val fileMeta = getFile(query) ?: return null
        val preSignRequest = try {
            s3PreSigner.presignGetObject(
                GetObjectPresignRequest.builder()
                    .signatureDuration(command.duration)
                    .getObjectRequest(
                        GetObjectRequest.builder()
                            .bucket(fileMeta.bucket)
                            .key(fileMeta.path)
                            .applyIf(command.contentDisposition != null) {
                                responseContentDisposition(command.contentDisposition.toString())
                            }
                            .build()
                    )
                    .build()
            )
        } catch (e: NoSuchKeyException) {
            return null
        }

        return OpenedUrl(
            url = preSignRequest.url().toString(),
            expiration = LocalDateTime.ofInstant(preSignRequest.expiration(), ZoneOffset.systemDefault())
        )
    }
}
