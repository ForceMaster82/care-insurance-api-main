package kr.caredoc.careinsurance.web.reception

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.message.SendingStatus
import kr.caredoc.careinsurance.reception.caregivingstartmessage.CaregivingStartMessageSummariesByFilterQueryHandler
import kr.caredoc.careinsurance.reception.caregivingstartmessage.CaregivingStartMessageSummarySearchQuery
import kr.caredoc.careinsurance.reception.caregivingstartmessage.CaregivingStartMessageSummarySearchQueryHandler
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.search.SearchCondition
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@CareInsuranceWebMvcTest(CaregivingStartMessageStatusController::class)
class CaregivingStartMessageStatusControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean
    private val caregivingStartMessageSummariesByFilterQueryHandler: CaregivingStartMessageSummariesByFilterQueryHandler,
    @MockkBean
    private val caregivingStartMessageSummarySearchQueryHandler: CaregivingStartMessageSummarySearchQueryHandler,
) : ShouldSpec({
    beforeEach {
        val pageRequestSlot = slot<Pageable>()

        every {
            caregivingStartMessageSummariesByFilterQueryHandler.getCaregivingStartMessageSummaries(
                match { it.filter.date == LocalDate.of(2023, 1, 30) && it.filter.sendingStatus == null },
                capture(pageRequestSlot),
            )
        } answers {
            PageImpl(
                listOf(
                    relaxedMock {
                        every { receptionId } returns "01GZGFTQTNW16H89AX42X09959"
                        every { firstCaregivingRoundId } returns "01GZGFVG6ZQSRKVZTYG04AZ88D"
                        every { sendingStatus } returns SendingStatus.READY
                        every { sentDate } returns null
                    },
                    relaxedMock {
                        every { receptionId } returns "01GZGGR522TR0GSKH1HBW25GCR"
                        every { firstCaregivingRoundId } returns "01GZGGR524BJWA5DMNV1H6QW8J"
                        every { sendingStatus } returns SendingStatus.SENT
                        every { sentDate } returns LocalDate.of(2023, 1, 30)
                    },
                    relaxedMock {
                        every { receptionId } returns "01GZGGR525R36YJ3S3XN4VJFJV"
                        every { firstCaregivingRoundId } returns "01GZGGR525DNB1E3H9FBYXH40X"
                        every { sendingStatus } returns SendingStatus.FAILED
                        every { sentDate } returns LocalDate.of(2023, 1, 30)
                    },
                    relaxedMock {
                        every { receptionId } returns "01GZGGR525455DS5GHV6TF2QZ8"
                        every { firstCaregivingRoundId } returns "01GZGGR525YH5DASJKFZYTQ4YY"
                        every { sendingStatus } returns SendingStatus.FAILED
                        every { sentDate } returns null
                    }
                ),
                pageRequestSlot.captured,
                4,
            )
        }

        every {
            caregivingStartMessageSummariesByFilterQueryHandler.getCaregivingStartMessageSummaries(
                match { it.filter.date == LocalDate.of(2023, 1, 30) && it.filter.sendingStatus == SendingStatus.READY },
                capture(pageRequestSlot),
            )
        } answers {
            PageImpl(
                listOf(
                    relaxedMock {
                        every { receptionId } returns "01GZGFTQTNW16H89AX42X09959"
                        every { firstCaregivingRoundId } returns "01GZGFVG6ZQSRKVZTYG04AZ88D"
                        every { sendingStatus } returns SendingStatus.READY
                        every { sentDate } returns null
                    },
                ),
                pageRequestSlot.captured,
                1,
            )
        }

        every {
            caregivingStartMessageSummarySearchQueryHandler.searchCaregivingStartMessageSummary(
                match {
                    it.filter.date == LocalDate.of(2023, 1, 30) &&
                        it.filter.sendingStatus == SendingStatus.READY &&
                        it.searchCondition.searchingProperty == CaregivingStartMessageSummarySearchQuery.SearchingProperty.ACCIDENT_NUMBER &&
                        it.searchCondition.keyword == "1997"
                },
                capture(pageRequestSlot),
            )
        } answers {
            PageImpl(
                listOf(
                    relaxedMock {
                        every { receptionId } returns "01GZGFTQTNW16H89AX42X09959"
                        every { firstCaregivingRoundId } returns "01GZGFVG6ZQSRKVZTYG04AZ88D"
                        every { sendingStatus } returns SendingStatus.READY
                        every { sentDate } returns null
                    },
                ),
                pageRequestSlot.captured,
                1,
            )
        }
    }

    afterEach { clearAllMocks() }

    context("간병 시작 메시지 발송 대상 및 발송 기록을 조회하면") {
        val request = get("/api/v1/caregiving-start-message-statuses")
            .queryParam("date", "2023-01-30")
            .queryParam("page-size", "20")
            .queryParam("page-number", "1")

        val expectResponse = ResponseMatcher(mockMvc, request)

        should("200 Ok로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("발송 대상 및 발송 기록을 페이로드에 포함하여 응답합니다.") {
            expectResponse(
                content().json(
                    """
                        {
                          "currentPageNumber": 1,
                          "lastPageNumber": 1,
                          "totalItemCount": 4,
                          "items": [
                            {
                              "receptionId": "01GZGFTQTNW16H89AX42X09959",
                              "firstCaregivingRoundId": "01GZGFVG6ZQSRKVZTYG04AZ88D",
                              "sendingStatus": "READY",
                              "sentDate": null
                            },
                            {
                              "receptionId": "01GZGGR522TR0GSKH1HBW25GCR",
                              "firstCaregivingRoundId": "01GZGGR524BJWA5DMNV1H6QW8J",
                              "sendingStatus": "SENT",
                              "sentDate": "2023-01-30"
                            },
                            {
                              "receptionId": "01GZGGR525R36YJ3S3XN4VJFJV",
                              "firstCaregivingRoundId": "01GZGGR525DNB1E3H9FBYXH40X",
                              "sendingStatus": "FAILED",
                              "sentDate": "2023-01-30"
                            },
                            {
                              "receptionId": "01GZGGR525455DS5GHV6TF2QZ8",
                              "firstCaregivingRoundId": "01GZGGR525YH5DASJKFZYTQ4YY",
                              "sendingStatus": "FAILED",
                              "sentDate": null
                            }
                          ]
                        }
                    """.trimIndent()
                )
            )
        }

        should("발송 대상 및 발송 기록 조회를 도메인 영역에 위입합니다.") {
            mockMvc.perform(request)

            verify {
                caregivingStartMessageSummariesByFilterQueryHandler.getCaregivingStartMessageSummaries(
                    withArg {
                        it.filter.date shouldBe LocalDate.of(2023, 1, 30)
                        it.filter.sendingStatus shouldBe null
                    },
                    withArg {
                        it.pageNumber shouldBe 0
                        it.pageSize shouldBe 20
                    }
                )
            }
        }
    }

    context("간병 시작 메시지 발송 대상 및 발송 기록을 발송 상태로 필터링하여 조회하면") {
        val request = get("/api/v1/caregiving-start-message-statuses")
            .queryParam("date", "2023-01-30")
            .queryParam("page-size", "20")
            .queryParam("page-number", "1")
            .queryParam("sending-status", "READY")

        val expectResponse = ResponseMatcher(mockMvc, request)

        should("200 Ok로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("발송 대상 및 발송 기록 조회를 도메인 영역에 위입합니다.") {
            mockMvc.perform(request)

            verify {
                caregivingStartMessageSummariesByFilterQueryHandler.getCaregivingStartMessageSummaries(
                    withArg {
                        it.filter.date shouldBe LocalDate.of(2023, 1, 30)
                        it.filter.sendingStatus shouldBe SendingStatus.READY
                    },
                    withArg {
                        it.pageNumber shouldBe 0
                        it.pageSize shouldBe 20
                    }
                )
            }
        }
    }

    context("간병 시작 메시지 발송 대상 및 발송 기록을 발송 상태로 필터링하고 검색 조건을 적용하여 검색하면") {
        val request = get("/api/v1/caregiving-start-message-statuses")
            .queryParam("date", "2023-01-30")
            .queryParam("page-size", "20")
            .queryParam("page-number", "1")
            .queryParam("sending-status", "READY")
            .queryParam("query", "accidentNumber:1997")

        val expectResponse = ResponseMatcher(mockMvc, request)

        should("200 Ok로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("발송 대상 및 발송 기록 검색을 도메인 영역에 위입합니다.") {
            mockMvc.perform(request)

            verify {
                caregivingStartMessageSummarySearchQueryHandler.searchCaregivingStartMessageSummary(
                    withArg {
                        it.filter.date shouldBe LocalDate.of(2023, 1, 30)
                        it.filter.sendingStatus shouldBe SendingStatus.READY
                        it.searchCondition shouldBe SearchCondition(
                            searchingProperty = CaregivingStartMessageSummarySearchQuery.SearchingProperty.ACCIDENT_NUMBER,
                            keyword = "1997",
                        )
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
