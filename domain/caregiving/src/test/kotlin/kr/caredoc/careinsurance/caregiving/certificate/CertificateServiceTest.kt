package kr.caredoc.careinsurance.caregiving.certificate

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kr.caredoc.careinsurance.LocalEncryption
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationByIdQueryHandler
import kr.caredoc.careinsurance.caregiving.CaregivingCharge
import kr.caredoc.careinsurance.caregiving.CaregivingChargeByCaregivingRoundIdQueryHandler
import kr.caredoc.careinsurance.caregiving.CaregivingProgressingStatus
import kr.caredoc.careinsurance.caregiving.CaregivingRound
import kr.caredoc.careinsurance.caregiving.CaregivingRoundByIdQueryHandler
import kr.caredoc.careinsurance.caregiving.CaregivingRoundsByReceptionIdQueryHandler
import kr.caredoc.careinsurance.caregiving.ClosingReasonType
import kr.caredoc.careinsurance.decryptableMockReception
import kr.caredoc.careinsurance.generateExternalCaregivingOrganizationManagerSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.patient.EncryptedPatientInfo
import kr.caredoc.careinsurance.patient.Sex
import kr.caredoc.careinsurance.phonenumber.PartialEncryptedPhoneNumber
import kr.caredoc.careinsurance.reception.ReceptionByIdQueryHandler
import kr.caredoc.careinsurance.relaxedMock
import org.springframework.security.access.AccessDeniedException
import java.time.LocalDate
import java.time.LocalDateTime

