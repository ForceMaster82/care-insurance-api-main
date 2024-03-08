package kr.caredoc.careinsurance.web.reception

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.LocalEncryption
import kr.caredoc.careinsurance.LocalEncryptionConfig
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.insurance.InsuranceInfo
import kr.caredoc.careinsurance.insurance.InsuranceManagerInfo
import kr.caredoc.careinsurance.patient.EncryptedPatientInfo
import kr.caredoc.careinsurance.patient.PatientInfo
import kr.caredoc.careinsurance.patient.Sex
import kr.caredoc.careinsurance.phonenumber.InvalidPhoneNumberException
import kr.caredoc.careinsurance.phonenumber.PartialEncryptedPhoneNumber
import kr.caredoc.careinsurance.reception.AccidentInfo
import kr.caredoc.careinsurance.reception.CaregivingManagerInfo
import kr.caredoc.careinsurance.reception.ClaimType
import kr.caredoc.careinsurance.reception.OrganizationType
import kr.caredoc.careinsurance.reception.Reception
import kr.caredoc.careinsurance.reception.ReceptionAccessPolicy
import kr.caredoc.careinsurance.reception.ReceptionByIdQueryHandler
import kr.caredoc.careinsurance.reception.ReceptionCreationCommandHandler
import kr.caredoc.careinsurance.reception.ReceptionCreationResult
import kr.caredoc.careinsurance.reception.ReceptionEditingCommandHandler
import kr.caredoc.careinsurance.reception.ReceptionProgressingStatus
import kr.caredoc.careinsurance.reception.ReceptionsByFilterQuery
import kr.caredoc.careinsurance.reception.ReceptionsByFilterQueryHandler
import kr.caredoc.careinsurance.reception.ReferenceCoverageNotExistsException
import kr.caredoc.careinsurance.reception.RegisterManagerInfo
import kr.caredoc.careinsurance.reception.exception.ReceptionNotFoundByIdException
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.security.encryption.Decryptor
import kr.caredoc.careinsurance.web.reception.response.DetailReceptionResponse
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.TimeZone

