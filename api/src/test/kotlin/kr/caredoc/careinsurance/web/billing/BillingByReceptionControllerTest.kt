package kr.caredoc.careinsurance.web.billing

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.billing.BillingByReceptionIdQueryHandler
import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import kr.caredoc.careinsurance.reception.exception.ReceptionNotFoundByIdException
import kr.caredoc.careinsurance.relaxedMock
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime

@CareInsuranceWebMvcTest(BillingByReceptionController::class)
class BillingByReceptionControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean
    private val billingByReceptionIdQueryHandler: BillingByReceptionIdQueryHandler,
) : ShouldSpec({
    context("접수의 간병회차별 청구를 조회하면") {
        val receptionId = "01GZXB3YRW0FJVW1SKGCA3ASG8"
        val request = get("/api/v1/receptions/$receptionId/billings")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every { billingByReceptionIdQueryHandler.getBillingReception(match { it.receptionId == receptionId }) } returns listOf(
                relaxedMock {
                    every { id } returns "01GRN4YX164VKDX72KJ3M4F2X1"
                    every { caregivingRoundInfo.caregivingRoundId } returns "01H2CWYXC9H0T5ZJ6DC2ZPCXHB"
                    every { caregivingRoundInfo.roundNumber } returns 1
                    every { billingProgressingStatus } returns BillingProgressingStatus.COMPLETED_DEPOSIT
                    every { caregivingRoundInfo.startDateTime } returns LocalDateTime.of(2023, 5, 6, 14, 0, 0)
                    every { caregivingRoundInfo.endDateTime } returns LocalDateTime.of(2023, 5, 8, 14, 0, 0)
                    every { billingDate } returns LocalDate.of(2023, 5, 9)
                    every { basicAmounts } returns mutableListOf(
                        relaxedMock {
                            every { totalAmount } returns 350000
                        },
                        relaxedMock {
                            every { totalAmount } returns 400000
                        }
                    )
                    every { additionalAmount } returns 40000
                    every { totalAmount } returns 790000
                }
            )
        }

        afterEach { clearAllMocks() }

        should("응답 상태 코드는 200입니다.") {
            expectResponse(status().isOk)
        }

        should("접수의 청구 목록을 도메인에 위임합니다.") {
            mockMvc.perform(request)

            verify {
                billingByReceptionIdQueryHandler.getBillingReception(
                    withArg {
                        it.receptionId shouldBe receptionId
                    }
                )
            }
        }

        should("응답 페이로드에 간병회차별 청구를 포함합니다.") {
            expectResponse(
                content().json(
                    """
                        [
                          {
                            "id": "01GRN4YX164VKDX72KJ3M4F2X1",
                            "caregivingRoundId": "01H2CWYXC9H0T5ZJ6DC2ZPCXHB",
                            "caregivingRoundNumber": 1,
                            "billingProgressingStatus": "COMPLETED_DEPOSIT",
                            "startDateTime": "2023-05-06T05:00:00Z",
                            "endDateTime": "2023-05-08T05:00:00Z",
                            "billingDate": "2023-05-09",
                            "basicAmount": 750000,
                            "additionalAmount": 40000,
                            "totalAmount": 790000
                          }
                        ]
                    """.trimIndent()
                )
            )
        }

        context("하지만 reception 이 존재하지 않는다면") {

            beforeEach {
                every {
                    billingByReceptionIdQueryHandler.getBillingReception(match { it.receptionId == receptionId })
                } throws ReceptionNotFoundByIdException(receptionId)
            }

            afterEach { clearAllMocks() }

            should("응답 상태 코드는 404입니다.") {
                expectResponse(status().isNotFound)
            }
        }
    }
})
