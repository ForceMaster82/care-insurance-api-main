package kr.caredoc.careinsurance.web.billing

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.billing.statistics.DailyBillingTransactionStatisticsByDateQueryHandler
import kr.caredoc.careinsurance.relaxedMock
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@CareInsuranceWebMvcTest(DailyBillingTransactionStatisticsController::class)
class DailyBillingTransactionStatisticsControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean
    private val dailyBillingTransactionStatisticsByDateQueryHandler: DailyBillingTransactionStatisticsByDateQueryHandler,
) : ShouldSpec({
    context("일자별 간병비 청구로 인한 입출금 통계를 조회하면") {
        val request = get("/api/v1/daily-billing-transaction-statistics")
            .queryParam("date", "2023-01-30")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                dailyBillingTransactionStatisticsByDateQueryHandler.getDailyBillingTransactionStatistics(
                    match {
                        it.date == LocalDate.of(2023, 1, 30)
                    }
                )
            } returns relaxedMock {
                every { date } returns LocalDate.of(2023, 1, 30)
                every { totalDepositAmount } returns 902000
                every { totalWithdrawalAmount } returns 2424000
            }
        }

        afterEach { clearAllMocks() }

        should("상태 코드 200 Ok로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("단일 날짜의 청구금 입출금 통계를 목록 형식으로 페이로드에 포함하여 응답합니다.") {
            expectResponse(
                content().json(
                    """
                        [
                          {
                            "date": "2023-01-30",
                            "totalDepositAmount": 902000,
                            "totalWithdrawalAmount": 2424000
                          }
                        ]
                    """.trimIndent()
                )
            )
        }

        should("청구금 입출금 통계 조회를 도메인 영역에 위임합니다.") {
            mockMvc.perform(request)

            verify {
                dailyBillingTransactionStatisticsByDateQueryHandler.getDailyBillingTransactionStatistics(
                    withArg {
                        it.date shouldBe LocalDate.of(2023, 1, 30)
                    }
                )
            }
        }

        context("하지만 해당 날짜의 통계가 집계되지 않았다면") {
            beforeEach {
                every {
                    dailyBillingTransactionStatisticsByDateQueryHandler.getDailyBillingTransactionStatistics(
                        match {
                            it.date == LocalDate.of(2023, 1, 30)
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
