package kr.caredoc.careinsurance.web.agency

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.account.AccountInfo
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganization
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationByIdQueryHandler
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationCreationCommandHandler
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationCreationResult
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationEditingCommandHandler
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationNotFoundByIdException
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationType
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationsByFilterQuery
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationsByFilterQueryHandler
import kr.caredoc.careinsurance.file.GeneratingOpenedFileUrlCommandHandler
import kr.caredoc.careinsurance.relaxedMock
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Duration
import java.time.LocalDateTime

@CareInsuranceWebMvcTest(ExternalCaregivingOrganizationController::class)
class ExternalCaregivingOrganizationControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean(relaxed = true)
    private val externalCaregivingOrganizationCreationCommandHandler: ExternalCaregivingOrganizationCreationCommandHandler,
    @MockkBean(relaxed = true)
    private val externalCaregivingOrganizationsByFilterQueryHandler: ExternalCaregivingOrganizationsByFilterQueryHandler,
    @MockkBean(relaxed = true)
    private val externalCaregivingOrganizationByIdQueryHandler: ExternalCaregivingOrganizationByIdQueryHandler,
    @MockkBean(relaxed = true)
    private val externalCaregivingOrganizationEditingCommandHandler: ExternalCaregivingOrganizationEditingCommandHandler,
    @MockkBean
    private val generatingOpenedFileUrlCommandHandler: GeneratingOpenedFileUrlCommandHandler,
) : ShouldSpec({
    context("when creating external caregiving organization") {
        val request = post("/api/v1/external-caregiving-organizations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                      "accountInfo": {
                        "bank": "국민은행",
                        "accountNumber": "085300-04-111424",
                        "accountHolder": "박한결"
                      },
                      "name": "케어라인",
                      "externalCaregivingOrganizationType": "ORGANIZATION",
                      "address": "서울시 강남구 삼성동 109-1",
                      "contractName": "김라인",
                      "phoneNumber": "010-1234-1234",
                      "profitAllocationRatio": 0.6
                    }
                """.trimIndent()
            )
        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                externalCaregivingOrganizationCreationCommandHandler.createExternalCaregivingOrganization(
                    match {
                        listOf(
                            it.name == "케어라인",
                            it.externalCaregivingOrganizationType == ExternalCaregivingOrganizationType.ORGANIZATION,
                            it.address == "서울시 강남구 삼성동 109-1",
                            it.contractName == "김라인",
                            it.phoneNumber == "010-1234-1234",
                            it.profitAllocationRatio == 0.6f,
                            it.accountInfo.bank == "국민은행",
                            it.accountInfo.accountNumber == "085300-04-111424",
                            it.accountInfo.accountHolder == "박한결",
                        ).all { predicate -> predicate }
                    }
                )
            } returns ExternalCaregivingOrganizationCreationResult("01GPX17R5WWE6143PNTF83JK6F")
        }
        afterEach { clearAllMocks() }

        should("response status should be 201 Created") {
            expectResponse(status().isCreated)
        }

        should("response payload should be empty") {
            expectResponse(content().string(""))
        }

        should("response header should be contains Location header") {
            expectResponse(
                header().string(
                    HttpHeaders.LOCATION,
                    "http://localhost/api/v1/external-caregiving-organizations/01GPX17R5WWE6143PNTF83JK6F"
                )
            )
        }
    }

    context("when creating external caregiving organization with allowed null properties ") {
        val request = post("/api/v1/external-caregiving-organizations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                      "accountInfo": {
                        "bank": null,
                        "accountNumber": null,
                        "accountHolder": null
                      },
                      "name": "케어라인",
                      "externalCaregivingOrganizationType": "ORGANIZATION",
                      "address": "서울시 강남구 삼성동 109-1",
                      "contractName": "김라인",
                      "phoneNumber": "010-1234-1234",
                      "profitAllocationRatio": 0.6
                    }
                """.trimIndent()
            )
        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                externalCaregivingOrganizationCreationCommandHandler.createExternalCaregivingOrganization(
                    match {
                        listOf(
                            it.name == "케어라인",
                            it.externalCaregivingOrganizationType == ExternalCaregivingOrganizationType.ORGANIZATION,
                            it.address == "서울시 강남구 삼성동 109-1",
                            it.contractName == "김라인",
                            it.phoneNumber == "010-1234-1234",
                            it.profitAllocationRatio == 0.6f,
                            it.accountInfo.bank == null,
                            it.accountInfo.accountNumber == null,
                            it.accountInfo.accountHolder == null,
                        ).all { predicate -> predicate }
                    }
                )
            } returns ExternalCaregivingOrganizationCreationResult("01GPX17R5WWE6143PNTF83JK6F")
        }
        afterEach { clearAllMocks() }

        should("response status should be 201 Created") {
            expectResponse(status().isCreated)
        }
    }

    context("when getting external caregiving organizations") {
        val request = get("/api/v1/external-caregiving-organizations")
            .queryParam("page-number", "1")
            .queryParam("page-size", "10")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                externalCaregivingOrganizationsByFilterQueryHandler.getExternalCaregivingOrganizations(
                    any(),
                    match {
                        it.pageNumber == 0 && it.pageSize == 10
                    }
                )
            } returns PageImpl(
                listOf<ExternalCaregivingOrganization>(
                    relaxedMock {
                        every { id } returns "01GQS23ZKMCJYX01TNR1E1GSZB"
                        every { name } returns "케어라인"
                        every { externalCaregivingOrganizationType } returns ExternalCaregivingOrganizationType.AFFILIATED
                    },
                ),
                PageRequest.of(0, 10),
                1,
            )
        }

        afterEach { clearAllMocks() }

        should("response status should be 200 Ok") {
            expectResponse(status().isOk)
        }

        should("response payload should contains paging meta data") {
            expectResponse(
                content().json(
                    """
                        {
                          "currentPageNumber": 1,
                          "lastPageNumber": 1,
                          "totalItemCount": 1
                        }
                    """.trimIndent()
                )
            )
        }

        should("response payload should contains queried external caregiving organizations") {
            expectResponse(
                content().json(
                    """
                        {
                          "items": [
                            {
                              "id": "01GQS23ZKMCJYX01TNR1E1GSZB",
                              "name": "케어라인",
                              "externalCaregivingOrganizationType": "AFFILIATED"
                            }
                           ]
                        }
                    """.trimIndent()
                )
            )
        }
    }

    context("when getting external caregiving organizations with search") {
        val request = get("/api/v1/external-caregiving-organizations")
            .queryParam("page-number", "1")
            .queryParam("page-size", "10")
            .queryParam("query", "externalCaregivingOrganizationName:케어라인")

        beforeEach {
            every {
                externalCaregivingOrganizationsByFilterQueryHandler.getExternalCaregivingOrganizations(any(), any())
            } returns PageImpl(
                listOf<ExternalCaregivingOrganization>(),
                PageRequest.of(0, 10),
                0,
            )
        }

        afterEach { clearAllMocks() }

        should("query external caregiving organizations using query parameters") {
            mockMvc.perform(request)

            verify {
                externalCaregivingOrganizationsByFilterQueryHandler.getExternalCaregivingOrganizations(
                    withArg {
                        it.searchCondition!!.searchingProperty shouldBe ExternalCaregivingOrganizationsByFilterQuery.SearchingProperty.EXTERNAL_CAREGIVING_ORGANIZATION_NAME
                        it.searchCondition!!.keyword shouldBe "케어라인"
                    },
                    any(),
                )
            }
        }
    }

    context("간병 협회 목록을 협회/제휴사 구분으로 필터링하여 조회하면") {
        val request = get("/api/v1/external-caregiving-organizations")
            .queryParam("page-number", "1")
            .queryParam("page-size", "10")
            .queryParam("external-caregiving-organization-type", "ORGANIZATION")

        beforeEach {
            every {
                externalCaregivingOrganizationsByFilterQueryHandler.getExternalCaregivingOrganizations(any(), any())
            } returns PageImpl(
                listOf<ExternalCaregivingOrganization>(),
                PageRequest.of(0, 10),
                0,
            )
        }

        afterEach { clearAllMocks() }

        should("query external caregiving organizations using query parameters") {
            mockMvc.perform(request)

            verify {
                externalCaregivingOrganizationsByFilterQueryHandler.getExternalCaregivingOrganizations(
                    withArg {
                        it.organizationType shouldBe ExternalCaregivingOrganizationType.ORGANIZATION
                    },
                    any(),
                )
            }
        }
    }

    context("when searching external caregiving organization with illegal query") {
        listOf(
            "ExternalCaregivingOrganizationName:케어라인",
            "케어라인",
            "ExternalCaregivingOrganizationName:",
            "ExternalCaregivingOrganizationName",
            ":",
        ).map { query ->
            get("/api/v1/external-caregiving-organizations")
                .queryParam("page-number", "1")
                .queryParam("page-size", "10")
                .queryParam("query", query)
        }.forEach { request ->
            val expectResponse = ResponseMatcher(mockMvc, request)

            should("response status should be 400 Bad Request") {
                expectResponse(status().isBadRequest)
            }

            should("response payload should contains error message and type") {
                expectResponse(
                    content().json(
                        """
                            {
                              "message": "해석할 수 없는 검색 조건입니다.",
                              "errorType": "ILLEGAL_SEARCH_QUERY"
                            }
                        """.trimIndent()
                    )
                )
            }
        }
    }

    context("when getting single external caregiving organization") {
        val request = get("/api/v1/external-caregiving-organizations/01GR3J31B63A59WMWW7180ZMV8")
        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                externalCaregivingOrganizationByIdQueryHandler.getExternalCaregivingOrganization(
                    match { it.id == "01GR3J31B63A59WMWW7180ZMV8" }
                )
            } returns relaxedMock {
                every { id } returns "01GR3J31B63A59WMWW7180ZMV8"
                every { name } returns "케어라인"
                every { externalCaregivingOrganizationType } returns ExternalCaregivingOrganizationType.AFFILIATED
                every { address } returns "서울시 강남구 삼성동 109-1"
                every { contractName } returns "김라인"
                every { phoneNumber } returns "010-1234-1234"
                every { profitAllocationRatio } returns 0.6f
                every { businessLicenseFileName } returns "(주)케어라인 사업자등록증.pdf"
                every { businessLicenseFileUrl } returns ""
                every { accountInfo } returns AccountInfo(
                    bank = "국민은행",
                    accountNumber = "085300-04-111424",
                    accountHolder = "박한결",
                )
            }

            every {
                generatingOpenedFileUrlCommandHandler.generateOpenedUrl(
                    match {
                        it.url == ""
                    },
                    match {
                        it.duration == Duration.ofSeconds(30)
                    }
                )
            } returns relaxedMock {
                every { url } returns ""
                every { expiration } returns LocalDateTime.of(2023, 4, 12, 15, 29, 15)
            }
        }

        afterEach { clearAllMocks() }

        should("response status should be 200 Ok") {
            expectResponse(status().isOk)
        }

        should("response payload should contains external caregiving organization info") {
            expectResponse(
                content().json(
                    """
                        {
                            "id": "01GR3J31B63A59WMWW7180ZMV8",
                            "name": "케어라인",
                            "externalCaregivingOrganizationType": "AFFILIATED",
                            "address": "서울시 강남구 삼성동 109-1",
                            "contractName": "김라인",
                            "phoneNumber": "010-1234-1234",
                            "profitAllocationRatio": 0.6,
                            "businessLicenseFileName": "(주)케어라인 사업자등록증.pdf",
                            "businessLicenseFileUrl": "",
                            "accountInfo": {
                              "bank": "국민은행",
                              "accountNumber": "085300-04-111424",
                              "accountHolder": "박한결"
                             }
                        }
                    """.trimIndent()
                )
            )
        }

        should("등록된 파일의 개방된 URL을 요청합니다.") {
            mockMvc.perform(request)

            verify {
                generatingOpenedFileUrlCommandHandler.generateOpenedUrl(
                    withArg {
                        it.url shouldBe ""
                    },
                    withArg {
                        it.duration shouldBe Duration.ofSeconds(30)
                        it.contentDisposition?.filename shouldBe "%28%EC%A3%BC%29%EC%BC%80%EC%96%B4%EB%9D%BC%EC%9D%B8%20%EC%82%AC%EC%97%85%EC%9E%90%EB%93%B1%EB%A1%9D%EC%A6%9D.pdf"
                        it.contentDisposition?.isAttachment shouldBe true
                    },
                )
            }
        }

        context("when getting single external caregiving organization with allowed null properties") {
            beforeEach {
                every {
                    externalCaregivingOrganizationByIdQueryHandler.getExternalCaregivingOrganization(
                        match { it.id == "01GR3J31B63A59WMWW7180ZMV8" }
                    )
                } returns relaxedMock {
                    every { id } returns "01GR3J31B63A59WMWW7180ZMV8"
                    every { businessLicenseFileName } returns null
                    every { businessLicenseFileUrl } returns null
                    every { accountInfo } returns AccountInfo(
                        bank = null,
                        accountNumber = null,
                        accountHolder = null,
                    )
                }
            }

            afterEach { clearAllMocks() }

            should("response status should be 200 Ok") {
                expectResponse(status().isOk)
            }

            should("response payload should contains external caregiving organization info") {
                expectResponse(
                    content().json(
                        """
                            {
                                "id": "01GR3J31B63A59WMWW7180ZMV8",
                                "businessLicenseFileName": null,
                                "businessLicenseFileUrl": null,
                                "accountInfo": {
                                  "bank": null,
                                  "accountNumber": null,
                                  "accountHolder": null
                                 }
                            }
                        """.trimIndent()
                    )
                )
            }
        }

        context("but external caregiving organization not exists") {
            beforeEach {
                every {
                    externalCaregivingOrganizationByIdQueryHandler.getExternalCaregivingOrganization(match { it.id == "01GR3J31B63A59WMWW7180ZMV8" })
                } throws ExternalCaregivingOrganizationNotFoundByIdException("01GR3J31B63A59WMWW7180ZMV8")
            }

            afterEach { clearAllMocks() }

            should("response status should be 404 Not Found") {
                expectResponse(status().isNotFound)
            }

            should("response payload should contains error message and error type") {
                expectResponse(
                    content().json(
                        """
                            {
                              "message": "조회하고자 하는 외부 간병 협회가 존재하지 않습니다.",
                              "errorType": "EXTERNAL_CAREGIVING_ORGANIZATION_NOT_EXISTS"
                            }
                        """.trimIndent()
                    )
                )
            }

            should("response payload should contains error data") {
                expectResponse(
                    content().json(
                        """
                            {
                              "data": {
                                "enteredExternalCaregivingOrganizationId": "01GR3J31B63A59WMWW7180ZMV8"
                              }
                            }
                        """.trimIndent()
                    )
                )
            }
        }
    }

    context("when editing external caregiving organization") {
        val request = put("/api/v1/external-caregiving-organizations/01GR7YWRAB712FF59ZR84RJP6X")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                        "name": "케어라인",
                        "externalCaregivingOrganizationType": "AFFILIATED",
                        "address": "서울시 강남구 삼성동 109-10",
                        "contractName": "김케어",
                        "phoneNumber": "010-1234-4444",
                        "profitAllocationRatio": 0.0,
                        "accountInfo": {
                            "bank": "신한은행",
                            "accountNumber": "111-444-555888",
                            "accountHolder": "박결한"
                        }
                    }
                """.trimIndent()
            )

        beforeEach {
            justRun {
                externalCaregivingOrganizationEditingCommandHandler.editExternalCaregivingOrganization(
                    match { it.externalCaregivingOrganizationId == "01GR7YWRAB712FF59ZR84RJP6X" }
                )
            }
        }

        afterEach { clearAllMocks() }

        val expectResponse = ResponseMatcher(mockMvc, request)

        should("response status be 204 No Content") {
            expectResponse(status().isNoContent)
        }

        should("response payload should be empty") {
            expectResponse(content().string(""))
        }

        should("should patch external caregiving organization using command handler") {
            mockMvc.perform(request)

            verify {
                externalCaregivingOrganizationEditingCommandHandler.editExternalCaregivingOrganization(
                    withArg {
                        it.name shouldBe "케어라인"
                        it.externalCaregivingOrganizationType shouldBe ExternalCaregivingOrganizationType.AFFILIATED
                        it.address shouldBe "서울시 강남구 삼성동 109-10"
                        it.contractName shouldBe "김케어"
                        it.phoneNumber shouldBe "010-1234-4444"
                        it.profitAllocationRatio shouldBe 0.0f
                        it.accountInfo.bank shouldBe "신한은행"
                        it.accountInfo.accountNumber shouldBe "111-444-555888"
                        it.accountInfo.accountHolder shouldBe "박결한"
                    }
                )
            }
        }

        context("but external caregiving organization not exists") {
            beforeEach {
                every {
                    externalCaregivingOrganizationEditingCommandHandler.editExternalCaregivingOrganization(
                        match { it.externalCaregivingOrganizationId == "01GR7YWRAB712FF59ZR84RJP6X" }
                    )
                } throws ExternalCaregivingOrganizationNotFoundByIdException("01GR7YWRAB712FF59ZR84RJP6X")
            }

            afterEach { clearAllMocks() }

            should("response status should be 404 Not Found") {
                expectResponse(status().isNotFound)
            }

            should("response payload should contains error message and error type") {
                expectResponse(
                    content().json(
                        """
                            {
                              "message": "조회하고자 하는 외부 간병 협회가 존재하지 않습니다.",
                              "errorType": "EXTERNAL_CAREGIVING_ORGANIZATION_NOT_EXISTS"
                            }
                        """.trimIndent()
                    )
                )
            }

            should("response payload should contains error data") {
                expectResponse(
                    content().json(
                        """
                            {
                              "data": {
                                "enteredExternalCaregivingOrganizationId": "01GR7YWRAB712FF59ZR84RJP6X"
                              }
                            }
                        """.trimIndent()
                    )
                )
            }
        }
    }

    context("when editing external caregiving organization with allowed null properties") {
        val request = put("/api/v1/external-caregiving-organizations/01GR88E58M82M9SB37HR6Z97WY")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                        "id": "01GR7YWRAB712FF59ZR84RJP6X",
                        "name": "케어라인",
                        "externalCaregivingOrganizationType": "AFFILIATED",
                        "address": "서울시 강남구 삼성동 109-10",
                        "contractName": "김케어",
                        "phoneNumber": "010-1234-4444",
                        "profitAllocationRatio": 0.0,
                        "accountInfo": {
                            "bank": null,
                            "accountNumber": null,
                            "accountHolder": null
                        }
                    }
                """.trimIndent()
            )

        beforeEach {
            justRun {
                externalCaregivingOrganizationEditingCommandHandler.editExternalCaregivingOrganization(
                    match { it.externalCaregivingOrganizationId == "01GR88E58M82M9SB37HR6Z97WY" }
                )
            }
        }

        afterEach { clearAllMocks() }

        val expectResponse = ResponseMatcher(mockMvc, request)

        should("response status be 204 No Content") {
            expectResponse(status().isNoContent)
        }

        should("should patch external caregiving organization using command handler") {
            mockMvc.perform(request)

            verify {
                externalCaregivingOrganizationEditingCommandHandler.editExternalCaregivingOrganization(
                    withArg {
                        it.accountInfo.bank shouldBe null
                        it.accountInfo.accountNumber shouldBe null
                        it.accountInfo.accountHolder shouldBe null
                    }
                )
            }
        }
    }
})
