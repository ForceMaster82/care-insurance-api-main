package kr.caredoc.careinsurance.web.reconciliation

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.reconciliation.statistics.MonthlyReconciliationStatisticsByYearAndMonthQueryHandler
import kr.caredoc.careinsurance.relaxedMock
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@CareInsuranceWebMvcTest(ReconciliationStatisticsController::class)
class ReconciliationStatisticsControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean
    private val monthlyReconciliationStatisticsByYearAndMonthQueryHandler: MonthlyReconciliationStatisticsByYearAndMonthQueryHandler,
) : ShouldSpec({
    context("월간 대사 통계를 조회하면") {
        val request = get("/api/v1/monthly-reconciliation-statistics")
            .queryParam("year", "2023")
            .queryParam("month", "11")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                monthlyReconciliationStatisticsByYearAndMonthQueryHandler.getMonthlyReconciliationStatistics(
                    match {
                        it.year == 2023 && it.month == 11
                    }
                )
            } returns relaxedMock {
                every { year } returns 2023
                every { month } returns 11
                every { receptionCount } returns 74
                every { caregiverCount } returns 78
                every { totalCaregivingPeriod } returns 597
                every { totalBillingAmount } returns 76997000
                every { totalSettlementAmount } returns 75175100
                every { totalSales } returns 1821900
                every { totalDistributedProfit } returns 1093140
            }
        }

        afterEach {
            clearAllMocks()
        }

        should("상태 코드 200 Ok 로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("요청한 범위의 통계를 페이로드에 포함하여 응답합니다.") {
            expectResponse(
                content().json(
                    """
                        [
                          {
                            "year": 2023,
                            "month": 11,
                            "receptionCount": 74,
                            "caregiverCount": 78,
                            "totalCaregivingPeriod": 597,
                            "totalBillingAmount": 76997000,
                            "totalSettlementAmount": 75175100,
                            "totalProfit": 1821900,
                            "totalDistributedProfit": 1093140
                          }
                        ]
                    """.trimIndent()
                )
            )
        }

        should("통계 조회를 도메인 영역에 위임합니다.") {
            mockMvc.perform(request)

            verify {
                monthlyReconciliationStatisticsByYearAndMonthQueryHandler.getMonthlyReconciliationStatistics(
                    withArg {
                        it.year shouldBe 2023
                        it.month shouldBe 11
                    }
                )
            }
        }

        context("하지만 조회하고자 하는 연/월의 통계가 없다면") {
            beforeEach {
                every {
                    monthlyReconciliationStatisticsByYearAndMonthQueryHandler.getMonthlyReconciliationStatistics(
                        match {
                            it.year == 2023 && it.month == 11
                        }
                    )
                } returns null
            }

            afterEach { clearAllMocks() }

            should("빈 배열을 페이로드에 포함하여 응답합니다.") {
                expectResponse(content().json("[]"))
            }
        }
    }
})
