package kr.caredoc.careinsurance.web.caregiving

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.mockk.clearAllMocks
import io.mockk.every
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import kr.caredoc.careinsurance.caregiving.CaregivingProgressingStatus
import kr.caredoc.careinsurance.caregiving.CaregivingRoundsByReceptionIdQueryHandler
import kr.caredoc.careinsurance.patient.Sex
import kr.caredoc.careinsurance.reception.OrganizationType
import kr.caredoc.careinsurance.reception.ReceptionProgressingStatus
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQueryHandler
import kr.caredoc.careinsurance.reception.exception.ReceptionNotFoundByIdException
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.settlement.SettlementProgressingStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@CareInsuranceWebMvcTest(ReceptionCaregivingRoundController::class)
class ReceptionCaregivingRoundControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean
    private val caregivingRoundsByReceptionIdQueryHandler: CaregivingRoundsByReceptionIdQueryHandler,
    @MockkBean
    private val receptionsByIdsQueryHandler: ReceptionsByIdsQueryHandler,
) : ShouldSpec({
    context("접수의 간병 목록을 요청하면") {
        val targetReceptionId = "01GSC4SKPGWMZDP7EKWQ0Z57NG"
        val request = get("/api/v1/receptions/$targetReceptionId/caregiving-rounds")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                caregivingRoundsByReceptionIdQueryHandler.getReceptionCaregivingRounds(
                    match {
                        it.receptionId == "01GSC4SKPGWMZDP7EKWQ0Z57NG"
                    }
                )
            } returns listOf(
                relaxedMock {
                    every { id } returns "01GSC7CMD4753B5EP4A889Q5HZ"
                    every { caregivingRoundNumber } returns 1
                    every { startDateTime } returns null
                    every { endDateTime } returns null
                    every { caregivingRoundClosingReasonType } returns null
                    every { caregivingRoundClosingReasonDetail } returns null
                    every { cancelDateTime } returns null
                    every { caregivingProgressingStatus } returns CaregivingProgressingStatus.NOT_STARTED
                    every { settlementProgressingStatus } returns SettlementProgressingStatus.NOT_STARTED
                    every { billingProgressingStatus } returns BillingProgressingStatus.NOT_STARTED
                    every { receptionInfo } returns relaxedMock {
                        every { receptionId } returns "01GSC4SKPGWMZDP7EKWQ0Z57NG"
                        every { insuranceNumber } returns "12345-12345"
                        every { accidentNumber } returns "2022-1234567"
                        every { expectedCaregivingStartDate } returns null
                        every { receptionProgressingStatus } returns ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                        every { caregivingManagerInfo } returns relaxedMock {
                            every { organizationType } returns OrganizationType.INTERNAL
                            every { organizationId } returns null
                            every { managingUserId } returns "01GSC7EWKR6T67KCN0SPE31F2N"
                        }
                        every { caregiverInfo } returns relaxedMock {
                            every { caregiverOrganizationId } returns null
                            every { name } returns "정만길"
                            every { sex } returns Sex.MALE
                            every { birthDate } returns LocalDate.of(1974, 1, 30)
                            every { phoneNumber } returns "01012341234"
                            every { insured } returns false
                            every { dailyCaregivingCharge } returns 0
                            every { commissionFee } returns 0
                            every { accountInfo } returns relaxedMock {
                                every { bank } returns null
                                every { accountNumber } returns null
                                every { accountHolder } returns null
                            }
                        }
                    }
                    every { remarks } returns "1회차 해보고 만족해서 계속한다고 함."
                },
            )

            every {
                receptionsByIdsQueryHandler.getReceptions(
                    match { it.receptionIds.contains("01GSC4SKPGWMZDP7EKWQ0Z57NG") }
                )
            } returns listOf(
                relaxedMock {
                    every { id } returns "01GSC4SKPGWMZDP7EKWQ0Z57NG"
                    every { patientInfo.name.masked } returns "김*자"
                    every { insuranceInfo.insuranceNumber } returns "12345-12345"
                    every { accidentInfo.accidentNumber } returns "2022-1234567"
                    every { expectedCaregivingStartDate } returns null
                    every { progressingStatus } returns ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                }
            )
        }

        afterEach { clearAllMocks() }

        should("응답 상태는 200 ok 일 것") {
            expectResponse(status().isOk)
        }

        should("응답 페이로드 결과는 접수의 간병 목록 포함 할 것") {
            expectResponse(
                MockMvcResultMatchers.content().json(
                    """
                      [
                        {
                          "id": "01GSC7CMD4753B5EP4A889Q5HZ",
                          "caregivingRoundNumber": 1,
                          "startDateTime": null,
                          "endDateTime": null,                          
                          "caregivingRoundClosingReasonType": null,
                          "caregivingRoundClosingReasonDetail": null,
                          "cancelDateTime": null,
                          "caregivingProgressingStatus": "NOT_STARTED",
                          "settlementProgressingStatus": "NOT_STARTED",
                          "billingProgressingStatus": "NOT_STARTED",
                          "caregiverInfo": {
                            "caregiverOrganizationId": null,
                            "name": "정만길",
                            "sex": "MALE",
                            "birthDate": "1974-01-30",
                            "insured": false,
                            "dailyCaregivingCharge": 0,
                            "commissionFee": 0,
                            "accountInfo": {
                              "bank": null,
                              "accountNumber": null,
                              "accountHolder": null
                            }
                          },
                          "receptionInfo": {
                            "receptionId": "01GSC4SKPGWMZDP7EKWQ0Z57NG",
                            "insuranceNumber": "12345-12345",
                            "accidentNumber": "2022-1234567",
                            "patientName": "김*자",
                            "expectedCaregivingStartDate": null,
                            "receptionProgressingStatus": "CAREGIVING_IN_PROGRESS",
                            "caregivingManagerInfo": {
                              "organizationType": "INTERNAL",
                              "organizationId": null,
                              "managingUserId": "01GSC7EWKR6T67KCN0SPE31F2N"
                            }
                          },
                          "remarks": "1회차 해보고 만족해서 계속한다고 함."
                        }
                      ]
                    """.trimIndent()
                )
            )
        }

        context("접수 정보가 존재하지 않을 경우") {
            beforeEach {
                every {
                    caregivingRoundsByReceptionIdQueryHandler.getReceptionCaregivingRounds(
                        match {
                            it.receptionId == "01GSC4SKPGWMZDP7EKWQ0Z57NG"
                        }
                    )
                } throws ReceptionNotFoundByIdException(receptionId = "01GSCPXAE83A5NJGEAF58MSYQK")
            }

            afterEach { clearAllMocks() }

            should("응답 상태는 404 Not Found 일 것 ") {
                expectResponse(status().isNotFound)
            }

            should("응답 페이로드 결과는 에러 메시지와 에러 타입 일 것") {
                expectResponse(
                    MockMvcResultMatchers.content().json(
                        """
                            {
                              "message": "조회하고자 하는 간병 접수가 존재하지 않습니다.",
                              "errorType": "RECEPTION_NOT_EXISTS"
                            }
                        """.trimIndent()
                    )
                )
            }

            should("응답 페이로드 결과는 입력한 접수 식별자 일 것") {
                expectResponse(
                    MockMvcResultMatchers.content().json(
                        """
                            {
                              "data": {
                                "enteredReceptionId": "01GSCPXAE83A5NJGEAF58MSYQK"
                              }
                            }
                        """.trimIndent()
                    )
                )
            }
        }
    }
})
