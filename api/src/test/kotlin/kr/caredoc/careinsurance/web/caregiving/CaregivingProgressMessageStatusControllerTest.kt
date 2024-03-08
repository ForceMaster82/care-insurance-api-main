package kr.caredoc.careinsurance.web.caregiving

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.caregiving.progressmessage.CaregivingProgressMessageSummariesByFilterQueryHandler
import kr.caredoc.careinsurance.caregiving.progressmessage.CaregivingProgressMessageSummariesSearchQuery
import kr.caredoc.careinsurance.caregiving.progressmessage.CaregivingProgressMessageSummariesSearchQueryHandler
import kr.caredoc.careinsurance.message.SendingStatus
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.search.SearchCondition
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime

@CareInsuranceWebMvcTest(CaregivingProgressMessageStatusController::class)
class CaregivingProgressMessageStatusControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean
    private val caregivingProgressMessageSummariesByFilterQueryHandler: CaregivingProgressMessageSummariesByFilterQueryHandler,
    @MockkBean
    private val caregivingProgressMessageSummariesSearchQueryHandler: CaregivingProgressMessageSummariesSearchQueryHandler,
) : ShouldSpec({
    beforeEach {
        val pageableSlot = slot<Pageable>()
        every {
            caregivingProgressMessageSummariesByFilterQueryHandler.getCaregivingProgressMessageSummaries(
                match {
                    it.filter.date == LocalDate.of(2023, 5, 15) && it.filter.sendingStatus == null
                },
                capture(pageableSlot),
            )
        } answers {
            PageImpl(
                listOf(
                    relaxedMock {
                        every { caregivingRoundId } returns "01GZX0EK7VNMWPR6ZR6545NJJQ"
                        every { receptionId } returns "01GZX0E40PKS0CNGTN4RRABRRP"
                        every { caregivingRoundNumber } returns 1
                        every { startDateTime } returns LocalDateTime.of(2023, 5, 10, 14, 44, 22)
                        every { sendingStatus } returns SendingStatus.READY
                        every { sentDate } returns null
                    },
                    relaxedMock {
                        every { caregivingRoundId } returns "01GZZHYDKT057QTK8TTJDRTJZX"
                        every { receptionId } returns "01H0P61EJHKC73AK11MPPKC8N0"
                        every { caregivingRoundNumber } returns 2
                        every { startDateTime } returns LocalDateTime.of(2023, 5, 5, 14, 22, 11)
                        every { sendingStatus } returns SendingStatus.READY
                        every { sentDate } returns null
                    },
                    relaxedMock {
                        every { caregivingRoundId } returns "01H0P62303EBZDJH8VMJX83JAY"
                        every { receptionId } returns "01H0P6138P2599ZASGVP9JBS6E"
                        every { caregivingRoundNumber } returns 3
                        every { startDateTime } returns LocalDateTime.of(2023, 5, 5, 14, 22, 11)
                        every { sendingStatus } returns SendingStatus.SENT
                        every { sentDate } returns LocalDate.of(2023, 5, 15)
                    },
                    relaxedMock {
                        every { caregivingRoundId } returns "01H0P62F5EA1XAEHQ6NR1G6387"
                        every { receptionId } returns "01H0P60M0DTBTJ4HS2GV2RWNPS"
                        every { caregivingRoundNumber } returns 4
                        every { startDateTime } returns LocalDateTime.of(2023, 5, 5, 14, 22, 11)
                        every { sendingStatus } returns SendingStatus.FAILED
                        every { sentDate } returns LocalDate.of(2023, 5, 15)
                    },
                    relaxedMock {
                        every { caregivingRoundId } returns "01H0P630YA1VWKK1MHCRP0MVST"
                        every { receptionId } returns "01H0P5ZW56Z306G9PV0YHFPJ3Y"
                        every { caregivingRoundNumber } returns 5
                        every { startDateTime } returns LocalDateTime.of(2023, 5, 5, 14, 22, 11)
                        every { sendingStatus } returns SendingStatus.FAILED
                        every { sentDate } returns null
                    },
                ),
                pageableSlot.captured,
                4,
            )
        }

        every {
            caregivingProgressMessageSummariesByFilterQueryHandler.getCaregivingProgressMessageSummaries(
                match {
                    it.filter.date == LocalDate.of(2023, 5, 15) && it.filter.sendingStatus == SendingStatus.READY
                },
                capture(pageableSlot),
            )
        } answers {
            PageImpl(
                listOf(
                    relaxedMock {
                        every { caregivingRoundId } returns "01GZX0EK7VNMWPR6ZR6545NJJQ"
                        every { receptionId } returns "01GZX0E40PKS0CNGTN4RRABRRP"
                        every { caregivingRoundNumber } returns 1
                        every { startDateTime } returns LocalDateTime.of(2023, 5, 10, 14, 44, 22)
                        every { sendingStatus } returns SendingStatus.READY
                        every { sentDate } returns null
                    },
                    relaxedMock {
                        every { caregivingRoundId } returns "01GZZHYDKT057QTK8TTJDRTJZX"
                        every { receptionId } returns "01H0P61EJHKC73AK11MPPKC8N0"
                        every { caregivingRoundNumber } returns 2
                        every { startDateTime } returns LocalDateTime.of(2023, 5, 5, 14, 22, 11)
                        every { sendingStatus } returns SendingStatus.READY
                        every { sentDate } returns null
                    },
                ),
                pageableSlot.captured,
                2,
            )
        }

        every {
            caregivingProgressMessageSummariesSearchQueryHandler.searchCaregivingProgressMessageSummaries(
                match {
                    it.filter.date == LocalDate.of(2023, 5, 15) &&
                        it.filter.sendingStatus == SendingStatus.READY &&
                        it.searchCondition.searchingProperty == CaregivingProgressMessageSummariesSearchQuery.SearchingProperty.ACCIDENT_NUMBER &&
                        it.searchCondition.keyword == "2022"
                },
                capture(pageableSlot),
            )
        } answers {
            PageImpl(
                listOf(
                    relaxedMock {
                        every { caregivingRoundId } returns "01GZZHYDKT057QTK8TTJDRTJZX"
                        every { receptionId } returns "01H0P61EJHKC73AK11MPPKC8N0"
                        every { caregivingRoundNumber } returns 2
                        every { startDateTime } returns LocalDateTime.of(2023, 5, 5, 14, 22, 11)
                        every { sendingStatus } returns SendingStatus.READY
                        every { sentDate } returns null
                    },
                ),
                pageableSlot.captured,
                1,
            )
        }
    }

    afterEach { clearAllMocks() }

    context("간병 진행 안내 메시지 발송 (예정) 목록을 조회하면") {
        val request = get("/api/v1/caregiving-progress-message-statuses")
            .queryParam("page-number", "1")
            .queryParam("page-size", "30")
            .queryParam("date", "2023-05-15")

        val expectResponse = ResponseMatcher(mockMvc, request)

        should("상태 코드 200 Ok로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("페이로드에 간병 진행 안내 알림톡 발송 (예정) 목록을 포함하여 응답합니다.") {
            expectResponse(
                content().json(
                    """
                        {
                          "currentPageNumber": 1,
                          "lastPageNumber": 1,
                          "totalItemCount": 5,
                          "items": [
                            {
                              "caregivingRoundId": "01GZX0EK7VNMWPR6ZR6545NJJQ",
                              "receptionId": "01GZX0E40PKS0CNGTN4RRABRRP",
                              "sendingStatus": "READY",
                              "sentDate": null
                            },
                            {
                              "caregivingRoundId": "01GZZHYDKT057QTK8TTJDRTJZX",
                              "receptionId": "01H0P61EJHKC73AK11MPPKC8N0",
                              "sendingStatus": "READY",
                              "sentDate": null
                            },
                            {
                              "caregivingRoundId": "01H0P62303EBZDJH8VMJX83JAY",
                              "receptionId": "01H0P6138P2599ZASGVP9JBS6E",
                              "sendingStatus": "SENT",
                              "sentDate": "2023-05-15"
                            },
                            {
                              "caregivingRoundId": "01H0P62F5EA1XAEHQ6NR1G6387",
                              "receptionId": "01H0P60M0DTBTJ4HS2GV2RWNPS",
                              "sendingStatus": "FAILED",
                              "sentDate": "2023-05-15"
                            },
                            {
                              "caregivingRoundId": "01H0P630YA1VWKK1MHCRP0MVST",
                              "receptionId": "01H0P5ZW56Z306G9PV0YHFPJ3Y",
                              "sendingStatus": "FAILED",
                              "sentDate": null
                            }
                          ]
                        }
                    """.trimIndent()
                )
            )
        }

        should("간병 진행 안내 알림톡 발송 (예정) 목록 조회를 도메인 영역으로 위임합니다.") {
            mockMvc.perform(request)

            verify {
                caregivingProgressMessageSummariesByFilterQueryHandler.getCaregivingProgressMessageSummaries(
                    withArg {
                        it.filter.date shouldBe LocalDate.of(2023, 5, 15)
                    },
                    withArg {
                        it.pageSize shouldBe 30
                        it.pageNumber shouldBe 0
                    }
                )
            }
        }
    }

    context("간병 진행 메시지 발송 (예정) 목록을 발송 상태로 필터링하여 조회하면") {
        val request = get("/api/v1/caregiving-progress-message-statuses")
            .queryParam("page-number", "1")
            .queryParam("page-size", "30")
            .queryParam("date", "2023-05-15")
            .queryParam("sending-status", "READY")

        val expectResponse = ResponseMatcher(mockMvc, request)

        should("상태 코드 200 Ok로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("간병 진행 안내 알림톡 발송 (예정) 목록 조회를 도메인 영역으로 위임합니다.") {
            mockMvc.perform(request)

            verify {
                caregivingProgressMessageSummariesByFilterQueryHandler.getCaregivingProgressMessageSummaries(
                    withArg {
                        it.filter.date shouldBe LocalDate.of(2023, 5, 15)
                        it.filter.sendingStatus shouldBe SendingStatus.READY
                    },
                    withArg {
                        it.pageSize shouldBe 30
                        it.pageNumber shouldBe 0
                    }
                )
            }
        }
    }

    context("간병 진행 메시지 발송 (예정) 목록을 발송 상태로 필터링하고 검색 조건을 적용하여 검색하면") {
        val request = get("/api/v1/caregiving-progress-message-statuses")
            .queryParam("page-number", "1")
            .queryParam("page-size", "30")
            .queryParam("date", "2023-05-15")
            .queryParam("sending-status", "READY")
            .queryParam("query", "accidentNumber:2022")

        val expectResponse = ResponseMatcher(mockMvc, request)

        should("상태 코드 200 Ok로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("간병 진행 안내 알림톡 발송 (예정) 목록 조회를 도메인 영역으로 위임합니다.") {
            mockMvc.perform(request)

            verify {
                caregivingProgressMessageSummariesSearchQueryHandler.searchCaregivingProgressMessageSummaries(
                    withArg {
                        it.filter.date shouldBe LocalDate.of(2023, 5, 15)
                        it.filter.sendingStatus shouldBe SendingStatus.READY
                        it.searchCondition shouldBe SearchCondition(
                            searchingProperty = CaregivingProgressMessageSummariesSearchQuery.SearchingProperty.ACCIDENT_NUMBER,
                            keyword = "2022",
                        )
                    },
                    withArg {
                        it.pageSize shouldBe 30
                        it.pageNumber shouldBe 0
                    }
                )
            }
        }
    }
})
