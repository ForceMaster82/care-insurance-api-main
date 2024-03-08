package kr.caredoc.careinsurance.web.reconciliation

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.reconciliation.ClosedReconciliationsByFilterQueryHandler
import kr.caredoc.careinsurance.reconciliation.ClosingStatus
import kr.caredoc.careinsurance.reconciliation.InvalidReconciliationClosingStatusTransitionException
import kr.caredoc.careinsurance.reconciliation.IssuedType
import kr.caredoc.careinsurance.reconciliation.OpenReconciliationsByFilterQuery
import kr.caredoc.careinsurance.reconciliation.OpenReconciliationsByFilterQueryHandler
import kr.caredoc.careinsurance.reconciliation.ReconciliationEditingCommandHandler
import kr.caredoc.careinsurance.reconciliation.ReferenceReconciliationNotExistsException
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.search.SearchCondition
import kr.caredoc.careinsurance.withFixedClock
import org.hamcrest.core.StringEndsWith
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime

@CareInsuranceWebMvcTest(ReconciliationController::class)
class ReconciliationControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean
    private val openReconciliationsByFilterQueryHandler: OpenReconciliationsByFilterQueryHandler,
    @MockkBean
    private val closedReconciliationsByFilterQueryHandler: ClosedReconciliationsByFilterQueryHandler,
    @MockkBean
    private val reconciliationEditingCommandHandler: ReconciliationEditingCommandHandler,
) : ShouldSpec({
    context("마감 전 대사 자료 목록을 조회하면") {
        val request = get("/api/v1/reconciliations")
            .queryParam("closing-status", "OPEN")
            .queryParam("issued-at-from", "2022-10-01")
            .queryParam("issued-at-until", "2022-11-30")
            .queryParam("page-number", "1")
            .queryParam("page-size", "10")
            .queryParam("query", "accidentNumber:2022-1111111")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                openReconciliationsByFilterQueryHandler.getOpenReconciliations(any(), any())
            } returns PageImpl(
                listOf(
                    relaxedMock {
                        every { receptionId } returns "01GVFNYEPYJD9TWBA27BN5V9KE"
                        every { caregivingRoundId } returns "01GVCXJ8CYTYGZVZFZW8GQ3R5Y"
                        every { billingAmount } returns 625000
                        every { settlementAmount } returns 500000
                        every { settlementDepositAmount } returns 0
                        every { settlementWithdrawalAmount } returns 0
                        every { issuedType } returns IssuedType.FINISH
                        every { profit } returns 35000
                        every { distributedProfit } returns 21000
                    },
                    relaxedMock {
                        every { receptionId } returns "01GVFNYEPYJD9TWBA27BN5V9KE"
                        every { caregivingRoundId } returns "01GVCXJ8CYTYGZVZFZW8GQ3R5Y"
                        every { billingAmount } returns -70000
                        every { settlementAmount } returns 0
                        every { settlementDepositAmount } returns 0
                        every { settlementWithdrawalAmount } returns 0
                        every { issuedType } returns IssuedType.ADDITIONAL
                        every { profit } returns -70000
                        every { distributedProfit } returns -42000
                    },
                ),
                PageRequest.of(0, 10),
                2,
            )
        }

        afterEach { clearAllMocks() }

        should("200 Ok로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("대사 자료 목록 조회를 도메인 영역에 위임합니다.") {
            mockMvc.perform(request)

            verify {
                openReconciliationsByFilterQueryHandler.getOpenReconciliations(
                    withArg {
                        it.from shouldBe LocalDate.of(2022, 10, 1)
                        it.until shouldBe LocalDate.of(2022, 11, 30)
                        it.searchCondition shouldBe SearchCondition(
                            searchingProperty = OpenReconciliationsByFilterQuery.SearchingProperty.ACCIDENT_NUMBER,
                            keyword = "2022-1111111",
                        )
                    },
                    withArg {
                        it.pageNumber shouldBe 0
                        it.pageSize shouldBe 10
                    }
                )
            }
        }

        should("대사 자료 목록을 페이로드에 포함하여 응답합니다.") {
            expectResponse(
                content().json(
                    """
                        {
                          "currentPageNumber": 1,
                          "lastPageNumber": 1,
                          "totalItemCount": 2,
                          "items": [
                            {
                              "receptionId": "01GVFNYEPYJD9TWBA27BN5V9KE",
                              "caregivingRoundId": "01GVCXJ8CYTYGZVZFZW8GQ3R5Y",
                              "billingAmount": 625000,
                              "settlementAmount": 500000,
                              "settlementDepositAmount": 0,
                              "settlementWithdrawalAmount": 0,
                              "issuedType": "FINISH",
                              "profit": 35000,
                              "distributedProfit": 21000
                            },
                            {
                              "receptionId": "01GVFNYEPYJD9TWBA27BN5V9KE",
                              "caregivingRoundId": "01GVCXJ8CYTYGZVZFZW8GQ3R5Y",
                              "billingAmount": -70000,
                              "settlementAmount": 0,
                              "settlementDepositAmount": 0,
                              "settlementWithdrawalAmount": 0,
                              "issuedType": "ADDITIONAL",
                              "profit": -70000,
                              "distributedProfit": -42000
                            }
                          ]
                        }
                    """.trimIndent()
                )
            )
        }
    }

    context("마감 전 대사 자료 목록을 CSV 형식으로 조회하면") {
        val request = get("/api/v1/reconciliations")
            .accept("text/csv")
            .queryParam("closing-status", "OPEN")
            .queryParam("issued-at-from", "2022-10-01")
            .queryParam("issued-at-until", "2022-11-30")
            .queryParam("query", "accidentNumber:2022-1111111")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                openReconciliationsByFilterQueryHandler.getOpenReconciliationsAsCsv(any())
            } returns """
                사고번호,고객명,간병차수,청구금액,간병인명,정산금액,출금액,입금액,회차수익,케어닥수익,제휴사,분배수익
                2022-1234567,홍*동,3,625000,오간병,590000,0,0,35000,14000,케어라인,21000
                2022-1234567,홍*동,3,-70000,오간병,0,0,0,-75000,-28000,케어라인,-42000
            """.trimIndent()
        }

        afterEach { clearAllMocks() }

        should("200 Ok로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("대사 자료 목록 조회를 도메인 영역에 위임합니다.") {
            mockMvc.perform(request)

            verify {
                openReconciliationsByFilterQueryHandler.getOpenReconciliationsAsCsv(
                    withArg {
                        it.from shouldBe LocalDate.of(2022, 10, 1)
                        it.until shouldBe LocalDate.of(2022, 11, 30)
                        it.searchCondition shouldBe SearchCondition(
                            searchingProperty = OpenReconciliationsByFilterQuery.SearchingProperty.ACCIDENT_NUMBER,
                            keyword = "2022-1111111",
                        )
                    },
                )
            }
        }

        should("대사 자료 목록을 페이로드에 포함하여 응답합니다.") {
            expectResponse(
                content().string(
                    StringEndsWith(
                        """
                            사고번호,고객명,간병차수,청구금액,간병인명,정산금액,출금액,입금액,회차수익,케어닥수익,제휴사,분배수익
                            2022-1234567,홍*동,3,625000,오간병,590000,0,0,35000,14000,케어라인,21000
                            2022-1234567,홍*동,3,-70000,오간병,0,0,0,-75000,-28000,케어라인,-42000
                        """.trimIndent()
                    )
                )
            )
        }

        should("Content-Disposition 헤더를 포함하여 응답합니다.") {
            withFixedClock(LocalDateTime.of(2023, 1, 17, 13, 7, 21)) {
                expectResponse(
                    header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"%EC%A0%95%EC%82%B0%EB%8C%80%EC%82%AC%ED%98%84%ED%99%A9_20230117.csv\"",
                    )
                )
            }
        }
    }

    context("마감한 대사 자료 목록을 조회하면") {
        val request = get("/api/v1/reconciliations")
            .queryParam("closing-status", "CLOSED")
            .queryParam("reconciled-year", "2023")
            .queryParam("reconciled-month", "11")
            .queryParam("page-number", "1")
            .queryParam("page-size", "10")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                closedReconciliationsByFilterQueryHandler.getClosedReconciliations(any(), any())
            } returns PageImpl(
                listOf(
                    relaxedMock {
                        every { id } returns "01GXWPTR15MV1XGQVMDW6A40FZ"
                        every { closingStatus } returns ClosingStatus.OPEN
                        every { receptionId } returns "01GVFNYEPYJD9TWBA27BN5V9KE"
                        every { caregivingRoundId } returns "01GVCXJ8CYTYGZVZFZW8GQ3R5Y"
                        every { billingAmount } returns 625000
                        every { settlementAmount } returns 500000
                        every { settlementDepositAmount } returns 0
                        every { settlementWithdrawalAmount } returns 0
                        every { issuedType } returns IssuedType.FINISH
                        every { profit } returns 35000
                        every { distributedProfit } returns 21000
                    },
                    relaxedMock {
                        every { id } returns "01GXWPT9QK5AZMGS1WYZ35NBJC"
                        every { closingStatus } returns ClosingStatus.OPEN
                        every { receptionId } returns "01GVFNYEPYJD9TWBA27BN5V9KE"
                        every { caregivingRoundId } returns "01GVCXJ8CYTYGZVZFZW8GQ3R5Y"
                        every { billingAmount } returns -70000
                        every { settlementAmount } returns 0
                        every { settlementDepositAmount } returns 0
                        every { settlementWithdrawalAmount } returns 0
                        every { issuedType } returns IssuedType.ADDITIONAL
                        every { profit } returns -70000
                        every { distributedProfit } returns -42000
                    },
                ),
                PageRequest.of(0, 10),
                2,
            )
        }

        afterEach { clearAllMocks() }

        should("200 Ok로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("대사 자료 목록 조회를 도메인 영역에 위임합니다.") {
            mockMvc.perform(request)

            verify {
                closedReconciliationsByFilterQueryHandler.getClosedReconciliations(
                    withArg {
                        it.year shouldBe 2023
                        it.month shouldBe 11
                    },
                    withArg {
                        it.pageNumber shouldBe 0
                        it.pageSize shouldBe 10
                    }
                )
            }
        }

        should("대사 자료 목록을 페이로드에 포함하여 응답합니다.") {
            expectResponse(
                content().json(
                    """
                        {
                          "currentPageNumber": 1,
                          "lastPageNumber": 1,
                          "totalItemCount": 2,
                          "items": [
                            {
                              "id": "01GXWPTR15MV1XGQVMDW6A40FZ",
                              "closingStatus": "OPEN",
                              "receptionId": "01GVFNYEPYJD9TWBA27BN5V9KE",
                              "caregivingRoundId": "01GVCXJ8CYTYGZVZFZW8GQ3R5Y",
                              "billingAmount": 625000,
                              "settlementAmount": 500000,
                              "settlementDepositAmount": 0,
                              "settlementWithdrawalAmount": 0,
                              "issuedType": "FINISH",
                              "profit": 35000,
                              "distributedProfit": 21000
                            },
                            {
                              "id": "01GXWPT9QK5AZMGS1WYZ35NBJC",
                              "closingStatus": "OPEN",
                              "receptionId": "01GVFNYEPYJD9TWBA27BN5V9KE",
                              "caregivingRoundId": "01GVCXJ8CYTYGZVZFZW8GQ3R5Y",
                              "billingAmount": -70000,
                              "settlementAmount": 0,
                              "settlementDepositAmount": 0,
                              "settlementWithdrawalAmount": 0,
                              "issuedType": "ADDITIONAL",
                              "profit": -70000,
                              "distributedProfit": -42000
                            }
                          ]
                        }
                    """.trimIndent()
                )
            )
        }
    }

    context("마감 된 대사 자료 목록을 CSV 형식으로 조회하면") {
        val request = get("/api/v1/reconciliations")
            .accept("text/csv")
            .queryParam("closing-status", "CLOSED")
            .queryParam("reconciled-year", "2023")
            .queryParam("reconciled-month", "11")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                closedReconciliationsByFilterQueryHandler.getClosedReconciliationsAsCsv(any())
            } returns """
                사고번호,고객명,간병차수,청구금액,간병인명,정산금액,출금액,입금액,회차수익,케어닥수익,제휴사,분배수익
                2022-1234567,홍*동,3,625000,오간병,590000,0,0,35000,14000,케어라인,21000
                2022-1234567,홍*동,3,-70000,오간병,0,0,0,-75000,-28000,케어라인,-42000
            """.trimIndent()
        }

        afterEach { clearAllMocks() }

        should("200 Ok로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("대사 자료 목록 조회를 도메인 영역에 위임합니다.") {
            mockMvc.perform(request)

            verify {
                closedReconciliationsByFilterQueryHandler.getClosedReconciliationsAsCsv(
                    withArg {
                        it.year shouldBe 2023
                        it.month shouldBe 11
                    }
                )
            }
        }

        should("대사 자료 목록을 페이로드에 포함하여 응답합니다.") {
            expectResponse(
                content().string(
                    StringEndsWith(
                        """
                            사고번호,고객명,간병차수,청구금액,간병인명,정산금액,출금액,입금액,회차수익,케어닥수익,제휴사,분배수익
                            2022-1234567,홍*동,3,625000,오간병,590000,0,0,35000,14000,케어라인,21000
                            2022-1234567,홍*동,3,-70000,오간병,0,0,0,-75000,-28000,케어라인,-42000
                        """.trimIndent()
                    )
                )
            )
        }

        should("Content-Disposition 헤더를 포함하여 응답합니다.") {
            withFixedClock(LocalDateTime.of(2023, 1, 17, 13, 7, 21)) {
                expectResponse(
                    header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"%EC%A0%95%EC%82%B0%EB%8C%80%EC%82%AC%ED%98%84%ED%99%A9_20230117.csv\"",
                    )
                )
            }
        }
    }

    context("대사들을 마감하면") {
        val request = patch("/api/v1/reconciliations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    [
                      {
                        "id": "01GXWPTR15MV1XGQVMDW6A40FZ",
                        "closingStatus": "CLOSED"
                      },
                      {
                        "id": "01GXWPT9QK5AZMGS1WYZ35NBJC",
                        "closingStatus": "CLOSED"
                      }
                    ]
                """.trimIndent()
            )

        beforeEach {
            justRun {
                reconciliationEditingCommandHandler.editReconciliations(any())
            }
        }

        afterEach { clearAllMocks() }

        val expectResponse = ResponseMatcher(mockMvc, request)

        should("상태 코드 204 No Content 로 응답합니다.") {
            expectResponse(status().isNoContent)
        }

        should("아무런 내용 없는 페이로드로 응답합니다.") {
            expectResponse(content().string(""))
        }

        should("도메인 영역에 대사 마감을 상태 수정의 형태로 위임합니다.") {
            mockMvc.perform(request)

            verify {
                reconciliationEditingCommandHandler.editReconciliations(
                    withArg {
                        it.find { pair ->
                            pair.first.reconciliationId == "01GXWPTR15MV1XGQVMDW6A40FZ" &&
                                pair.second.closingStatus == ClosingStatus.CLOSED
                        } shouldNotBe null
                        it.find { pair ->
                            pair.first.reconciliationId == "01GXWPT9QK5AZMGS1WYZ35NBJC" &&
                                pair.second.closingStatus == ClosingStatus.CLOSED
                        } shouldNotBe null
                    }
                )
            }
        }

        context("만약 변경하고자 하는 대사가 존재하지 않다면") {
            beforeEach {
                every {
                    reconciliationEditingCommandHandler.editReconciliations(
                        match {
                            it.find { each -> each.first.reconciliationId == "01GXWPTR15MV1XGQVMDW6A40FZ" } != null
                        }
                    )
                } throws ReferenceReconciliationNotExistsException("01GXWPTR15MV1XGQVMDW6A40FZ")
            }

            afterEach { clearAllMocks() }

            should("상태 코드 422 Unprocessable Entity로 응답합니다.") {
                expectResponse(status().isUnprocessableEntity)
            }

            should("메시지와 에러 타입, 에러 데이터를 페이로드에 포함하여 응답합니다.") {
                expectResponse(
                    content().json(
                        """
                            {
                              "message": "요청에 포함된 대사가 존재하지 않습니다.",
                              "errorType": "REFERENCE_RECONCILIATION_NOT_EXISTS",
                              "data": {
                                "enteredReconciliationId": "01GXWPTR15MV1XGQVMDW6A40FZ"
                              }
                            }
                        """.trimIndent()
                    )
                )
            }
        }

        context("만약 변경 요청이 유효하지 않다면") {
            beforeEach {
                every {
                    reconciliationEditingCommandHandler.editReconciliations(
                        match {
                            it.find { each ->
                                each.first.reconciliationId == "01GXWPTR15MV1XGQVMDW6A40FZ" &&
                                    each.second.closingStatus == ClosingStatus.CLOSED
                            } != null
                        }
                    )
                } throws InvalidReconciliationClosingStatusTransitionException(
                    ClosingStatus.CLOSED,
                    ClosingStatus.CLOSED,
                )
            }

            afterEach { clearAllMocks() }

            should("상태 코드 422 Unprocessable Entity로 응답합니다.") {
                expectResponse(status().isUnprocessableEntity)
            }

            should("메시지와 에러 타입, 에러 데이터를 페이로드에 포함하여 응답합니다.") {
                expectResponse(
                    content().json(
                        """
                            {
                              "message": "지정한 상태로 대사 마감 상태를 변경할 수 없습니다.",
                              "errorType": "INVALID_RECONCILIATION_CLOSING_STATUS_TRANSITION",
                              "data": {
                                "currentReconciliationClosingStatus": "CLOSED",
                                "enteredReconciliationClosingStatus": "CLOSED"
                              }
                            }
                        """.trimIndent()
                    )
                )
            }
        }
    }
})
