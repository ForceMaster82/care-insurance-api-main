package kr.caredoc.careinsurance.web.reception

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.reception.statistics.DailyReceptionStatisticsByDateRangeQueryHandler
import kr.caredoc.careinsurance.relaxedMock
import org.hamcrest.core.StringEndsWith
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@CareInsuranceWebMvcTest(ReceptionStatisticsController::class)
class ReceptionStatisticsControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean
    private val dailyReceptionStatisticsByDateRangeQueryHandler: DailyReceptionStatisticsByDateRangeQueryHandler,
) : ShouldSpec({
    context("일자별 접수 통계를 조회하면") {
        val request = get("/api/v1/daily-reception-statistics")
            .queryParam("from", "2022-11-01")
            .queryParam("until", "2022-11-30")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                dailyReceptionStatisticsByDateRangeQueryHandler.getDailyReceptionStatistics(
                    match {
                        it.from == LocalDate.of(2022, 11, 1) &&
                            it.until == LocalDate.of(2022, 11, 30)
                    }
                )
            } returns listOf(
                relaxedMock {
                    every { receivedDate } returns LocalDate.of(2022, 11, 1)
                    every { receptionCount } returns 21
                    every { canceledReceptionCount } returns 2
                    every { canceledByPersonalCaregiverReceptionCount } returns 1
                    every { canceledByMedicalRequestReceptionCount } returns 0
                    every { requestedBillingCount } returns 79
                    every { requestedBillingAmount } returns 44893000
                    every { depositCount } returns 74
                    every { depositAmount } returns 42809000
                    every { withdrawalCount } returns 0
                    every { withdrawalAmount } returns 0
                    every { sameDayAssignmentReceptionCount } returns 6
                    every { startedSameDayAssignmentReceptionCount } returns 5
                    every { shortTermReceptionCount } returns 2
                    every { startedShortTermReceptionCount } returns 2
                },
                relaxedMock {
                    every { receivedDate } returns LocalDate.of(2022, 11, 2)
                    every { receptionCount } returns 19
                    every { canceledReceptionCount } returns 1
                    every { canceledByPersonalCaregiverReceptionCount } returns 2
                    every { canceledByMedicalRequestReceptionCount } returns 0
                    every { requestedBillingCount } returns 22
                    every { requestedBillingAmount } returns 13108000
                    every { depositCount } returns 19
                    every { depositAmount } returns 12333000
                    every { withdrawalCount } returns 0
                    every { withdrawalAmount } returns 0
                    every { sameDayAssignmentReceptionCount } returns 7
                    every { startedSameDayAssignmentReceptionCount } returns 5
                    every { shortTermReceptionCount } returns 2
                    every { startedShortTermReceptionCount } returns 2
                },
            )
        }

        afterEach { clearAllMocks() }

        should("상태 코드 200 Ok 로 응답한다.") {
            expectResponse(status().isOk)
        }

        should("일자별 접수 통계를 페이로드에 포함하여 응답한다.") {
            expectResponse(
                content().json(
                    """
                        [
                          {
                            "receivedDate": "2022-11-01",
                            "receptionCount": 21,
                            "canceledReceptionCount": 2,
                            "canceledReceptionCountsByReason": {
                              "CANCELED_BY_PERSONAL_CAREGIVER": 1,
                              "CANCELED_BY_MEDICAL_REQUEST": 0
                            },
                            "requestedBillingCount": 79,
                            "requestedBillingAmount": 44893000,
                            "depositCount": 74,
                            "depositAmount": 42809000,
                            "withdrawalCount": 0,
                            "withdrawalAmount": 0,
                            "sameDayAssignmentReceptionCount": 6,
                            "startedSameDayAssignmentReceptionCount": 5,
                            "shortTermReceptionCount": 2,
                            "startedShortTermReceptionCount": 2
                          },
                          {
                            "receivedDate": "2022-11-02",
                            "receptionCount": 19,
                            "canceledReceptionCount": 1,
                            "canceledReceptionCountsByReason": {
                              "CANCELED_BY_PERSONAL_CAREGIVER": 2,
                              "CANCELED_BY_MEDICAL_REQUEST": 0
                            },
                            "requestedBillingCount": 22,
                            "requestedBillingAmount": 13108000,
                            "depositCount": 19,
                            "depositAmount": 12333000,
                            "withdrawalCount": 0,
                            "withdrawalAmount": 0,
                            "sameDayAssignmentReceptionCount": 7,
                            "startedSameDayAssignmentReceptionCount": 5,
                            "shortTermReceptionCount": 2,
                            "startedShortTermReceptionCount": 2
                          }
                        ]
                    """.trimIndent()
                )
            )
        }

        should("접수 통계 조회를 도메인 영역에 위임한다.") {
            mockMvc.perform(request)

            verify {
                dailyReceptionStatisticsByDateRangeQueryHandler.getDailyReceptionStatistics(
                    withArg {
                        it.from shouldBe LocalDate.of(2022, 11, 1)
                        it.until shouldBe LocalDate.of(2022, 11, 30)
                    }
                )
            }
        }
    }

    context("일자별 접수 통계를 CSV 형식으로 조회하면") {
        val request = get("/api/v1/daily-reception-statistics")
            .queryParam("from", "2022-11-01")
            .queryParam("until", "2022-11-30")
            .header(HttpHeaders.ACCEPT, "text/csv")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                dailyReceptionStatisticsByDateRangeQueryHandler.getDailyReceptionStatisticsAsCsv(
                    match {
                        it.from == LocalDate.of(2022, 11, 1) &&
                            it.until == LocalDate.of(2022, 11, 30)
                    }
                )
            } returns """
              구분,요일,전체 배정 건,간병인 파견 건,개인구인 건,석션 등 의료행위,취소 건,지급 요청 건,지급요청금액,지급받은 건,지급받은 금액,환수 건,환수금액,민원 건,당일배정요청 건,배정 건,단기 요청 건,배정 건
              2022-11-01,화,21,18,1,0,2,79,44893000,74,42809000,0,0,0,6,5,2,2
              2022-11-02,수,19,16,2,0,1,22,13108000,19,12333000,0,0,0,7,6,2,2
            """.trimIndent()
        }

        afterEach { clearAllMocks() }

        should("상태 코드 200 Ok 로 응답한다.") {
            expectResponse(status().isOk)
        }

        should("일자별 접수 통계를 페이로드에 포함하여 응답한다.") {
            expectResponse(
                content().string(
                    StringEndsWith.endsWith(
                        """
                          구분,요일,전체 배정 건,간병인 파견 건,개인구인 건,석션 등 의료행위,취소 건,지급 요청 건,지급요청금액,지급받은 건,지급받은 금액,환수 건,환수금액,민원 건,당일배정요청 건,배정 건,단기 요청 건,배정 건
                          2022-11-01,화,21,18,1,0,2,79,44893000,74,42809000,0,0,0,6,5,2,2
                          2022-11-02,수,19,16,2,0,1,22,13108000,19,12333000,0,0,0,7,6,2,2
                        """.trimIndent()
                    )
                )
            )
        }

        should("접수 통계 조회를 도메인 영역에 위임한다.") {
            mockMvc.perform(request)

            verify {
                dailyReceptionStatisticsByDateRangeQueryHandler.getDailyReceptionStatisticsAsCsv(
                    withArg {
                        it.from shouldBe LocalDate.of(2022, 11, 1)
                        it.until shouldBe LocalDate.of(2022, 11, 30)
                    }
                )
            }
        }
    }
})
