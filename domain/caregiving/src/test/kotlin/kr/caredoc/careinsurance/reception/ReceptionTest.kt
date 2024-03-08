package kr.caredoc.careinsurance.reception

import com.github.guepardoapps.kulid.ULID
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import kr.caredoc.careinsurance.LocalEncryption
import kr.caredoc.careinsurance.decodeHex
import kr.caredoc.careinsurance.generateExternalCaregivingOrganizationManagerSubject
import kr.caredoc.careinsurance.generateGuestSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.insurance.InsuranceInfo
import kr.caredoc.careinsurance.insurance.InsuranceManagerInfo
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.patient.PatientInfo
import kr.caredoc.careinsurance.patient.Sex
import kr.caredoc.careinsurance.security.accesscontrol.ObjectAttribute
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.security.personaldata.PatientInfoEncryptor
import kr.caredoc.careinsurance.withFixedClock
import org.springframework.security.access.AccessDeniedException
import java.time.LocalDate
import java.time.LocalDateTime

class ReceptionTest : BehaviorSpec({
    val encryptor = LocalEncryption.LocalEncryptor
    val decryptor = LocalEncryption.LocalDecryptor
    val patientInfoEncryptor = PatientInfoEncryptor(encryptor, LocalEncryption.patientNameHasher)

    given("reception creating arguments") {
        val id = "01GWVQRD04F6213P3QS3VCWJPA"
        val insuranceInfo = InsuranceInfo(
            insuranceNumber = "11111-1111",
            subscriptionDate = LocalDate.of(2022, 9, 1),
            coverageId = "01GPD5EE21TGK5A5VCYWQ9Z73W",
            caregivingLimitPeriod = 180,
        )
        val patientInfo = patientInfoEncryptor.encrypt(
            PatientInfo(
                name = "임석민",
                nickname = "뽀리스",
                age = 31,
                sex = Sex.MALE,
                height = null,
                weight = null,
                primaryContact = PatientInfo.Contact(
                    phoneNumber = "01011112222",
                    relationshipWithPatient = "본인",
                ),
                secondaryContact = PatientInfo.Contact(
                    phoneNumber = "01011113333",
                    relationshipWithPatient = "형제",
                )
            )
        )
        val accidentInfo = AccidentInfo(
            accidentNumber = "2022-1111111",
            accidentDateTime = LocalDateTime.of(2023, 1, 12, 18, 7, 55),
            claimType = ClaimType.ACCIDENT,
            patientDescription = "자력으로 호흡 불가능",
            admissionDateTime = LocalDateTime.of(2023, 1, 12, 19, 3, 12),
            hospitalAndRoomInfo = AccidentInfo.HospitalAndRoomInfo(
                state = null,
                city = null,
                hospitalAndRoom = "케어닥 병원 304호실",
            ),
        )
        val insuranceManagerInfo = InsuranceManagerInfo(
            branchName = "메리츠 증권 평양 지점",
            receptionistName = "김정은",
            phoneNumber = "01011114444",
        )
        val registerManagerInfo = RegisterManagerInfo(
            managingUserId = "01GP2EK7XN2T9PK2Q262FXX5VA",
        )
        val receivedDateTime = LocalDateTime.of(2023, 1, 12, 12, 30, 45)
        val desiredCaregivingStartDate = LocalDate.of(2023, 1, 13)
        val urgency = Reception.Urgency.URGENT
        val desiredCaregivingPeriod = 180
        val additionalRequests = "몸무게가 많이 나가서 힘이 센분이여야 합니다."

        `when`("creating reception") {
            fun behavior() = Reception(
                id = id,
                insuranceInfo = insuranceInfo,
                patientInfo = patientInfo,
                accidentInfo = accidentInfo,
                insuranceManagerInfo = insuranceManagerInfo,
                receivedDateTime = receivedDateTime,
                desiredCaregivingStartDate = desiredCaregivingStartDate,
                urgency = urgency,
                desiredCaregivingPeriod = desiredCaregivingPeriod,
                registerManagerInfo = registerManagerInfo,
                additionalRequests = additionalRequests,
                notifyCaregivingProgress = true,
            )

            then("expectedCaregivingLimitDate should be calculated using receive date time and caregiving limit period") {
                val actualResult = withFixedClock(LocalDateTime.of(2023, 1, 12, 20, 3, 12)) {
                    behavior()
                }

                // 2023년 1월 12일로부터 180일 경과
                actualResult.expectedCaregivingLimitDate shouldBe LocalDate.of(2023, 7, 11)
            }

            then("ReceptionReceived 이벤트가 발생합니다.") {
                val actualResult = withFixedClock(LocalDateTime.of(2023, 1, 12, 20, 3, 12)) {
                    behavior()
                }

                val occurredEvent = actualResult.domainEvents.find { it is ReceptionReceived } as ReceptionReceived

                occurredEvent.receptionId shouldBe "01GWVQRD04F6213P3QS3VCWJPA"
                occurredEvent.receivedDateTime shouldBe LocalDateTime.of(2023, 1, 12, 12, 30, 45)
                occurredEvent.desiredCaregivingStartDate shouldBe LocalDate.of(2023, 1, 13)
                occurredEvent.urgency shouldBe Reception.Urgency.URGENT
                occurredEvent.periodType shouldBe Reception.PeriodType.NORMAL
            }

            then("암호화된 환자 이름은 복호화 가능한 형태로 저장됩니다.") {
                val actualResult = behavior()

                decryptor.decryptAsString(actualResult.patientInfo.name.encrypted.decodeHex()) shouldBe "임석민"
            }

            then("마스킹된 환자 이름이 등록됩니다.") {
                val actualResult = behavior()

                actualResult.patientInfo.name.masked shouldBe "임*민"
            }
        }
    }

    given("reception") {
        lateinit var reception: Reception

        beforeEach {
            reception = Reception(
                id = ULID.random(),
                insuranceInfo = InsuranceInfo(
                    insuranceNumber = "11111-1111",
                    subscriptionDate = LocalDate.of(2022, 9, 1),
                    coverageId = "01GPD5EE21TGK5A5VCYWQ9Z73W",
                    caregivingLimitPeriod = 180,
                ),
                patientInfo = patientInfoEncryptor.encrypt(
                    PatientInfo(
                        name = "임석민",
                        nickname = "뽀리스",
                        age = 31,
                        sex = Sex.MALE,
                        height = null,
                        weight = null,
                        primaryContact = PatientInfo.Contact(
                            phoneNumber = "01011112222",
                            relationshipWithPatient = "본인",
                        ),
                        secondaryContact = PatientInfo.Contact(
                            phoneNumber = "01011113333",
                            relationshipWithPatient = "형제",
                        )
                    )
                ),
                accidentInfo = AccidentInfo(
                    accidentNumber = "2022-1111111",
                    accidentDateTime = LocalDateTime.of(2023, 1, 12, 18, 7, 55),
                    claimType = ClaimType.ACCIDENT,
                    patientDescription = "자력으로 호흡 불가능",
                    admissionDateTime = LocalDateTime.of(2023, 1, 12, 19, 3, 12),
                    hospitalAndRoomInfo = AccidentInfo.HospitalAndRoomInfo(
                        state = null,
                        city = null,
                        hospitalAndRoom = "케어닥 병원 304호실",
                    ),
                ),
                insuranceManagerInfo = InsuranceManagerInfo(
                    branchName = "메리츠 증권 평양 지점",
                    receptionistName = "김정은",
                    phoneNumber = "01011114444",
                ),
                registerManagerInfo = RegisterManagerInfo(
                    managingUserId = "01GP2EK7XN2T9PK2Q262FXX5VA",
                ),
                receivedDateTime = LocalDateTime.of(2023, 1, 12, 12, 30, 45),
                desiredCaregivingStartDate = LocalDate.of(2023, 1, 13),
                urgency = Reception.Urgency.URGENT,
                desiredCaregivingPeriod = 180,
                notifyCaregivingProgress = true,
                additionalRequests = "몸무게가 많이 나가서 힘이 센분이여야 합니다.",
            )
        }

        afterEach { clearAllMocks() }

        fun generateReceptionEditingCommand(
            subject: Subject = generateInternalCaregivingManagerSubject(),
            progressStatus: ReceptionProgressingStatus = ReceptionProgressingStatus.RECEIVED,
            reasonForCancellation: String? = null,
        ) =
            ReceptionEditingCommand(
                insuranceInfo = InsuranceInfo(
                    insuranceNumber = "22222-1111",
                    subscriptionDate = LocalDate.of(2022, 9, 2),
                    coverageId = "01GR65F9DVHBRK25YMRXNRTXET",
                    caregivingLimitPeriod = 90,
                ),
                patientInfo = PatientInfo(
                    name = "방미영",
                    nickname = "레나",
                    age = 41,
                    sex = Sex.FEMALE,
                    height = 173,
                    weight = 77,
                    primaryContact = PatientInfo.Contact(
                        phoneNumber = "01011113333",
                        relationshipWithPatient = "장본인",
                    ),
                    secondaryContact = PatientInfo.Contact(
                        phoneNumber = "01011114444",
                        relationshipWithPatient = "자매",
                    ),
                ),
                accidentInfo = AccidentInfo(
                    accidentNumber = "2022-2222222",
                    accidentDateTime = LocalDateTime.of(2023, 1, 12, 18, 7, 56),
                    claimType = ClaimType.SICKNESS,
                    patientDescription = "자력으로 식사 불가능",
                    admissionDateTime = LocalDateTime.of(2023, 1, 12, 19, 3, 13),
                    hospitalAndRoomInfo = AccidentInfo.HospitalAndRoomInfo(
                        state = "서울턱별시",
                        city = "갱냄구",
                        hospitalAndRoom = "케어닥 병원 305호실",
                    )
                ),
                caregivingManagerInfo = CaregivingManagerInfo(
                    organizationType = OrganizationType.AFFILIATED,
                    organizationId = "01GPWXVJB2WPDNXDT5NE3B964N",
                    managingUserId = "01GR8BNHFPYQW55PNGKHAKBNS6",
                ),
                desiredCaregivingStartDate = LocalDate.of(2023, 1, 14),
                desiredCaregivingPeriod = 3,
                additionalRequests = "",
                expectedCaregivingLimitDate = LocalDate.of(2023, 1, 14),
                progressingStatus = progressStatus,
                reasonForCancellation = reasonForCancellation,
                notifyCaregivingProgress = true,
                expectedCaregivingStartDate = null,
                subject = subject,
            )

        `when`("editing reception") {
            val command = generateReceptionEditingCommand()

            fun behavior() = reception.inEncryptionContext(
                patientInfoEncryptor,
                decryptor,
            ) {
                edit(command)
            }

            then("reception metadata should be changed") {
                behavior()

                reception.insuranceInfo shouldBe InsuranceInfo(
                    insuranceNumber = "22222-1111",
                    subscriptionDate = LocalDate.of(2022, 9, 2),
                    coverageId = "01GR65F9DVHBRK25YMRXNRTXET",
                    caregivingLimitPeriod = 90,
                )
                reception.patientInfo.name.masked shouldBe "방*영"
                reception.patientInfo.nickname shouldBe "레나"
                reception.patientInfo.age shouldBe 41
                reception.patientInfo.sex shouldBe Sex.FEMALE
                reception.patientInfo.height shouldBe 173
                reception.patientInfo.weight shouldBe 77
                reception.accidentInfo shouldBe AccidentInfo(
                    accidentNumber = "2022-2222222",
                    accidentDateTime = LocalDateTime.of(2023, 1, 12, 18, 7, 56),
                    claimType = ClaimType.SICKNESS,
                    patientDescription = "자력으로 식사 불가능",
                    admissionDateTime = LocalDateTime.of(2023, 1, 12, 19, 3, 13),
                    hospitalAndRoomInfo = AccidentInfo.HospitalAndRoomInfo(
                        state = "서울턱별시",
                        city = "갱냄구",
                        hospitalAndRoom = "케어닥 병원 305호실",
                    )
                )
                reception.caregivingManagerInfo shouldBe CaregivingManagerInfo(
                    organizationType = OrganizationType.AFFILIATED,
                    organizationId = "01GPWXVJB2WPDNXDT5NE3B964N",
                    managingUserId = "01GR8BNHFPYQW55PNGKHAKBNS6",
                )
                reception.desiredCaregivingStartDate shouldBe LocalDate.of(2023, 1, 14)
                reception.desiredCaregivingPeriod shouldBe 3
                reception.additionalRequests shouldBe ""
                reception.expectedCaregivingLimitDate shouldBe LocalDate.of(2023, 1, 14)
                reception.notifyCaregivingProgress shouldBe true
            }

            then("변경된 접수 정보가 변경되었음을 이벤트로 알립니다.") {
                behavior()
                val occurredEvent = reception.domainEvents.find { it is ReceptionModified } as ReceptionModified

                occurredEvent.receptionId shouldBe reception.id
                occurredEvent.insuranceInfo.current shouldBe InsuranceInfo(
                    insuranceNumber = "22222-1111",
                    subscriptionDate = LocalDate.of(2022, 9, 2),
                    coverageId = "01GR65F9DVHBRK25YMRXNRTXET",
                    caregivingLimitPeriod = 90,
                )
                occurredEvent.accidentInfo.current shouldBe AccidentInfo(
                    accidentNumber = "2022-2222222",
                    accidentDateTime = LocalDateTime.of(2023, 1, 12, 18, 7, 56),
                    claimType = ClaimType.SICKNESS,
                    patientDescription = "자력으로 식사 불가능",
                    admissionDateTime = LocalDateTime.of(2023, 1, 12, 19, 3, 13),
                    hospitalAndRoomInfo = AccidentInfo.HospitalAndRoomInfo(
                        state = "서울턱별시",
                        city = "갱냄구",
                        hospitalAndRoom = "케어닥 병원 305호실",
                    )
                )
                occurredEvent.patientInfo.current.name.masked shouldBe "방*영"
                occurredEvent.patientInfo.current.nickname shouldBe "레나"
                occurredEvent.patientInfo.current.age shouldBe 41
                occurredEvent.patientInfo.current.sex shouldBe Sex.FEMALE
                occurredEvent.patientInfo.current.height shouldBe 173
                occurredEvent.patientInfo.current.weight shouldBe 77
                occurredEvent.expectedCaregivingStartDate.current shouldBe null
                occurredEvent.progressingStatus shouldBe Modification(
                    ReceptionProgressingStatus.RECEIVED,
                    ReceptionProgressingStatus.RECEIVED
                )
                occurredEvent.caregivingManagerInfo.current shouldBe CaregivingManagerInfo(
                    organizationType = OrganizationType.AFFILIATED,
                    organizationId = "01GPWXVJB2WPDNXDT5NE3B964N",
                    managingUserId = "01GR8BNHFPYQW55PNGKHAKBNS6",
                )
                occurredEvent.periodType shouldBe Modification(Reception.PeriodType.NORMAL, Reception.PeriodType.SHORT)
                occurredEvent.desiredCaregivingStartDate shouldBe Modification(
                    LocalDate.of(2023, 1, 13),
                    LocalDate.of(2023, 1, 14)
                )
            }

            and("접수 등록 완료 처리일 때") {
                `when`("보류상태로 변경하면") {
                    fun behavior() = reception.inEncryptionContext(
                        patientInfoEncryptor,
                        decryptor,
                    ) {
                        edit(generateReceptionEditingCommand(progressStatus = ReceptionProgressingStatus.PENDING))
                    }

                    then("간병 접수의 상태가 보류 상태로 변경됩니다.") {
                        reception.progressingStatus shouldBe ReceptionProgressingStatus.RECEIVED

                        behavior()

                        reception.progressingStatus shouldBe ReceptionProgressingStatus.PENDING
                    }
                }
                `when`("매칭상태로 변경하면") {
                    fun behavior() = reception.inEncryptionContext(
                        patientInfoEncryptor,
                        decryptor,
                    ) {
                        edit(generateReceptionEditingCommand(progressStatus = ReceptionProgressingStatus.MATCHING))
                    }

                    then("간병 접수의 상태가 매칭 상태로 변경됩니다.") {
                        reception.progressingStatus shouldBe ReceptionProgressingStatus.RECEIVED

                        behavior()

                        reception.progressingStatus shouldBe ReceptionProgressingStatus.MATCHING
                    }
                }

                `when`("접수 취소로 변경하면") {
                    fun behavior() = reception.inEncryptionContext(
                        patientInfoEncryptor,
                        decryptor,
                    ) {
                        edit(generateReceptionEditingCommand(progressStatus = ReceptionProgressingStatus.CANCELED))
                    }

                    then("간병 접수의 상태가 접수 취소 상태로 변경됩니다.") {
                        reception.progressingStatus shouldBe ReceptionProgressingStatus.RECEIVED

                        behavior()

                        reception.progressingStatus shouldBe ReceptionProgressingStatus.CANCELED
                    }
                }

                `when`("개인 구인 취소로 변경하면") {
                    fun behavior() = reception.inEncryptionContext(
                        patientInfoEncryptor,
                        decryptor,
                    ) {
                        edit(generateReceptionEditingCommand(progressStatus = ReceptionProgressingStatus.CANCELED_BY_PERSONAL_CAREGIVER))
                    }

                    then("간병 접수의 상태가 개인 구인 상태로 변경됩니다.") {
                        reception.progressingStatus shouldBe ReceptionProgressingStatus.RECEIVED

                        behavior()

                        reception.progressingStatus shouldBe ReceptionProgressingStatus.CANCELED_BY_PERSONAL_CAREGIVER
                    }
                }

                `when`("개인 구인 취소(의료)로 변경하면") {
                    fun behavior() = reception.inEncryptionContext(
                        patientInfoEncryptor,
                        decryptor,
                    ) {
                        edit(generateReceptionEditingCommand(progressStatus = ReceptionProgressingStatus.CANCELED_BY_MEDICAL_REQUEST))
                    }

                    then("간병 접수의 상태가 개인 구인 취소(의료) 상태로 변경됩니다.") {
                        reception.progressingStatus shouldBe ReceptionProgressingStatus.RECEIVED

                        behavior()

                        reception.progressingStatus shouldBe ReceptionProgressingStatus.CANCELED_BY_MEDICAL_REQUEST
                    }
                }
            }

            and("간병인이 등록되어 있을때") {
                beforeEach {
                    reception.inEncryptionContext(
                        patientInfoEncryptor,
                        decryptor,
                    ) {
                        edit(generateReceptionEditingCommand(progressStatus = ReceptionProgressingStatus.MATCHING))
                    }
                }

                afterEach { clearAllMocks() }

                `when`("매칭 보류로 변경하면") {
                    fun behavior() = reception.inEncryptionContext(
                        patientInfoEncryptor,
                        decryptor,
                    ) {
                        edit(generateReceptionEditingCommand(progressStatus = ReceptionProgressingStatus.PENDING_MATCHING))
                    }

                    then("간병 접수의 상태가 매칭 보류 상태로 변경됩니다.") {
                        reception.progressingStatus shouldBe ReceptionProgressingStatus.MATCHING

                        behavior()

                        reception.progressingStatus shouldBe ReceptionProgressingStatus.PENDING_MATCHING
                    }
                }
            }

            and("간병 접수가 취소 가능한 상태일 때") {
                `when`("취소 사유와 함께 간병을 취소하면") {
                    fun behavior() = reception.inEncryptionContext(
                        patientInfoEncryptor,
                        decryptor,
                    ) {
                        edit(
                            generateReceptionEditingCommand(
                                progressStatus = ReceptionProgressingStatus.CANCELED,
                                reasonForCancellation = "마법같이 회복함"
                            )
                        )
                    }

                    then("간병 상태가 취소로 변경됩니다.") {
                        reception.progressingStatus shouldBe ReceptionProgressingStatus.RECEIVED

                        behavior()

                        reception.progressingStatus shouldBe ReceptionProgressingStatus.CANCELED
                    }

                    then("간병 취소 사유가 기록됩니다.") {
                        reception.reasonForCancellation shouldBe null

                        behavior()

                        reception.reasonForCancellation shouldBe "마법같이 회복함"
                    }

                    then("간병 취소 일자가 기록됩니다.") {
                        reception.canceledDateTime shouldBe null

                        withFixedClock(LocalDateTime.of(2023, 7, 10, 9, 30, 0)) {
                            behavior()
                        }

                        reception.canceledDateTime shouldBe LocalDateTime.of(2023, 7, 10, 9, 30, 0)
                    }
                }

                `when`("취소 사유와 함께 간병을 개인구인 취소하면") {
                    fun behavior() = reception.inEncryptionContext(
                        patientInfoEncryptor,
                        decryptor,
                    ) {
                        edit(
                            generateReceptionEditingCommand(
                                progressStatus = ReceptionProgressingStatus.CANCELED_BY_PERSONAL_CAREGIVER,
                                reasonForCancellation = "단순 변심"
                            )
                        )
                    }

                    then("간병 상태가 개인구인 취소로 변경됩니다.") {
                        reception.progressingStatus shouldBe ReceptionProgressingStatus.RECEIVED

                        behavior()

                        reception.progressingStatus shouldBe ReceptionProgressingStatus.CANCELED_BY_PERSONAL_CAREGIVER
                    }

                    then("간병 취소 사유가 기록됩니다.") {
                        reception.reasonForCancellation shouldBe null

                        behavior()

                        reception.reasonForCancellation shouldBe "단순 변심"
                    }

                    then("간병 취소 일자가 기록됩니다.") {
                        reception.canceledDateTime shouldBe null

                        withFixedClock(LocalDateTime.of(2023, 7, 11, 9, 30, 0)) {
                            behavior()
                        }

                        reception.canceledDateTime shouldBe LocalDateTime.of(2023, 7, 11, 9, 30, 0)
                    }
                }

                `when`("취소 사유와 함께 간병을 개인구인(의료) 취소하면") {
                    fun behavior() = reception.inEncryptionContext(
                        patientInfoEncryptor,
                        decryptor,
                    ) {
                        edit(
                            generateReceptionEditingCommand(
                                progressStatus = ReceptionProgressingStatus.CANCELED_BY_MEDICAL_REQUEST,
                                reasonForCancellation = "석션 불가"
                            )
                        )
                    }

                    then("간병 상태가 개인구인(의료) 취소로 변경됩니다.") {
                        reception.progressingStatus shouldBe ReceptionProgressingStatus.RECEIVED

                        behavior()

                        reception.progressingStatus shouldBe ReceptionProgressingStatus.CANCELED_BY_MEDICAL_REQUEST
                    }

                    then("간병 취소 사유가 기록됩니다.") {
                        reception.reasonForCancellation shouldBe null

                        behavior()

                        reception.reasonForCancellation shouldBe "석션 불가"
                    }

                    then("간병 취소 일자가 기록됩니다.") {
                        reception.canceledDateTime shouldBe null

                        withFixedClock(LocalDateTime.of(2023, 7, 12, 9, 30, 0)) {
                            behavior()
                        }

                        reception.canceledDateTime shouldBe LocalDateTime.of(2023, 7, 12, 9, 30, 0)
                    }
                }
            }
        }

        `when`("editing reception without user attribute") {
            val command = generateReceptionEditingCommand(
                subject = generateGuestSubject()
            )

            fun behavior() = reception.inEncryptionContext(
                patientInfoEncryptor,
                decryptor,
            ) {
                edit(command)
            }

            then("should throws AccessDeniedException") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }

        `when`("getting ASSIGNED_ORGANIZATION_ID attribute") {
            fun behavior() = reception[ObjectAttribute.ASSIGNED_ORGANIZATION_ID]
            then("returns empty set") {
                behavior() shouldBe setOf()
            }
        }

        `when`("간병인 신청서의 정보 반영 요청이 생기면") {
            val fileName = "temp.pdf"
            val url =
                "https://careinsurance-reception-application-dev.s3.ap-northeast-2.amazonaws.com/01H2YZAAVAP2WG20BBE4VPSW94"

            fun behavior() = reception.updateReceptionApplicationFileInfo(
                ReceptionApplicationFileInfo(
                    receptionApplicationFileName = fileName,
                    receptionApplicationFileUrl = url,
                ),
                generateInternalCaregivingManagerSubject(),
            )

            then("간병인 신청서의 파일 이름과 url을 저장합니다.") {
                behavior()

                reception.applicationFileInfo shouldBe ReceptionApplicationFileInfo(fileName, url)
            }

            then("ReceptionModified 이벤트가 등록됩니다.") {
                behavior()

                val occurredEvent = reception.domainEvents.find { it is ReceptionModified } as ReceptionModified
                occurredEvent.applicationFileInfo.previous shouldBe null
                occurredEvent.applicationFileInfo.current?.receptionApplicationFileUrl shouldBe "https://careinsurance-reception-application-dev.s3.ap-northeast-2.amazonaws.com/01H2YZAAVAP2WG20BBE4VPSW94"
                occurredEvent.applicationFileInfo.current?.receptionApplicationFileName shouldBe "temp.pdf"
            }
        }
    }

    given("reception what allocated to external organization") {
        lateinit var reception: Reception

        beforeEach {
            reception = withFixedClock(LocalDateTime.of(2023, 1, 12, 20, 3, 12)) {
                Reception(
                    id = ULID.random(),
                    insuranceInfo = InsuranceInfo(
                        insuranceNumber = "11111-1111",
                        subscriptionDate = LocalDate.of(2022, 9, 1),
                        coverageId = "01GPD5EE21TGK5A5VCYWQ9Z73W",
                        caregivingLimitPeriod = 180,
                    ),
                    patientInfo = patientInfoEncryptor.encrypt(
                        PatientInfo(
                            name = "임석민",
                            nickname = "뽀리스",
                            age = 31,
                            sex = Sex.MALE,
                            height = null,
                            weight = null,
                            primaryContact = PatientInfo.Contact(
                                phoneNumber = "01011112222",
                                relationshipWithPatient = "본인",
                            ),
                            secondaryContact = PatientInfo.Contact(
                                phoneNumber = "01011113333",
                                relationshipWithPatient = "형제",
                            )
                        )
                    ),
                    accidentInfo = AccidentInfo(
                        accidentNumber = "2022-1111111",
                        accidentDateTime = LocalDateTime.of(2023, 1, 12, 18, 7, 55),
                        claimType = ClaimType.ACCIDENT,
                        patientDescription = "자력으로 호흡 불가능",
                        admissionDateTime = LocalDateTime.of(2023, 1, 12, 19, 3, 12),
                        hospitalAndRoomInfo = AccidentInfo.HospitalAndRoomInfo(
                            state = null,
                            city = null,
                            hospitalAndRoom = "케어닥 병원 304호실",
                        ),
                    ),
                    insuranceManagerInfo = InsuranceManagerInfo(
                        branchName = "메리츠 증권 평양 지점",
                        receptionistName = "김정은",
                        phoneNumber = "01011114444",
                    ),
                    registerManagerInfo = RegisterManagerInfo(
                        managingUserId = "01GP2EK7XN2T9PK2Q262FXX5VA",
                    ),
                    caregivingManagerInfo = CaregivingManagerInfo(
                        organizationType = OrganizationType.AFFILIATED,
                        organizationId = "01GPWXVJB2WPDNXDT5NE3B964N",
                        managingUserId = "01GR8BNHFPYQW55PNGKHAKBNS6",
                    ),
                    receivedDateTime = LocalDateTime.of(2023, 1, 12, 12, 30, 45),
                    desiredCaregivingStartDate = LocalDate.of(2023, 1, 11),
                    urgency = Reception.Urgency.URGENT,
                    desiredCaregivingPeriod = 180,
                    additionalRequests = "몸무게가 많이 나가서 힘이 센분이여야 합니다.",
                    notifyCaregivingProgress = true,
                )
            }
        }

        afterEach { clearAllMocks() }

        fun generateReceptionEditingCommand(
            name: String = "임석민",
            subject: Subject,
        ) =
            ReceptionEditingCommand(
                insuranceInfo = InsuranceInfo(
                    insuranceNumber = "11111-1111",
                    subscriptionDate = LocalDate.of(2022, 9, 1),
                    coverageId = "01GPD5EE21TGK5A5VCYWQ9Z73W",
                    caregivingLimitPeriod = 180,
                ),
                patientInfo = PatientInfo(
                    name = name,
                    nickname = "뽀리스",
                    age = 31,
                    sex = Sex.MALE,
                    height = 173,
                    weight = 77,
                    primaryContact = PatientInfo.Contact(
                        phoneNumber = "01011112222",
                        relationshipWithPatient = "본인",
                    ),
                    secondaryContact = PatientInfo.Contact(
                        phoneNumber = "01011113333",
                        relationshipWithPatient = "형제",
                    )
                ),
                accidentInfo = AccidentInfo(
                    accidentNumber = "2022-1111111",
                    accidentDateTime = LocalDateTime.of(2023, 1, 12, 18, 7, 55),
                    claimType = ClaimType.ACCIDENT,
                    patientDescription = "자력으로 식사 불가능",
                    admissionDateTime = LocalDateTime.of(2023, 1, 12, 19, 3, 13),
                    hospitalAndRoomInfo = AccidentInfo.HospitalAndRoomInfo(
                        state = "서울턱별시",
                        city = "갱냄구",
                        hospitalAndRoom = "케어닥 병원 305호실",
                    )
                ),
                caregivingManagerInfo = CaregivingManagerInfo(
                    organizationType = OrganizationType.AFFILIATED,
                    organizationId = "01GPWXVJB2WPDNXDT5NE3B964N",
                    managingUserId = "01GR8BNHFPYQW55PNGKHAKBNS6",
                ),
                desiredCaregivingStartDate = LocalDate.of(2023, 1, 14),
                desiredCaregivingPeriod = 90,
                additionalRequests = "",
                expectedCaregivingLimitDate = LocalDate.of(2023, 7, 11),
                progressingStatus = ReceptionProgressingStatus.RECEIVED,
                reasonForCancellation = null,
                notifyCaregivingProgress = true,
                expectedCaregivingStartDate = null,
                subject = subject,
            )

        `when`("editing reception with external organization user attribute") {
            val command = generateReceptionEditingCommand(
                subject = generateExternalCaregivingOrganizationManagerSubject(
                    "01GPWXVJB2WPDNXDT5NE3B964N"
                ),
            )

            fun behavior() = reception.inEncryptionContext(
                patientInfoEncryptor,
                decryptor,
            ) {
                edit(command)
            }

            then("reception metadata should be changed") {
                behavior()

                reception.patientInfo.nickname shouldBe "뽀리스"
                reception.patientInfo.height shouldBe 173
                reception.patientInfo.weight shouldBe 77
                reception.accidentInfo.admissionDateTime shouldBe LocalDateTime.of(2023, 1, 12, 19, 3, 13)
                reception.accidentInfo.hospitalAndRoomInfo shouldBe AccidentInfo.HospitalAndRoomInfo(
                    state = "서울턱별시",
                    city = "갱냄구",
                    hospitalAndRoom = "케어닥 병원 305호실",
                )
                reception.accidentInfo.patientDescription shouldBe "자력으로 식사 불가능"
                reception.desiredCaregivingStartDate shouldBe LocalDate.of(2023, 1, 14)
                reception.desiredCaregivingPeriod shouldBe 90
                reception.additionalRequests shouldBe ""
            }
        }

        `when`("editing reception with another external organization user attribute") {
            val command = generateReceptionEditingCommand(
                subject = generateExternalCaregivingOrganizationManagerSubject(
                    "01GR8CKF9Y8AN0HNJX5PPYQ445"
                ),
            )

            fun behavior() = reception.inEncryptionContext(
                patientInfoEncryptor,
                decryptor,
            ) {
                edit(command)
            }

            then("reception metadata should be changed") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }

        `when`("editing reception without external organization user attribute") {
            val command = generateReceptionEditingCommand(
                subject = generateGuestSubject()
            )

            fun behavior() = reception.inEncryptionContext(
                patientInfoEncryptor,
                decryptor,
            ) {
                edit(command)
            }

            then("should throws AccessDeniedException") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }

        `when`("editing reception including not allowed property with external organization user attribute") {
            val command = generateReceptionEditingCommand(
                name = "방미영",
                subject = generateExternalCaregivingOrganizationManagerSubject(
                    "01GPWXVJB2WPDNXDT5NE3B964N"
                ),
            )

            fun behavior() = reception.inEncryptionContext(
                patientInfoEncryptor,
                decryptor,
            ) {
                edit(command)
            }

            then("should throws AccessDeniedException") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }

        `when`("getting ASSIGNED_ORGANIZATION_ID attribute") {
            fun behavior() = reception[ObjectAttribute.ASSIGNED_ORGANIZATION_ID]
            then("returns assigned organization id") {
                behavior() shouldBe setOf("01GPWXVJB2WPDNXDT5NE3B964N")
            }
        }
    }

    given("reception what already assigned to caregiving manager") {
        val reception = Reception(
            id = ULID.random(),
            insuranceInfo = InsuranceInfo(
                insuranceNumber = "11111-1111",
                subscriptionDate = LocalDate.of(2022, 9, 1),
                coverageId = "01GPD5EE21TGK5A5VCYWQ9Z73W",
                caregivingLimitPeriod = 180,
            ),
            patientInfo = patientInfoEncryptor.encrypt(
                PatientInfo(
                    name = "임석민",
                    nickname = "뽀리스",
                    age = 31,
                    sex = Sex.MALE,
                    height = null,
                    weight = null,
                    primaryContact = PatientInfo.Contact(
                        phoneNumber = "01011112222",
                        relationshipWithPatient = "본인",
                    ),
                    secondaryContact = PatientInfo.Contact(
                        phoneNumber = "01011113333",
                        relationshipWithPatient = "형제",
                    )
                )
            ),
            accidentInfo = AccidentInfo(
                accidentNumber = "2022-1111111",
                accidentDateTime = LocalDateTime.of(2023, 1, 12, 18, 7, 55),
                claimType = ClaimType.ACCIDENT,
                patientDescription = "자력으로 호흡 불가능",
                admissionDateTime = LocalDateTime.of(2023, 1, 12, 19, 3, 12),
                hospitalAndRoomInfo = AccidentInfo.HospitalAndRoomInfo(
                    state = null,
                    city = null,
                    hospitalAndRoom = "케어닥 병원 304호실",
                ),
            ),
            caregivingManagerInfo = CaregivingManagerInfo(
                organizationType = OrganizationType.AFFILIATED,
                organizationId = "01GPWXVJB2WPDNXDT5NE3B964N",
                managingUserId = "01GR8BNHFPYQW55PNGKHAKBNS6",
            ),
            insuranceManagerInfo = InsuranceManagerInfo(
                branchName = "메리츠 증권 평양 지점",
                receptionistName = "김정은",
                phoneNumber = "01011114444",
            ),
            registerManagerInfo = RegisterManagerInfo(
                managingUserId = "01GP2EK7XN2T9PK2Q262FXX5VA",
            ),
            receivedDateTime = LocalDateTime.of(2023, 1, 12, 12, 30, 45),
            desiredCaregivingStartDate = LocalDate.of(2023, 1, 13),
            urgency = Reception.Urgency.URGENT,
            desiredCaregivingPeriod = 180,
            additionalRequests = "몸무게가 많이 나가서 힘이 센분이여야 합니다.",
            notifyCaregivingProgress = true,
        )

        `when`("editing another allowed property") {
            val command = ReceptionEditingCommand(
                insuranceInfo = InsuranceInfo(
                    insuranceNumber = "22222-1111",
                    subscriptionDate = LocalDate.of(2022, 9, 2),
                    coverageId = "01GR65F9DVHBRK25YMRXNRTXET",
                    caregivingLimitPeriod = 90,
                ),
                patientInfo = PatientInfo(
                    name = "방미영",
                    nickname = "",
                    age = 41,
                    sex = Sex.FEMALE,
                    height = 173,
                    weight = 77,
                    primaryContact = PatientInfo.Contact(
                        phoneNumber = "01011113333",
                        relationshipWithPatient = "장본인",
                    ),
                    secondaryContact = PatientInfo.Contact(
                        phoneNumber = "01011114444",
                        relationshipWithPatient = "자매",
                    ),
                ),
                accidentInfo = AccidentInfo(
                    accidentNumber = "2022-2222222",
                    accidentDateTime = LocalDateTime.of(2023, 1, 12, 18, 7, 56),
                    claimType = ClaimType.SICKNESS,
                    patientDescription = "자력으로 식사 불가능",
                    admissionDateTime = LocalDateTime.of(2023, 1, 12, 19, 3, 13),
                    hospitalAndRoomInfo = AccidentInfo.HospitalAndRoomInfo(
                        state = "서울턱별시",
                        city = "갱냄구",
                        hospitalAndRoom = "케어닥 병원 305호실",
                    )
                ),
                caregivingManagerInfo = CaregivingManagerInfo(
                    organizationType = OrganizationType.AFFILIATED,
                    organizationId = "01GPWXVJB2WPDNXDT5NE3B964N",
                    managingUserId = "01GR8BNHFPYQW55PNGKHAKBNS6",
                ),
                desiredCaregivingStartDate = LocalDate.of(2023, 1, 14),
                desiredCaregivingPeriod = 90,
                additionalRequests = "",
                expectedCaregivingLimitDate = LocalDate.of(2023, 1, 14),
                progressingStatus = ReceptionProgressingStatus.RECEIVED,
                reasonForCancellation = null,
                notifyCaregivingProgress = true,
                expectedCaregivingStartDate = LocalDate.of(2023, 1, 14),
                subject = generateInternalCaregivingManagerSubject(),
            )

            fun behavior() = reception.inEncryptionContext(
                patientInfoEncryptor,
                decryptor,
            ) {
                edit(command)
            }

            then("property should be changed") {
                behavior()

                reception.expectedCaregivingLimitDate shouldBe LocalDate.of(2023, 1, 14)
                reception.expectedCaregivingStartDate shouldBe LocalDate.of(2023, 1, 14)
            }
        }
    }

    given("접수 보류 상태의 접수가 주어졌을때") {
        lateinit var reception: Reception
        beforeEach {
            reception = Reception(
                id = ULID.random(),
                insuranceInfo = InsuranceInfo(
                    insuranceNumber = "11111-1111",
                    subscriptionDate = LocalDate.of(2022, 9, 1),
                    coverageId = "01GPD5EE21TGK5A5VCYWQ9Z73W",
                    caregivingLimitPeriod = 180,
                ),
                patientInfo = patientInfoEncryptor.encrypt(
                    PatientInfo(
                        name = "임석민",
                        nickname = "뽀리스",
                        age = 31,
                        sex = Sex.MALE,
                        height = null,
                        weight = null,
                        primaryContact = PatientInfo.Contact(
                            phoneNumber = "01011112222",
                            relationshipWithPatient = "본인",
                        ),
                        secondaryContact = PatientInfo.Contact(
                            phoneNumber = "01011113333",
                            relationshipWithPatient = "형제",
                        )
                    )
                ),
                accidentInfo = AccidentInfo(
                    accidentNumber = "2022-1111111",
                    accidentDateTime = LocalDateTime.of(2023, 1, 12, 18, 7, 55),
                    claimType = ClaimType.ACCIDENT,
                    patientDescription = "자력으로 호흡 불가능",
                    admissionDateTime = LocalDateTime.of(2023, 1, 12, 19, 3, 12),
                    hospitalAndRoomInfo = AccidentInfo.HospitalAndRoomInfo(
                        state = null,
                        city = null,
                        hospitalAndRoom = "케어닥 병원 304호실",
                    ),
                ),
                caregivingManagerInfo = CaregivingManagerInfo(
                    organizationType = OrganizationType.AFFILIATED,
                    organizationId = "01GPWXVJB2WPDNXDT5NE3B964N",
                    managingUserId = "01GR8BNHFPYQW55PNGKHAKBNS6",
                ),
                insuranceManagerInfo = InsuranceManagerInfo(
                    branchName = "메리츠 증권 평양 지점",
                    receptionistName = "김정은",
                    phoneNumber = "01011114444",
                ),
                registerManagerInfo = RegisterManagerInfo(
                    managingUserId = "01GP2EK7XN2T9PK2Q262FXX5VA",
                ),
                receivedDateTime = LocalDateTime.of(2023, 1, 12, 12, 30, 45),
                desiredCaregivingStartDate = LocalDate.of(2023, 1, 13),
                urgency = Reception.Urgency.URGENT,
                desiredCaregivingPeriod = 180,
                additionalRequests = "몸무게가 많이 나가서 힘이 센분이여야 합니다.",
                notifyCaregivingProgress = true,
            )

            reception.editProgressingStatus(
                ReceptionProgressingStatus.PENDING,
                generateInternalCaregivingManagerSubject()
            )
        }

        afterEach { clearAllMocks() }

        `when`("접수를 매칭 상태로 진행하면") {
            fun behavior() = reception.editProgressingStatus(
                ReceptionProgressingStatus.MATCHING,
                generateInternalCaregivingManagerSubject(),
            )

            then("접수 상태가 MATCHING으로 변경됩니다.") {
                reception.progressingStatus shouldBe ReceptionProgressingStatus.PENDING

                behavior()

                reception.progressingStatus shouldBe ReceptionProgressingStatus.MATCHING
            }
        }
    }
})
