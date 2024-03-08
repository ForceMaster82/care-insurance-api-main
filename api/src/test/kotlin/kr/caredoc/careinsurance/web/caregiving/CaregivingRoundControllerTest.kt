package kr.caredoc.careinsurance.web.caregiving

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.account.AccountInfo
import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import kr.caredoc.careinsurance.caregiving.CaregiverInfo
import kr.caredoc.careinsurance.caregiving.CaregivingCharge
import kr.caredoc.careinsurance.caregiving.CaregivingChargeByCaregivingRoundIdQueryHandler
import kr.caredoc.careinsurance.caregiving.CaregivingChargeConfirmStatus
import kr.caredoc.careinsurance.caregiving.CaregivingChargeEditingCommandHandler
import kr.caredoc.careinsurance.caregiving.CaregivingChargeNotEnteredException
import kr.caredoc.careinsurance.caregiving.CaregivingProgressingStatus
import kr.caredoc.careinsurance.caregiving.CaregivingRound
import kr.caredoc.careinsurance.caregiving.CaregivingRoundByIdQueryHandler
import kr.caredoc.careinsurance.caregiving.CaregivingRoundEditingCommandHandler
import kr.caredoc.careinsurance.caregiving.CaregivingRoundsByFilterQuery
import kr.caredoc.careinsurance.caregiving.CaregivingRoundsByFilterQueryHandler
import kr.caredoc.careinsurance.caregiving.exception.CaregivingRoundNotFoundByIdException
import kr.caredoc.careinsurance.patch.Patches
import kr.caredoc.careinsurance.patient.Sex
import kr.caredoc.careinsurance.reception.OrganizationType
import kr.caredoc.careinsurance.reception.ReceptionByIdQueryHandler
import kr.caredoc.careinsurance.reception.ReceptionProgressingStatus
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQueryHandler
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.settlement.SettlementProgressingStatus
import org.hamcrest.core.StringEndsWith
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime

