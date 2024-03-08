package kr.caredoc.careinsurance.web.settlement

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.settlement.SettlementNotFoundByIdException
import kr.caredoc.careinsurance.settlement.SettlementTransactionRecordingCommandHandler
import kr.caredoc.careinsurance.settlement.TransactionsBySettlementIdQueryHandler
import kr.caredoc.careinsurance.transaction.TransactionType
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime

@CareInsuranceWebMvcTest(SettlementTransactionController::class)
class SettlementTransactionControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean
    private val transactionsBySettlementIdQueryHandler: TransactionsBySettlementIdQueryHandler,
    @MockkBean
    private val settlementTransactionRecordingCommandHandler: SettlementTransactionRecordingCommandHandler,
) : ShouldSpec({
    context("정산의 입출금 기록을 조회하면") {
        val request = get("/api/v1/settlements/01GVCX47T2590S6RYTTFDGJQP6/transactions")
            .queryParam("page-number", "1")
            .queryParam("page-size", "20")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                transactionsBySettlementIdQueryHandler.getTransactions(
                    match {
                        it.settlementId == "01GVCX47T2590S6RYTTFDGJQP6"
                    },
                    match {
                        it.pageSize == 20 && it.pageNumber == 0
                    },
                )
            } returns PageImpl(
                listOf(
                    relaxedMock {
                        every { transactionType } returns TransactionType.WITHDRAWAL
                        every { amount } returns 5000
                        every { transactionDate } returns LocalDate.of(2022, 1, 30)
                        every { enteredDateTime } returns LocalDateTime.of(2022, 2, 1, 17, 50, 32)
                        every { transactionSubjectId } returns "01GW1160R5ZC9E3P5V57TYQX0E"
                    },
                    relaxedMock {
                        every { transactionType } returns TransactionType.DEPOSIT
                        every { amount } returns 35000
                        every { transactionDate } returns LocalDate.of(2022, 1, 28)
                        every { enteredDateTime } returns LocalDateTime.of(2022, 1, 30, 14, 30, 24)
                        every { transactionSubjectId } returns "01GW118Y6KWZX0QYCBSKE5NZFB"
                    },
                    relaxedMock {
                        every { transactionType } returns TransactionType.WITHDRAWAL
                        every { amount } returns 625000
                        every { transactionDate } returns LocalDate.of(2022, 1, 25)
                        every { enteredDateTime } returns LocalDateTime.of(2022, 1, 25, 14, 30, 45)
                        every { transactionSubjectId } returns "01GW1160R5ZC9E3P5V57TYQX0E"
                    },
                ),
                PageRequest.of(0, 20),
                3,
            )
        }

        afterEach { clearAllMocks() }

        should("상태 코드 200 Ok 로 응답한다.") {
            expectResponse(status().isOk)
        }

        should("페이로드에 정산의 입출금 기록을 포함하여 응답한다.") {
            expectResponse(
                content().json(
                    """
                        {
                          "currentPageNumber": 1,
                          "lastPageNumber": 1,
                          "totalItemCount": 3,
                          "items": [
                            {
                              "transactionType": "WITHDRAWAL",
                              "amount": 5000,
                              "transactionDate": "2022-01-30",
                              "enteredDateTime": "2022-02-01T08:50:32Z",
                              "transactionSubjectId": "01GW1160R5ZC9E3P5V57TYQX0E"
                            },
                            {
                              "transactionType": "DEPOSIT",
                              "amount": 35000,
                              "transactionDate": "2022-01-28",
                              "enteredDateTime": "2022-01-30T05:30:24Z",
                              "transactionSubjectId": "01GW118Y6KWZX0QYCBSKE5NZFB"
                            },
                            {
                              "transactionType": "WITHDRAWAL",
                              "amount": 625000,
                              "transactionDate": "2022-01-25",
                              "enteredDateTime": "2022-01-25T05:30:45Z",
                              "transactionSubjectId": "01GW1160R5ZC9E3P5V57TYQX0E"
                            }
                          ]
                        }
                    """.trimIndent()
                )
            )
        }

        should("정산의 입출금 기록 조회를 도메인 영역에 위임합니다.") {
            mockMvc.perform(request)

            verify {
                transactionsBySettlementIdQueryHandler.getTransactions(
                    withArg {
                        it.settlementId shouldBe "01GVCX47T2590S6RYTTFDGJQP6"
                    },
                    withArg {
                        it.pageSize shouldBe 20
                        it.pageNumber shouldBe 0
                    }
                )
            }
        }

        context("하지만 정산이 존재하지 않다면") {
            beforeEach {
                every {
                    transactionsBySettlementIdQueryHandler.getTransactions(
                        match {
                            it.settlementId == "01GVCX47T2590S6RYTTFDGJQP6"
                        },
                        any(),
                    )
                } throws SettlementNotFoundByIdException("01GVCX47T2590S6RYTTFDGJQP6")
            }

            afterEach { clearAllMocks() }

            should("상태 코드 404 Not Found 로 응답한다.") {
                expectResponse(status().isNotFound)
            }

            should("페이로드에 에러 데이터를 포함하여 응답한다.") {
                expectResponse(
                    content().json(
                        """
                            {
                              "message": "조회하고자 하는 정산이 존재하지 않습니다.",
                              "errorType": "SETTLEMENT_NOT_EXISTS",
                              "data": {
                                "enteredSettlementId": "01GVCX47T2590S6RYTTFDGJQP6"
                              }
                            }
                        """.trimIndent()
                    )
                )
            }
        }
    }

    context("정산의 입출금을 기록하면") {
        val request = post("/api/v1/settlements/01GVCX47T2590S6RYTTFDGJQP6/transactions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                      "transactionType": "WITHDRAWAL",
                      "amount": 5000,
                      "transactionDate": "2022-01-30",
                      "transactionSubjectId": "01GW1160R5ZC9E3P5V57TYQX0E"
                    }
                """.trimIndent()
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            justRun {
                settlementTransactionRecordingCommandHandler.recordTransaction(any(), any())
            }
        }

        afterEach { clearAllMocks() }

        should("상태 코드 204 No Content 로 응답한다.") {
            expectResponse(status().isNoContent)
        }

        should("페이로드를 비운채로 응답한다.") {
            expectResponse(content().string(""))
        }

        should("입출금 내역 기록을 도메인 영역에 위임합니다.") {
            mockMvc.perform(request)

            verify {
                settlementTransactionRecordingCommandHandler.recordTransaction(
                    withArg {
                        it.settlementId shouldBe "01GVCX47T2590S6RYTTFDGJQP6"
                    },
                    withArg {
                        it.transactionType shouldBe TransactionType.WITHDRAWAL
                        it.amount shouldBe 5000
                        it.transactionDate shouldBe LocalDate.of(2022, 1, 30)
                        it.transactionSubjectId shouldBe "01GW1160R5ZC9E3P5V57TYQX0E"
                    },
                )
            }
        }

        context("하지만 정산이 존재하지 않다면") {
            beforeEach {
                every {
                    settlementTransactionRecordingCommandHandler.recordTransaction(
                        match {
                            it.settlementId == "01GVCX47T2590S6RYTTFDGJQP6"
                        },
                        any(),
                    )
                } throws SettlementNotFoundByIdException("01GVCX47T2590S6RYTTFDGJQP6")
            }

            afterEach { clearAllMocks() }

            should("상태 코드 404 Not Found 로 응답한다.") {
                expectResponse(status().isNotFound)
            }
        }
    }
})
