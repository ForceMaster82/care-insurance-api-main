package kr.caredoc.careinsurance.web.settlement

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.settlement.statistics.DailyCaregivingRoundSettlementTransactionStatisticsByDateQueryHandler
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@CareInsuranceWebMvcTest(DailyCaregivingRoundSettlementTransactionStatisticsController::class)
class DailyCaregivingRoundSettlementTransactionStatisticsControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean
    private val dailyCaregivingRoundSettlementTransactionStatisticsByDateQueryHandler: DailyCaregivingRoundSettlementTransactionStatisticsByDateQueryHandler,
) : ShouldSpec({
    context("날짜/간병회차별 정산금 입출금 통계를 조회하면") {
        val request = get("/api/v1/daily-caregiving-round-settlement-transaction-statistics")
            .queryParam("date", "2023-01-30")
            .queryParam("page-number", "1")
            .queryParam("page-size", "20")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            val pageableSlot = slot<Pageable>()
            every {
                dailyCaregivingRoundSettlementTransactionStatisticsByDateQueryHandler.getDailyCaregivingRoundSettlementTransactionStatistics(
                    match {
                        it.date == LocalDate.of(2023, 1, 30)
                    },
                    capture(pageableSlot),
                )
            } answers {
                PageImpl(
                    listOf(
                        relaxedMock {
                            every { receptionId } returns "01GWK30517ZTHWDW1QQ22V6QZC"
                            every { caregivingRoundId } returns "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                            every { date } returns LocalDate.of(2023, 1, 30)
                            every { totalDepositAmount } returns 902000
                            every { totalWithdrawalAmount } returns 2424000
                        },
                        relaxedMock {
                            every { receptionId } returns "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                            every { caregivingRoundId } returns "01GVCXJ8CYTYGZVZFZW8GQ3R5Y"
                            every { date } returns LocalDate.of(2023, 1, 30)
                            every { totalDepositAmount } returns 902000
                            every { totalWithdrawalAmount } returns 2424000
                        }
                    ),
                    pageableSlot.captured,
                    2,
                )
            }
        }

        afterEach { clearAllMocks() }

        should("상태 코드 200 Ok 로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("요청한 범위의 통계를 페이로드에 포함하여 응답합니다.") {
            expectResponse(
                content().json(
                    """
                        {
                          "currentPageNumber": 1,
                          "lastPageNumber": 1,
                          "totalItemCount": 2,
                          "items": [
                            {
                              "receptionId": "01GWK30517ZTHWDW1QQ22V6QZC",
                              "caregivingRoundId": "01GSM0PQ5G8HW2GKYXH3VGGMZG",
                              "date": "2023-01-30",
                              "totalDepositAmount": 902000,
                              "totalWithdrawalAmount": 2424000
                            },
                            {
                              "receptionId": "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                              "caregivingRoundId": "01GVCXJ8CYTYGZVZFZW8GQ3R5Y",
                              "date": "2023-01-30",
                              "totalDepositAmount": 902000,
                              "totalWithdrawalAmount": 2424000
                            }
                          ]
                        }
                    """.trimIndent()
                )
            )
        }

        should("통계 조회를 도메인 영역에 위임합니다.") {
            mockMvc.perform(request)

            verify {
                dailyCaregivingRoundSettlementTransactionStatisticsByDateQueryHandler.getDailyCaregivingRoundSettlementTransactionStatistics(
                    withArg {
                        it.date shouldBe LocalDate.of(2023, 1, 30)
                    },
                    withArg {
                        it.pageNumber shouldBe 0
                        it.pageSize shouldBe 20
                    }
                )
            }
        }
    }
})