@CareInsuranceWebMvcTest(CaregivingRoundController::class)
class CaregivingRoundControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean
    private val caregivingRoundsByFilterQueryHandler: CaregivingRoundsByFilterQueryHandler,
    @MockkBean
    private val caregivingRoundEditingCommandHandler: CaregivingRoundEditingCommandHandler,
    @MockkBean
    private val caregivingChargeEditingCommandHandler: CaregivingChargeEditingCommandHandler,
    @MockkBean
    private val caregivingChargeByCaregivingRoundIdQueryHandler: CaregivingChargeByCaregivingRoundIdQueryHandler,
    @MockkBean
    private val caregivingRoundByIdQueryHandler: CaregivingRoundByIdQueryHandler,
    @MockkBean
    private val receptionByIdQueryHandler: ReceptionByIdQueryHandler,
    @MockkBean
    private val receptionsByIdsQueryHandler: ReceptionsByIdsQueryHandler,
) : ShouldSpec({
    context("when getting caregiving rounds") {
        val request = get("/api/v1/caregiving-rounds")
            .queryParam("page-number", "1")
            .queryParam("page-size", "20")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                caregivingRoundsByFilterQueryHandler.getCaregivingRounds(
                    match {
                        listOf(
                            it.from == null,
                            it.until == null,
                        ).all { predicate -> predicate }
                    },
                    match {
                        it.pageNumber == 0 && it.pageSize == 20
                    }
                )
            } returns PageImpl(
                listOf<CaregivingRound>(
                    relaxedMock {
                        every { id } returns "01GRR1T9W6P1HBSYHWCRBBYW1N"
                        every { caregivingRoundNumber } returns 1
                        every { startDateTime } returns LocalDateTime.of(2023, 2, 1, 14, 0, 0)
                        every { endDateTime } returns null
                        every { caregivingProgressingStatus } returns CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
                        every { settlementProgressingStatus } returns SettlementProgressingStatus.NOT_STARTED
                        every { billingProgressingStatus } returns BillingProgressingStatus.NOT_STARTED
                        every { receptionInfo } returns relaxedMock {
                            every { receptionId } returns "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                            every { insuranceNumber } returns "11111-1111"
                            every { accidentNumber } returns "2022-1111111"
                            every { expectedCaregivingStartDate } returns null
                            every { receptionProgressingStatus } returns ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                            every { caregivingManagerInfo } returns relaxedMock {
                                every { organizationType } returns OrganizationType.ORGANIZATION
                                every { organizationId } returns "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                                every { managingUserId } returns "01GQ23MVTBAKS526S0WGS9CS0A"
                            }
                        }
                    },
                ),
                PageRequest.of(0, 20),
                1,
            )

            every { receptionsByIdsQueryHandler.getReceptions(match { it.receptionIds.contains("01GPJMK7ZPBBKTY3TP0NN5JWCJ") }) } returns listOf(
                relaxedMock {
                    every { id } returns "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                    every { patientInfo.name.masked } returns "김*자"
                }
            )
        }

        afterEach { clearAllMocks() }

        should("response status should be 200 ok") {
            expectResponse(status().isOk)
        }

        should("response payload should contains paging meta data") {
            expectResponse(
                content().json(
                    """
                        {
                          "currentPageNumber": 1,
                          "lastPageNumber": 1,
                          "totalItemCount": 1
                        }
                    """.trimIndent()
                )
            )
        }

        should("response payload should contains queried caregiving rounds") {
            expectResponse(
                content().json(
                    """
                        {
                          "items": [
                            {
                              "id": "01GRR1T9W6P1HBSYHWCRBBYW1N",
                              "caregivingRoundNumber": 1,
                              "startDateTime": "2023-02-01T05:00:00Z",
                              "endDateTime": null,
                              "caregivingProgressingStatus": "CAREGIVING_IN_PROGRESS",
                              "settlementProgressingStatus": "NOT_STARTED",
                              "billingProgressingStatus": "NOT_STARTED",
                              "receptionInfo": {
                                "receptionId": "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                                "insuranceNumber": "11111-1111",
                                "accidentNumber": "2022-1111111",
                                "patientName": "김*자",
                                "expectedCaregivingStartDate": null,
                                "receptionProgressingStatus": "CAREGIVING_IN_PROGRESS",
                                "caregivingManagerInfo": {
                                  "organizationType": "ORGANIZATION",
                                  "organizationId": "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                                  "managingUserId": "01GQ23MVTBAKS526S0WGS9CS0A"
                                }
                              }
                            }
                          ]
                        }
                    """.trimIndent()
                )
            )
        }
    }

    context("시작일 범위를 지정하여 간병 회차를 조회하면") {
        val request = get("/api/v1/caregiving-rounds")
            .queryParam("page-number", "1")
            .queryParam("page-size", "20")
            .queryParam("from", "2023-02-01")
            .queryParam("until", "2023-02-06")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                caregivingRoundsByFilterQueryHandler.getCaregivingRounds(
                    match {
                        listOf(
                            it.from == LocalDate.of(2023, 2, 1),
                            it.until == LocalDate.of(2023, 2, 6),
                        ).all { predicate -> predicate }
                    },
                    match {
                        it.pageNumber == 0 && it.pageSize == 20
                    }
                )
            } returns PageImpl(
                listOf<CaregivingRound>(
                    relaxedMock {
                        every { id } returns "01GRR1T9W6P1HBSYHWCRBBYW1N"
                        every { caregivingRoundNumber } returns 1
                        every { startDateTime } returns LocalDateTime.of(2023, 2, 1, 14, 0, 0)
                        every { endDateTime } returns null
                        every { caregivingProgressingStatus } returns CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
                        every { settlementProgressingStatus } returns SettlementProgressingStatus.NOT_STARTED
                        every { billingProgressingStatus } returns BillingProgressingStatus.NOT_STARTED
                        every { receptionInfo } returns relaxedMock {
                            every { receptionId } returns "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                            every { insuranceNumber } returns "11111-1111"
                            every { accidentNumber } returns "2022-1111111"
                            every { expectedCaregivingStartDate } returns null
                            every { receptionProgressingStatus } returns ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                            every { caregivingManagerInfo } returns relaxedMock {
                                every { organizationType } returns OrganizationType.ORGANIZATION
                                every { organizationId } returns "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                                every { managingUserId } returns "01GQ23MVTBAKS526S0WGS9CS0A"
                            }
                        }
                    },
                ),
                PageRequest.of(0, 20),
                1,
            )

            every { receptionsByIdsQueryHandler.getReceptions(match { it.receptionIds.contains("01GPJMK7ZPBBKTY3TP0NN5JWCJ") }) } returns listOf(
                relaxedMock {
                    every { id } returns "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                    every { patientInfo.name.masked } returns "김*자"
                }
            )
        }

        afterEach { clearAllMocks() }

        should("상태코드 200 Ok로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("기간을 필터링하여 간병 회차 목록을 조회합니다.") {
            mockMvc.perform(request)

            verify {
                caregivingRoundsByFilterQueryHandler.getCaregivingRounds(
                    withArg {
                        it.from shouldBe LocalDate.of(2023, 2, 1)
                        it.until shouldBe LocalDate.of(2023, 2, 6)
                    },
                    any()
                )
            }
        }
    }

    context("when getting caregiving rounds with filters and search") {
        val request = get("/api/v1/caregiving-rounds")
            .queryParam("page-number", "1")
            .queryParam("page-size", "20")
            .queryParam("from", "2023-02-01")
            .queryParam("until", "2023-02-06")
            .queryParam("expected-caregiving-start-date", "2023-02-01")
            .queryParam("reception-progressing-status", "CAREGIVING_IN_PROGRESS")
            .queryParam(
                "caregiving-progressing-status",
                "NOT_STARTED", "REMATCHING", "PENDING_REMATCHING"
            )
            .queryParam("settlement-progressing-status", "NOT_STARTED")
            .queryParam("billing-progressing-status", "NOT_STARTED")
            .queryParam("query", "accidentNumber:2022-1111111")

        beforeEach {
            every {
                caregivingRoundsByFilterQueryHandler.getCaregivingRounds(any(), any())
            } returns PageImpl(
                listOf<CaregivingRound>(),
                PageRequest.of(0, 20),
                0,
            )
        }

        afterEach { clearAllMocks() }

        should("query caregiving rounds using query parameters") {
            mockMvc.perform(request)

            verify {
                caregivingRoundsByFilterQueryHandler.getCaregivingRounds(
                    withArg {
                        it.expectedCaregivingStartDate shouldBe LocalDate.of(2023, 2, 1)
                        it.receptionProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                            ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                        )
                        it.caregivingProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                            CaregivingProgressingStatus.NOT_STARTED,
                            CaregivingProgressingStatus.REMATCHING,
                            CaregivingProgressingStatus.PENDING_REMATCHING
                        )
                        it.settlementProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                            SettlementProgressingStatus.NOT_STARTED
                        )
                        it.billingProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                            BillingProgressingStatus.NOT_STARTED
                        )
                        it.searchCondition!!.searchingProperty shouldBe CaregivingRoundsByFilterQuery.SearchingProperty.ACCIDENT_NUMBER
                        it.searchCondition!!.keyword shouldBe "2022-1111111"
                    },
                    any()
                )
            }
        }
    }

    context("간병 예상일자를 기준으로 간병 회차 목록을 CSV 형식으로 조회하면") {
        val request = get("/api/v1/caregiving-rounds")
            .header(HttpHeaders.ACCEPT, "text/csv")
            .queryParam("from", "2023-07-01")
            .queryParam("until", "2023-09-01")
            .queryParam("expected-caregiving-start-date", "2023-08-01")
            .queryParam("reception-progressing-status", "CAREGIVING_IN_PROGRESS")
            .queryParam("caregiving-progressing-status", "CAREGIVING_IN_PROGRESS")
            .queryParam("settlement-progressing-status", "NOT_STARTED")
            .queryParam("billing-progressing-status", "NOT_STARTED")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            mockkObject(Clock)
            every { Clock.today() } returns LocalDate.of(2023, 10, 10)

            every {
                caregivingRoundsByFilterQueryHandler.getCaregivingRoundsAsCsv(
                    match {
                        listOf(
                            it.expectedCaregivingStartDate == LocalDate.of(2023, 8, 1),
                            it.receptionProgressingStatuses == setOf(
                                ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                            ),
                            it.caregivingProgressingStatuses == setOf(
                                CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
                            ),
                            it.settlementProgressingStatuses == setOf(
                                SettlementProgressingStatus.NOT_STARTED
                            ),
                            it.billingProgressingStatuses == setOf(
                                BillingProgressingStatus.NOT_STARTED
                            ),
                        ).all { predicate -> predicate }
                    },
                )
            } returns """
                간병 예상일자,병실정보,환자명,간병인명,배정담당자 소속
                2023-08-01,대구 영남대병원,김환자,우간병,대구 엄마손
                2023-08-01,케어닥 병원 304호실 (서울특별시 강남구),방환자,정간병,케어닥
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
                MockMvcResultMatchers.header().string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"%5B%EA%B0%84%EB%B3%91%EA%B4%80%EB%A6%AC%5D20230801.csv\""
                )
            )
        }

        should("병실정보 확인을 위한 CSV 데이터를 페이로드에 포함하여 응답한다.") {
            expectResponse(
                content().string(
                    StringEndsWith(
                        """
                            간병 예상일자,병실정보,환자명,간병인명,배정담당자 소속
                            2023-08-01,대구 영남대병원,김환자,우간병,대구 엄마손
                            2023-08-01,케어닥 병원 304호실 (서울특별시 강남구),방환자,정간병,케어닥
                        """.trimIndent()
                    )
                )
            )
        }

        should("간병 회차 목록의 CSV 추출을 도메인 영역에 위임한다.") {
            mockMvc.perform(request)

            verify {
                caregivingRoundsByFilterQueryHandler.getCaregivingRoundsAsCsv(
                    withArg {
                        it.expectedCaregivingStartDate shouldBe LocalDate.of(2023, 8, 1)
                        it.receptionProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                            ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                        )
                        it.caregivingProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                            CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
                        )
                        it.settlementProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                            SettlementProgressingStatus.NOT_STARTED
                        )
                        it.billingProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                            BillingProgressingStatus.NOT_STARTED
                        )
                    }
                )
            }
        }
    }

    context("when searching caregiving rounds with illegal query") {
        listOf(
            "AccidentNumber:2022-1111111",
            "2022-1111111",
            "AccidentNumber:",
            "AccidentNumber",
            ":",
        ).map { query ->
            get("/api/v1/caregiving-rounds")
                .queryParam("page-number", "1")
                .queryParam("page-size", "20")
                .queryParam("from", "2023-02-01")
                .queryParam("until", "2023-02-06")
                .queryParam("reception-progressing-status", "CAREGIVING_IN_PROGRESS")
                .queryParam("caregiving-progressing-status", "CAREGIVING_IN_PROGRESS")
                .queryParam("settlement-progressing-status", "NOT_STARTED")
                .queryParam("billing-progressing-status", "NOT_STARTED")
                .queryParam("query", query)
        }.forEach { request ->
            val expectResponse = ResponseMatcher(mockMvc, request)

            should("response status should be 400 Bad Request") {
                expectResponse(status().isBadRequest)
            }

            should("response payload should contains error message and type") {
                expectResponse(
                    content().json(
                        """
                            {
                              "message": "해석할 수 없는 검색 조건입니다.",
                              "errorType": "ILLEGAL_SEARCH_QUERY"
                            }
                        """.trimIndent()
                    )
                )
            }
        }
    }

    context("간병 메타 정보 수정할 때") {
        val request = put("/api/v1/caregiving-rounds/01GSSJGJAY2F4CJ11MMJ03C4W5")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                        "caregivingProgressingStatus": "NOT_STARTED",
                        "startDateTime": null,
                        "endDateTime": null,
                        "caregivingRoundClosingReasonType": null,
                        "caregivingRoundClosingReasonDetail": null,
                        "caregiverInfo": {
                            "caregiverOrganizationId": null,
                            "name": "정만길",
                            "sex": "MALE",
                            "birthDate": "1964-02-14",
                            "insured": true,
                            "phoneNumber": "01012341234",
                            "dailyCaregivingCharge": 150000,
                            "commissionFee": 3000,
                            "accountInfo": {
                                "bank": "국민은행",
                                "accountNumber": null,
                                "accountHolder": null
                            }
                        },
                        "remarks": "1회차 해보고 만족해서 계속한다고 함."
                    }
                """.trimIndent()
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            justRun {
                caregivingRoundEditingCommandHandler.editCaregivingRound(
                    match { it.caregivingRoundId == "01GSSJGJAY2F4CJ11MMJ03C4W5" },
                    any(),
                )
            }
        }

        afterEach { clearAllMocks() }

        should("응답 결과는 204 no Content 일 것") {
            expectResponse(status().isNoContent)
        }

        should("응답 받은 페이로드 결과는 공백일 것") {
            expectResponse(content().string(""))
        }

        should("페이로드로 받은 정보로 간병 수정할 것") {
            mockMvc.perform(request)

            verify {
                caregivingRoundEditingCommandHandler.editCaregivingRound(
                    withArg {
                        it.caregivingRoundId shouldBe "01GSSJGJAY2F4CJ11MMJ03C4W5"
                    },
                    withArg {
                        it.caregivingProgressingStatus shouldBe Patches.ofValue(CaregivingProgressingStatus.NOT_STARTED)
                        it.startDateTime shouldBe null
                        it.endDateTime shouldBe null
                        it.caregivingRoundClosingReasonType shouldBe null
                        it.caregivingRoundClosingReasonDetail shouldBe null
                        it.caregiverInfo?.caregiverOrganizationId shouldBe null
                        it.caregiverInfo shouldBe CaregiverInfo(
                            caregiverOrganizationId = null,
                            name = "정만길",
                            sex = Sex.MALE,
                            birthDate = LocalDate.of(1964, 2, 14),
                            insured = true,
                            phoneNumber = "01012341234",
                            dailyCaregivingCharge = 150000,
                            commissionFee = 3000,
                            accountInfo = AccountInfo(
                                bank = "국민은행",
                                accountNumber = null,
                                accountHolder = null,
                            ),
                        )
                        it.remarks shouldBe "1회차 해보고 만족해서 계속한다고 함."
                    }
                )
            }
        }
    }

    context("산정된 간병비 정보를 수정할 때") {
        val targetCaregivingRoundId = "01GSSJGJAY2F4CJ11MMJ03C4W5"
        val request = put("/api/v1/caregiving-rounds/$targetCaregivingRoundId/caregiving-charge")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                        "additionalHoursCharge": 20000,
                        "mealCost": 0,
                        "transportationFee": 10000,
                        "holidayCharge": 40000,
                        "caregiverInsuranceFee": 0,
                        "commissionFee": -5000,
                        "vacationCharge": 0,
                        "patientConditionCharge": 50000,
                        "covid19TestingCost": 4500,
                        "outstandingAmount": 50000,
                        "additionalCharges": [
                            {
                              "name": "특별 보너스비",
                              "amount": 5000
                            },
                            {
                              "name": "고객 보상비",
                              "amount": -10000
                            }
                        ],
                        "isCancelAfterArrived": true,
                        "expectedSettlementDate": "2023-03-10",
                        "caregivingChargeConfirmStatus": "NOT_STARTED"
                      }
                """.trimIndent()
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            justRun {
                caregivingChargeEditingCommandHandler.createOrEditCaregivingCharge(
                    match { it.caregivingRoundId == "01GSSJGJAY2F4CJ11MMJ03C4W5" },
                    any()
                )
            }
        }

        afterEach { clearAllMocks() }

        should("응답 결과는 204 no Content 일 것") {
            expectResponse(status().isNoContent)
        }

        should("응답 받은 페이로드 결과는 공백일 것") {
            expectResponse(content().string(""))
        }

        should("페이로드로 받은 정보로 간병비 산정 입력할 것") {
            mockMvc.perform(request)

            verify {
                caregivingChargeEditingCommandHandler.createOrEditCaregivingCharge(
                    withArg {
                        it.caregivingRoundId shouldBe "01GSSJGJAY2F4CJ11MMJ03C4W5"
                    },
                    withArg {
                        it.additionalHoursCharge shouldBe 20000
                        it.mealCost shouldBe 0
                        it.transportationFee shouldBe 10000
                        it.holidayCharge shouldBe 40000
                        it.caregiverInsuranceFee shouldBe 0
                        it.commissionFee shouldBe -5000
                        it.vacationCharge shouldBe 0
                        it.patientConditionCharge shouldBe 50000
                        it.covid19TestingCost shouldBe 4500
                        it.outstandingAmount shouldBe 50000
                        it.additionalCharges shouldContain CaregivingCharge.AdditionalCharge(
                            name = "특별 보너스비",
                            amount = 5000,
                        )
                        it.additionalCharges shouldContain CaregivingCharge.AdditionalCharge(
                            name = "고객 보상비",
                            amount = -10000,
                        )
                        it.isCancelAfterArrived shouldBe true
                        it.expectedSettlementDate shouldBe LocalDate.of(2023, 3, 10)
                        it.caregivingChargeConfirmStatus shouldBe CaregivingChargeConfirmStatus.NOT_STARTED
                    }
                )
            }
        }
    }

    context("단일 간병비 산정정보를 조회할 때") {
        val request = get("/api/v1/caregiving-rounds/01GSSJGJAY2F4CJ11MMJ03C4W5/caregiving-charge")
        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                caregivingChargeByCaregivingRoundIdQueryHandler.getCaregivingCharge(
                    match { it.caregivingRoundId == "01GSSJGJAY2F4CJ11MMJ03C4W5" }
                )
            } returns relaxedMock {
                every { id } returns "01GV2KS7SFZCRAYGP7QGGR8DXN"
                every { caregivingRoundInfo } returns CaregivingCharge.CaregivingRoundInfo(
                    caregivingRoundId = "01GSM0PQ5G8HW2GKYXH3VGGMZG",
                    caregivingRoundNumber = 1,
                    startDateTime = LocalDateTime.of(2023, 3, 3, 10, 0, 0),
                    endDateTime = LocalDateTime.of(2023, 3, 8, 10, 0, 0),
                    dailyCaregivingCharge = 150000,
                    receptionId = "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                )
                every { additionalHoursCharge } returns 0
                every { mealCost } returns 10000
                every { transportationFee } returns 0
                every { holidayCharge } returns 0
                every { caregiverInsuranceFee } returns 0
                every { commissionFee } returns -5000
                every { vacationCharge } returns 0
                every { patientConditionCharge } returns 50000
                every { covid19TestingCost } returns 4500
                every { outstandingAmount } returns 0
                every { additionalCharges } returns listOf(
                    relaxedMock {
                        every { name } returns "특별 보상비"
                        every { amount } returns 5000
                    },
                    relaxedMock {
                        every { name } returns "고객 보상비"
                        every { amount } returns -10000
                    },
                )
                every { isCancelAfterArrived } returns false
                every { caregivingChargeConfirmStatus } returns CaregivingChargeConfirmStatus.NOT_STARTED
                every { basicAmount } returns 750000
                every { additionalAmount } returns 54500
                every { totalAmount } returns 804500
            }
        }

        afterEach { clearAllMocks() }

        should("응답 결과는 200 ok 일 것") {
            expectResponse(status().isOk)
        }

        should("응답 받은 페이로드에 간병비 산정정보가 포함될 것") {
            expectResponse(
                content().json(
                    """
                        {
                          "id": "01GV2KS7SFZCRAYGP7QGGR8DXN",
                          "caregivingRoundInfo": {
                            "caregivingRoundId": "01GSM0PQ5G8HW2GKYXH3VGGMZG",
                            "caregivingRoundNumber": 1,
                            "startDateTime": "2023-03-03T01:00:00Z",
                            "endDateTime": "2023-03-08T01:00:00Z",
                            "dailyCaregivingCharge": 150000,
                            "receptionId": "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                          },
                          "additionalHoursCharge": 0,
                          "mealCost": 10000,
                          "transportationFee": 0,
                          "holidayCharge": 0,
                          "caregiverInsuranceFee": 0,
                          "commissionFee": -5000,
                          "vacationCharge": 0,
                          "patientConditionCharge": 50000,
                          "covid19TestingCost": 4500,
                          "outstandingAmount": 0,
                          "additionalCharges": [
                            {
                              "name": "특별 보상비",
                              "amount": 5000
                            },
                            {
                               "name": "고객 보상비",
                               "amount": -10000
                            }
                          ],
                          "isCancelAfterArrived": false,
                          "caregivingChargeConfirmStatus": "NOT_STARTED",
                          "basicAmount": 750000,
                          "additionalAmount": 54500,
                          "totalAmount": 804500
                        }
                    """.trimIndent()
                )
            )
        }

        context("간병 회차가 존재하지 않다면") {
            beforeEach {
                every {
                    caregivingChargeByCaregivingRoundIdQueryHandler.getCaregivingCharge(
                        match { it.caregivingRoundId == "01GSSJGJAY2F4CJ11MMJ03C4W5" }
                    )
                } throws CaregivingRoundNotFoundByIdException("01GSSJGJAY2F4CJ11MMJ03C4W5")
            }

            afterEach { clearAllMocks() }

            should("404 NotFound로 응답합니다.") {
                expectResponse(status().isNotFound())
            }

            should("에러 메시지와 에러 타입과 에러 데이터를 페이로드에 포함하여 응답합니다.") {
                expectResponse(
                    content().json(
                        """
                            {
                              "message": "조회하고자 하는 간병 회차 정보가 존재하지 않습니다.",
                              "errorType": "CAREGIVING_ROUND_NOT_EXISTS",
                              "data": {
                                "enteredCaregivingRoundId": "01GSSJGJAY2F4CJ11MMJ03C4W5"
                              }
                            }
                        """.trimIndent()
                    )
                )
            }
        }

        context("간병비가 아직 산정되지 않은 상태라면") {
            beforeEach {
                every {
                    caregivingChargeByCaregivingRoundIdQueryHandler.getCaregivingCharge(
                        match { it.caregivingRoundId == "01GSSJGJAY2F4CJ11MMJ03C4W5" }
                    )
                } throws CaregivingChargeNotEnteredException("01GSSJGJAY2F4CJ11MMJ03C4W5")
            }

            afterEach { clearAllMocks() }

            should("404 NotFound로 응답합니다.") {
                expectResponse(status().isNotFound())
            }

            should("에러 메시지와 에러 타입과 에러 데이터를 페이로드에 포함하여 응답합니다.") {
                expectResponse(
                    content().json(
                        """
                            {
                              "message": "산정된 간병비가 존재하지 않습니다.",
                              "errorType": "CAREGIVING_CHARGE_NOT_ENTERED",
                              "data": {
                                "enteredCaregivingRoundId": "01GSSJGJAY2F4CJ11MMJ03C4W5"
                              }
                            }
                        """.trimIndent()
                    )
                )
            }
        }
    }

    context("간병 회차를 조회하면") {
        val request = get("/api/v1/caregiving-rounds/01GSC7CMD4753B5EP4A889Q5HZ")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                caregivingRoundByIdQueryHandler.getCaregivingRound(
                    match { it.caregivingRoundId == "01GSC7CMD4753B5EP4A889Q5HZ" }
                )
            } returns relaxedMock {
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
            }

            every { receptionByIdQueryHandler.getReception(match { it.receptionId == "01GSC4SKPGWMZDP7EKWQ0Z57NG" }) } returns relaxedMock {
                every { id } returns "01GSC4SKPGWMZDP7EKWQ0Z57NG"
                every { patientInfo.name.masked } returns "김*자"
                every { insuranceInfo.insuranceNumber } returns "12345-12345"
                every { accidentInfo.accidentNumber } returns "2022-1234567"
                every { expectedCaregivingStartDate } returns null
                every { progressingStatus } returns ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
            }
        }

        afterEach { clearAllMocks() }

        should("200 Ok로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("간병 회차 데이터를 페이로드에 포함하여 응답합니다.") {
            expectResponse(
                content().json(
                    """
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
                    """.trimIndent()
                )
            )
        }

        should("간병 회차 조회를 도메인 영역에 위임합니다.") {
            mockMvc.perform(request)

            verify {
                caregivingRoundByIdQueryHandler.getCaregivingRound(
                    withArg {
                        it.caregivingRoundId shouldBe "01GSC7CMD4753B5EP4A889Q5HZ"
                    }
                )
            }
        }
    }
})
