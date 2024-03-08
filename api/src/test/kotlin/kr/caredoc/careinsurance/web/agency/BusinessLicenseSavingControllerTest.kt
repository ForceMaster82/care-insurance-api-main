package kr.caredoc.careinsurance.web.agency

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.agency.BusinessLicenseSavingCommandHandler
import kr.caredoc.careinsurance.relaxedMock
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@CareInsuranceWebMvcTest(BusinessLicenseSavingController::class)
class BusinessLicenseSavingControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean(relaxed = true)
    private val businessLicenseSavingCommandHandler: BusinessLicenseSavingCommandHandler
) : ShouldSpec({
    context("POST /api/v1/external-caregiving-organizations/{external-caregiving-organization-id}/business-license") {
        val externalCaregivingOrganizationId = "01GQ4PE5J0SHCS5BQTJBBKTX8Q"

        val businessLicenseFile = byteArrayOf(1, 1, 1, 1)

        val request =
            multipart("/api/v1/external-caregiving-organizations/$externalCaregivingOrganizationId/business-license")
                .file(
                    MockMultipartFile(
                        "business-license-file",
                        "케어닥 사업자등록증.pdf",
                        MediaType.APPLICATION_PDF_VALUE,
                        businessLicenseFile,
                    )
                ).param("external-caregiving-organization-id", externalCaregivingOrganizationId)

        val savedBusinessLicenseId = "01GQCMWFTFWEJ39F9SZSKV6PS2"
        val businessLicenseFileLocation =
            "http://careinsurance-business-license-dev.s3.ap-northeast-2.amazonaws.com/$savedBusinessLicenseId"

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                businessLicenseSavingCommandHandler.saveBusinessLicenseFile(
                    match {
                        it.externalCaregivingOrganizationId == externalCaregivingOrganizationId
                    }
                )
            } returns relaxedMock {
                every { savedBusinessLicenseFileUrl } returns
                    "http://careinsurance-business-license-dev.s3.ap-northeast-2.amazonaws.com/01GQCMWFTFWEJ39F9SZSKV6PS2"
            }
        }
        afterEach {
            clearAllMocks()
        }

        should("response status should be 201 Created") {
            expectResponse(MockMvcResultMatchers.status().isCreated)
        }

        should("response payload should be empty") {
            expectResponse(MockMvcResultMatchers.content().string(""))
        }

        should("response contains Location header") {
            expectResponse(MockMvcResultMatchers.header().string(HttpHeaders.LOCATION, businessLicenseFileLocation))
        }

        should("사업자 등록증 등록을 도메인 영역에 위임합니다.") {
            mockMvc.perform(request)

            verify {
                businessLicenseSavingCommandHandler.saveBusinessLicenseFile(
                    withArg {
                        it.externalCaregivingOrganizationId shouldBe "01GQ4PE5J0SHCS5BQTJBBKTX8Q"
                        it.businessLicenseFileName shouldBe "케어닥 사업자등록증.pdf"
                        it.mime shouldBe "application/pdf"
                        it.contentLength shouldBe businessLicenseFile.size
                    }
                )
            }
        }
    }
})
