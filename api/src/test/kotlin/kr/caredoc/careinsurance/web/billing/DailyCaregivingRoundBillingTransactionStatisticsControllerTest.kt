package kr.caredoc.careinsurance.web.billing

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.billing.statistics.DailyCaregivingRoundBillingTransactionStatisticsByDateQueryHandler
import kr.caredoc.careinsurance.relaxedMock
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@CareInsuranceWebMvcTest(DailyCaregivingRoundBillingTransactionStatisticsController::class)
class DailyCaregivingRoundBillingTransactionStatisticsControllerTest(
    val mockMvc: MockMvc,
    @MockkBean
    val dailyCaregivingRoundBillingTransactionStatisticsByDateQueryHandler: DailyCaregivingRoundBillingTransactionStatisticsByDateQueryHandler,
) : ShouldSpec({
    context("일자별 청구 입/출금 현황 목록을 조회하면") {
        val request = get("/api/v1/daily-caregiving-round-billing-transaction-statistics")
            .queryParam("date", "2023-04-30")
            .queryParam("page-number", "1")
            .queryParam("page-size", "2")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            val pageableSlot = slot<Pageable>()
            every {
                dailyCaregivingRoundBillingTransactionStatisticsByDateQueryHandler.getDailyCaregivingRoundBillingTransactionStatistics(
                    match {
                        it.date == LocalDate.of(2023, 4, 30)
                    },
                    capture(pageableSlot),
                )
            } answers {
                PageImpl(
                    listOf(
                        relaxedMock {
                            every { receptionId } returns "01GZDFS2MTJ9JWXQTVVVGXVMRM"
                            every { caregivingRoundId } returns "01GZDFS73SJA1ST9H842SEBRT0"
                            every { date } returns LocalDate.of(2023, 4, 30)
                            every { totalDepositAmount } returns 649500
                            every { totalWithdrawalAmount } returns 649500
                        },
                        relaxedMock {
                            every { receptionId } returns "01GZDFSHFMGDCXTNEQN60E6KWM"
                            every { caregivingRoundId } returns "01GZDFSPEK34AWBJZZ9NY9H83B"
                            every { date } returns LocalDate.of(2023, 4, 30)
                            every { totalDepositAmount } returns 649500
                            every { totalWithdrawalAmount } returns 649500
                        }
                    ),
                    pageableSlot.captured, 2,
                )
            }
        }

        afterEach { clearAllMocks() }

        should("응답 상태 코드는 200 입니다.") {
            expectResponse(status().isOk)
        }

        should("페이로드에 일자별 청구 입/출금 현황 목록과 페이지에 대한 내용을 포함합니다.") {
            expectResponse(
                content().json(
                    """ 
                        {
                          "currentPageNumber": 1,
                          "lastPageNumber": 1,
                          "totalItemCount": 2,
                          "items": [
                            {
                              "receptionId": "01GZDFS2MTJ9JWXQTVVVGXVMRM",
                              "caregivingRoundId": "01GZDFS73SJA1ST9H842SEBRT0",
                              "date": "2023-04-30",
                              "totalDepositAmount": 649500,
                              "totalWithdrawalAmount": 649500
                            },
                            {
                              "receptionId": "01GZDFSHFMGDCXTNEQN60E6KWM",
                              "caregivingRoundId": "01GZDFSPEK34AWBJZZ9NY9H83B",
                              "date": "2023-04-30",
                              "totalDepositAmount": 649500,
                              "totalWithdrawalAmount": 649500
                            }
                          ]
                        }
                    """.trimIndent()
                )
            )
        }

        should("일자별 청구 입/출금 현황 목록 조회를 요청합니다.") {
            mockMvc.perform(request)

            verify {
                dailyCaregivingRoundBillingTransactionStatisticsByDateQueryHandler.getDailyCaregivingRoundBillingTransactionStatistics(
                    withArg {
                        it.date shouldBe LocalDate.of(2023, 4, 30)
                    },
                    withArg {
                        it.pageSize shouldBe 2
                        it.pageNumber shouldBe 0
                    }
                )
            }
        }
    }
})
