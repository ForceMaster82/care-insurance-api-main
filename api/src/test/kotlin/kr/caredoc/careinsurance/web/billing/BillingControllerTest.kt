package kr.caredoc.careinsurance.web.billing

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.billing.Billing
import kr.caredoc.careinsurance.billing.BillingByFilterQuery
import kr.caredoc.careinsurance.billing.BillingByFilterQueryHandler
import kr.caredoc.careinsurance.billing.BillingByIdQueryHandler
import kr.caredoc.careinsurance.billing.BillingNotExistsException
import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import kr.caredoc.careinsurance.billing.BillingTransactionRecordingCommandHandler
import kr.caredoc.careinsurance.billing.CoverageInfo
import kr.caredoc.careinsurance.billing.DownloadCertificateCommandHandler
import kr.caredoc.careinsurance.billing.InvalidBillingProgressingStatusChangeException
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQueryHandler
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.transaction.TransactionType
import kr.caredoc.careinsurance.withFixedClock
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime

@CareInsuranceWebMvcTest(BillingController::class)
class BillingControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean(relaxed = true)
    private val downloadCertificateCommandHandler: DownloadCertificateCommandHandler,
    @MockkBean(relaxed = true)
    private val billingByIdQueryHandler: BillingByIdQueryHandler,
    @MockkBean(relaxed = true)
    private val billingTransactionRecordingCommandHandler: BillingTransactionRecordingCommandHandler,
    @MockkBean(relaxed = true)
    private val billingByFilterQueryHandler: BillingByFilterQueryHandler,
    @MockkBean(relaxed = true)
    private val receptionsByIdsQueryHandler: ReceptionsByIdsQueryHandler,
) : ShouldSpec({
    context("사용 확인서를 다운받을 때") {
        val billingId = "01GW1SWAQEV3SCCJFKQSTBZB8Q"
        val request = get("/api/v1/billings/$billingId/certificate")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                downloadCertificateCommandHandler.downloadCertification(
                    match { it.billingId == "01GW1SWAQEV3SCCJFKQSTBZB8Q" }
                )
            } returns byteArrayOf(1, 2, 3)

            every {
                billingByIdQueryHandler.getBilling(match { it.billingId == "01GW1SWAQEV3SCCJFKQSTBZB8Q" })
            } returns relaxedMock {
                every { receptionInfo.accidentNumber } returns "2022_1234567"
                every { caregivingRoundInfo.roundNumber } returns 3
                every { billingProgressingStatus } returns BillingProgressingStatus.WAITING_FOR_BILLING
            }
        }

        afterEach {
            clearAllMocks()
        }

        should("http status 는 200 입니다.") {
            expectResponse(status().isOk)
        }

        should("사용확인서 조회를 도메인 영역에 위임합니다.") {
            mockMvc.perform(request)

            verify {
                downloadCertificateCommandHandler.downloadCertification(
                    withArg {
                        it.billingId shouldBe "01GW1SWAQEV3SCCJFKQSTBZB8Q"
                    }
                )
            }
        }

        should("사용 확인서의 크기를 Content-Length 헤더에 포함하여 응답합니다.") {
            expectResponse(header().string(HttpHeaders.CONTENT_LENGTH, "3"))
        }

        should("Content-Type은 application/pdf로 응답합니다.") {
            expectResponse(header().string(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE))
        }

        should("사용 확인서를 페이로드에 포함하여 응답합니다.") {
            expectResponse(content().bytes(byteArrayOf(1, 2, 3)))
        }

        should("Content-Disposition 헤더를 포함하여 응답합니다.") {
            withFixedClock(LocalDateTime.of(2022, 1, 30, 14, 30, 22)) {
                expectResponse(
                    header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"%5B%EC%B2%AD%EA%B5%AC%EA%B4%80%EB%A6%AC%5D2022_1234567_3_20220130.jpg\""
                    )
                )
            }
        }

        context("하지만 billing 이 존재하지 않는다면") {
            beforeEach {
                every { downloadCertificateCommandHandler.downloadCertification(match { it.billingId == billingId }) } throws BillingNotExistsException(
                    billingId
                )
            }

            afterEach {
                clearAllMocks()
            }

            should("http status 는 404 입니다.") {
                expectResponse(status().isNotFound)
            }

            should("응답에는 message, errorType 을 포함하고 있습니다. ") {
                expectResponse(
                    content().json(
                        """
                        {
                          "message": "조회하고자 하는 청구가 존재하지 않습니다.",
                          "errorType": "BILLING_NOT_EXISTS"
                        }
                        """.trimIndent()
                    )
                )
            }

            should("응답 데이터에는 입력된 billing id 를 포함하고 있습니다.") {
                expectResponse(
                    content().json(
                        """
                        {
                          "data": {
                            "enteredBillingId": "01GW1SWAQEV3SCCJFKQSTBZB8Q"
                          }
                        }
                        """.trimIndent()
                    )
                )
            }
        }

        context("하지만 청구의 상태가 청구 대기가 아니라면") {
            beforeEach {
                every {
                    downloadCertificateCommandHandler.downloadCertification(
                        match { it.billingId == "01GW1SWAQEV3SCCJFKQSTBZB8Q" }
                    )
                } throws InvalidBillingProgressingStatusChangeException(
                    currentBillingProgressingStatus = BillingProgressingStatus.UNDER_DEPOSIT,
                    enteredBillingProgressingStatus = BillingProgressingStatus.WAITING_DEPOSIT
                )
            }

            afterEach {
                clearAllMocks()
            }

            should("422 Unprocessable Entity로 응답한다.") {
                expectResponse(status().isUnprocessableEntity)
            }

            should("에러 메시지와 에러 타입과 에러 데이터를 페이로드에 포함하여 응답합니다.") {
                expectResponse(
                    content().json(
                        """
                            {
                              "message": "지정한 상태로 청구 상태를 변경할 수 없습니다.",
                              "errorType": "INVALID_BILLING_STATE_TRANSITION",
                              "data": {
                                "currentBillingProgressingStatus" : "UNDER_DEPOSIT",
                                "enteredBillingProgressingStatus" : "WAITING_DEPOSIT"
                              }
                            }
                        """.trimIndent()
                    )
                )
            }
        }
    }

    context("청구 상세 조회할 때") {
        val billingId = "01GWVCJFDFEHFAHVSS92QT6R0P"
        val request = get("/api/v1/billings/$billingId")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every { billingByIdQueryHandler.getBilling(match { it.billingId == billingId }) } returns relaxedMock {
                every { receptionInfo.receptionId } returns "01GXA7C9DP74XN7BPBXREG1P4B"
                every { receptionInfo.accidentNumber } returns "2023-2222222"
                every { receptionInfo.subscriptionDate } returns LocalDate.of(2018, 11, 1)

                every { caregivingRoundInfo.roundNumber } returns 7
                every { caregivingRoundInfo.startDateTime } returns LocalDateTime.of(2023, 3, 28, 14, 0, 0)
                every { caregivingRoundInfo.endDateTime } returns LocalDateTime.of(2023, 3, 30, 14, 0, 0)

                every { basicAmounts } returns mutableListOf(
                    relaxedMock {
                        every { targetAccidentYear } returns 2023
                        every { dailyCaregivingCharge } returns 100000
                        every { caregivingDays } returns 2
                        every { totalAmount } returns 200000
                    }
                )

                every { additionalHours } returns 3
                every { additionalAmount } returns 60000
                every { totalAmount } returns 260000
                every { totalDepositAmount } returns 260000
                every { totalWithdrawalAmount } returns 0
            }
        }

        should("http status 는 200 입니다.") {
            expectResponse(status().isOk)
        }

        should("응답 본문은 조회하고자 하는 billing 의 상세 정보를 포함합니다.") {
            expectResponse(
                content().json(
                    """
                        {
                          "accidentNumber": "2023-2222222",
                          "subscriptionDate": "2018-11-01",
                          "roundNumber": 7,
                          "startDateTime": "2023-03-28T05:00:00Z",
                          "endDateTime": "2023-03-30T05:00:00Z",
                          "basicAmounts": [
                            {
                              "targetAccidentYear": 2023,
                              "dailyCaregivingCharge": 100000,
                              "caregivingDays": 2,
                              "totalAmount": 200000
                            }
                          ],
                          "additionalHours": 3,
                          "additionalAmount": 60000,
                          "totalAmount": 260000,
                          "totalDepositAmount": 260000,
                          "totalWithdrawalAmount": 0,
                          "receptionId": "01GXA7C9DP74XN7BPBXREG1P4B"
                        }                            
                    """.trimIndent()
                )
            )
        }

        context("하지만 billing 이 존재하지 않는다면") {

            beforeEach {
                every { billingByIdQueryHandler.getBilling(match { it.billingId == billingId }) } throws BillingNotExistsException(
                    billingId
                )
            }

            afterEach {
                clearAllMocks()
            }

            should("http status 는 404 입니다.") {
                expectResponse(status().isNotFound)
            }

            should("응답에는 message, errorType 을 포함하고 있습니다. ") {
                expectResponse(
                    content().json(
                        """
                        {
                          "message": "조회하고자 하는 청구가 존재하지 않습니다.",
                          "errorType": "BILLING_NOT_EXISTS"
                        }
                        """.trimIndent()
                    )
                )
            }

            should("응답 데이터에는 입력된 billing id 를 포함하고 있습니다.") {
                expectResponse(
                    content().json(
                        """
                        {
                          "data": {
                            "enteredBillingId": "01GWVCJFDFEHFAHVSS92QT6R0P"
                          }
                        }
                        """.trimIndent()
                    )
                )
            }
        }
    }

    context("입/출금 내역을 조회하면") {
        val request = get("/api/v1/billings/01GXQSGR6X22JY5PRSA6EENM28/transactions")
            .queryParam("page-number", "1")
            .queryParam("page-size", "10")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                billingByIdQueryHandler.getBilling(match { it.billingId == "01GXQSGR6X22JY5PRSA6EENM28" })
            } returns relaxedMock {
                every { transactions } returns mutableListOf(
                    relaxedMock {
                        every { transactionType } returns TransactionType.DEPOSIT
                        every { amount } returns 655000
                        every { transactionDate } returns LocalDate.of(2023, 1, 28)
                        every { enteredDateTime } returns LocalDateTime.of(2023, 1, 30, 14, 30, 24)
                        every { transactionSubjectId } returns "01H1N7P4ESGZ6B4VAP60MT5WDA"
                    },
                    relaxedMock {
                        every { transactionType } returns TransactionType.WITHDRAWAL
                        every { amount } returns 30000
                        every { transactionDate } returns LocalDate.of(2023, 1, 30)
                        every { enteredDateTime } returns LocalDateTime.of(2023, 2, 1, 17, 50, 32)
                        every { transactionSubjectId } returns "01H1N7PCCBM3NMQGADV5JDD91W"
                    },
                )
            }
        }

        afterEach { clearAllMocks() }

        should("200 Ok로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("입출금 내역을 페이로드에 포함하여 응답합니다.") {
            expectResponse(
                content().json(
                    """
                        {
                          "currentPageNumber": 1,
                          "lastPageNumber": 1,
                          "totalItemCount": 2,
                          "items": [
                            {
                              "transactionType": "WITHDRAWAL",
                              "amount": 30000,
                              "transactionDate": "2023-01-30",
                              "enteredDateTime": "2023-02-01T08:50:32Z",
                              "transactionSubjectId": "01H1N7PCCBM3NMQGADV5JDD91W"
                            },
                            {
                              "transactionType": "DEPOSIT",
                              "amount": 655000,
                              "transactionDate": "2023-01-28",
                              "enteredDateTime": "2023-01-30T05:30:24Z",
                              "transactionSubjectId": "01H1N7P4ESGZ6B4VAP60MT5WDA"
                            }
                          ]
                        }
                    """.trimIndent()
                )
            )
        }

        should("도메인 영역에 청구 조회를 요청합니다.") {
            mockMvc.perform(request)

            verify {
                billingByIdQueryHandler.getBilling(
                    withArg {
                        it.billingId shouldBe "01GXQSGR6X22JY5PRSA6EENM28"
                    }
                )
            }
        }
    }

    context("입/출금 내역을 등록하면") {
        val billingId = "01GXQSGR6X22JY5PRSA6EENM28"
        val request = post("/api/v1/billings/$billingId/transactions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                  {
                    "transactionType": "DEPOSIT",
                    "amount": 455000,
                    "transactionDate": "2023-04-10",
                    "transactionSubjectId": "01GXQS5TQSVZGM65CZXG2EZ079"
                  }
                """.trimIndent()
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every { billingTransactionRecordingCommandHandler.recordTransaction(any(), any()) } returns Unit
        }

        afterEach { clearAllMocks() }

        should("request 에 대한 http status 는 204 입니다.") {
            expectResponse(status().isNoContent)
        }

        should("응답 본문애 내용이 없습니다.") {
            expectResponse(content().string(""))
        }

        should("billing 의 입/출금 내역 추가를 요청합니다.") {
            mockMvc.perform(request)

            verify {
                billingTransactionRecordingCommandHandler.recordTransaction(
                    withArg {
                        it.billingId shouldBe billingId
                    },
                    withArg {
                        it.transactionType shouldBe TransactionType.DEPOSIT
                        it.amount shouldBe 455000
                        it.transactionDate shouldBe LocalDate.of(2023, 4, 10)
                        it.transactionSubjectId shouldBe "01GXQS5TQSVZGM65CZXG2EZ079"
                    }
                )
            }
        }
    }

    context("청구 목록을 조회하면") {
        val request = get("/api/v1/billings")
            .queryParam("progressing-status", "WAITING_FOR_BILLING")
            .queryParam("page-number", "1")
            .queryParam("page-size", "10")
            .queryParam("used-period-from", "2023-04-01")
            .queryParam("used-period-until", "2023-04-30")
            .queryParam("sort", "ID_DESC")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every { billingByFilterQueryHandler.getBillings(any(), any()) } returns generateBillings()

            every {
                receptionsByIdsQueryHandler.getReceptions(
                    match {
                        it.receptionIds.contains("01GXN1KRX44WS9BGR112XF5KZ9")
                    }
                )
            } returns listOf(
                relaxedMock {
                    every { id } returns "01GXN1KRX44WS9BGR112XF5KZ9"
                    every { accidentInfo.accidentNumber } returns "2023-1234567"
                    every { patientInfo.name } returns relaxedMock {
                        every { masked } returns "김*철"
                    }
                }
            )
        }

        afterEach { clearAllMocks() }

        should("상태 코드 200 ok로 응답합니다. ") {
            expectResponse(status().isOk)
        }

        should("청구 목록에 대한 내용을 응답 본문에 포함하여 응답합니다.") {
            expectResponse(
                content().json(
                    """
                       {
                         "currentPageNumber": 1,
                         "lastPageNumber": 1,
                         "totalItemCount": 1,
                         "items": [
                           {
                             "id": "01GXN1KB8VN3B60CVMZG9S82FF",
                             "receptionId": "01GXN1KRX44WS9BGR112XF5KZ9",
                             "accidentNumber": "2023-1234567",
                             "patientName": "김*철",
                             "roundNumber": 3,
                             "startDateTime": "2023-04-10T06:00:00Z",
                             "endDateTime": "2023-04-20T06:00:00Z",
                             "actualUsagePeriod": "10일",
                             "billingDate": null,
                             "totalAmount": 2000000,
                             "totalDepositAmount": 0,
                             "totalWithdrawalAmount": 0,
                             "transactionDate": null,
                             "billingProgressingStatus": "WAITING_FOR_BILLING"
                           }
                         ]
                       }
                    """.trimIndent()
                )
            )
        }

        should("청구 목록 조회를 요청합니다.") {
            mockMvc.perform(request)

            verify {
                billingByFilterQueryHandler.getBillings(
                    withArg {
                        it.progressingStatus shouldBe setOf(BillingProgressingStatus.WAITING_FOR_BILLING)
                        it.usedPeriodFrom shouldBe LocalDate.of(2023, 4, 1)
                        it.usedPeriodUntil shouldBe LocalDate.of(2023, 4, 30)
                        it.searchQuery shouldBe null
                        it.sorting shouldBe BillingByFilterQuery.Sorting.ID_DESC
                    },
                    any(),
                )
            }
        }
    }
})

private fun generateBillings() = PageImpl(
    listOf(
        Billing(
            id = "01GXN1KB8VN3B60CVMZG9S82FF",
            receptionInfo = Billing.ReceptionInfo(
                receptionId = "01GXN1KRX44WS9BGR112XF5KZ9",
                accidentNumber = "2023-1234567",
                subscriptionDate = LocalDate.of(2015, 3, 24),
            ),
            caregivingRoundInfo = Billing.CaregivingRoundInfo(
                caregivingRoundId = "01GW692NXFNWT7S85RJPYR9WVZ",
                roundNumber = 3,
                startDateTime = LocalDateTime.of(2023, 4, 10, 15, 0, 0),
                endDateTime = LocalDateTime.of(2023, 4, 20, 15, 0, 0),
            ),
            billingProgressingStatus = BillingProgressingStatus.WAITING_FOR_BILLING,
            coverageInfo = CoverageInfo(
                targetSubscriptionYear = 2012,
                renewalType = CoverageInfo.RenewalType.TEN_YEAR,
                annualCoveredCaregivingCharges = listOf(
                    CoverageInfo.AnnualCoveredCaregivingCharge(
                        targetAccidentYear = 2022,
                        caregivingCharge = 100000
                    ),
                    CoverageInfo.AnnualCoveredCaregivingCharge(
                        targetAccidentYear = 2023,
                        caregivingCharge = 200000
                    ),
                )
            ),
            isCancelAfterArrived = false,
        )
    ),
    Pageable.ofSize(1),
    1
)
