package kr.caredoc.careinsurance.web.caregiving

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import kr.caredoc.careinsurance.caregiving.CaregivingProgressingStatus
import kr.caredoc.careinsurance.caregiving.CaregivingRound
import kr.caredoc.careinsurance.caregiving.CaregivingRoundsByFilterQuery
import kr.caredoc.careinsurance.caregiving.CaregivingRoundsByFilterQueryHandler
import kr.caredoc.careinsurance.reception.OrganizationType
import kr.caredoc.careinsurance.reception.ReceptionProgressingStatus
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQueryHandler
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.settlement.SettlementProgressingStatus
import org.hamcrest.core.StringEndsWith
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.LocalDate
import java.time.LocalDateTime

@CareInsuranceWebMvcTest(OrganizationCaregivingRoundController::class)
class OrganizationCaregivingRoundControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean
    private val caregivingRoundsByFilterQueryHandler: CaregivingRoundsByFilterQueryHandler,
    @MockkBean
    private val receptionsByIdsQueryHandler: ReceptionsByIdsQueryHandler,
) : ShouldSpec({
    context("when getting organization caregiving rounds") {
        val request = MockMvcRequestBuilders.get("/api/v1/organizations/01GRWDRWTM6ENFSQXTDN9HDDWK/caregiving-rounds")
            .queryParam("page-number", "1")
            .queryParam("page-size", "20")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                caregivingRoundsByFilterQueryHandler.getCaregivingRounds(
                    match { it.organizationId == "01GRWDRWTM6ENFSQXTDN9HDDWK" },
                    match { it.pageNumber == 0 && it.pageSize == 20 }
                )
            } returns PageImpl(
                listOf<CaregivingRound>(
                    relaxedMock {
                        every { id } returns "01GRR1T9W6P1HBSYHWCRBBYW1N"
                        every { caregivingRoundNumber } returns 1
                        every { startDateTime } returns LocalDateTime.of(2023, 2, 1, 14, 0, 0)
                        every { endDateTime } returns null
                        every { caregivingProgressingStatus } returns CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
                        every { settlementProgressingStatus } returns SettlementProgressingStatus.NOT_STARTED
                        every { billingProgressingStatus } returns BillingProgressingStatus.NOT_STARTED
                        every { receptionInfo } returns relaxedMock {
                            every { receptionId } returns "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                            every { insuranceNumber } returns "11111-1111"
                            every { accidentNumber } returns "2022-1111111"
                            every { expectedCaregivingStartDate } returns null
                            every { receptionProgressingStatus } returns ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                            every { caregivingManagerInfo } returns relaxedMock {
                                every { organizationType } returns OrganizationType.ORGANIZATION
                                every { organizationId } returns "01GRWDRWTM6ENFSQXTDN9HDDWK"
                                every { managingUserId } returns "01GQ23MVTBAKS526S0WGS9CS0A"
                            }
                        }
                    },
                ),
                PageRequest.of(0, 20),
                1,
            )

            every {
                receptionsByIdsQueryHandler.getReceptions(
                    match {
                        it.receptionIds.contains("01GPJMK7ZPBBKTY3TP0NN5JWCJ")
                    }
                )
            } returns listOf(
                relaxedMock {
                    every { id } returns "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                    every { patientInfo.name.masked } returns "김*자"
                }
            )
        }

        afterEach { clearAllMocks() }

        should("response status should be 200 ok") {
            expectResponse(MockMvcResultMatchers.status().isOk)
        }

        should("response payload should contains paging meta data") {
            expectResponse(
                MockMvcResultMatchers.content().json(
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

        should("response payload should contains queried caregiving rounds by organization") {
            expectResponse(
                MockMvcResultMatchers.content().json(
                    """
                    {
                      "items": [
                        {
                          "id": "01GRR1T9W6P1HBSYHWCRBBYW1N",
                          "caregivingRoundNumber": 1,
                          "startDateTime": "2023-02-01T05:00:00Z",
                          "endDateTime": null,
                          "caregivingProgressingStatus": "CAREGIVING_IN_PROGRESS",
                          "settlementProgressingStatus": "NOT_STARTED",
                          "billingProgressingStatus": "NOT_STARTED",
                          "receptionInfo": {
                            "receptionId": "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                            "insuranceNumber": "11111-1111",
                            "accidentNumber": "2022-1111111",
                            "patientName": "김*자",
                            "expectedCaregivingStartDate": null,
                            "caregivingManagerInfo": {
                              "organizationType": "ORGANIZATION",
                              "organizationId": "01GRWDRWTM6ENFSQXTDN9HDDWK",
                              "managingUserId": "01GQ23MVTBAKS526S0WGS9CS0A"
                            }
                          }
                        }
                      ]
                    }
                    """.trimIndent()
                )
            )
        }
    }

    context("외부 간병 업체의 간병 회차를 간병 시작일 범위로 필터링하여 조회하면") {
        val request = MockMvcRequestBuilders.get("/api/v1/organizations/01GRWDRWTM6ENFSQXTDN9HDDWK/caregiving-rounds")
            .queryParam("page-number", "1")
            .queryParam("page-size", "20")
            .queryParam("from", "2023-02-01")
            .queryParam("until", "2023-02-06")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                caregivingRoundsByFilterQueryHandler.getCaregivingRounds(
                    match {
                        listOf(
                            it.from == LocalDate.of(2023, 2, 1),
                            it.until == LocalDate.of(2023, 2, 6),
                            it.organizationId == "01GRWDRWTM6ENFSQXTDN9HDDWK"
                        ).all { predicate -> predicate }
                    },
                    match {
                        it.pageNumber == 0 && it.pageSize == 20
                    }
                )
            } returns PageImpl(
                listOf<CaregivingRound>(
                    relaxedMock {
                        every { id } returns "01GRR1T9W6P1HBSYHWCRBBYW1N"
                        every { caregivingRoundNumber } returns 1
                        every { startDateTime } returns LocalDateTime.of(2023, 2, 1, 14, 0, 0)
                        every { endDateTime } returns null
                        every { caregivingProgressingStatus } returns CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
                        every { settlementProgressingStatus } returns SettlementProgressingStatus.NOT_STARTED
                        every { billingProgressingStatus } returns BillingProgressingStatus.NOT_STARTED
                        every { receptionInfo } returns relaxedMock {
                            every { receptionId } returns "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                            every { insuranceNumber } returns "11111-1111"
                            every { accidentNumber } returns "2022-1111111"
                            every { expectedCaregivingStartDate } returns null
                            every { receptionProgressingStatus } returns ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                            every { caregivingManagerInfo } returns relaxedMock {
                                every { organizationType } returns OrganizationType.ORGANIZATION
                                every { organizationId } returns "01GRWDRWTM6ENFSQXTDN9HDDWK"
                                every { managingUserId } returns "01GQ23MVTBAKS526S0WGS9CS0A"
                            }
                        }
                    },
                ),
                PageRequest.of(0, 20),
                1,
            )

            every {
                receptionsByIdsQueryHandler.getReceptions(
                    match {
                        it.receptionIds.contains("01GPJMK7ZPBBKTY3TP0NN5JWCJ")
                    }
                )
            } returns listOf(
                relaxedMock {
                    every { id } returns "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                    every { patientInfo.name.masked } returns "김*자"
                }
            )
        }

        afterEach { clearAllMocks() }

        should("200 Ok로 응답합니다.") {
            expectResponse(MockMvcResultMatchers.status().isOk)
        }

        should("간병 업체 아이디와 간병 시작일 범위를 지정하여 간병 회차 목록을 조회합니다.") {
            mockMvc.perform(request)

            verify {
                caregivingRoundsByFilterQueryHandler.getCaregivingRounds(
                    withArg {
                        it.organizationId shouldBe "01GRWDRWTM6ENFSQXTDN9HDDWK"
                        it.from shouldBe LocalDate.of(2023, 2, 1)
                        it.until shouldBe LocalDate.of(2023, 2, 6)
                    },
                    any()
                )
            }
        }
    }

    context("when getting organization caregiving rounds with filters and search") {
        val request = MockMvcRequestBuilders.get("/api/v1/organizations/01GRWDRWTM6ENFSQXTDN9HDDWK/caregiving-rounds")
            .queryParam("page-number", "1")
            .queryParam("page-size", "20")
            .queryParam("from", "2023-02-01")
            .queryParam("until", "2023-02-06")
            .queryParam("expected-caregiving-start-date", "2023-02-01")
            .queryParam("reception-progressing-status", "CAREGIVING_IN_PROGRESS")
            .queryParam(
                "caregiving-progressing-status",
                "NOT_STARTED", "REMATCHING", "PENDING_REMATCHING"
            )
            .queryParam("settlement-progressing-status", "NOT_STARTED")
            .queryParam("billing-progressing-status", "NOT_STARTED")
            .queryParam("query", "accidentNumber:2022-1111111")

        beforeEach {
            every {
                caregivingRoundsByFilterQueryHandler.getCaregivingRounds(any(), any())
            } returns PageImpl(
                listOf<CaregivingRound>(),
                PageRequest.of(0, 20),
                0,
            )
        }

        afterEach { clearAllMocks() }

        should("query by organization caregiving rounds using query parameters") {
            mockMvc.perform(request)

            verify {
                caregivingRoundsByFilterQueryHandler.getCaregivingRounds(
                    withArg {
                        it.expectedCaregivingStartDate shouldBe LocalDate.of(2023, 2, 1)
                        it.receptionProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                            ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                        )
                        it.caregivingProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                            CaregivingProgressingStatus.NOT_STARTED,
                            CaregivingProgressingStatus.REMATCHING,
                            CaregivingProgressingStatus.PENDING_REMATCHING
                        )
                        it.settlementProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                            SettlementProgressingStatus.NOT_STARTED
                        )
                        it.billingProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                            BillingProgressingStatus.NOT_STARTED
                        )
                        it.searchCondition!!.searchingProperty shouldBe CaregivingRoundsByFilterQuery.SearchingProperty.ACCIDENT_NUMBER
                        it.searchCondition!!.keyword shouldBe "2022-1111111"
                    },
                    any()
                )
            }
        }
    }

    context("when searching caregiving rounds with illegal query") {
        listOf(
            "AccidentNumber:2022-1111111",
            "2022-1111111",
            "AccidentNumber:",
            "AccidentNumber",
            ":",
        ).map { query ->
            MockMvcRequestBuilders.get("/api/v1/organizations/01GRR1T9W6P1HBSYHWCRBBYW1N/caregiving-rounds")
                .queryParam("page-number", "1")
                .queryParam("page-size", "20")
                .queryParam("from", "2023-02-01")
                .queryParam("until", "2023-02-06")
                .queryParam("reception-progressing-status", "CAREGIVING_IN_PROGRESS")
                .queryParam("caregiving-progressing-status", "CAREGIVING_IN_PROGRESS")
                .queryParam("settlement-progressing-status", "NOT_STARTED")
                .queryParam("billing-progressing-status", "NOT_STARTED")
                .queryParam("query", query)
        }.forEach { request ->
            val expectResponse = ResponseMatcher(mockMvc, request)

            should("response status should be 400 Bad Request") {
                expectResponse(MockMvcResultMatchers.status().isBadRequest)
            }

            should("response payload should contains error message and type") {
                expectResponse(
                    MockMvcResultMatchers.content().json(
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

    context("간병 예상일자를 기준으로 외부 간병 업체의 간병 회차 목록을 CSV 형식으로 조회하면") {
        val request = MockMvcRequestBuilders.get("/api/v1/organizations/01GRWDRWTM6ENFSQXTDN9HDDWK/caregiving-rounds")
            .header(HttpHeaders.ACCEPT, "text/csv")
            .queryParam("from", "2023-07-01")
            .queryParam("until", "2023-09-01")
            .queryParam("expected-caregiving-start-date", "2023-08-01")
            .queryParam("reception-progressing-status", "CAREGIVING_IN_PROGRESS")
            .queryParam("caregiving-progressing-status", "CAREGIVING_IN_PROGRESS")
            .queryParam("settlement-progressing-status", "NOT_STARTED")
            .queryParam("billing-progressing-status", "NOT_STARTED")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            mockkObject(Clock)
            every { Clock.today() } returns LocalDate.of(2023, 10, 10)

            every {
                caregivingRoundsByFilterQueryHandler.getCaregivingRoundsAsCsv(
                    match {
                        listOf(
                            it.organizationId == "01GRWDRWTM6ENFSQXTDN9HDDWK",
                            it.expectedCaregivingStartDate == LocalDate.of(2023, 8, 1),
                            it.receptionProgressingStatuses == setOf(
                                ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                            ),
                            it.caregivingProgressingStatuses == setOf(
                                CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
                            ),
                            it.settlementProgressingStatuses == setOf(
                                SettlementProgressingStatus.NOT_STARTED
                            ),
                            it.billingProgressingStatuses == setOf(
                                BillingProgressingStatus.NOT_STARTED
                            ),
                        ).all { predicate -> predicate }
                    },
                )
            } returns """
            간병 예상일자,병실정보,환자명,간병인명,배정담당자 소속
            2023-08-01,대구 영남대병원,김환자,우간병,대구 엄마손
            """.trimIndent()
        }

        afterEach {
            clearAllMocks()
            unmockkObject(Clock)
        }

        should("200 Ok로 응답한다.") {
            expectResponse(MockMvcResultMatchers.status().isOk)
        }

        should("다운로드 정보를 담은 Content-Disposition 헤더를 포함하여 응답한다.") {
            expectResponse(
                MockMvcResultMatchers.header().string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"%5B%EA%B0%84%EB%B3%91%EA%B4%80%EB%A6%AC%5D20230801.csv\""
                )
            )
        }

        should("병실정보 확인을 위한 CSV 데이터를 페이로드에 포함하여 응답한다.") {
            expectResponse(
                MockMvcResultMatchers.content().string(
                    StringEndsWith(
                        """
                            간병 예상일자,병실정보,환자명,간병인명,배정담당자 소속
                            2023-08-01,대구 영남대병원,김환자,우간병,대구 엄마손
                        """.trimIndent()
                    )
                )
            )
        }

        should("간병 회차 목록의 CSV 추출을 도메인 영역에 위임한다.") {
            mockMvc.perform(request)

            verify {
                caregivingRoundsByFilterQueryHandler.getCaregivingRoundsAsCsv(
                    withArg {
                        it.expectedCaregivingStartDate shouldBe LocalDate.of(2023, 8, 1)
                        it.receptionProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                            ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                        )
                        it.caregivingProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                            CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
                        )
                        it.settlementProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                            SettlementProgressingStatus.NOT_STARTED
                        )
                        it.billingProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                            BillingProgressingStatus.NOT_STARTED
                        )
                    }
                )
            }
        }
    }
})
