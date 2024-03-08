package kr.caredoc.careinsurance.web.reception

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.reception.history.ReceptionModificationHistoriesByReceptionIdQueryHandler
import kr.caredoc.careinsurance.reception.history.ReceptionModificationHistory
import kr.caredoc.careinsurance.relaxedMock
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@CareInsuranceWebMvcTest(ReceptionModificationHistoryController::class)
class ReceptionModificationHistoryControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean(relaxed = true)
    private val receptionModificationHistoriesByReceptionIdQueryHandler: ReceptionModificationHistoriesByReceptionIdQueryHandler
) : ShouldSpec({
    context("접수 수정 이력을 조회하면") {
        val request =
            MockMvcRequestBuilders.get("/api/v1/receptions/01GYYE2VV1Y70S77NR8DP5XPQE/reception-modification-history")
                .queryParam("page-number", "1")
                .queryParam("page-size", "10")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                receptionModificationHistoriesByReceptionIdQueryHandler.getReceptionModificationHistories(
                    match {
                        it.receptionId == "01GYYE2VV1Y70S77NR8DP5XPQE"
                    },
                    any(),
                )
            } returns PageImpl(
                listOf(
                    relaxedMock {
                        every { modifiedProperty } returns ReceptionModificationHistory.ModificationProperty.INSURANCE_NUMBER
                        every { previous } returns "12345-12345"
                        every { modified } returns "12345-12346"
                        every { modifierId } returns "01GYS7MER33TDH6PJFQC7XYA90"
                        every { modifiedDateTime } returns LocalDateTime.of(2023, 4, 26, 16, 12, 32)
                    },
                    relaxedMock {
                        every { modifiedProperty } returns ReceptionModificationHistory.ModificationProperty.ACCIDENT_DATE_TIME
                        every { previous } returns "2023-04-12T19:03:12"
                        every { modified } returns "2023-04-12T19:13:12"
                        every { modifierId } returns "01GYS7MER33TDH6PJFQC7XYA90"
                        every { modifiedDateTime } returns LocalDateTime.of(2023, 4, 26, 16, 12, 32)
                    },
                    relaxedMock {
                        every { modifiedProperty } returns ReceptionModificationHistory.ModificationProperty.CAREGIVING_LIMIT_PERIOD
                        every { previous } returns "2"
                        every { modified } returns "180"
                        every { modifierId } returns "01GYS7MER33TDH6PJFQC7XYA90"
                        every { modifiedDateTime } returns LocalDateTime.of(2023, 4, 26, 16, 12, 32)
                    },
                    relaxedMock {
                        every { modifiedProperty } returns ReceptionModificationHistory.ModificationProperty.PATIENT_HEIGHT
                        every { previous } returns null
                        every { modified } returns "185"
                        every { modifierId } returns "01GYS7MER33TDH6PJFQC7XYA90"
                        every { modifiedDateTime } returns LocalDateTime.of(2023, 4, 26, 16, 12, 32)
                    },
                    relaxedMock {
                        every { modifiedProperty } returns ReceptionModificationHistory.ModificationProperty.EXPECTED_CAREGIVING_START_DATE
                        every { previous } returns "2023-04-23"
                        every { modified } returns "2023-04-29"
                        every { modifierId } returns "01GYS7MER33TDH6PJFQC7XYA90"
                        every { modifiedDateTime } returns LocalDateTime.of(2023, 4, 26, 16, 12, 32)
                    },
                    relaxedMock {
                        every { modifiedProperty } returns ReceptionModificationHistory.ModificationProperty.NOTIFY_CAREGIVING_PROGRESS
                        every { previous } returns "true"
                        every { modified } returns "false"
                        every { modifierId } returns "01GYS7MER33TDH6PJFQC7XYA90"
                        every { modifiedDateTime } returns LocalDateTime.of(2023, 4, 26, 16, 12, 32)
                    },
                    relaxedMock {
                        every { modifiedProperty } returns ReceptionModificationHistory.ModificationProperty.RECEPTION_APPLICATION_FILE_NAME
                        every { previous } returns "[메리츠] SHENSHUNFU_간병인신청서.pdf"
                        every { modified } returns "여행경비정산표.pdf"
                        every { modifierId } returns "01GYS7MER33TDH6PJFQC7XYA90"
                        every { modifiedDateTime } returns LocalDateTime.of(2023, 4, 26, 16, 12, 32)
                    },
                ),
                PageRequest.of(0, 10),
                7,
            )
        }

        afterEach { clearAllMocks() }

        should("접수 수정 이력을 페이로드에 포함하여 응답합니다.") {
            expectResponse(
                content().json(
                    """
                        {
                          "currentPageNumber": 1,
                          "lastPageNumber": 1,
                          "totalItemCount": 7,
                          "items": [
                            {
                              "modifiedProperty": "INSURANCE_NUMBER",
                              "previous": "12345-12345",
                              "modified": "12345-12346",
                              "modifierId": "01GYS7MER33TDH6PJFQC7XYA90",
                              "modifiedDateTime": "2023-04-26T07:12:32Z"
                            },
                            {
                               "modifiedProperty": "ACCIDENT_DATE_TIME",
                               "previous": "2023-04-12T10:03:12Z",
                               "modified": "2023-04-12T10:13:12Z",
                               "modifierId": "01GYS7MER33TDH6PJFQC7XYA90",
                               "modifiedDateTime": "2023-04-26T07:12:32Z"
                             },
                             {
                               "modifiedProperty": "CAREGIVING_LIMIT_PERIOD",
                               "previous": 2,
                               "modified": 180,
                               "modifierId": "01GYS7MER33TDH6PJFQC7XYA90",
                               "modifiedDateTime": "2023-04-26T07:12:32Z"
                             },
                             {
                               "modifiedProperty": "PATIENT_HEIGHT",
                               "previous": null, 
                               "modified": 185,
                               "modifierId": "01GYS7MER33TDH6PJFQC7XYA90",
                               "modifiedDateTime": "2023-04-26T07:12:32Z"
                             },
                             {
                               "modifiedProperty": "EXPECTED_CAREGIVING_START_DATE",
                               "previous": "2023-04-23",
                               "modified": "2023-04-29",
                               "modifierId": "01GYS7MER33TDH6PJFQC7XYA90",
                               "modifiedDateTime": "2023-04-26T07:12:32Z"
                             },
                             {
                               "modifiedProperty": "NOTIFY_CAREGIVING_PROGRESS",
                               "previous": true,
                               "modified": false,
                               "modifierId": "01GYS7MER33TDH6PJFQC7XYA90",
                               "modifiedDateTime": "2023-04-26T07:12:32Z"
                             },
                             {
                               "modifiedProperty": "RECEPTION_APPLICATION_FILE_NAME",
                               "previous": "[메리츠] SHENSHUNFU_간병인신청서.pdf",
                               "modified": "여행경비정산표.pdf",
                               "modifierId": "01GYS7MER33TDH6PJFQC7XYA90",
                               "modifiedDateTime": "2023-04-26T07:12:32Z"
                             }
                          ]
                        }
                    """.trimIndent()
                )
            )
        }

        should("200 ok로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("접수 수정 내역 조회를 도메인 영역에 위임합니다.") {
            mockMvc.perform(request)

            verify {
                receptionModificationHistoriesByReceptionIdQueryHandler.getReceptionModificationHistories(
                    withArg {
                        it.receptionId shouldBe "01GYYE2VV1Y70S77NR8DP5XPQE"
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