@CareInsuranceWebMvcTest(ReceptionController::class)
@Import(LocalEncryptionConfig::class)
class ReceptionControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean
    private val receptionCreationCommandHandler: ReceptionCreationCommandHandler,
    @MockkBean
    private val receptionsByFilterQueryHandler: ReceptionsByFilterQueryHandler,
    @MockkBean
    private val receptionByIdQueryHandler: ReceptionByIdQueryHandler,
    @MockkBean
    private val receptionEditingCommandHandler: ReceptionEditingCommandHandler,
) : ShouldSpec({
    context("when creating reception") {
        val request = post("/api/v1/receptions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                      "insuranceInfo": {
                        "insuranceNumber": "11111-1111",
                        "subscriptionDate": "2022-09-01",
                        "coverageId": "01GPD5EE21TGK5A5VCYWQ9Z73W",
                        "caregivingLimitPeriod": 180
                      },
                      "patientInfo": {
                        "name": "임석민",
                        "nickname": "뽀리스",
                        "age": 31,
                        "sex": "MALE",
                        "primaryContact": {
                          "phoneNumber": "01011112222",
                          "relationshipWithPatient": "본인"
                        },
                        "secondaryContact": {
                          "phoneNumber": "01011113333",
                          "relationshipWithPatient": "형제"
                        }
                      },
                      "accidentInfo": {
                        "accidentNumber": "2022-1111111",
                        "accidentDateTime": "2023-01-12T09:07:55Z",
                        "claimType": "ACCIDENT",
                        "patientDescription": "자력으로 호흡 불가능",
                        "admissionDateTime": "2023-01-12T10:03:12Z",
                        "hospitalRoomInfo": {
                          "hospitalAndRoom": "케어닥 병원 304호실"
                        }
                      },
                      "insuranceManagerInfo": {
                        "branchName": "메리츠 증권 평양 지점",
                        "receptionistName": "김정은",
                        "phoneNumber": "01011114444"
                      },
                      "registerManagerInfo": {
                        "managingUserId": "01GP2EK7XN2T9PK2Q262FXX5VA"
                      },
                      "receivedDateTime": "2023-01-12T12:30:45Z",
                      "desiredCaregivingStartDate": "2023-01-13",
                      "urgency": "URGENT",
                      "desiredCaregivingPeriod": 180,
                      "additionalRequests": "몸무게가 많이 나가서 힘이 센분이여야 합니다.",
                      "notifyCaregivingProgress": true
                    }
                """.trimIndent()
            )
        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every { receptionCreationCommandHandler.createReception(any()) } returns ReceptionCreationResult("01GPWNKNMTRT8PJXJHXVA921NQ")
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
        }

        afterEach { clearAllMocks() }

        should("response status should be 201 Created") {
            expectResponse(status().isCreated)
        }

        should("response payload should be empty") {
            expectResponse(content().string(""))
        }

        should("response header should contains Location header") {
            expectResponse(
                header().string(
                    HttpHeaders.LOCATION,
                    "http://localhost/api/v1/receptions/01GPWNKNMTRT8PJXJHXVA921NQ"
                )
            )
        }

        should("create reception using arguments from request payload") {
            mockMvc.perform(request)

            verify {
                receptionCreationCommandHandler.createReception(
                    withArg {
                        it.insuranceInfo.insuranceNumber shouldBe "11111-1111"
                        it.insuranceInfo.subscriptionDate shouldBe LocalDate.of(2022, 9, 1)
                        it.insuranceInfo.coverageId shouldBe "01GPD5EE21TGK5A5VCYWQ9Z73W"
                        it.insuranceInfo.caregivingLimitPeriod shouldBe 180
                        it.patientInfo.name shouldBe "임석민"
                        it.patientInfo.nickname shouldBe "뽀리스"
                        it.patientInfo.age shouldBe 31
                        it.patientInfo.sex shouldBe Sex.MALE
                        it.patientInfo.primaryContact.phoneNumber shouldBe "01011112222"
                        it.patientInfo.primaryContact.relationshipWithPatient shouldBe "본인"
                        it.patientInfo.secondaryContact!!.phoneNumber shouldBe "01011113333"
                        it.patientInfo.secondaryContact!!.relationshipWithPatient shouldBe "형제"
                        it.accidentInfo.accidentNumber shouldBe "2022-1111111"
                        it.accidentInfo.accidentDateTime shouldBe LocalDateTime.of(2023, 1, 12, 18, 7, 55)
                        it.accidentInfo.claimType shouldBe ClaimType.ACCIDENT
                        it.accidentInfo.patientDescription shouldBe "자력으로 호흡 불가능"
                        it.accidentInfo.admissionDateTime shouldBe LocalDateTime.of(2023, 1, 12, 19, 3, 12)
                        it.accidentInfo.hospitalAndRoomInfo.hospitalAndRoom shouldBe "케어닥 병원 304호실"
                        it.insuranceManagerInfo.branchName shouldBe "메리츠 증권 평양 지점"
                        it.insuranceManagerInfo.receptionistName shouldBe "김정은"
                        it.insuranceManagerInfo.phoneNumber shouldBe "01011114444"
                        it.registerManagerInfo.managingUserId shouldBe "01GP2EK7XN2T9PK2Q262FXX5VA"
                        it.receivedDateTime shouldBe LocalDateTime.of(2023, 1, 12, 21, 30, 45)
                        it.desiredCaregivingStartDate shouldBe LocalDate.of(2023, 1, 13)
                        it.urgency shouldBe Reception.Urgency.URGENT
                        it.desiredCaregivingPeriod shouldBe 180
                        it.additionalRequests shouldBe "몸무게가 많이 나가서 힘이 센분이여야 합니다."
                        it.notifyCaregivingProgress shouldBe true
                    }
                )
            }
        }

        context("when reference coverage not exists") {
            beforeEach {
                every {
                    receptionCreationCommandHandler.createReception(
                        match { it.insuranceInfo.coverageId == "01GPD5EE21TGK5A5VCYWQ9Z73W" }
                    )
                } throws ReferenceCoverageNotExistsException("01GPD5EE21TGK5A5VCYWQ9Z73W")
            }

            afterEach { clearAllMocks() }

            should("response status should be 422 Unprocessable Entity") {
                expectResponse(status().isUnprocessableEntity)
            }

            should("response payload should contains error message and type") {
                expectResponse(
                    content().json(
                        """
                            {
                              "message": "요청에 포함된 가입 담보가 존재하지 않습니다.",
                              "errorType": "REFERENCE_COVERAGE_NOT_EXISTS"
                            }
                        """.trimIndent()
                    )
                )
            }

            should("response payload should contains requested coverage id as data") {
                expectResponse(
                    content().json(
                        """
                            {
                              "data": {
                                "enteredCoverageId": "01GPD5EE21TGK5A5VCYWQ9Z73W"
                              }
                            }
                        """.trimIndent()
                    )
                )
            }
        }

        context("하지만 제공받은 전화번호가 잘못된 형식이라면") {
            beforeEach {
                every { receptionCreationCommandHandler.createReception(any()) } throws InvalidPhoneNumberException("01011112222")
            }

            afterEach { clearAllMocks() }

            should("400 Bad Request로 응답합니다.") {
                expectResponse(status().isBadRequest)
            }

            should("에러 메시지와 에러 타입, 데이터를 포함하여 응답합니다.") {
                expectResponse(
                    content().json(
                        """
                            {
                              "message": "잘못된 핸드폰 번호 형식이 입력되었습니다.",
                              "errorType": "ILLEGAL_PHONE_NUMBER",
                              "data": {
                                "enteredPhoneNumber": "01011112222"
                              }
                            }
                        """.trimIndent()
                    )
                )
            }
        }
    }

    context("when creating reception with allowed null properties") {
        val request = post("/api/v1/receptions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                      "insuranceInfo": {
                        "insuranceNumber": "11111-1111",
                        "subscriptionDate": "2022-09-01",
                        "coverageId": "01GPD5EE21TGK5A5VCYWQ9Z73W",
                        "caregivingLimitPeriod": 180
                      },
                      "patientInfo": {
                        "name": "임석민",
                        "nickname": null,
                        "age": 31,
                        "sex": "MALE",
                        "primaryContact": {
                          "phoneNumber": "01011112222",
                          "relationshipWithPatient": "본인"
                        },
                        "secondaryContact": null
                      },
                      "accidentInfo": {
                        "accidentNumber": "2022-1111111",
                        "accidentDateTime": "2023-01-12T09:07:55Z",
                        "claimType": "ACCIDENT",
                        "patientDescription": "자력으로 호흡 불가능",
                        "admissionDateTime": "2023-01-12T10:03:12Z",
                        "hospitalRoomInfo": {
                          "hospitalAndRoom": "케어닥 병원 304호실"
                        }
                      },
                      "insuranceManagerInfo": {
                        "branchName": "메리츠 증권 평양 지점",
                        "receptionistName": "김정은",
                        "phoneNumber": "01011114444"
                      },
                      "registerManagerInfo": {
                        "managingUserId": "01GP2EK7XN2T9PK2Q262FXX5VA"
                      },
                      "receivedDateTime": "2023-01-12T12:30:45Z",
                      "desiredCaregivingStartDate": "2023-01-13",
                      "urgency": "URGENT",
                      "desiredCaregivingPeriod": null,
                      "additionalRequests": "몸무게가 많이 나가서 힘이 센분이여야 합니다."
                    }
                """.trimIndent()
            )
        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every { receptionCreationCommandHandler.createReception(any()) } returns ReceptionCreationResult("01GPWNKNMTRT8PJXJHXVA921NQ")
        }

        afterEach { clearAllMocks() }

        should("response status should be 201 Created") {
            expectResponse(status().isCreated)
        }

        should("create reception using null arguments") {
            mockMvc.perform(request)

            verify {
                receptionCreationCommandHandler.createReception(
                    withArg {
                        it.patientInfo.nickname shouldBe null
                        it.patientInfo.secondaryContact shouldBe null
                        it.desiredCaregivingPeriod shouldBe null
                    }
                )
            }
        }
    }

    context("when getting receptions") {
        val request = get("/api/v1/receptions")
            .queryParam("page-number", "1")
            .queryParam("page-size", "10")
            .queryParam("from", "2022-01-30")
            .queryParam("until", "2022-01-30")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                receptionsByFilterQueryHandler.getReceptions(
                    match {
                        listOf(
                            it.from == LocalDate.of(2022, 1, 30),
                            it.until == LocalDate.of(2022, 1, 30),
                        ).all { predicate -> predicate }
                    },
                    match {
                        it.pageNumber == 0 && it.pageSize == 10
                    }
                )
            } returns PageImpl(
                listOf<Reception>(
                    relaxedMock {
                        every { id } returns "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                        every { insuranceInfo } returns relaxedMock {
                            every { insuranceNumber } returns "11111-1111"
                            every { coverageId } returns "01GPD5EE21TGK5A5VCYWQ9Z73W"
                        }
                        every { patientInfo } returns relaxedMock {
                            every { name.masked } returns "임*민"
                            every { age } returns 31
                            every { sex } returns Sex.MALE
                            every { primaryContact } returns relaxedMock {
                                every { partialEncryptedPhoneNumber } returns relaxedMock phoneNumberMock@{
                                    every { this@phoneNumberMock.toString() } returns "010****2222"
                                    every { relationshipWithPatient } returns "본인"
                                }
                            }
                        }
                        every { accidentInfo } returns relaxedMock {
                            every { accidentNumber } returns "2022-1111111"
                            every { hospitalAndRoomInfo } returns relaxedMock {
                                every { state } returns null
                                every { city } returns null
                                every { hospitalAndRoom } returns "케어닥병원 304호"
                            }
                        }
                        every { caregivingManagerInfo } returns relaxedMock {
                            every { organizationType } returns OrganizationType.ORGANIZATION
                            every { organizationId } returns "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                            every { managingUserId } returns "01GQ23MVTBAKS526S0WGS9CS0A"
                        }
                        every { progressingStatus } returns ReceptionProgressingStatus.MATCHING
                        every { desiredCaregivingStartDate } returns LocalDate.of(2023, 1, 13)
                        every { urgency } returns Reception.Urgency.URGENT
                        every { desiredCaregivingPeriod } returns 180
                        every { periodType } returns Reception.PeriodType.NORMAL
                        every { receivedDateTime } returns LocalDateTime.of(2023, 1, 12, 22, 5, 22)
                    },
                ),
                PageRequest.of(0, 10),
                1,
            )
        }

        afterEach { clearAllMocks() }

        should("response status should be 200 Ok") {
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

        should("response payload should contains queried receptions") {
            expectResponse(
                content().json(
                    """
                        {
                          "items": [
                            {
                              "id": "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                              "insuranceInfo": {
                                "insuranceNumber": "11111-1111",
                                "coverageId": "01GPD5EE21TGK5A5VCYWQ9Z73W"
                              },
                              "patientInfo": {
                                "name": "임*민",
                                "age": 31,
                                "sex": "MALE",
                                "primaryContact": {
                                  "phoneNumber": "010****2222",
                                  "relationshipWithPatient": "본인"
                                }
                              },
                              "accidentInfo": {
                                "accidentNumber": "2022-1111111",
                                "hospitalRoomInfo": { 
                                  "state": null,
                                  "city": null,
                                  "hospitalAndRoom": "케어닥병원 304호"
                                }
                              },
                              "caregivingManagerInfo": {
                                "organizationType": "ORGANIZATION",
                                "organizationId": "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                                "managingUserId": "01GQ23MVTBAKS526S0WGS9CS0A"
                              },
                              "progressingStatus": "MATCHING",
                              "desiredCaregivingStartDate": "2023-01-13",
                              "urgency": "URGENT",
                              "desiredCaregivingPeriod": 180,
                              "periodType": "NORMAL",
                              "receivedDateTime": "2023-01-12T13:05:22Z"
                            }
                          ]
                        }
                    """.trimIndent()
                )
            )
        }
    }

    context("when getting receptions with filters and search") {
        val request = get("/api/v1/receptions")
            .queryParam("page-number", "1")
            .queryParam("page-size", "10")
            .queryParam("from", "2022-01-30")
            .queryParam("until", "2022-01-30")
            .queryParam("urgency", "URGENT")
            .queryParam("period-type", "SHORT")
            .queryParam("organization-type", "AFFILIATED")
            .queryParam(
                "progressing-status",
                "CANCELED",
                "CANCELED_BY_PERSONAL_CAREGIVER",
                "CANCELED_BY_MEDICAL_REQUEST"
            )
            .queryParam("query", "caregivingManagerName:보리스")

        beforeEach {
            every {
                receptionsByFilterQueryHandler.getReceptions(any(), any())
            } returns PageImpl(
                listOf<Reception>(),
                PageRequest.of(0, 10),
                0,
            )
        }

        afterEach { clearAllMocks() }

        should("query receptions using query parameters") {
            mockMvc.perform(request)

            verify {
                receptionsByFilterQueryHandler.getReceptions(
                    withArg {
                        it.urgency shouldBe Reception.Urgency.URGENT
                        it.periodType shouldBe Reception.PeriodType.SHORT
                        it.organizationType shouldBe OrganizationType.AFFILIATED
                        it.progressingStatuses shouldContainExactlyInAnyOrder setOf(
                            ReceptionProgressingStatus.CANCELED,
                            ReceptionProgressingStatus.CANCELED_BY_MEDICAL_REQUEST,
                            ReceptionProgressingStatus.CANCELED_BY_PERSONAL_CAREGIVER
                        )
                        it.searchCondition!!.searchingProperty shouldBe ReceptionsByFilterQuery.SearchingProperty.CAREGIVING_MANAGER_NAME
                        it.searchCondition!!.keyword shouldBe "보리스"
                    },
                    any(),
                )
            }
        }
    }

    context("when searching receptions with illegal query") {
        listOf(
            "CaregivingManagerName:보리스",
            "보리스",
            "CaregivingManagerName:",
            "CaregivingManagerName",
            ":",
        ).map { query ->
            get("/api/v1/receptions")
                .queryParam("page-number", "1")
                .queryParam("page-size", "10")
                .queryParam("from", "2022-01-30")
                .queryParam("until", "2022-01-30")
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

    context("when getting single reception") {
        val request = get("/api/v1/receptions/01GPJMK7ZPBBKTY3TP0NN5JWCJ")
        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            val mapperSlot = slot<(Reception) -> DetailReceptionResponse>()
            every {
                receptionByIdQueryHandler.getReception(
                    match { it.receptionId == "01GPJMK7ZPBBKTY3TP0NN5JWCJ" },
                    capture(mapperSlot),
                )
            } answers {
                mapperSlot.captured(
                    relaxedMock {
                        every { id } returns "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                        every { insuranceInfo } returns InsuranceInfo(
                            insuranceNumber = "11111-1111",
                            subscriptionDate = LocalDate.of(2022, 9, 1),
                            coverageId = "01GPD5EE21TGK5A5VCYWQ9Z73W",
                            caregivingLimitPeriod = 180,
                        )
                        every { patientInfo } returns EncryptedPatientInfo(
                            name = EncryptedPatientInfo.EncryptedPatientName(
                                "임석민",
                                LocalEncryption.patientNameHasher,
                                LocalEncryption.LocalEncryptor,
                            ),
                            nickname = "뽀리스",
                            age = 31,
                            sex = Sex.MALE,
                            height = null,
                            weight = null,
                            primaryContact = EncryptedPatientInfo.EncryptedContact(
                                partialEncryptedPhoneNumber = relaxedMock phoneNumberMock@{
                                    every { this@phoneNumberMock.toString() } returns "010****2222"
                                },
                                relationshipWithPatient = "본인",
                            ),
                            secondaryContact = EncryptedPatientInfo.EncryptedContact(
                                partialEncryptedPhoneNumber = relaxedMock phoneNumberMock@{
                                    every { this@phoneNumberMock.toString() } returns "010****3333"
                                },
                                relationshipWithPatient = "형제",
                            ),
                        )
                        every { accidentInfo } returns AccidentInfo(
                            accidentNumber = "2022-1111111",
                            accidentDateTime = LocalDateTime.of(2023, 1, 12, 0, 7, 55),
                            claimType = ClaimType.ACCIDENT,
                            patientDescription = "자력으로 호흡 불가능",
                            admissionDateTime = LocalDateTime.of(2023, 1, 12, 1, 3, 12),
                            hospitalAndRoomInfo = AccidentInfo.HospitalAndRoomInfo(
                                state = null,
                                city = null,
                                hospitalAndRoom = "케어닥 병원 304호실",
                            )
                        )
                        every { insuranceManagerInfo } returns InsuranceManagerInfo(
                            branchName = "메리츠 증권 평양 지점",
                            receptionistName = "김정은",
                            phoneNumber = "01011114444",
                        )
                        every { caregivingManagerInfo } returns CaregivingManagerInfo(
                            organizationType = OrganizationType.ORGANIZATION,
                            organizationId = "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                            managingUserId = "01GP2EK7XN2T9PK2Q262FXX5VA",
                        )
                        every { registerManagerInfo } returns RegisterManagerInfo(
                            managingUserId = "01GP2EK7XN2T9PK2Q262FXX5VA",
                        )
                        every { desiredCaregivingStartDate } returns LocalDate.of(2023, 1, 13)
                        every { urgency } returns Reception.Urgency.URGENT
                        every { desiredCaregivingPeriod } returns 180
                        every { additionalRequests } returns "몸무게가 많이 나가서 힘이 센분이여야 합니다."
                        every { expectedCaregivingLimitDate } returns LocalDate.of(2023, 7, 13)
                        every { progressingStatus } returns ReceptionProgressingStatus.MATCHING
                        every { periodType } returns Reception.PeriodType.NORMAL
                        every { receivedDateTime } returns LocalDateTime.of(2023, 1, 12, 4, 5, 22)
                        every { notifyCaregivingProgress } returns true
                    }
                )
            }
        }

        afterEach { clearAllMocks() }

        should("response status should be 200 Ok") {
            expectResponse(status().isOk)
        }

        should("response payload should contains reception info") {
            expectResponse(
                content().json(
                    """
                        {
                          "id": "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                          "insuranceInfo": {
                            "insuranceNumber": "11111-1111",
                            "subscriptionDate": "2022-09-01",
                            "coverageId": "01GPD5EE21TGK5A5VCYWQ9Z73W",
                            "caregivingLimitPeriod": 180
                          },
                          "patientInfo": {
                            "name": "임*민",
                            "nickname": "뽀리스",
                            "age": 31,
                            "sex": "MALE",
                            "primaryContact": {
                              "phoneNumber": "010****2222",
                              "relationshipWithPatient": "본인"
                            },
                            "secondaryContact": {
                              "phoneNumber": "010****3333",
                              "relationshipWithPatient": "형제"
                            }
                          },
                          "accidentInfo": {
                            "accidentNumber": "2022-1111111",
                            "accidentDateTime": "2023-01-11T15:07:55Z",
                            "claimType": "ACCIDENT",
                            "patientDescription": "자력으로 호흡 불가능",
                            "admissionDateTime": "2023-01-11T16:03:12Z",
                            "hospitalRoomInfo": {
                              "state": null,
                              "city": null,
                              "hospitalAndRoom" : "케어닥 병원 304호실"
                            }
                          },
                          "insuranceManagerInfo": {
                            "branchName": "메리츠 증권 평양 지점",
                            "receptionistName": "김정은",
                            "phoneNumber": "01011114444"
                          },
                          "caregivingManagerInfo": {
                            "organizationType": "ORGANIZATION",
                            "organizationId": "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                            "managingUserId": "01GP2EK7XN2T9PK2Q262FXX5VA"
                          },
                          "registerManagerInfo": {
                            "managingUserId": "01GP2EK7XN2T9PK2Q262FXX5VA"
                          },
                          "desiredCaregivingStartDate": "2023-01-13",
                          "urgency": "URGENT",
                          "desiredCaregivingPeriod": 180,
                          "additionalRequests": "몸무게가 많이 나가서 힘이 센분이여야 합니다.",
                          "expectedCaregivingLimitDate": "2023-07-13",
                          "progressingStatus": "MATCHING",
                          "periodType": "NORMAL",
                          "receivedDateTime": "2023-01-11T19:05:22Z",
                          "notifyCaregivingProgress": true
                        }
                    """.trimIndent()
                )
            )
        }

        context("but reception not exists") {
            beforeEach {
                every {
                    receptionByIdQueryHandler.getReception(
                        match { it.receptionId == "01GPJMK7ZPBBKTY3TP0NN5JWCJ" },
                        any<(Reception) -> DetailReceptionResponse>(),
                    )
                } throws ReceptionNotFoundByIdException(receptionId = "01GPJMK7ZPBBKTY3TP0NN5JWCJ")
            }

            afterEach { clearAllMocks() }

            should("response status should be 404 Not Found") {
                expectResponse(status().isNotFound)
            }

            should("response payload should contains error message and error type") {
                expectResponse(
                    content().json(
                        """
                            {
                              "message": "조회하고자 하는 간병 접수가 존재하지 않습니다.",
                              "errorType": "RECEPTION_NOT_EXISTS"
                            }
                        """.trimIndent()
                    )
                )
            }

            should("response payload should contains entered reception id as error data") {
                expectResponse(
                    content().json(
                        """
                            {
                              "data": {
                                "enteredReceptionId": "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                              }
                            }
                        """.trimIndent()
                    )
                )
            }
        }
    }

    fun generatePutReceptionPayload(
        height: Int? = 183,
        weight: Int? = 87,
        secondaryContact: String? = """
            {
              "phoneNumber": "01011113333",
              "relationshipWithPatient": "형제"
            }
        """.trimIndent(),
        nickname: String? = "뽀리스",
        caregivingManagerInfo: String? = null,
    ) =
        """
            {
              "insuranceInfo": {
                "insuranceNumber": "11111-1111",
                "subscriptionDate": "2022-09-01",
                "coverageId": "01GPD5EE21TGK5A5VCYWQ9Z73W",
                "caregivingLimitPeriod": 180
              },
              "patientInfo": {
                "name": "임석민",
                "nickname": ${nickname?.let { "\"$nickname\"" }},
                "age": 31,
                "sex": "MALE",
                "height": $height,
                "weight": $weight,
                "primaryContact": {
                  "phoneNumber": "01011112222",
                  "relationshipWithPatient": "본인"
                },
                "secondaryContact": $secondaryContact
              },
              "accidentInfo": {
                "accidentNumber": "2022-1111111",
                "accidentDateTime": "2023-01-12T09:07:55Z",
                "claimType": "ACCIDENT",
                "patientDescription": "자력으로 호흡 불가능",
                "admissionDateTime": "2023-01-12T10:03:12Z",
                "hospitalRoomInfo": {
                  "state": "서울특별시",
                  "city": "강남구",
                  "hospitalAndRoom": "케어닥 병원 304호실"
                }
              },
              "caregivingManagerInfo": $caregivingManagerInfo,
              "desiredCaregivingStartDate": "2023-01-13",
              "desiredCaregivingPeriod": 180,
              "additionalRequests": "몸무게가 많이 나가서 힘이 센분이여야 합니다.",
              "expectedCaregivingLimitDate": "2023-07-13",
              "progressingStatus": "PENDING",
              "notifyCaregivingProgress": true
            }
        """.trimIndent()

    context("when editing reception") {
        val request = put("/api/v1/receptions/01GPWNKNMTRT8PJXJHXVA921NQ")
            .contentType(MediaType.APPLICATION_JSON)
            .content(generatePutReceptionPayload())

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            justRun {
                receptionEditingCommandHandler.editReception(
                    match { it.receptionId == "01GPWNKNMTRT8PJXJHXVA921NQ" },
                    any(),
                )
            }
        }

        afterEach { clearAllMocks() }

        should("response status should be 204 No Content") {
            expectResponse(status().isNoContent)
        }

        should("response payload should be empty") {
            expectResponse(content().string(""))
        }

        should("edit reception using arguments from payload") {
            mockMvc.perform(request)

            verify {
                receptionEditingCommandHandler.editReception(
                    withArg {
                        it.receptionId shouldBe "01GPWNKNMTRT8PJXJHXVA921NQ"
                    },
                    withArg {
                        it.insuranceInfo shouldBe InsuranceInfo(
                            insuranceNumber = "11111-1111",
                            subscriptionDate = LocalDate.of(2022, 9, 1),
                            coverageId = "01GPD5EE21TGK5A5VCYWQ9Z73W",
                            caregivingLimitPeriod = 180,
                        )
                        it.patientInfo shouldBe PatientInfo(
                            name = "임석민",
                            nickname = "뽀리스",
                            age = 31,
                            sex = Sex.MALE,
                            height = 183,
                            weight = 87,
                            primaryContact = PatientInfo.Contact(
                                phoneNumber = "01011112222",
                                relationshipWithPatient = "본인",
                            ),
                            secondaryContact = PatientInfo.Contact(
                                phoneNumber = "01011113333",
                                relationshipWithPatient = "형제",
                            ),
                        )
                        it.accidentInfo shouldBe AccidentInfo(
                            accidentNumber = "2022-1111111",
                            accidentDateTime = LocalDateTime.of(2023, 1, 12, 18, 7, 55),
                            claimType = ClaimType.ACCIDENT,
                            patientDescription = "자력으로 호흡 불가능",
                            admissionDateTime = LocalDateTime.of(2023, 1, 12, 19, 3, 12),
                            hospitalAndRoomInfo = AccidentInfo.HospitalAndRoomInfo(
                                state = "서울특별시",
                                city = "강남구",
                                hospitalAndRoom = "케어닥 병원 304호실",
                            )
                        )
                        it.desiredCaregivingStartDate shouldBe LocalDate.of(2023, 1, 13)
                        it.desiredCaregivingPeriod shouldBe 180
                        it.additionalRequests shouldBe "몸무게가 많이 나가서 힘이 센분이여야 합니다."
                        it.expectedCaregivingLimitDate shouldBe LocalDate.of(2023, 7, 13)
                        it.progressingStatus shouldBe ReceptionProgressingStatus.PENDING
                        it.notifyCaregivingProgress shouldBe true
                    },
                )
            }
        }

        context("but reception not exists") {
            beforeEach {
                every {
                    receptionEditingCommandHandler.editReception(
                        match { it.receptionId == "01GPWNKNMTRT8PJXJHXVA921NQ" },
                        any(),
                    )
                } throws ReceptionNotFoundByIdException(receptionId = "01GPWNKNMTRT8PJXJHXVA921NQ")
            }

            afterEach { clearAllMocks() }

            should("response status should be 404 Not Found") {
                expectResponse(status().isNotFound)
            }
        }

        context("but referenced coverage not exists") {
            beforeEach {
                every {
                    receptionEditingCommandHandler.editReception(
                        any(),
                        match {
                            it.insuranceInfo.coverageId == "01GPD5EE21TGK5A5VCYWQ9Z73W"
                        },
                    )
                } throws ReferenceCoverageNotExistsException(referenceCoverageId = "01GPD5EE21TGK5A5VCYWQ9Z73W")
            }

            afterEach { clearAllMocks() }

            should("response status should be 422 Unprocessable entity") {
                expectResponse(status().isUnprocessableEntity)
            }
        }
    }

    context("when editing reception using null arguments") {
        val request = put("/api/v1/receptions/01GPWNKNMTRT8PJXJHXVA921NQ")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                generatePutReceptionPayload(
                    weight = null,
                    height = null,
                    secondaryContact = null,
                    nickname = null,
                )
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            justRun {
                receptionEditingCommandHandler.editReception(
                    match { it.receptionId == "01GPWNKNMTRT8PJXJHXVA921NQ" },
                    any(),
                )
            }
        }

        afterEach { clearAllMocks() }

        should("response status should be 204 No Content") {
            expectResponse(status().isNoContent)
        }

        should("edit reception using null arguments") {
            mockMvc.perform(request)

            verify {
                receptionEditingCommandHandler.editReception(
                    any(),
                    withArg {
                        it.patientInfo.weight shouldBe null
                        it.patientInfo.height shouldBe null
                        it.patientInfo.secondaryContact shouldBe null
                        it.patientInfo.nickname shouldBe null
                    },
                )
            }
        }
    }

    context("when assigning internal caregiving manager") {
        val request = put("/api/v1/receptions/01GPWNKNMTRT8PJXJHXVA921NQ")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                generatePutReceptionPayload(
                    caregivingManagerInfo = """
                        {
                            "organizationType": "INTERNAL",
                            "organizationId": null,
                            "managingUserId": "01GP2EK7XN2T9PK2Q262FXX5VA"
                        }
                    """.trimIndent()
                )
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            justRun {
                receptionEditingCommandHandler.editReception(
                    match { it.receptionId == "01GPWNKNMTRT8PJXJHXVA921NQ" },
                    any(),
                )
            }
        }

        afterEach { clearAllMocks() }

        should("response status should be 204 No Content") {
            expectResponse(status().isNoContent)
        }

        should("edit reception using caregiving manager from payload") {
            mockMvc.perform(request)

            verify {
                receptionEditingCommandHandler.editReception(
                    any(),
                    withArg {
                        it.caregivingManagerInfo shouldBe CaregivingManagerInfo(
                            organizationType = OrganizationType.INTERNAL,
                            organizationId = null,
                            managingUserId = "01GP2EK7XN2T9PK2Q262FXX5VA",
                        )
                    },
                )
            }
        }
    }

    context("when assigning external affiliated caregiving manager") {
        val request = put("/api/v1/receptions/01GPWNKNMTRT8PJXJHXVA921NQ")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                generatePutReceptionPayload(
                    caregivingManagerInfo = """
                        {
                            "organizationType": "AFFILIATED",
                            "organizationId": "01GR34Z09TK5DZK27HCM0FEV54",
                            "managingUserId": "01GRAPC8T3AYFDWEJHZ7F3NQ2E"
                        }
                    """.trimIndent()
                )
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            justRun {
                receptionEditingCommandHandler.editReception(
                    match { it.receptionId == "01GPWNKNMTRT8PJXJHXVA921NQ" },
                    any(),
                )
            }
        }

        afterEach { clearAllMocks() }

        should("response status should be 204 No Content") {
            expectResponse(status().isNoContent)
        }

        should("edit reception using caregiving manager from payload") {
            mockMvc.perform(request)

            verify {
                receptionEditingCommandHandler.editReception(
                    any(),
                    withArg {
                        it.caregivingManagerInfo shouldBe CaregivingManagerInfo(
                            organizationType = OrganizationType.AFFILIATED,
                            organizationId = "01GR34Z09TK5DZK27HCM0FEV54",
                            managingUserId = "01GRAPC8T3AYFDWEJHZ7F3NQ2E",
                        )
                    },
                )
            }
        }
    }

    context("when assigning external organization caregiving manager") {
        val request = put("/api/v1/receptions/01GPWNKNMTRT8PJXJHXVA921NQ")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                generatePutReceptionPayload(
                    caregivingManagerInfo = """
                        {
                            "organizationType": "ORGANIZATION",
                            "organizationId": "01GPWXVJB2WPDNXDT5NE3B964N",
                            "managingUserId": "01GRAPFA0M4RTGAE138CT6MWFT"
                        }
                    """.trimIndent()
                )
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            justRun {
                receptionEditingCommandHandler.editReception(
                    match { it.receptionId == "01GPWNKNMTRT8PJXJHXVA921NQ" },
                    any(),
                )
            }
        }

        afterEach { clearAllMocks() }

        should("response status should be 204 No Content") {
            expectResponse(status().isNoContent)
        }

        should("edit reception using caregiving manager from payload") {
            mockMvc.perform(request)

            verify {
                receptionEditingCommandHandler.editReception(
                    any(),
                    withArg {
                        it.caregivingManagerInfo shouldBe CaregivingManagerInfo(
                            organizationType = OrganizationType.ORGANIZATION,
                            organizationId = "01GPWXVJB2WPDNXDT5NE3B964N",
                            managingUserId = "01GRAPFA0M4RTGAE138CT6MWFT",
                        )
                    },
                )
            }
        }
    }

    context("평문 조회할 개인 정보를 지정하여 간병 접수를 조회하면") {
        val request = get("/api/v1/receptions/01GPJMK7ZPBBKTY3TP0NN5JWCJ")
            .queryParam("unmasked-property", "PATIENT_NAME")
            .queryParam("unmasked-property", "PATIENT_PRIMARY_PHONE_NUMBER")
            .queryParam("unmasked-property", "PATIENT_SECONDARY_PHONE_NUMBER")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            val mapperSlot = slot<(Reception) -> DetailReceptionResponse>()
            every {
                receptionByIdQueryHandler.getReception(
                    match { it.receptionId == "01GPJMK7ZPBBKTY3TP0NN5JWCJ" },
                    capture(mapperSlot)
                )
            } answers {
                mapperSlot.captured(
                    relaxedMock {
                        every { id } returns "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                        every { insuranceInfo } returns InsuranceInfo(
                            insuranceNumber = "11111-1111",
                            subscriptionDate = LocalDate.of(2022, 9, 1),
                            coverageId = "01GPD5EE21TGK5A5VCYWQ9Z73W",
                            caregivingLimitPeriod = 180,
                        )
                        every { patientInfo } returns EncryptedPatientInfo(
                            name = EncryptedPatientInfo.EncryptedPatientName(
                                "임석민",
                                LocalEncryption.patientNameHasher,
                                LocalEncryption.LocalEncryptor,
                            ),
                            nickname = "뽀리스",
                            age = 31,
                            sex = Sex.MALE,
                            height = null,
                            weight = null,
                            primaryContact = EncryptedPatientInfo.EncryptedContact(
                                partialEncryptedPhoneNumber = PartialEncryptedPhoneNumber.encrypt(
                                    "01011112222",
                                    LocalEncryption.LocalEncryptor,
                                ),
                                relationshipWithPatient = "본인",
                            ),
                            secondaryContact = EncryptedPatientInfo.EncryptedContact(
                                partialEncryptedPhoneNumber = PartialEncryptedPhoneNumber.encrypt(
                                    "01011113333",
                                    LocalEncryption.LocalEncryptor,
                                ),
                                relationshipWithPatient = "형제",
                            ),
                        )
                        every { accidentInfo } returns AccidentInfo(
                            accidentNumber = "2022-1111111",
                            accidentDateTime = LocalDateTime.of(2023, 1, 12, 0, 7, 55),
                            claimType = ClaimType.ACCIDENT,
                            patientDescription = "자력으로 호흡 불가능",
                            admissionDateTime = LocalDateTime.of(2023, 1, 12, 1, 3, 12),
                            hospitalAndRoomInfo = AccidentInfo.HospitalAndRoomInfo(
                                state = null,
                                city = null,
                                hospitalAndRoom = "케어닥 병원 304호실",
                            )
                        )
                        every { insuranceManagerInfo } returns InsuranceManagerInfo(
                            branchName = "메리츠 증권 평양 지점",
                            receptionistName = "김정은",
                            phoneNumber = "01011114444",
                        )
                        every { caregivingManagerInfo } returns CaregivingManagerInfo(
                            organizationType = OrganizationType.ORGANIZATION,
                            organizationId = "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                            managingUserId = "01GP2EK7XN2T9PK2Q262FXX5VA",
                        )
                        every { registerManagerInfo } returns RegisterManagerInfo(
                            managingUserId = "01GP2EK7XN2T9PK2Q262FXX5VA",
                        )
                        every { desiredCaregivingStartDate } returns LocalDate.of(2023, 1, 13)
                        every { urgency } returns Reception.Urgency.URGENT
                        every { desiredCaregivingPeriod } returns 180
                        every { additionalRequests } returns "몸무게가 많이 나가서 힘이 센분이여야 합니다."
                        every { expectedCaregivingLimitDate } returns LocalDate.of(2023, 7, 13)
                        every { progressingStatus } returns ReceptionProgressingStatus.MATCHING
                        every { periodType } returns Reception.PeriodType.NORMAL
                        every { receivedDateTime } returns LocalDateTime.of(2023, 1, 12, 4, 5, 22)
                        every { notifyCaregivingProgress } returns true

                        val decryptorSlot = slot<Decryptor>()
                        val subjectSlot = slot<Subject>()
                        val blockSlot = slot<Reception.DecryptionContext.() -> String>()
                        every {
                            inDecryptionContext(
                                capture(decryptorSlot),
                                capture(subjectSlot),
                                capture(blockSlot)
                            )
                        } answers {
                            blockSlot.captured(DecryptionContext(decryptorSlot.captured, subjectSlot.captured))
                        }
                    }
                )
            }

            mockkObject(ReceptionAccessPolicy)

            // 서브젝트 아규먼트 리졸빙이 모킹이 되지 않아 만든 미봉책, 추후 수정해야함.
            justRun { ReceptionAccessPolicy.check(any(), any(), any()) }
        }

        afterEach { clearAllMocks() }

        should("평문으로 복호화된 환자 정보를 포함하여 응답합니다.") {
            expectResponse(
                content().json(
                    """
                        {
                          "patientInfo": {
                            "name": "임석민",
                            "primaryContact": {
                              "phoneNumber": "01011112222",
                              "relationshipWithPatient": "본인"
                            },
                            "secondaryContact": {
                              "phoneNumber": "01011113333",
                              "relationshipWithPatient": "형제"
                            }
                          }
                        }
                    """.trimIndent()
                )
            )
        }
    }
})