class CertificateServiceTest : BehaviorSpec({
    given("사용 확인서 서비스가 주어졌을때") {
        val receptionByIdQueryHandler = relaxedMock<ReceptionByIdQueryHandler>()
        val caregivingRoundByIdQueryHandler = relaxedMock<CaregivingRoundByIdQueryHandler>()
        val caregivingRoundsByReceptionIdQueryHandler = relaxedMock<CaregivingRoundsByReceptionIdQueryHandler>()
        val externalCaregivingOrganizationByIdQueryHandler =
            relaxedMock<ExternalCaregivingOrganizationByIdQueryHandler>()
        val caregivingChargeByCaregivingRoundIdQueryHandler =
            relaxedMock<CaregivingChargeByCaregivingRoundIdQueryHandler>()
        val decryptor = LocalEncryption.LocalDecryptor
        val service = CertificateService(
            receptionByIdQueryHandler = receptionByIdQueryHandler,
            caregivingRoundByIdQueryHandler = caregivingRoundByIdQueryHandler,
            caregivingRoundsByReceptionIdQueryHandler = caregivingRoundsByReceptionIdQueryHandler,
            externalCaregivingOrganizationByIdQueryHandler = externalCaregivingOrganizationByIdQueryHandler,
            caregivingChargeByCaregivingRoundIdQueryHandler = caregivingChargeByCaregivingRoundIdQueryHandler,
            decryptor = decryptor,
        )

        and("그리고 간병 접수와 간병 회차들이 주어졌을때") {
            fun generateCaregivingRoundMock(
                id: String,
                receptionId: String,
                roundNumber: Int,
                progressingStatus: CaregivingProgressingStatus,
                closingReasonType: ClosingReasonType,
            ) = relaxedMock<CaregivingRound> {
                every { this@relaxedMock.id } returns id
                every { caregivingProgressingStatus } returns progressingStatus
                every { caregivingRoundClosingReasonType } returns closingReasonType
                every { receptionInfo.receptionId } returns receptionId
                every { this@relaxedMock.caregivingRoundNumber } returns roundNumber
                every { caregiverInfo } returns relaxedMock {
                    every { name } returns "나간병"
                    every { birthDate } returns LocalDate.of(1959, 5, 2)
                    every { sex } returns Sex.FEMALE
                    every { phoneNumber } returns "01011112222"
                    every { caregiverOrganizationId } returns "01GPWXVJB2WPDNXDT5NE3B964N"
                    every { startDateTime } returns LocalDateTime.of(2023, 3, 16, 9, 21, 31)
                    every { endDateTime } returns LocalDateTime.of(2023, 3, 17, 9, 21, 31)
                }
            }

            val encryptedPatientPhoneNumber = relaxedMock<PartialEncryptedPhoneNumber>()
            val caregivingCharge = relaxedMock<CaregivingCharge>()

            beforeEach {
                val progressedCaregivingRound = generateCaregivingRoundMock(
                    id = "01GXCTR1Q7D26BK9A1FQ0EB5R0",
                    receptionId = "01GVQ7A3RSQQ6J36EB2SFMS82P",
                    progressingStatus = CaregivingProgressingStatus.COMPLETED,
                    closingReasonType = ClosingReasonType.FINISHED_CONTINUE,
                    roundNumber = 1,
                )
                val completedRestartingRound = generateCaregivingRoundMock(
                    id = "01GXDCW9FZ04M621A0MDTY0S14",
                    receptionId = "01GVQ7A3RSQQ6J36EB2SFMS82P",
                    progressingStatus = CaregivingProgressingStatus.COMPLETED,
                    closingReasonType = ClosingReasonType.FINISHED_RESTARTING,
                    roundNumber = 2,
                )
                val lastCaregivingRound = generateCaregivingRoundMock(
                    id = "01GXDC6772M11WKRGFA8M47EZ8",
                    receptionId = "01GVQ7A3RSQQ6J36EB2SFMS82P",
                    progressingStatus = CaregivingProgressingStatus.COMPLETED,
                    closingReasonType = ClosingReasonType.FINISHED_USING_PERSONAL_CAREGIVER,
                    roundNumber = 3,
                )

                val reception = decryptableMockReception {
                    every { id } returns "01GVQ7A3RSQQ6J36EB2SFMS82P"
                    every { accidentInfo.accidentNumber } returns "2023-1111111"
                    every { insuranceInfo.subscriptionDate } returns LocalDate.of(2022, 9, 1)
                    every { patientInfo.name } returns EncryptedPatientInfo.EncryptedPatientName(
                        "eddy",
                        LocalEncryption.patientNameHasher,
                        LocalEncryption.LocalEncryptor,
                    )
                    every { patientInfo.age } returns 38
                    every { patientInfo.sex } returns Sex.MALE
                    every { patientInfo.primaryContact.partialEncryptedPhoneNumber } returns encryptedPatientPhoneNumber
                    every { patientInfo.primaryContact.relationshipWithPatient } returns "가족"
                    every { accidentInfo.accidentDateTime } returns LocalDateTime.of(2023, 3, 14, 13, 3, 21)
                    every { accidentInfo.patientDescription } returns "콩팥 돌석 제거 (2~3 달전 진단)\n거동 가능, 식사 가능"
                    every { accidentInfo.hospitalAndRoomInfo.state } returns "경남"
                    every { accidentInfo.hospitalAndRoomInfo.city } returns "진주"
                    every { accidentInfo.hospitalAndRoomInfo.hospitalAndRoom } returns "경상대학교 병원"
                }

                every { encryptedPatientPhoneNumber.toString() } returns "01011111111"

                with(caregivingRoundByIdQueryHandler) {
                    every {
                        getCaregivingRound(match { it.caregivingRoundId == "01GXCTR1Q7D26BK9A1FQ0EB5R0" })
                    } returns progressedCaregivingRound
                    every {
                        getCaregivingRound(match { it.caregivingRoundId == "01GXDCW9FZ04M621A0MDTY0S14" })
                    } returns completedRestartingRound
                    every {
                        getCaregivingRound(match { it.caregivingRoundId == "01GXDC6772M11WKRGFA8M47EZ8" })
                    } returns lastCaregivingRound
                }

                every {
                    receptionByIdQueryHandler.getReception(
                        match {
                            it.receptionId == "01GVQ7A3RSQQ6J36EB2SFMS82P"
                        }
                    )
                } returns reception

                every {
                    caregivingRoundsByReceptionIdQueryHandler.getReceptionCaregivingRounds(
                        match {
                            it.receptionId == "01GVQ7A3RSQQ6J36EB2SFMS82P"
                        }
                    )
                } returns listOf(
                    lastCaregivingRound,
                    completedRestartingRound,
                    progressedCaregivingRound,
                )
                every {
                    externalCaregivingOrganizationByIdQueryHandler.getExternalCaregivingOrganization(
                        match {
                            it.id == "01GPWXVJB2WPDNXDT5NE3B964N"
                        }
                    )
                } returns relaxedMock {
                    every { name } returns "울산천사"
                    every { phoneNumber } returns "01011113333"
                }

                every {
                    caregivingChargeByCaregivingRoundIdQueryHandler.getCaregivingCharge(
                        match { it.caregivingRoundId == "01GXDC6772M11WKRGFA8M47EZ8" }
                    )
                } returns caregivingCharge

                every { caregivingCharge.isCancelAfterArrived } returns false

                mockkObject(CertificateTemplate)
            }

            afterEach {
                clearAllMocks()
                unmockkObject(CertificateTemplate)
            }

            `when`("계속건에 대한 사용 확인서 생성을 요청하면") {
                val query = CertificateByCaregivingRoundIdQuery(
                    caregivingRoundId = "01GXCTR1Q7D26BK9A1FQ0EB5R0",
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = service.getCertificate(query)

                then("사용확인서 생성을 위한 간병 회차 데이터를 조회합니다.") {
                    behavior()

                    verify {
                        caregivingRoundByIdQueryHandler.getCaregivingRound(
                            withArg {
                                it.caregivingRoundId shouldBe "01GXCTR1Q7D26BK9A1FQ0EB5R0"
                            }
                        )
                    }
                }

                then("사용확인서 생성을 위한 접수 데이터를 조회합니다.") {
                    behavior()

                    verify {
                        receptionByIdQueryHandler.getReception(
                            withArg {
                                it.receptionId shouldBe "01GVQ7A3RSQQ6J36EB2SFMS82P"
                            }
                        )
                    }
                }

                then("환자의 전화번호를 복호화 합니다.") {
                    behavior()

                    verify {
                        encryptedPatientPhoneNumber.decrypt(decryptor)
                    }
                }

                then("템플릿으로 사용확인서를 작성합니다.") {
                    behavior()

                    verify {
                        CertificateTemplate.generate(
                            withArg {
                                it.accidentNumber shouldBe "2023-1111111"
                                it.subscriptionDate shouldBe LocalDate.of(2022, 9, 1)
                                it.patientName shouldBe "eddy"
                                it.patientAge shouldBe 38
                                it.patientSex shouldBe Sex.MALE
                                it.patientPhoneNumber shouldBe "01011111111"
                                it.relationshipBetweenPatientAndPhoneOwner shouldBe "가족"
                                it.accidentDate shouldBe LocalDate.of(2023, 3, 14)
                                it.patientDescription shouldBe "콩팥 돌석 제거 (2~3 달전 진단)\n거동 가능, 식사 가능"
                                it.hospitalState shouldBe "경남"
                                it.hospitalCity shouldBe "진주"
                                it.hospitalAndRoom shouldBe "경상대학교 병원"
                                it.caregiverName shouldBe "나간병"
                                it.caregiverSex shouldBe Sex.FEMALE
                                it.caregiverPhoneNumber shouldBe "01011112222"
                                it.caregivingOrganizationName shouldBe "울산천사"
                                it.caregivingOrganizationPhoneNumber shouldBe "01011113333"
                                it.caregivingStartDateTime shouldBe LocalDateTime.of(2023, 3, 16, 9, 21, 31)
                                it.caregivingEndDateTime shouldBe LocalDateTime.of(2023, 3, 17, 9, 21, 31)
                                it.remarks shouldBe "간병 중"
                                it.continueWithSameCaregiver shouldBe true
                            }
                        )
                    }
                }
            }

            `when`("마지막 간병에 대한 사용 확인서 생성을 요청하면") {
                val query = CertificateByCaregivingRoundIdQuery(
                    caregivingRoundId = "01GXDC6772M11WKRGFA8M47EZ8",
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = service.getCertificate(query)

                then("특이 사항에 간병 종료로 기입하도록 사용 확인서를 작성합니다.") {
                    behavior()

                    verify {
                        CertificateTemplate.generate(
                            withArg {
                                it.remarks shouldBe "간병 종료(개인구인 안내)"
                            }
                        )
                    }
                }

                then("간병인 계속 사용 여부를 미사용으로 기입하도록 사용 확인서를 작성합니다.") {
                    behavior()

                    verify {
                        CertificateTemplate.generate(
                            withArg {
                                it.continueWithSameCaregiver shouldBe false
                            }
                        )
                    }
                }
            }

            `when`("마지막 간병이 도착후 취소로 종료된 간병의 사용 확인서 생성을 요청하면") {
                val query = CertificateByCaregivingRoundIdQuery(
                    caregivingRoundId = "01GXDC6772M11WKRGFA8M47EZ8",
                    subject = generateInternalCaregivingManagerSubject(),
                )

                beforeEach {
                    every { caregivingCharge.isCancelAfterArrived } returns true
                }

                afterEach { clearAllMocks() }

                fun behavior() = service.getCertificate(query)

                then("특이사항으로 '간병 종료(개인구인 안내) - 도착 후 취소'로 입력됩니다.") {
                    behavior()

                    verify {
                        CertificateTemplate.generate(
                            withArg {
                                it.remarks shouldBe "간병 종료(개인구인 안내) - 도착 후 취소"
                            }
                        )
                    }
                }
            }

            `when`("중단 계속건에 대한 사용 확인서 생성을 요청하면") {
                val query = CertificateByCaregivingRoundIdQuery(
                    caregivingRoundId = "01GXDCW9FZ04M621A0MDTY0S14",
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = service.getCertificate(query)

                then("특이 사항에 간병 종료로 기입하도록 사용 확인서를 작성합니다.") {
                    behavior()

                    verify {
                        CertificateTemplate.generate(
                            withArg {
                                it.remarks shouldBe "간병 중(중단-계속)"
                            }
                        )
                    }
                }
            }

            `when`("내부 사용자 권한 없이 사용 확인서 생성을 요청하면") {
                val query = CertificateByCaregivingRoundIdQuery(
                    caregivingRoundId = "01GXCTR1Q7D26BK9A1FQ0EB5R0",
                    subject = generateExternalCaregivingOrganizationManagerSubject("01GRWDRWTM6ENFSQXTDN9HDDWK"),
                )

                fun behavior() = service.getCertificate(query)

                then("AccessDeniedException이 발생합니다.") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }
        }
    }
})
