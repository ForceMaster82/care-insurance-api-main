package kr.caredoc.careinsurance.web.coverage

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.slot
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.coverage.AllCoveragesQueryHandler
import kr.caredoc.careinsurance.coverage.AnnualCoverageDuplicatedException
import kr.caredoc.careinsurance.coverage.Coverage
import kr.caredoc.careinsurance.coverage.CoverageByIdQueryHandler
import kr.caredoc.careinsurance.coverage.CoverageCreationCommandHandler
import kr.caredoc.careinsurance.coverage.CoverageEditingCommandHandler
import kr.caredoc.careinsurance.coverage.CoverageNameDuplicatedException
import kr.caredoc.careinsurance.coverage.CoverageNotFoundByIdException
import kr.caredoc.careinsurance.coverage.CoveragesBySearchConditionQuery
import kr.caredoc.careinsurance.coverage.CoveragesBySearchConditionQueryHandler
import kr.caredoc.careinsurance.coverage.IllegalRenewalTypeEnteredException
import kr.caredoc.careinsurance.coverage.RenewalType
import kr.caredoc.careinsurance.coverage.SubscriptionYearDuplicatedException
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.web.coverage.response.DetailCoverageResponse
import kr.caredoc.careinsurance.web.paging.PagingRequest
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
import java.time.LocalDateTime

