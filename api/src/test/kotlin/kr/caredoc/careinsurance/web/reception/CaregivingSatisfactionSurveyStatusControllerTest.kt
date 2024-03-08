package kr.caredoc.careinsurance.web.reception

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.reception.caregivingsatisfactionsurvey.CaregivingSatisfactionSurveyStatusSearchQuery
import kr.caredoc.careinsurance.reception.caregivingsatisfactionsurvey.CaregivingSatisfactionSurveyStatusSearchQueryHandler
import kr.caredoc.careinsurance.reception.caregivingsatisfactionsurvey.CaregivingSatisfactionSurveyStatusesByFilterQueryHandler
import kr.caredoc.careinsurance.reception.caregivingsatisfactionsurvey.ReservationStatus
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.search.SearchCondition
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@CareInsuranceWebMvcTest(CaregivingSatisfactionSurveyStatusController::class)
class CaregivingSatisfactionSurveyStatusControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean
    private val caregivingSatisfactionSurveyStatusesByFilterQueryHandler: CaregivingSatisfactionSurveyStatusesByFilterQueryHandler,
    @MockkBean
    private val caregivingSatisfactionSurveyStatusSearchQueryHandler: CaregivingSatisfactionSurveyStatusSearchQueryHandler,
) : ShouldSpec({
    context("간병 만족도 조사 상태 목록을 조회하면") {
        val request = get("/api/v1/caregiving-satisfaction-survey-statuses")
            .queryParam("page-number", "1")
            .queryParam("page-size", "10")
            .queryParam("date", "2023-01-03")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                caregivingSatisfactionSurveyStatusesByFilterQueryHandler.getCaregivingSatisfactionSurveyStatuses(
                    match { it.filter.date == LocalDate.of(2023, 1, 3) },
                    match { it.pageNumber == 0 && it.pageSize == 10 }
                )
            } returns PageImpl(
                listOf(
                    relaxedMock {
                        every { receptionId } returns "01GZGFTQTNW16H89AX42X09959"
                        every { caregivingRoundId } returns "01GZGFVG6ZQSRKVZTYG04AZ88D"
                        every { reservationStatus } returns ReservationStatus.READY
                    },
                    relaxedMock {
                        every { receptionId } returns "01GZGGR522TR0GSKH1HBW25GCR"
                        every { caregivingRoundId } returns "01GZGGR524BJWA5DMNV1H6QW8J"
                        every { reservationStatus } returns ReservationStatus.RESERVED
                    },
                    relaxedMock {
                        every { receptionId } returns "01GZGGR525R36YJ3S3XN4VJFJV"
                        every { caregivingRoundId } returns "01GZGGR525DNB1E3H9FBYXH40X"
                        every { reservationStatus } returns ReservationStatus.FAILED
                    },
                ),
                PageRequest.of(0, 10),
                3,
            )
        }

        afterEach { clearAllMocks() }

        should("200 Ok로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("간병 만족도 조사 상태를 페이로드에 포함하여 응답합니다.") {
            expectResponse(
                content().json(
                    """
                        {
                          "currentPageNumber": 1,
                          "lastPageNumber": 1,
                          "totalItemCount": 3,
                          "items": [
                            {
                              "receptionId": "01GZGFTQTNW16H89AX42X09959",
                              "lastCaregivingRoundId": "01GZGFVG6ZQSRKVZTYG04AZ88D",
                              "reservationStatus": "READY"
                            },
                            {
                              "receptionId": "01GZGGR522TR0GSKH1HBW25GCR",
                              "lastCaregivingRoundId": "01GZGGR524BJWA5DMNV1H6QW8J",
                              "reservationStatus": "RESERVED"
                            },
                            {
                              "receptionId": "01GZGGR525R36YJ3S3XN4VJFJV",
                              "lastCaregivingRoundId": "01GZGGR525DNB1E3H9FBYXH40X",
                              "reservationStatus": "FAILED"
                            }
                          ]
                        }
                    """.trimIndent()
                )
            )
        }

        should("간병 만족도 조사 상태 목록 조회를 도메인 영역에 위임합니다.") {
            mockMvc.perform(request)

            verify {
                caregivingSatisfactionSurveyStatusesByFilterQueryHandler.getCaregivingSatisfactionSurveyStatuses(
                    withArg {
                        it.filter.date shouldBe LocalDate.of(2023, 1, 3)
                    },
                    withArg {
                        it.pageNumber shouldBe 0
                        it.pageSize shouldBe 10
                    }
                )
            }
        }
    }

    context("간병 만족도 조사 상태 목록을 검색하면") {
        val request = get("/api/v1/caregiving-satisfaction-survey-statuses")
            .queryParam("page-number", "1")
            .queryParam("page-size", "10")
            .queryParam("date", "2023-01-03")
            .queryParam("query", "patientName:홍길동")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                caregivingSatisfactionSurveyStatusSearchQueryHandler.searchCaregivingSatisfactionSurveyStatus(
                    match {
                        listOf(
                            it.filter.date == LocalDate.of(2023, 1, 3),
                            it.searchCondition == SearchCondition(
                                CaregivingSatisfactionSurveyStatusSearchQuery.SearchingProperty.PATIENT_NAME,
                                "홍길동",
                            )
                        ).all { predicate -> predicate }
                    },
                    match { it.pageNumber == 0 && it.pageSize == 10 }
                )
            } returns PageImpl(
                listOf(
                    relaxedMock {
                        every { receptionId } returns "01GZGFTQTNW16H89AX42X09959"
                        every { caregivingRoundId } returns "01GZGFVG6ZQSRKVZTYG04AZ88D"
                        every { reservationStatus } returns ReservationStatus.READY
                    },
                    relaxedMock {
                        every { receptionId } returns "01GZGGR522TR0GSKH1HBW25GCR"
                        every { caregivingRoundId } returns "01GZGGR524BJWA5DMNV1H6QW8J"
                        every { reservationStatus } returns ReservationStatus.RESERVED
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

        should("간병 만족도 조사 상태를 페이로드에 포함하여 응답합니다.") {
            expectResponse(
                content().json(
                    """
                        {
                          "currentPageNumber": 1,
                          "lastPageNumber": 1,
                          "totalItemCount": 2,
                          "items": [
                            {
                              "receptionId": "01GZGFTQTNW16H89AX42X09959",
                              "lastCaregivingRoundId": "01GZGFVG6ZQSRKVZTYG04AZ88D",
                              "reservationStatus": "READY"
                            },
                            {
                              "receptionId": "01GZGGR522TR0GSKH1HBW25GCR",
                              "lastCaregivingRoundId": "01GZGGR524BJWA5DMNV1H6QW8J",
                              "reservationStatus": "RESERVED"
                            }
                          ]
                        }
                    """.trimIndent()
                )
            )
        }

        should("간병 만족도 조사 상태 검색을 도메인 영역에 위임합니다.") {
            mockMvc.perform(request)

            verify {
                caregivingSatisfactionSurveyStatusSearchQueryHandler.searchCaregivingSatisfactionSurveyStatus(
                    withArg {
                        it.filter.date shouldBe LocalDate.of(2023, 1, 3)
                        it.searchCondition shouldBe SearchCondition(
                            CaregivingSatisfactionSurveyStatusSearchQuery.SearchingProperty.PATIENT_NAME,
                            "홍길동",
                        )
                    },
                    withArg {
                        it.pageNumber shouldBe 0
                        it.pageSize shouldBe 10
                    }
                )
            }
        }
    }
})
