package kr.caredoc.careinsurance.web.settlement

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQueryHandler
import kr.caredoc.careinsurance.reception.exception.ReceptionNotFoundByIdException
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.settlement.SettlementProgressingStatus
import kr.caredoc.careinsurance.settlement.SettlementsByReceptionIdQueryHandler
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime

@CareInsuranceWebMvcTest(SettlementsByReceptionController::class)
class SettlementsByReceptionControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean
    private val settlementsByReceptionIdQueryHandler: SettlementsByReceptionIdQueryHandler,
    @MockkBean
    private val receptionsByIdsQueryHandler: ReceptionsByIdsQueryHandler,
) : ShouldSpec({
    context("접수에 포함된 정산 목록을 조회하면") {
        val request = get("/api/v1/receptions/01GPWNKNMTRT8PJXJHXVA921NQ/settlements")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                settlementsByReceptionIdQueryHandler.getSettlements(
                    match {
                        it.receptionId == "01GPWNKNMTRT8PJXJHXVA921NQ"
                    }
                )
            } returns listOf(
                relaxedMock {
                    every { id } returns "01GVCX47T2590S6RYTTFDGJQP6"
                    every { receptionId } returns "01GVFNYEPYJD9TWBA27BN5V9KE"
                    every { caregivingRoundId } returns "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                    every { accidentNumber } returns "2022-1111111"
                    every { caregivingRoundNumber } returns 2
                    every { progressingStatus } returns SettlementProgressingStatus.CONFIRMED
                    every { dailyCaregivingCharge } returns 121000
                    every { basicAmount } returns 605000
                    every { additionalAmount } returns 20000
                    every { totalAmount } returns 625000
                    every { lastCalculationDateTime } returns LocalDateTime.of(2022, 1, 30, 23, 21, 31)
                    every { expectedSettlementDate } returns LocalDate.of(2022, 1, 30)
                    every { totalDepositAmount } returns 100000
                    every { totalWithdrawalAmount } returns 725000
                    every { lastTransactionDatetime } returns LocalDateTime.of(2022, 3, 14, 23, 21, 31)
                    every { settlementCompletionDateTime } returns null
                    every { settlementManagerId } returns null
                },
                relaxedMock {
                    every { id } returns "01GVCXJ4RX7A8KP4DPB7CDWRV8"
                    every { receptionId } returns "01GVFNYEPYJD9TWBA27BN5V9KE"
                    every { caregivingRoundId } returns "01GVCXJ8CYTYGZVZFZW8GQ3R5Y"
                    every { accidentNumber } returns "2022-1111111"
                    every { caregivingRoundNumber } returns 1
                    every { progressingStatus } returns SettlementProgressingStatus.COMPLETED
                    every { dailyCaregivingCharge } returns 121000
                    every { basicAmount } returns 605000
                    every { additionalAmount } returns 20000
                    every { totalAmount } returns 625000
                    every { lastCalculationDateTime } returns LocalDateTime.of(2022, 1, 25, 23, 21, 31)
                    every { expectedSettlementDate } returns LocalDate.of(2022, 1, 25)
                    every { totalDepositAmount } returns 0
                    every { totalWithdrawalAmount } returns 0
                    every { lastTransactionDatetime } returns null
                    every { settlementCompletionDateTime } returns LocalDateTime.of(2022, 1, 26, 3, 21, 31)
                    every { settlementManagerId } returns "01GVCZ7W7MAYAC6C7JMJHSNEJR"
                },
            )

            every {
                receptionsByIdsQueryHandler.getReceptions(
                    match { it.receptionIds.contains("01GVFNYEPYJD9TWBA27BN5V9KE") }
                )
            } returns listOf(
                relaxedMock {
                    every { id } returns "01GVFNYEPYJD9TWBA27BN5V9KE"
                    every { patientInfo.name.masked } returns "박*병"
                }
            )
        }

        afterEach { clearAllMocks() }

        should("상태 코드 200 Ok로 응답한다.") {
            expectResponse(status().isOk)
        }

        should("페이로드에 정산 목록을 포함하여 응답한다.") {
            expectResponse(
                content().json(
                    """
                        [
                            {
                                "id": "01GVCX47T2590S6RYTTFDGJQP6",
                                "receptionId": "01GVFNYEPYJD9TWBA27BN5V9KE",
                                "caregivingRoundId": "01GSM0PQ5G8HW2GKYXH3VGGMZG",
                                "accidentNumber": "2022-1111111",
                                "caregivingRoundNumber": 2,
                                "progressingStatus": "CONFIRMED",
                                "patientName": "박*병",
                                "dailyCaregivingCharge": 121000,
                                "basicAmount": 605000,
                                "additionalAmount": 20000,
                                "totalAmount": 625000,
                                "lastCalculationDateTime": "2022-01-30T14:21:31Z",
                                "expectedSettlementDate": "2022-01-30",
                                "totalDepositAmount": 100000,
                                "totalWithdrawalAmount": 725000,
                                "lastTransactionDateTime": "2022-03-14T14:21:31Z",
                                "settlementCompletionDateTime": null,
                                "settlementManagerId": null
                            },
                            {
                                "id": "01GVCXJ4RX7A8KP4DPB7CDWRV8",
                                "receptionId": "01GVFNYEPYJD9TWBA27BN5V9KE",
                                "caregivingRoundId": "01GVCXJ8CYTYGZVZFZW8GQ3R5Y",
                                "accidentNumber": "2022-1111111",
                                "caregivingRoundNumber": 1,
                                "progressingStatus": "COMPLETED",
                                "patientName": "박*병",
                                "dailyCaregivingCharge": 121000,
                                "basicAmount": 605000,
                                "additionalAmount": 20000,
                                "totalAmount": 625000,
                                "lastCalculationDateTime": "2022-01-25T14:21:31Z",
                                "expectedSettlementDate": "2022-01-25",
                                "totalDepositAmount": 0,
                                "totalWithdrawalAmount": 0,
                                "lastTransactionDateTime": null,
                                "settlementCompletionDateTime": "2022-01-25T18:21:31Z",
                                "settlementManagerId": "01GVCZ7W7MAYAC6C7JMJHSNEJR"
                            }
                        ]
                    """.trimIndent()
                )
            )
        }

        should("도메인 영역으로부터 정산 목록을 조회한다.") {
            mockMvc.perform(request)

            verify {
                settlementsByReceptionIdQueryHandler.getSettlements(
                    withArg {
                        it.receptionId shouldBe "01GPWNKNMTRT8PJXJHXVA921NQ"
                    }
                )
            }
        }

        context("하지만 정산이 존재하지 않다면") {
            beforeEach {
                every {
                    settlementsByReceptionIdQueryHandler.getSettlements(
                        match {
                            it.receptionId == "01GPWNKNMTRT8PJXJHXVA921NQ"
                        }
                    )
                } throws ReceptionNotFoundByIdException("01GPWNKNMTRT8PJXJHXVA921NQ")
            }

            afterEach { clearAllMocks() }

            should("상태 코드 404 Not Found 로 응답한다.") {
                expectResponse(status().isNotFound)
            }
            // Reception 조회시 발생하는 404 에러와 동일하므로 그 외 생략
        }
    }
})