@CareInsuranceWebMvcTest(CoverageController::class)
class CoverageControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean
    private val allCoveragesQueryHandler: AllCoveragesQueryHandler,
    @MockkBean
    private val coverageByIdQueryHandler: CoverageByIdQueryHandler,
    @MockkBean
    private val coverageCreationCommandHandler: CoverageCreationCommandHandler,
    @MockkBean
    private val coverageEditingCommandHandler: CoverageEditingCommandHandler,
    @MockkBean
    private val coveragesBySearchConditionQueryHandler: CoveragesBySearchConditionQueryHandler,
) : ShouldSpec({
    fun Collection<Coverage.AnnualCoveredCaregivingCharge>.hasDuplicatedYear() =
        this.asSequence()
            .map { annualCoverage -> annualCoverage.targetAccidentYear }
            .toSet().size != this.size

    context("가입 담보를 검색하면") {
        val request = get("/api/v1/coverages")
            .queryParam("page-number", "1")
            .queryParam("page-size", "2")
            .queryParam("query", "name:질병")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                coveragesBySearchConditionQueryHandler.getCoverages(
                    match {
                        it.searchCondition.searchingProperty == CoveragesBySearchConditionQuery.SearchingProperty.NAME &&
                            it.searchCondition.keyword == "질병"
                    },
                    match {
                        it.pageNumber == 0 && it.pageSize == 2
                    }
                )
            } returns PageImpl(
                listOf(
                    relaxedMock {
                        every { id } returns "01GP2QZ9SMRBFGJW69Z518XG79"
                        every { name } returns "질병 3 년형 (2022)"
                        every { targetSubscriptionYear } returns 2022
                        every { renewalType } returns RenewalType.TEN_YEAR
                        every { lastModifiedDateTime } returns LocalDateTime.of(2023, 1, 25, 10, 23, 45)
                    },
                ),
                PageRequest.of(0, 2),
                1,
            )
        }

        afterEach { clearAllMocks() }

        should("상태 코드 200 Ok로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("검색 결과를 페이로드에 포함하여 응답합니다.") {
            expectResponse(
                content().json(
                    """
                        {
                          "items": [
                            {
                              "id": "01GP2QZ9SMRBFGJW69Z518XG79",
                              "name": "질병 3 년형 (2022)",
                              "renewalType": "TEN_YEAR",
                              "targetSubscriptionYear": 2022,
                              "lastModifiedDateTime": "2023-01-25T01:23:45Z"
                            }
                          ]
                        }
                    """.trimIndent()
                )
            )
        }

        should("가입 담보 검색을 도메인 영역에 위임합니다.") {
            mockMvc.perform(request)

            verify {
                coveragesBySearchConditionQueryHandler.getCoverages(
                    withArg {
                        it.searchCondition.searchingProperty shouldBe CoveragesBySearchConditionQuery.SearchingProperty.NAME
                        it.searchCondition.keyword shouldBe "질병"
                    },
                    withArg {
                        it.pageNumber shouldBe 0
                        it.pageSize shouldBe 2
                    }
                )
            }
        }
    }

    context("잘못된 검색 형식으로 가입 담보를 검색하면") {
        val request = get("/api/v1/coverages")
            .queryParam("page-number", "1")
            .queryParam("page-size", "2")
            .queryParam("query", "name질병")

        val expectResponse = ResponseMatcher(mockMvc, request)

        should("상태 코드 400 BadRequest로 응답합니다.") {
            expectResponse(status().isBadRequest)
        }

        should("에러 메시지와 타입을 페이로드에 포함하여 응답합니다.") {
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

    context("when getting insurance coverages") {
        val request = get("/api/v1/coverages")
            .queryParam("page-number", "1")
            .queryParam("page-size", "2")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                allCoveragesQueryHandler.getCoverages(
                    any(),
                    match {
                        it.pageNumber == 0 && it.pageSize == 2
                    }
                )
            } returns PageImpl(
                listOf(
                    relaxedMock {
                        every { id } returns "01GP2QZ9SMRBFGJW69Z518XG79"
                        every { name } returns "질병 3 년형 (2022)"
                        every { targetSubscriptionYear } returns 2022
                        every { renewalType } returns RenewalType.TEN_YEAR
                        every { lastModifiedDateTime } returns LocalDateTime.of(2023, 1, 25, 10, 23, 45)
                    },
                    relaxedMock {
                        every { id } returns "01GP2R2WN0DDQ2H715V2NEHDPB"
                        every { name } returns "상해 3 년형 (2022)"
                        every { targetSubscriptionYear } returns 2022
                        every { renewalType } returns RenewalType.THREE_YEAR
                        every { lastModifiedDateTime } returns LocalDateTime.of(2023, 1, 25, 10, 23, 45)
                    },
                ),
                PageRequest.of(0, 2),
                4,
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
                          "lastPageNumber": 2,
                          "totalItemCount": 4
                        }
                    """.trimIndent()
                )
            )
        }

        should("response payload should contains coverages as items") {
            expectResponse(
                content().json(
                    """
                        {
                          "items": [
                            {
                              "id": "01GP2QZ9SMRBFGJW69Z518XG79",
                              "name": "질병 3 년형 (2022)",
                              "renewalType": "TEN_YEAR",
                              "targetSubscriptionYear": 2022,
                              "lastModifiedDateTime": "2023-01-25T01:23:45Z"
                            },
                            {
                              "id": "01GP2R2WN0DDQ2H715V2NEHDPB",
                              "name": "상해 3 년형 (2022)",
                              "renewalType": "THREE_YEAR",
                              "targetSubscriptionYear": 2022,
                              "lastModifiedDateTime": "2023-01-25T01:23:45Z"
                            }
                          ]
                        }
                    """.trimIndent()
                )
            )
        }
    }

    context("when getting insurance coverages with illegal paging properties") {
        listOf(
            PagingRequest(2, 0),
            PagingRequest(0, 1),
            PagingRequest(-1, -1),
        ).map { pagingRequest ->
            val request = get("/api/v1/coverages")
                .queryParam("page-number", pagingRequest.pageNumber.toString())
                .queryParam("page-size", pagingRequest.pageSize.toString())

            val expectResponse = ResponseMatcher(mockMvc, request)

            should("response status should be 400 Bad request") {
                expectResponse(status().isBadRequest)
            }

            should("response payload should contains error message and type") {
                expectResponse(
                    content().json(
                        """
                            {
                                "message": "잘못된 페이지 요청입니다.",
                                "errorType": "ILLEGAL_PAGE_REQUEST"
                            }
                        """.trimIndent()
                    )
                )
            }

            should("response payload should contains entered page size and number as data") {
                expectResponse(
                    content().json(
                        """
                            {
                              "data": {
                                "enteredPageSize": ${pagingRequest.pageSize},
                                "enteredPageNumber": ${pagingRequest.pageNumber}
                              }
                            }
                        """.trimIndent()
                    )
                )
            }
        }
    }

    context("when creating coverage") {
        val request = post("/api/v1/coverages")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                      "name": "질병 3년형 (2022)",
                      "targetSubscriptionYear": 2022,
                      "renewalType": "TEN_YEAR",
                      "annualCoveredCaregivingCharges": [
                        {
                          "targetAccidentYear": 2022,
                          "caregivingCharge": 90000
                        },
                        {
                          "targetAccidentYear": 2023,
                          "caregivingCharge": 100000
                        }
                      ]
                    }
                """.trimIndent()
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                coverageCreationCommandHandler.createCoverage(
                    match {
                        it.name == "질병 3년형 (2022)"
                    }
                )
            } returns relaxedMock {
                every { createdCoverageId } returns "01GPB41SGVP14Q28MMCXDZHBCD"
            }
        }

        afterEach { clearAllMocks() }

        should("response status should be 201 Created") {
            expectResponse(status().isCreated)
        }

        should("response payload should be empty") {
            expectResponse(content().string(""))
        }

        should("response header should contains Location header pointing created resource") {
            expectResponse(
                header().string(
                    HttpHeaders.LOCATION,
                    "http://localhost/api/v1/coverages/01GPB41SGVP14Q28MMCXDZHBCD"
                )
            )
        }

        should("create coverage") {
            mockMvc.perform(request)
            verify {
                coverageCreationCommandHandler.createCoverage(
                    withArg {
                        it.name shouldBe "질병 3년형 (2022)"
                        it.targetSubscriptionYear shouldBe 2022
                        it.renewalType shouldBe RenewalType.TEN_YEAR
                        it.annualCoveredCaregivingCharges shouldContain Coverage.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2022,
                            caregivingCharge = 90000,
                        )
                        it.annualCoveredCaregivingCharges shouldContain Coverage.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2023,
                            caregivingCharge = 100000,
                        )
                    }
                )
            }
        }

        context("but entered renewal type was illegal") {
            beforeEach {
                every {
                    coverageCreationCommandHandler.createCoverage(
                        match {
                            it.renewalType == RenewalType.TEN_YEAR
                        }
                    )
                } throws IllegalRenewalTypeEnteredException(RenewalType.TEN_YEAR)
            }

            afterEach { clearAllMocks() }

            should("response status should be 400 Bad Request") {
                expectResponse(status().isBadRequest)
            }

            should("response payload should contains error message and type") {
                expectResponse(
                    content().json(
                        """
                            {
                              "message": "허용되지 않은 갱신 구분입니다.",
                              "errorType": "ILLEGAL_RENEWAL_TYPE"
                            }
                        """.trimIndent()
                    )
                )
            }

            should("response payload should contains suspended user id in data") {
                expectResponse(
                    content().json(
                        """
                            {
                              "data": {
                                "enteredRenewalType": "TEN_YEAR"
                              }
                            }
                        """.trimIndent()
                    )
                )
            }
        }

        context("하지만 이미 동일한 연도에 가입 담보가 등록되어 있다면") {
            beforeEach {
                every {
                    coverageCreationCommandHandler.createCoverage(
                        match {
                            it.targetSubscriptionYear == 2022 && it.renewalType == RenewalType.TEN_YEAR
                        }
                    )
                } throws SubscriptionYearDuplicatedException(
                    duplicatedSubscriptionYear = 2022,
                    duplicatedRenewalType = RenewalType.TEN_YEAR,
                )
            }

            afterEach { clearAllMocks() }

            should("상태 코드 422 Unprocessable Entity로 응답합니다.") {
                expectResponse(status().isUnprocessableEntity)
            }

            should("메시지와 에러 타입을 페이로드에 포함하여 응답합니다.") {
                expectResponse(
                    content().json(
                        """
                            {
                              "message": "동일한 연도에 이미 가입 담보가 등록되어 있습니다.",
                              "errorType": "ACCIDENT_YEAR_ALREADY_REGISTERED"
                            }
                        """.trimIndent()
                    )
                )
            }
        }

        context("하지만 이미 동일한 이름의 가입 담보가 등록되어 있다면") {
            beforeEach {
                every {
                    coverageCreationCommandHandler.createCoverage(
                        match {
                            it.name == "질병 3년형 (2022)"
                        }
                    )
                } throws CoverageNameDuplicatedException(
                    duplicatedCoverageName = "질병 3년형 (2022)",
                )
            }

            afterEach { clearAllMocks() }

            should("상태 코드 422 Unprocessable Entity로 응답합니다.") {
                expectResponse(status().isUnprocessableEntity)
            }

            should("메시지와 에러 타입을 페이로드에 포함하여 응답합니다.") {
                expectResponse(
                    content().json(
                        """
                            {
                              "message": "동일한 이름을 가입 담보가 이미 등록되어 있습니다.",
                              "errorType": "NAME_ALREADY_REGISTERED"
                            }
                        """.trimIndent()
                    )
                )
            }
        }
    }

    context("when creating coverage with duplicated covered year") {
        val request = post("/api/v1/coverages")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                      "name": "질병 3년형 (2022)",
                      "targetSubscriptionYear": 2022,
                      "renewalType": "TEN_YEAR",
                      "annualCoveredCaregivingCharges": [
                        {
                          "targetAccidentYear": 2022,
                          "caregivingCharge": 90000
                        },
                        {
                          "targetAccidentYear": 2022,
                          "caregivingCharge": 100000
                        }
                      ]
                    }
                """.trimIndent()
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                coverageCreationCommandHandler.createCoverage(
                    match { it.annualCoveredCaregivingCharges.hasDuplicatedYear() }
                )
            } throws AnnualCoverageDuplicatedException(setOf(2022))
        }

        afterEach { clearAllMocks() }

        should("response status should be 400 Bad Request") {
            expectResponse(status().isBadRequest)
        }

        should("response payload should contains error message and error type") {
            expectResponse(
                content().json(
                    """
                        {
                          "message": "중복된 기준일자가 존재합니다.",
                          "errorType": "DUPLICATED_ACCIDENT_YEAR"
                        }
                    """.trimIndent()
                )
            )
        }

        should("response payload should contains duplicated accident year as data") {
            expectResponse(
                content().json(
                    """
                        {
                          "data": {
                            "duplicatedAccidentYear": [
                                2022
                            ]
                          }
                        }
                    """.trimIndent()
                )
            )
        }
    }

    context("when getting single coverage") {
        val request = get("/api/v1/coverages/01GPD5EE21TGK5A5VCYWQ9Z73W")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            val converterSlot = slot<(Coverage) -> DetailCoverageResponse>()
            every {
                coverageByIdQueryHandler.getCoverage(
                    match { it.coverageId == "01GPD5EE21TGK5A5VCYWQ9Z73W" },
                    capture(converterSlot)
                )
            } answers {
                converterSlot.captured(
                    relaxedMock {
                        every { id } returns "01GPD5EE21TGK5A5VCYWQ9Z73W"
                        every { name } returns "질병 3년형 (2022)"
                        every { targetSubscriptionYear } returns 2022
                        every { renewalType } returns RenewalType.THREE_YEAR
                        every { annualCoveredCaregivingCharges } returns listOf(
                            relaxedMock {
                                every { targetAccidentYear } returns 2022
                                every { caregivingCharge } returns 90000
                            },
                            relaxedMock {
                                every { targetAccidentYear } returns 2023
                                every { caregivingCharge } returns 100000
                            },
                        )
                        every { lastModifiedDateTime } returns LocalDateTime.of(2023, 1, 25, 10, 23, 45)
                    }
                )
            }
        }

        afterEach { clearAllMocks() }

        should("response status should be 200 Ok") {
            expectResponse(status().isOk)
        }

        should("response payload should contains coverage detail data") {
            expectResponse(
                content().json(
                    """
                        {
                          "id": "01GPD5EE21TGK5A5VCYWQ9Z73W",
                          "name": "질병 3년형 (2022)",
                          "targetSubscriptionYear": 2022,
                          "renewalType": "THREE_YEAR",
                          "annualCoveredCaregivingCharges": [
                            {
                              "targetAccidentYear": 2022,
                              "caregivingCharge": 90000
                            },
                            {
                              "targetAccidentYear": 2023,
                              "caregivingCharge": 100000
                            }
                          ],
                          "lastModifiedDateTime": "2023-01-25T01:23:45Z"
                        }
                    """.trimIndent()
                )
            )
        }

        context("but coverage not exists") {
            beforeEach {
                every {
                    coverageByIdQueryHandler.getCoverage(
                        match { it.coverageId == "01GPD5EE21TGK5A5VCYWQ9Z73W" },
                        any<(Coverage) -> DetailCoverageResponse>()
                    )
                } throws CoverageNotFoundByIdException("01GPD5EE21TGK5A5VCYWQ9Z73W")
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
                              "message": "가입 담보를 찾을 수 없습니다.",
                              "errorType": "COVERAGE_NOT_EXISTS"
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
                                "enteredCoverageId": "01GPD5EE21TGK5A5VCYWQ9Z73W"
                              }
                            }
                        """.trimIndent()
                    )
                )
            }
        }
    }

    context("when editing coverage") {
        val request = put("/api/v1/coverages/01GPD5EE21TGK5A5VCYWQ9Z73W")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                      "name": "질병 3년형 - 특약 (2022)",
                      "targetSubscriptionYear": 2022,
                      "annualCoveredCaregivingCharges": [
                        {
                          "targetAccidentYear": 2022,
                          "caregivingCharge": 90000
                        },
                        {
                          "targetAccidentYear": 2023,
                          "caregivingCharge": 100000
                        },
                        {
                          "targetAccidentYear": 2024,
                          "caregivingCharge": 110000
                        }
                      ]
                    }
                    
                """.trimIndent()
            )

        beforeEach {
            justRun {
                coverageEditingCommandHandler.editCoverage(
                    match { it.coverageId == "01GPD5EE21TGK5A5VCYWQ9Z73W" }
                )
            }
        }

        afterEach { clearAllMocks() }

        val expectResponse = ResponseMatcher(mockMvc, request)

        should("response status should be 204 No Content") {
            expectResponse(status().isNoContent)
        }

        should("response payload should be empty") {
            expectResponse(content().string(""))
        }

        should("should patch coverage using command handler") {
            mockMvc.perform(request)

            verify {
                coverageEditingCommandHandler.editCoverage(
                    withArg {
                        it.coverageId shouldBe "01GPD5EE21TGK5A5VCYWQ9Z73W"
                        it.name shouldBe "질병 3년형 - 특약 (2022)"
                        it.targetSubscriptionYear shouldBe 2022
                        it.annualCoveredCaregivingCharges shouldContainAll listOf(
                            Coverage.AnnualCoveredCaregivingCharge(
                                targetAccidentYear = 2022,
                                caregivingCharge = 90000
                            ),
                            Coverage.AnnualCoveredCaregivingCharge(
                                targetAccidentYear = 2023,
                                caregivingCharge = 100000
                            ),
                            Coverage.AnnualCoveredCaregivingCharge(
                                targetAccidentYear = 2024,
                                caregivingCharge = 110000
                            ),
                        )
                    }
                )
            }
        }

        context("but coverage not exists") {
            beforeEach {
                every {
                    coverageEditingCommandHandler.editCoverage(
                        match { it.coverageId == "01GPD5EE21TGK5A5VCYWQ9Z73W" }
                    )
                } throws CoverageNotFoundByIdException("01GPD5EE21TGK5A5VCYWQ9Z73W")
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
                              "message": "가입 담보를 찾을 수 없습니다.",
                              "errorType": "COVERAGE_NOT_EXISTS"
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
                                "enteredCoverageId": "01GPD5EE21TGK5A5VCYWQ9Z73W"
                              }
                            }
                        """.trimIndent()
                    )
                )
            }
        }

        context("하지만 이미 동일한 이름의 가입 담보가 등록되어 있다면") {
            beforeEach {
                every {
                    coverageEditingCommandHandler.editCoverage(
                        match {
                            it.name == "질병 3년형 - 특약 (2022)"
                        }
                    )
                } throws CoverageNameDuplicatedException(
                    duplicatedCoverageName = "질병 3년형 - 특약 (2022)",
                )
            }

            afterEach { clearAllMocks() }

            should("상태 코드 422 Unprocessable Entity로 응답합니다.") {
                expectResponse(status().isUnprocessableEntity)
            }

            should("메시지와 에러 타입을 페이로드에 포함하여 응답합니다.") {
                expectResponse(
                    content().json(
                        """
                            {
                              "message": "동일한 이름을 가입 담보가 이미 등록되어 있습니다.",
                              "errorType": "NAME_ALREADY_REGISTERED"
                            }
                        """.trimIndent()
                    )
                )
            }
        }
    }

    context("when editing coverage with duplicated covered year") {
        val request = put("/api/v1/coverages/01GPD5EE21TGK5A5VCYWQ9Z73W")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                      "name": "질병 3년형 - 특약 (2022)",
                      "targetSubscriptionYear": 2022,
                      "annualCoveredCaregivingCharges": [
                        {
                          "targetAccidentYear": 2022,
                          "caregivingCharge": 90000
                        },
                        {
                          "targetAccidentYear": 2023,
                          "caregivingCharge": 100000
                        },
                        {
                          "targetAccidentYear": 2023,
                          "caregivingCharge": 110000
                        }
                      ]
                    }
                    
                """.trimIndent()
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                coverageEditingCommandHandler.editCoverage(
                    match { it.annualCoveredCaregivingCharges.hasDuplicatedYear() }
                )
            } throws AnnualCoverageDuplicatedException(setOf(2022))
        }

        afterEach { clearAllMocks() }

        should("response status should be 400 Bad Request") {
            expectResponse(status().isBadRequest)
        }

        should("response payload should contains error message and error type") {
            expectResponse(
                content().json(
                    """
                        {
                          "message": "중복된 기준일자가 존재합니다.",
                          "errorType": "DUPLICATED_ACCIDENT_YEAR"
                        }
                    """.trimIndent()
                )
            )
        }

        should("response payload should contains duplicated accident year as data") {
            expectResponse(
                content().json(
                    """
                        {
                          "data": {
                            "duplicatedAccidentYear": [
                                2022
                            ]
                          }
                        }
                    """.trimIndent()
                )
            )
        }
    }
})
