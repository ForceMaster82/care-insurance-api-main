package kr.caredoc.careinsurance.web.settlement

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.patch.OverwritePatch
import kr.caredoc.careinsurance.patch.Patches
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQueryHandler
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.settlement.InvalidSettlementProgressingStatusTransitionException
import kr.caredoc.careinsurance.settlement.ReferenceSettlementNotExistsException
import kr.caredoc.careinsurance.settlement.SettlementEditingCommandHandler
import kr.caredoc.careinsurance.settlement.SettlementProgressingStatus
import kr.caredoc.careinsurance.settlement.SettlementsSearchQuery
import kr.caredoc.careinsurance.settlement.SettlementsSearchQueryHandler
import kr.caredoc.careinsurance.user.ReferenceInternalCaregivingManagerNotExistsException
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

@CareInsuranceWebMvcTest(SettlementController::class)
class SettlementControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean(relaxed = true)
    private val settlementsSearchQueryHandler: SettlementsSearchQueryHandler,
    @MockkBean(relaxed = true)
    private val settlementEditingCommandHandler: SettlementEditingCommandHandler,
    @MockkBean(relaxed = true)
    private val receptionsByIdsQueryHandler: ReceptionsByIdsQueryHandler,
) : ShouldSpec({
    context("정산 예정 일자를 기준으로 조건 검색을 추가하여 정산 목록을 조회하면") {
        val request = get("/api/v1/settlements")
            .queryParam("page-size", "10")
            .queryParam("page-number", "1")
            .queryParam("progressing-status", "CONFIRMED")
            .queryParam("from", "2023-03-01")
            .queryParam("until", "2023-03-31")
            .queryParam("transaction-date-from", null)
            .queryParam("transaction-date-until", null)
            .queryParam("query", "patientName:박재병")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                settlementsSearchQueryHandler.getSettlements(
                    match {
                        listOf(
                            it.progressingStatus == SettlementProgressingStatus.CONFIRMED,
                            it.expectedSettlementDate?.from == LocalDate.of(2023, 3, 1),
                            it.expectedSettlementDate?.until == LocalDate.of(2023, 3, 31),
                            it.transactionDate?.from == null,
                            it.transactionDate?.until == null,
                            it.searchCondition?.searchingProperty == SettlementsSearchQuery.SearchingProperty.PATIENT_NAME,
                            it.searchCondition?.keyword == "박재병",
                        ).all { predicate -> predicate }
                    },
                    match {
                        it.pageSize == 10 && it.pageNumber == 0
                    }
                )
            } returns PageImpl(
                listOf(
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
                        every { lastCalculationDateTime } returns LocalDateTime.of(2022, 3, 30, 23, 21, 31)
                        every { expectedSettlementDate } returns LocalDate.of(2022, 3, 30)
                        every { totalDepositAmount } returns 100000
                        every { totalWithdrawalAmount } returns 725000
                        every { lastTransactionDatetime } returns null
                        every { settlementCompletionDateTime } returns null
                        every { settlementManagerId } returns null
                    },
                    relaxedMock {
                        every { id } returns "01GVCXJ4RX7A8KP4DPB7CDWRV8"
                        every { receptionId } returns "01GVFNYEPYJD9TWBA27BN5V9KE"
                        every { caregivingRoundId } returns "01GVCXJ8CYTYGZVZFZW8GQ3R5Y"
                        every { accidentNumber } returns "2022-1111111"
                        every { caregivingRoundNumber } returns 1
                        every { progressingStatus } returns SettlementProgressingStatus.CONFIRMED
                        every { dailyCaregivingCharge } returns 121000
                        every { basicAmount } returns 605000
                        every { additionalAmount } returns 20000
                        every { totalAmount } returns 625000
                        every { lastCalculationDateTime } returns LocalDateTime.of(2022, 3, 25, 23, 21, 31)
                        every { expectedSettlementDate } returns LocalDate.of(2022, 3, 25)
                        every { totalDepositAmount } returns 0
                        every { totalWithdrawalAmount } returns 0
                        every { lastTransactionDatetime } returns null
                        every { settlementCompletionDateTime } returns null
                        every { settlementManagerId } returns null
                    },
                ),
                PageRequest.of(0, 10),
                2,
            )

            every {
                receptionsByIdsQueryHandler.getReceptions(
                    match {
                        it.receptionIds.contains("01GVFNYEPYJD9TWBA27BN5V9KE")
                    }
                )
            } returns listOf(
                relaxedMock {
                    every { id } returns "01GVFNYEPYJD9TWBA27BN5V9KE"
                    every { patientInfo.name.masked } returns "박*병"
                }
            )
        }

        afterEach { clearAllMocks() }

        should("200 Ok로 응답한다.") {
            expectResponse(status().isOk)
        }

        should("페이로드에 정산 목록을 포함하여 응답한다.") {
            expectResponse(
                content().json(
                    """
                        {
                          "currentPageNumber": 1,
                          "lastPageNumber": 1,
                          "totalItemCount": 2,
                          "items": [
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
                              "lastCalculationDateTime": "2022-03-30T14:21:31Z",
                              "expectedSettlementDate": "2022-03-30",
                              "totalDepositAmount": 100000,
                              "totalWithdrawalAmount": 725000,
                              "lastTransactionDateTime": null,
                              "settlementCompletionDateTime": null,
                              "settlementManagerId": null
                            },
                            {
                              "id": "01GVCXJ4RX7A8KP4DPB7CDWRV8",
                              "receptionId": "01GVFNYEPYJD9TWBA27BN5V9KE",
                              "caregivingRoundId": "01GVCXJ8CYTYGZVZFZW8GQ3R5Y",
                              "accidentNumber": "2022-1111111",
                              "caregivingRoundNumber": 1,
                              "progressingStatus": "CONFIRMED",
                              "patientName": "박*병",
                              "dailyCaregivingCharge": 121000,
                              "basicAmount": 605000,
                              "additionalAmount": 20000,
                              "totalAmount": 625000,
                              "lastCalculationDateTime": "2022-03-25T14:21:31Z",
                              "expectedSettlementDate": "2022-03-25",
                              "totalDepositAmount": 0,
                              "totalWithdrawalAmount": 0,
                              "lastTransactionDateTime": null,
                              "settlementCompletionDateTime": null,
                              "settlementManagerId": null
                            }
                          ]
                        }
                    """.trimIndent()
                )
            )
        }

        should("도메인 영역으로부터 정산 목록을 조회한다.") {
            mockMvc.perform(request)

            verify {
                settlementsSearchQueryHandler.getSettlements(
                    withArg {
                        it.progressingStatus shouldBe SettlementProgressingStatus.CONFIRMED
                        it.expectedSettlementDate?.from shouldBe LocalDate.of(2023, 3, 1)
                        it.expectedSettlementDate?.until shouldBe LocalDate.of(2023, 3, 31)
                        it.sorting shouldBe SettlementsSearchQuery.Sorting.EXPECTED_SETTLEMENT_DATE_DESC_ACCIDENT_NUMBER_DESC
                        it.searchCondition?.keyword shouldBe "박재병"
                        it.searchCondition?.searchingProperty shouldBe SettlementsSearchQuery.SearchingProperty.PATIENT_NAME
                    },
                    withArg {
                        it.pageSize shouldBe 10
                        it.pageNumber shouldBe 0
                    }
                )
            }
        }
    }

    context("정렬 기준을 포함하여 정산 목록을 조회하면") {
        val request = get("/api/v1/settlements")
            .queryParam("page-size", "10")
            .queryParam("page-number", "1")
            .queryParam("progressing-status", "CONFIRMED")
            .queryParam("from", "2023-03-01")
            .queryParam("until", "2023-03-31")
            .queryParam("transaction-date-from", null)
            .queryParam("transaction-date-until", null)
            .queryParam("query", "patientName:박재병")
            .queryParam("sort", "LAST_TRANSACTION_DATE_TIME_DESC")

        should("도메인 영역으로부터 정산 목록을 조회할때 정렬 기준을 전달한다.") {
            mockMvc.perform(request)

            verify {
                settlementsSearchQueryHandler.getSettlements(
                    withArg {
                        it.sorting shouldBe SettlementsSearchQuery.Sorting.LAST_TRANSACTION_DATE_TIME_DESC
                    },
                    any(),
                )
            }
        }
    }

    context("여러 정산의 상태를 수정하면") {
        val request = patch("/api/v1/settlements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    [
                      {
                        "id": "01GVCX47T2590S6RYTTFDGJQP6",
                        "progressingStatus": "COMPLETED",
                        "settlementManagerId": "01GVCZ7W7MAYAC6C7JMJHSNEJR"
                      },
                      {
                        "id": "01GVCXJ4RX7A8KP4DPB7CDWRV8",
                        "progressingStatus": "COMPLETED",
                        "settlementManagerId": "01GVCZ7W7MAYAC6C7JMJHSNEJR"
                      }
                    ]
                """.trimIndent()
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        should("204 No Content로 응답한다.") {
            expectResponse(status().isNoContent)
        }

        should("빈 페이로드로 응답한다.") {
            expectResponse(content().string(""))
        }

        should("도메인 영역에 정산 수정을 위임한다") {
            mockMvc.perform(request)

            verify {
                settlementEditingCommandHandler.editSettlements(
                    withArg { commands ->
                        with(commands.find { it.first.settlementId == "01GVCX47T2590S6RYTTFDGJQP6" }) {
                            this?.second?.progressingStatus shouldBe OverwritePatch(SettlementProgressingStatus.COMPLETED)
                            this?.second?.settlementManagerId shouldBe OverwritePatch("01GVCZ7W7MAYAC6C7JMJHSNEJR")
                        }
                        with(commands.find { it.first.settlementId == "01GVCXJ4RX7A8KP4DPB7CDWRV8" }) {
                            this?.second?.progressingStatus shouldBe OverwritePatch(SettlementProgressingStatus.COMPLETED)
                            this?.second?.settlementManagerId shouldBe OverwritePatch("01GVCZ7W7MAYAC6C7JMJHSNEJR")
                        }
                    }
                )
            }
        }

        context("지정한 정산 관리자가 내부 관리자가 아니라면") {
            beforeEach {
                every {
                    settlementEditingCommandHandler.editSettlements(
                        match { commands ->
                            commands.find { it.second.settlementManagerId == Patches.ofValue("01GVCZ7W7MAYAC6C7JMJHSNEJR") } != null
                        }
                    )
                } throws ReferenceInternalCaregivingManagerNotExistsException("01GVCZ7W7MAYAC6C7JMJHSNEJR")
            }

            afterEach { clearAllMocks() }

            should("422 Unprocessable Entity로 응답한다.") {
                expectResponse(status().isUnprocessableEntity)
            }

            should("에러 메시지와 에러 타입과 에러 데이터를 페이로드에 포함하여 응답한다.") {
                expectResponse(
                    content().json(
                        """
                            {
                              "message": "요청에 포함된 내부 간병 관리자가 존재하지 않습니다.",
                              "errorType": "REFERENCE_INTERNAL_CAREGIVING_MANAGER_NOT_EXISTS",
                              "data": {
                                "enteredInternalCaregivingManagerId": "01GVCZ7W7MAYAC6C7JMJHSNEJR"
                              }
                            }
                        """.trimIndent()
                    )
                )
            }
        }

        context("정산의 현재 상태가 목표하는 상태로 전이할 수 없는 상태라면") {
            beforeEach {
                every {
                    settlementEditingCommandHandler.editSettlements(
                        match { commands ->
                            commands.find { it.first.settlementId == "01GVCX47T2590S6RYTTFDGJQP6" } != null
                        }
                    )
                } throws InvalidSettlementProgressingStatusTransitionException(
                    currentSettlementProgressingStatus = SettlementProgressingStatus.CONFIRMED,
                    enteredSettlementProgressingStatus = SettlementProgressingStatus.COMPLETED,
                )
            }

            afterEach { clearAllMocks() }

            should("422 Unprocessable Entity로 응답한다.") {
                expectResponse(status().isUnprocessableEntity)
            }

            should("에러 메시지와 에러 타입과 에러 데이터를 페이로드에 포함하여 응답한다.") {
                expectResponse(
                    content().json(
                        """
                            {
                              "message": "지정한 상태로 정산 진행 상태 변경할 수 없습니다.",
                              "errorType": "INVALID_SETTLEMENT_PROGRESSING_STATUS_TRANSITION",
                              "data": {
                                "currentSettlementProgressingStatus": "CONFIRMED",
                                "enteredSettlementProgressingStatus": "COMPLETED"
                              }
                            }
                        """.trimIndent()
                    )
                )
            }
        }

        context("수정하고자 하는 정산이 존재하지 않다면") {
            beforeEach {
                every {
                    settlementEditingCommandHandler.editSettlements(
                        match { commands ->
                            commands.find { it.first.settlementId == "01GVCXJ4RX7A8KP4DPB7CDWRV8" } != null
                        }
                    )
                } throws ReferenceSettlementNotExistsException("01GVCXJ4RX7A8KP4DPB7CDWRV8")
            }

            afterEach { clearAllMocks() }
            should("422 Unprocessable Entity로 응답한다.") {
                expectResponse(status().isUnprocessableEntity)
            }

            should("에러 메시지와 에러 타입과 에러 데이터를 페이로드에 포함하여 응답한다.") {
                expectResponse(
                    content().json(
                        """
                            {
                              "message": "요청에 포함된 정산이 존재하지 않습니다.",
                              "errorType": "REFERENCE_SETTLEMENT_NOT_EXISTS",
                              "data": {
                                "enteredSettlementId": "01GVCXJ4RX7A8KP4DPB7CDWRV8"
                              }
                            }
                        """.trimIndent()
                    )
                )
            }
        }
    }
    context("정산 목록을 CSV 형식으로 조회하면") {
        val request = get("/api/v1/settlements")
            .header(HttpHeaders.ACCEPT, "text/csv")
            .queryParam("progressing-status", "CONFIRMED")
            .queryParam("from", "2023-03-01")
            .queryParam("until", "2023-03-31")
            .queryParam("query", "patientName:박재병")
            .queryParam("sort", "EXPECTED_SETTLEMENT_DATE_DESC_ACCIDENT_NUMBER_DESC")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            mockkObject(Clock)
            every { Clock.today() } returns LocalDate.of(2022, 1, 30)

            every {
                settlementsSearchQueryHandler.getSettlementsAsCsv(
                    match {
                        listOf(
                            it.progressingStatus == SettlementProgressingStatus.CONFIRMED,
                            it.expectedSettlementDate?.from == LocalDate.of(2023, 3, 1),
                            it.expectedSettlementDate?.until == LocalDate.of(2023, 3, 31),
                            it.searchCondition?.searchingProperty == SettlementsSearchQuery.SearchingProperty.PATIENT_NAME,
                            it.searchCondition?.keyword == "박재병",
                            it.sorting == SettlementsSearchQuery.Sorting.EXPECTED_SETTLEMENT_DATE_DESC_ACCIDENT_NUMBER_DESC,
                        ).all { predicate -> predicate }
                    },
                )
            } returns """
                입금은행,입금계좌번호,입금액,예상예금주,입금통장표시,출금통장표시,메모,CMS코드,받는분 휴대폰번호
                신한은행,110-110-111111,625000,홍길동,오윤섭/홍길동
                국민은행,110-110-111111,625000,홍길가,오윤가/홍길가
            """.trimIndent()
        }

        afterEach {
            clearAllMocks()
            unmockkObject(Clock)
        }

        should("200 Ok로 응답한다.") {
            expectResponse(status().isOk)
        }

        should("다운로드 정보를 담은 Content-Disposition 헤더를 포함하여 응답한다.") {
            expectResponse(
                header().string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"%5B%EC%A0%95%EC%82%B0%EA%B4%80%EB%A6%AC%5D20220130.csv\""
                )
            )
        }

        should("text/csv 형식임을 알리는 Content-Type 헤더를 포함하여 응답한다.") {
            expectResponse(header().string(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8"))
        }

        should("대량이체를 위한 CSV 데이터를 페이로드에 포함하여 응답한다.") {
            expectResponse(
                content().string(
                    StringEndsWith(
                        """
                            입금은행,입금계좌번호,입금액,예상예금주,입금통장표시,출금통장표시,메모,CMS코드,받는분 휴대폰번호
                            신한은행,110-110-111111,625000,홍길동,오윤섭/홍길동
                            국민은행,110-110-111111,625000,홍길가,오윤가/홍길가
                        """.trimIndent()
                    )
                )
            )
        }

        should("정산 목록의 CSV 추출을 도메인 영역에 위임한다.") {
            mockMvc.perform(request)

            verify {
                settlementsSearchQueryHandler.getSettlementsAsCsv(
                    withArg {
                        it.progressingStatus shouldBe SettlementProgressingStatus.CONFIRMED
                        it.expectedSettlementDate?.from shouldBe LocalDate.of(2023, 3, 1)
                        it.expectedSettlementDate?.until shouldBe LocalDate.of(2023, 3, 31)
                        it.searchCondition?.searchingProperty shouldBe SettlementsSearchQuery.SearchingProperty.PATIENT_NAME
                        it.searchCondition?.keyword shouldBe "박재병"
                        it.sorting shouldBe SettlementsSearchQuery.Sorting.EXPECTED_SETTLEMENT_DATE_DESC_ACCIDENT_NUMBER_DESC
                    },
                )
            }
        }
    }
})
