package kr.caredoc.careinsurance.web.agency

import kr.caredoc.careinsurance.agency.BusinessLicenseSavingCommand
import kr.caredoc.careinsurance.agency.BusinessLicenseSavingCommandHandler
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.net.URI

@RestController
@RequestMapping("/api/v1/external-caregiving-organizations/{external-caregiving-organization-id}/business-license")
class BusinessLicenseSavingController(
    private val businessLicenseSavingCommandHandler: BusinessLicenseSavingCommandHandler
) {
    @PostMapping
    fun saveBusinessLicenseFile(
        @PathVariable("external-caregiving-organization-id") id: String,
        @RequestParam("business-license-file") file: MultipartFile,
        subject: Subject,
    ) = businessLicenseSavingCommandHandler.saveBusinessLicenseFile(
        BusinessLicenseSavingCommand(
            externalCaregivingOrganizationId = id,
            businessLicenseFile = file.inputStream,
            businessLicenseFileName = file.originalFilename ?: "사업자 등록증",
            mime = file.contentType.toString(),
            contentLength = file.size,
            subject = subject,
        )
    ).let { result ->
        ResponseEntity.created(URI(result.savedBusinessLicenseFileUrl)).build<Unit>()
    }
}
