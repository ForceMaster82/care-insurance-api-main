package kr.caredoc.careinsurance.web.reception

import kr.caredoc.careinsurance.file.FileByUrlQuery
import kr.caredoc.careinsurance.file.GeneratingOpenedFileUrlCommand
import kr.caredoc.careinsurance.file.GeneratingOpenedFileUrlCommandHandler
import kr.caredoc.careinsurance.reception.ReceptionApplicationByReceptionIdQuery
import kr.caredoc.careinsurance.reception.ReceptionApplicationByReceptionIdQueryHandler
import kr.caredoc.careinsurance.reception.ReceptionApplicationCreationCommand
import kr.caredoc.careinsurance.reception.ReceptionApplicationCreationCommandHandler
import kr.caredoc.careinsurance.reception.ReceptionApplicationFileInfoResult
import kr.caredoc.careinsurance.reception.exception.ReceptionApplicationNotFoundException
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.web.reception.response.EnteredReceptionIdNotRegisteredData
import kr.caredoc.careinsurance.web.reception.response.ReceptionApplicationResponse
import kr.caredoc.careinsurance.web.response.GeneralErrorResponse
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.net.URLEncoder
import java.time.Duration

@RestController
@RequestMapping("/api/v1/receptions/{receptionId}/application")
class ReceptionApplicationController(
    private val receptionApplicationCreationCommandHandler: ReceptionApplicationCreationCommandHandler,
    private val receptionApplicationByReceptionIdQueryHandler: ReceptionApplicationByReceptionIdQueryHandler,
    private val generatingOpenedFileUrlCommandHandler: GeneratingOpenedFileUrlCommandHandler,
) {

    @PostMapping
    fun createApplication(
        @PathVariable receptionId: String,
        @RequestPart("reception-application-file") file: MultipartFile,
        subject: Subject,
    ): ResponseEntity<Unit> {
        receptionApplicationCreationCommandHandler.createReceptionApplication(
            ReceptionApplicationCreationCommand(
                receptionId = receptionId,
                fileName = file.originalFilename ?: "간병인 신청서.pdf",
                file = file.inputStream,
                contentLength = file.size,
                mime = file.contentType.toString(),
                subject = subject,
            )
        )
        return ResponseEntity.noContent().build()
    }

    @GetMapping
    fun getApplication(
        @PathVariable receptionId: String,
        subject: Subject,
    ): ResponseEntity<ReceptionApplicationResponse> {
        val receptionApplicationFileInfo = receptionApplicationByReceptionIdQueryHandler.getReceptionApplication(
            ReceptionApplicationByReceptionIdQuery(
                receptionId,
                subject
            )
        )

        val url = generateReceptionApplicationFileUrl(receptionApplicationFileInfo)
            ?: throw ReceptionApplicationNotFoundException(receptionId)

        return ResponseEntity.ok(
            ReceptionApplicationResponse(
                receptionApplicationFileInfo.receptionApplicationFileName,
                url,
            )
        )
    }

    @DeleteMapping
    fun deleteApplication(
        @PathVariable receptionId: String,
        subject: Subject,
    ): ResponseEntity<Unit> {
        receptionApplicationByReceptionIdQueryHandler.deleteReceptionApplication(
            ReceptionApplicationByReceptionIdQuery(
                receptionId,
                subject,
            )
        )
        return ResponseEntity.noContent().build()
    }

    private fun generateReceptionApplicationFileUrl(fileInfo: ReceptionApplicationFileInfoResult): String? {
        return generateOpenedAttachmentUrl(fileInfo.receptionApplicationFileUrl, fileInfo.receptionApplicationFileName)
    }

    private fun generateOpenedAttachmentUrl(url: String, fileName: String) =
        generatingOpenedFileUrlCommandHandler.generateOpenedUrl(
            query = FileByUrlQuery(url),
            command = GeneratingOpenedFileUrlCommand(
                duration = Duration.ofSeconds(30),
                contentDisposition = ContentDisposition.attachment()
                    .filename(
                        URLEncoder.encode(fileName, "UTF-8").replace("+", "%20")
                    ).build()
            ),
        )?.url

    @ExceptionHandler(ReceptionApplicationNotFoundException::class)
    private fun handleReceptionApplicationNotFound(e: ReceptionApplicationNotFoundException) =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(
                GeneralErrorResponse(
                    message = "지정한 간병인 신청서를 찾을 수 없습니다.",
                    errorType = "RECEPTION_APPLICATION_NOT_FOUND",
                    data = EnteredReceptionIdNotRegisteredData(e.receptionId),
                )
            )
}
