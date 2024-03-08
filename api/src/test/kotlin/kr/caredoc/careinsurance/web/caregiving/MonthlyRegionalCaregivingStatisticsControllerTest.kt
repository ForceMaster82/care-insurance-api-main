package kr.caredoc.careinsurance.web.caregiving

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.caregiving.MonthlyRegionalCaregivingStatisticsByFilterQueryHandler
import kr.caredoc.careinsurance.relaxedMock
import org.hamcrest.core.StringEndsWith
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@CareInsuranceWebMvcTest(MonthlyRegionalCaregivingStatisticsController::class)
class MonthlyRegionalCaregivingStatisticsControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean
    private val monthlyRegionalCaregivingStatisticsByFilterQueryHandler: MonthlyRegionalCaregivingStatisticsByFilterQueryHandler,
) : ShouldSpec({
    context("월별 지역별 간병 통계를 조회하면") {
        val request = get("/api/v1/monthly-regional-caregiving-statistics")
            .queryParam("year", "2023")
            .queryParam("month", "11")
            .queryParam("page-number", "1")
            .queryParam("page-size", "2")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                monthlyRegionalCaregivingStatisticsByFilterQueryHandler.getMonthlyRegionalCaregivingStatistics(
                    match {
                        it.year == 2023 && it.month == 11
                    },
                    any(),
                )
            } returns PageImpl(
                listOf(
                    relaxedMock {
                        every { state } returns "서울특별시"
                        every { city } returns "강남구"
                        every { receptionCount } returns 24265
                    },
                    relaxedMock {
                        every { state } returns "서울특별시"
                        every { city } returns "강동구"
                        every { receptionCount } returns 24265
                    },
                ),
                PageRequest.of(0, 2),
                2,
            )
        }

        afterEach { clearAllMocks() }

        should("200 Ok로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("월별 지역별 간병 통계를 페이로드에 포함하여 응답합니다.") {
            expectResponse(
                content().json(
                    """
                        {
                          "currentPageNumber": 1,
                          "lastPageNumber": 1,
                          "totalItemCount": 2,
                          "items": [
                            {
                              "year": 2023,
                              "month": 11,
                              "state": "서울특별시",
                              "city": "강남구",
                              "receptionCount": 24265
                            },
                            {
                              "year": 2023,
                              "month": 11,
                              "state": "서울특별시",
                              "city": "강동구",
                              "receptionCount": 24265
                            }
                          ]
                        }
                    """.trimIndent()
                )
            )
        }

        should("도메인 영역에 통계 조회를 위임합니다.") {
            mockMvc.perform(request)

            verify {
                monthlyRegionalCaregivingStatisticsByFilterQueryHandler.getMonthlyRegionalCaregivingStatistics(
                    withArg {
                        it.year shouldBe 2023
                        it.month shouldBe 11
                        it.stateFilter shouldBe null
                        it.cityFilter shouldBe null
                    },
                    withArg {
                        it.pageSize shouldBe 2
                        it.pageNumber shouldBe 0
                    }
                )
            }
        }
    }

    context("월별 지역별 간병 통계를 지역별로 필터링하여 조회하면") {
        val request = get("/api/v1/monthly-regional-caregiving-statistics")
            .queryParam("year", "2023")
            .queryParam("month", "11")
            .queryParam("page-number", "1")
            .queryParam("page-size", "2")
            .queryParam("state", "서울특별시")
            .queryParam("city", "강남구")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                monthlyRegionalCaregivingStatisticsByFilterQueryHandler.getMonthlyRegionalCaregivingStatistics(
                    match {
                        it.year == 2023 &&
                            it.month == 11 &&
                            it.stateFilter == "서울특별시" &&
                            it.cityFilter == "강남구"
                    },
                    any(),
                )
            } returns PageImpl(
                listOf(
                    relaxedMock {
                        every { state } returns "서울특별시"
                        every { city } returns "강남구"
                        every { receptionCount } returns 24265
                    },
                ),
                PageRequest.of(0, 2),
                1,
            )
        }

        afterEach { clearAllMocks() }

        should("200 Ok로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("도메인 영역에 통계 조회를 위임합니다.") {
            mockMvc.perform(request)

            verify {
                monthlyRegionalCaregivingStatisticsByFilterQueryHandler.getMonthlyRegionalCaregivingStatistics(
                    withArg {
                        it.year shouldBe 2023
                        it.month shouldBe 11
                        it.stateFilter shouldBe "서울특별시"
                        it.cityFilter shouldBe "강남구"
                    },
                    withArg {
                        it.pageSize shouldBe 2
                        it.pageNumber shouldBe 0
                    }
                )
            }
        }
    }

    context("월별 지역별 간병 통계를 CSV 형식으로 조회하면") {
        val request = get("/api/v1/monthly-regional-caregiving-statistics")
            .accept("text/csv")
            .queryParam("year", "2023")
            .queryParam("month", "11")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                monthlyRegionalCaregivingStatisticsByFilterQueryHandler.getMonthlyRegionalCaregivingStatisticsAsCsv(
                    match {
                        it.year == 2023 &&
                            it.month == 11
                    },
                )
            } returns """
                시/도,시/군/구,돌봄환자 수
                서울특별시,강남구,24265
                서울특별시,강동구,24265
            """.trimIndent()
        }

        afterEach { clearAllMocks() }

        should("200 Ok로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("월별 지역별 간병 통계를 CSV 형식으로 페이로드에 포함하여 응답합니다.") {
            expectResponse(
                content().string(
                    StringEndsWith.endsWith(
                        """
                            시/도,시/군/구,돌봄환자 수
                            서울특별시,강남구,24265
                            서울특별시,강동구,24265
                        """.trimIndent()
                    )
                )
            )
        }

        should("도메인 영역에 통계 조회를 위임합니다.") {
            mockMvc.perform(request)

            verify {
                monthlyRegionalCaregivingStatisticsByFilterQueryHandler.getMonthlyRegionalCaregivingStatisticsAsCsv(
                    withArg {
                        it.year shouldBe 2023
                        it.month shouldBe 11
                        it.stateFilter shouldBe null
                        it.cityFilter shouldBe null
                    }
                )
            }
        }
    }

    context("월별 지역별 간병 통계를 지역별로 필터링하여 CSV 형식으로 조회하면") {
        val request = get("/api/v1/monthly-regional-caregiving-statistics")
            .accept("text/csv")
            .queryParam("year", "2023")
            .queryParam("month", "11")
            .queryParam("state", "서울특별시")
            .queryParam("city", "강남구")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                monthlyRegionalCaregivingStatisticsByFilterQueryHandler.getMonthlyRegionalCaregivingStatisticsAsCsv(
                    match {
                        it.year == 2023 &&
                            it.month == 11 &&
                            it.stateFilter == "서울특별시" &&
                            it.cityFilter == "강남구"
                    },
                )
            } returns """
                시/도,시/군/구,돌봄환자 수
                서울특별시,강남구,24265
            """.trimIndent()
        }

        afterEach { clearAllMocks() }

        should("200 Ok로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("도메인 영역에 통계 조회를 위임합니다.") {
            mockMvc.perform(request)

            verify {
                monthlyRegionalCaregivingStatisticsByFilterQueryHandler.getMonthlyRegionalCaregivingStatisticsAsCsv(
                    withArg {
                        it.year shouldBe 2023
                        it.month shouldBe 11
                        it.stateFilter shouldBe "서울특별시"
                        it.cityFilter shouldBe "강남구"
                    }
                )
            }
        }
    }
})
