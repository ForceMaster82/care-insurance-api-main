package kr.caredoc.careinsurance.web.reception

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.reception.modification.CaregivingChargeModificationHistoriesByReceptionIdQueryHandler
import kr.caredoc.careinsurance.reception.modification.CaregivingChargeModificationHistory
import kr.caredoc.careinsurance.relaxedMock
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@CareInsuranceWebMvcTest(CaregivingChargeModificationHistoryController::class)
class CaregivingChargeModificationHistoryControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean
    private val caregivingChargeModificationHistoriesByReceptionIdQueryHandler: CaregivingChargeModificationHistoriesByReceptionIdQueryHandler,
) : ShouldSpec({
    context("간병비 산정 수정 내역을 조회하면") {
        val request = get("/api/v1/receptions/01GPJMK7ZPBBKTY3TP0NN5JWCJ/caregiving-charge-modification-history")
            .queryParam("page-number", "1")
            .queryParam("page-size", "10")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                caregivingChargeModificationHistoriesByReceptionIdQueryHandler.getCaregivingChargeModificationHistories(
                    match {
                        it.receptionId == "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                    },
                    any(),
                )
            } returns PageImpl(
                listOf(
                    relaxedMock {
                        every { caregivingRoundNumber } returns 4
                        every { modifiedProperty } returns CaregivingChargeModificationHistory.ModifiedProperty.IS_CANCEL_AFTER_ARRIVED
                        every { previous } returns "true"
                        every { modified } returns "false"
                        every { modifierId } returns "01GNXBZAQ0J9J8DD8K461RDF26"
                        every { modifiedDateTime } returns LocalDateTime.of(2023, 4, 27, 16, 12, 32)
                    },
                    relaxedMock {
                        every { caregivingRoundNumber } returns 3
                        every { modifiedProperty } returns CaregivingChargeModificationHistory.ModifiedProperty.COVID_19_TESTING_COST
                        every { previous } returns "30"
                        every { modified } returns "3000"
                        every { modifierId } returns "01GNXBZAQ0J9J8DD8K461RDF26"
                        every { modifiedDateTime } returns LocalDateTime.of(2023, 4, 26, 16, 12, 32)
                    },
                    relaxedMock {
                        every { caregivingRoundNumber } returns 2
                        every { modifiedProperty } returns CaregivingChargeModificationHistory.ModifiedProperty.MEAL_COST
                        every { previous } returns "5000"
                        every { modified } returns "10000"
                        every { modifierId } returns "01GNXBZAQ0J9J8DD8K461RDF26"
                        every { modifiedDateTime } returns LocalDateTime.of(2023, 4, 25, 16, 12, 32)
                    },
                    relaxedMock {
                        every { caregivingRoundNumber } returns 1
                        every { modifiedProperty } returns CaregivingChargeModificationHistory.ModifiedProperty.ADDITIONAL_CHARGE_1
                        every { previous } returns "(OOO비) 5,000"
                        every { modified } returns "(ㅁㅁㅁ비) 5,000"
                        every { modifierId } returns "01GNXBZAQ0J9J8DD8K461RDF26"
                        every { modifiedDateTime } returns LocalDateTime.of(2023, 4, 24, 16, 12, 32)
                    },
                ),
                PageRequest.of(0, 10),
                3,
            )
        }

        afterEach { clearAllMocks() }

        should("간병비 산정 수정 내역을 페이로드에 포함하여 응답합니다.") {
            expectResponse(
                content().json(
                    """
                        {
                          "currentPageNumber": 1,
                          "lastPageNumber": 1,
                          "totalItemCount": 4,
                          "items": [
                            {
                              "caregivingRoundNumber": 4,
                              "modifiedProperty": "IS_CANCEL_AFTER_ARRIVED",
                              "previous": true,
                              "modified": false,
                              "modifierId": "01GNXBZAQ0J9J8DD8K461RDF26",
                              "modifiedDateTime": "2023-04-27T07:12:32Z"
                            },
                            {
                              "caregivingRoundNumber": 3,
                              "modifiedProperty": "COVID_19_TESTING_COST",
                              "previous": 30,
                              "modified": 3000,
                              "modifierId": "01GNXBZAQ0J9J8DD8K461RDF26",
                              "modifiedDateTime": "2023-04-26T07:12:32Z"
                            },
                            {
                              "caregivingRoundNumber": 2,
                              "modifiedProperty": "MEAL_COST",
                              "previous": 5000,
                              "modified": 10000,
                              "modifierId": "01GNXBZAQ0J9J8DD8K461RDF26",
                              "modifiedDateTime": "2023-04-25T07:12:32Z"
                            },
                            {
                              "caregivingRoundNumber": 1,
                              "modifiedProperty": "ADDITIONAL_CHARGE_1",
                              "previous": "(OOO비) 5,000",
                              "modified": "(ㅁㅁㅁ비) 5,000",
                              "modifierId": "01GNXBZAQ0J9J8DD8K461RDF26",
                              "modifiedDateTime": "2023-04-24T07:12:32Z"
                            }
                          ]
                        }
                    """.trimIndent()
                )
            )
        }

        should("200 Ok로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("간병비 산정 수정 내역 조회를 도메인 영역에 위임합니다.") {
            mockMvc.perform(request)

            verify {
                caregivingChargeModificationHistoriesByReceptionIdQueryHandler.getCaregivingChargeModificationHistories(
                    withArg {
                        it.receptionId shouldBe "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
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
