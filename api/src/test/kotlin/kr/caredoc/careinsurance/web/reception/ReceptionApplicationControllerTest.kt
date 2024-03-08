package kr.caredoc.careinsurance.web.reception

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.file.GeneratingOpenedFileUrlCommandHandler
import kr.caredoc.careinsurance.reception.ReceptionApplicationByReceptionIdQueryHandler
import kr.caredoc.careinsurance.reception.ReceptionApplicationCreationCommandHandler
import kr.caredoc.careinsurance.reception.exception.ReceptionApplicationNotFoundException
import kr.caredoc.careinsurance.reception.exception.ReceptionNotFoundByIdException
import kr.caredoc.careinsurance.relaxedMock
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Duration
import java.time.LocalDateTime

@CareInsuranceWebMvcTest(ReceptionApplicationController::class)
class ReceptionApplicationControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean
    private val receptionApplicationCreationCommandHandler: ReceptionApplicationCreationCommandHandler,
    @MockkBean
    private val receptionApplicationByReceptionIdQueryHandler: ReceptionApplicationByReceptionIdQueryHandler,
    @MockkBean
    private val generatingOpenedFileUrlCommandHandler: GeneratingOpenedFileUrlCommandHandler,
) : ShouldSpec({
    context("간병인 신청서를 업로드할 때") {
        val receptionId = "01H2WAFFHKT2Q0RE7N1BVDRAET"

        val mockMultipartFile = MockMultipartFile(
            "reception-application-file",
            "간병인 신청서.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            byteArrayOf(1, 2, 3, 4, 5, 6).inputStream()
        )

        val request = multipart("/api/v1/receptions/$receptionId/application")
            .file(mockMultipartFile)

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            justRun { receptionApplicationCreationCommandHandler.createReceptionApplication(match { it.receptionId == receptionId }) }
        }

        afterEach { clearAllMocks() }

        should("상태 코드 204 No Content 를 응답합니다.") {
            expectResponse(status().isNoContent)
        }

        should("응답 페이로드는 비어있어야 합니다.") {
            expectResponse(content().string(""))
        }

        should("간병인 신청서의 업로드를 도메인에 위임합니다.") {
            mockMvc.perform(request)

            verify {
                receptionApplicationCreationCommandHandler.createReceptionApplication(
                    withArg {
                        it.receptionId shouldBe receptionId
                        it.fileName shouldBe mockMultipartFile.originalFilename
                        it.contentLength shouldBe mockMultipartFile.size
                        it.mime shouldBe mockMultipartFile.contentType
                    }
                )
            }
        }

        context("간병 접수가 존재하지 않다면") {
            beforeEach {
                every { receptionApplicationCreationCommandHandler.createReceptionApplication(any()) } throws ReceptionNotFoundByIdException(
                    receptionId
                )
            }

            afterEach { clearAllMocks() }

            should("응답에는 message, errorType 을 포함하고 있습니다. ") {
                expectResponse(
                    content().json(
                        """
                          {
                            "message": "조회하고자 하는 간병 접수가 존재하지 않습니다.",
                            "errorType": "RECEPTION_NOT_EXISTS"
                          }
                        """.trimIndent()
                    )
                )
            }

            should("응답 데이터에는 입력된 reception id 를 포함하고 있습니다.") {
                expectResponse(
                    content().json(
                        """
                          {
                            "data": {
                              "enteredReceptionId": "$receptionId"
                            }
                          }
                        """.trimIndent()
                    )
                )
            }
        }
    }

    context("간병인 신청서를 조회할 때") {
        val receptionId = "01H3E9RBZ726C1M9A8ZHJTW58R"
        val path = "01H3H248BPCV7S2RJFFM0XZ4DJ"

        val request = get("/api/v1/receptions/$receptionId/application")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every { receptionApplicationByReceptionIdQueryHandler.getReceptionApplication(any()) } returns relaxedMock {
                every { receptionApplicationFileName } returns "간병인 신청서.pdf"
                every { receptionApplicationFileUrl } returns "https://careinsurance-reception-application-dev.s3.ap-northeast-2.amazonaws.com/$path"
            }

            every {
                generatingOpenedFileUrlCommandHandler.generateOpenedUrl(
                    match {
                        it.url == "https://careinsurance-reception-application-dev.s3.ap-northeast-2.amazonaws.com/$path"
                    },
                    match {
                        it.duration == Duration.ofSeconds(30)
                    }
                )
            } returns relaxedMock {
                every { url } returns "https://careinsurance-reception-application-dev.s3.ap-northeast-2.amazonaws.com/$path\\?response-content-disposition\\=attachment%3B%20filename%3D%22%EA%B0%84%EB%B3%91%EC%9D%B8%20%EC%8B%A0%EC%B2%AD%EC%84%9C%2E%70%64%66%22\\&X-Amz-Security-Token\\=IQoJb3JpZ2luX2VjEIr%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaDmFwLW5vcnRoZWFzdC0yIkcwRQIgeyH3OZwCz3si81O1DehWPDfgbRkd%2FAsqR12a2OjSJwcCIQC8e5x3hg6QqnvZpQvEroXBMWBC4Zj2juv3BmEwuBYB1CrWBQjT%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F8BEAIaDDMxOTc0NzgzMjMxNSIMQbWsUtm%2FL2fLMvibKqoF5aNCHmR5%2Foxtlll8di7g7tN1jVQldY4WMDSd0QL9ovYdR0Jekcx5eYRrL5Vn8daWOqIbaUlwAkJKPjdUzGSI2zDKt7tN%2FDtzy1sM0ZoZ5NWJSxLNIJUyJZCYthak6Hp6Rvc1ONreCpCOhuAjj4qfEBGG%2Bg%2BBed9Q3Xm7DrBlE8TAe60tylPje4Isn%2B7dQPZap06zeoRucBTeaqxO3s2dAcnCfuvKO9d0%2BsBkNK8u7bVG71Pq4vzHihvz%2F0WOvrrqUnHbx%2BVR1Ym8ucUdd2we7gQgmKoKGvfPup%2FhN0LcHYAjANfSo4fD8%2FQnIQrM9x%2BD4yokrF5ESXZFzhEIzZ8WJ2jw6mUnrk1KtBprt4sZtjOGntSZBbUfC%2B4LICsAwM%2BpfvgnG9qVyfb8l140NIZ%2B9I%2BGDITbVYoeXi7UHQ%2B9TlNYS%2Bcxh0UjCjbNw8qIsHkfFUgVsoCjU%2BiHuHv6FETA60XcFwdVXTa01wisuGhDgMGDMfO5BQ1IpzAo0%2Fgf9EHynxOBDDUd24QYzVMj%2BO56YAa1KmXsUvIxri0lD5XRA%2FT5k4uZYgHG%2BJi1LOJB8Q%2FMaNwYklI3rzUmcm%2FN73JAwOdJWBmk6g6auhNXx8NtPPBKEqqDPhGYgGPXO%2BmporTjNM9w0xaPnm9x10PEuAQ5I1z7eHX8WnpJaI16oWm6BW4FR9mlq4Un%2BkEP6B5%2FGlKcWIDxQeY6BN%2BOtDLu8hS7wOO6EBOpxhEphMP8%2BoTfJ%2FwJ0XconPByDHwXmzIQ%2BZ6FXRyEDALr5LzuKeSm1V2IihCwjKxL24nwkA9qXC%2FL5xiwL%2BdpiNVn%2F1hbQOWlGz2uxDuonZ4NiKU3aBz2%2B6OBqKZTYNQMNL1t5IiiPAD658WzaMPBPehuBrd69%2FUExDrFgkrHn1nS381XXDDn0JukBjqxAX7lPY%2FWoaWSrgEFNaz0u82e1xJkaFI9o4FDMhkKQU%2Fm5kCnQcQox9Om2WK7G1SkDGApghtAkbPN9TCmCvtjSD%2BYTHvKfrfRT8f%2B404RekmHTO7A1Yvij%2FEr8Fcc5XS0lyUqKa8AH6mRB4DEde9Z98JLciW0QKWWszuCBDtdMy0AP%2FWYcoxUNfJzLUXlfDAv9yvOV46QzXAuVM5DLLvS%2BydkTdgeYa%2BC7GGgZoy2JW7cfg%3D%3D\\&X-Amz-Algorithm\\=AWS4-HMAC-SHA256\\&X-Amz-Date\\=20230612T094830Z\\&X-Amz-SignedHeaders\\=host\\&X-Amz-Expires\\=30\\&X-Amz-Credential\\=ASIAUU4TUXH57VTS7VXM%2F20230612%2Fap-northeast-2%2Fs3%2Faws4_request\\&X-Amz-Signature\\=d24e0ccb5c4d5f29e9e5c26e836fcc4ec4ae60006be45436d843105ea6eee8b9"
                every { expiration } returns LocalDateTime.of(2023, 6, 22, 12, 30, 30)
            }
        }

        afterEach { clearAllMocks() }

        should("상태 코드는 200 Ok 입니다.") {
            expectResponse(status().isOk)
        }

        should("응답 페이로드에 presigned url을 포함하여 응답합니다.") {
            expectResponse(
                content().json(
                    """
                        {
                          "fileName": "간병인 신청서.pdf",
                          "url": "https://careinsurance-reception-application-dev.s3.ap-northeast-2.amazonaws.com/$path\\?response-content-disposition\\=attachment%3B%20filename%3D%22%EA%B0%84%EB%B3%91%EC%9D%B8%20%EC%8B%A0%EC%B2%AD%EC%84%9C%2E%70%64%66%22\\&X-Amz-Security-Token\\=IQoJb3JpZ2luX2VjEIr%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaDmFwLW5vcnRoZWFzdC0yIkcwRQIgeyH3OZwCz3si81O1DehWPDfgbRkd%2FAsqR12a2OjSJwcCIQC8e5x3hg6QqnvZpQvEroXBMWBC4Zj2juv3BmEwuBYB1CrWBQjT%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F8BEAIaDDMxOTc0NzgzMjMxNSIMQbWsUtm%2FL2fLMvibKqoF5aNCHmR5%2Foxtlll8di7g7tN1jVQldY4WMDSd0QL9ovYdR0Jekcx5eYRrL5Vn8daWOqIbaUlwAkJKPjdUzGSI2zDKt7tN%2FDtzy1sM0ZoZ5NWJSxLNIJUyJZCYthak6Hp6Rvc1ONreCpCOhuAjj4qfEBGG%2Bg%2BBed9Q3Xm7DrBlE8TAe60tylPje4Isn%2B7dQPZap06zeoRucBTeaqxO3s2dAcnCfuvKO9d0%2BsBkNK8u7bVG71Pq4vzHihvz%2F0WOvrrqUnHbx%2BVR1Ym8ucUdd2we7gQgmKoKGvfPup%2FhN0LcHYAjANfSo4fD8%2FQnIQrM9x%2BD4yokrF5ESXZFzhEIzZ8WJ2jw6mUnrk1KtBprt4sZtjOGntSZBbUfC%2B4LICsAwM%2BpfvgnG9qVyfb8l140NIZ%2B9I%2BGDITbVYoeXi7UHQ%2B9TlNYS%2Bcxh0UjCjbNw8qIsHkfFUgVsoCjU%2BiHuHv6FETA60XcFwdVXTa01wisuGhDgMGDMfO5BQ1IpzAo0%2Fgf9EHynxOBDDUd24QYzVMj%2BO56YAa1KmXsUvIxri0lD5XRA%2FT5k4uZYgHG%2BJi1LOJB8Q%2FMaNwYklI3rzUmcm%2FN73JAwOdJWBmk6g6auhNXx8NtPPBKEqqDPhGYgGPXO%2BmporTjNM9w0xaPnm9x10PEuAQ5I1z7eHX8WnpJaI16oWm6BW4FR9mlq4Un%2BkEP6B5%2FGlKcWIDxQeY6BN%2BOtDLu8hS7wOO6EBOpxhEphMP8%2BoTfJ%2FwJ0XconPByDHwXmzIQ%2BZ6FXRyEDALr5LzuKeSm1V2IihCwjKxL24nwkA9qXC%2FL5xiwL%2BdpiNVn%2F1hbQOWlGz2uxDuonZ4NiKU3aBz2%2B6OBqKZTYNQMNL1t5IiiPAD658WzaMPBPehuBrd69%2FUExDrFgkrHn1nS381XXDDn0JukBjqxAX7lPY%2FWoaWSrgEFNaz0u82e1xJkaFI9o4FDMhkKQU%2Fm5kCnQcQox9Om2WK7G1SkDGApghtAkbPN9TCmCvtjSD%2BYTHvKfrfRT8f%2B404RekmHTO7A1Yvij%2FEr8Fcc5XS0lyUqKa8AH6mRB4DEde9Z98JLciW0QKWWszuCBDtdMy0AP%2FWYcoxUNfJzLUXlfDAv9yvOV46QzXAuVM5DLLvS%2BydkTdgeYa%2BC7GGgZoy2JW7cfg%3D%3D\\&X-Amz-Algorithm\\=AWS4-HMAC-SHA256\\&X-Amz-Date\\=20230612T094830Z\\&X-Amz-SignedHeaders\\=host\\&X-Amz-Expires\\=30\\&X-Amz-Credential\\=ASIAUU4TUXH57VTS7VXM%2F20230612%2Fap-northeast-2%2Fs3%2Faws4_request\\&X-Amz-Signature\\=d24e0ccb5c4d5f29e9e5c26e836fcc4ec4ae60006be45436d843105ea6eee8b9"    
                        }
                    """.trimIndent()
                )
            )
        }

        context("간병 접수가 존재하지 않다면") {
            beforeEach {
                every { receptionApplicationByReceptionIdQueryHandler.getReceptionApplication(any()) } throws ReceptionNotFoundByIdException(
                    receptionId
                )
            }

            afterEach { clearAllMocks() }

            should("응답에는 message, errorType 을 포함하고 있습니다. ") {
                expectResponse(
                    content().json(
                        """
                          {
                            "message": "조회하고자 하는 간병 접수가 존재하지 않습니다.",
                            "errorType": "RECEPTION_NOT_EXISTS"
                          }
                        """.trimIndent()
                    )
                )
            }

            should("응답 데이터에는 입력된 reception id 를 포함하고 있습니다.") {
                expectResponse(
                    content().json(
                        """
                          {
                            "data": {
                              "enteredReceptionId": "$receptionId"
                            }
                          }
                        """.trimIndent()
                    )
                )
            }
        }

        context("간병인 신청서가 존재하지 않는다면") {
            beforeEach {
                every { receptionApplicationByReceptionIdQueryHandler.getReceptionApplication(match { it.receptionId == receptionId }) } throws ReceptionApplicationNotFoundException(
                    receptionId
                )
            }

            afterEach { clearAllMocks() }

            should("응답에는 message, errorType 을 포함하고 있습니다. ") {
                expectResponse(
                    content().json(
                        """
                          {
                            "message": "지정한 간병인 신청서를 찾을 수 없습니다.",
                            "errorType": "RECEPTION_APPLICATION_NOT_FOUND"
                          }
                        """.trimIndent()
                    )
                )
            }

            should("응답 데이터에는 입력된 reception id 를 포함하고 있습니다.") {
                expectResponse(
                    content().json(
                        """
                          {
                            "data": {
                              "enteredReceptionId": "$receptionId"
                            }
                          }
                        """.trimIndent()
                    )
                )
            }
        }
    }

    context("간병인 신청서를 삭제할 때") {
        val receptionId = "01H3TAFEATRXVDAP1DZ8ZEP0D8"

        val request = delete("/api/v1/receptions/$receptionId/application")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            justRun { receptionApplicationByReceptionIdQueryHandler.deleteReceptionApplication(match { it.receptionId == receptionId }) }
        }

        afterEach { clearAllMocks() }

        should("상태 코드 204 No Content 를 응답합니다.") {
            expectResponse(status().isNoContent)
        }

        should("응답 페이로드는 비어있어야 합니다.") {
            expectResponse(content().string(""))
        }

        should("간병인 신청서의 업로드를 도메인에 위임합니다.") {
            mockMvc.perform(request)

            verify {
                receptionApplicationByReceptionIdQueryHandler.deleteReceptionApplication(
                    withArg {
                        it.receptionId shouldBe receptionId
                    }
                )
            }
        }

        context("간병 접수가 존재하지 않다면") {
            beforeEach {
                every { receptionApplicationByReceptionIdQueryHandler.deleteReceptionApplication(match { it.receptionId == receptionId }) } throws ReceptionNotFoundByIdException(
                    receptionId
                )
            }

            afterEach { clearAllMocks() }

            should("응답에는 message, errorType 을 포함하고 있습니다. ") {
                expectResponse(
                    content().json(
                        """
                          {
                            "message": "조회하고자 하는 간병 접수가 존재하지 않습니다.",
                            "errorType": "RECEPTION_NOT_EXISTS"
                          }
                        """.trimIndent()
                    )
                )
            }

            should("응답 데이터에는 입력된 reception id 를 포함하고 있습니다.") {
                expectResponse(
                    content().json(
                        """
                          {
                            "data": {
                              "enteredReceptionId": "$receptionId"
                            }
                          }
                        """.trimIndent()
                    )
                )
            }
        }

        context("간병인 신청서가 존재하지 않는다면") {
            beforeEach {
                every { receptionApplicationByReceptionIdQueryHandler.deleteReceptionApplication(match { it.receptionId == receptionId }) } throws ReceptionApplicationNotFoundException(
                    receptionId
                )
            }

            afterEach { clearAllMocks() }

            should("응답에는 message, errorType 을 포함하고 있습니다. ") {
                expectResponse(
                    content().json(
                        """
                          {
                            "message": "지정한 간병인 신청서를 찾을 수 없습니다.",
                            "errorType": "RECEPTION_APPLICATION_NOT_FOUND"
                          }
                        """.trimIndent()
                    )
                )
            }

            should("응답 데이터에는 입력된 reception id 를 포함하고 있습니다.") {
                expectResponse(
                    content().json(
                        """
                          {
                            "data": {
                              "enteredReceptionId": "$receptionId"
                            }
                          }
                        """.trimIndent()
                    )
                )
            }
        }
    }
})
