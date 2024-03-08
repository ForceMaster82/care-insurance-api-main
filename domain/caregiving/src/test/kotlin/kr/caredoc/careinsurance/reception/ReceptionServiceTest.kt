package kr.caredoc.careinsurance.reception

import com.github.guepardoapps.kulid.ULID
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import kr.caredoc.careinsurance.LocalEncryption
import kr.caredoc.careinsurance.coverage.CoverageByIdQueryHandler
import kr.caredoc.careinsurance.coverage.CoverageNotFoundByIdException
import kr.caredoc.careinsurance.file.FileByUrlQueryHandler
import kr.caredoc.careinsurance.file.FileSavingCommandHandler
import kr.caredoc.careinsurance.generateExternalCaregivingOrganizationManagerSubject
import kr.caredoc.careinsurance.generateGuestSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.generateSystemSubject
import kr.caredoc.careinsurance.insurance.InsuranceInfo
import kr.caredoc.careinsurance.insurance.InsuranceManagerInfo
import kr.caredoc.careinsurance.patient.PatientInfo
import kr.caredoc.careinsurance.patient.Sex
import kr.caredoc.careinsurance.reception.exception.ReceptionApplicationNotFoundException
import kr.caredoc.careinsurance.reception.exception.ReceptionNotFoundByIdException
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.search.SearchCondition
import kr.caredoc.careinsurance.security.accesscontrol.ObjectAttribute
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.security.personaldata.PatientInfoEncryptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.AccessDeniedException
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
class ReceptionServiceTest(
    @Autowired
    private val cacheReceptionRepository: ReceptionRepository
) : BehaviorSpec({
    fun generateReceptionCreationCommand(subject: Subject = generateInternalCaregivingManagerSubject()) =
        ReceptionCreationCommand(
            InsuranceInfo(
                insuranceNumber = "11111-1111",
                subscriptionDate = LocalDate.of(2022, 9, 1),
                coverageId = "01GPD5EE21TGK5A5VCYWQ9Z73W",
                caregivingLimitPeriod = 180,
            ),
            PatientInfo(
                name = "임석민",
                nickname = "뽀리스",
                age = 31,
                sex = Sex.MALE,
                weight = null,
                height = null,
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
                patientDescription = "자력으로 호흡 불가능",
                admissionDateTime = LocalDateTime.of(2023, 1, 12, 19, 3, 12),
                hospitalAndRoomInfo = AccidentInfo.HospitalAndRoomInfo(
                    state = null,
                    city = null,
                    hospitalAndRoom = "케어닥 병원 304호실"
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
            additionalRequests = "몸무게가 많이 나가서 힘이 센분이여야 합니다.",
            notifyCaregivingProgress = true,
            subject = subject,
        )

    given("reception service") {
        val coverageByIdQueryHandler = relaxedMock<CoverageByIdQueryHandler>()
        val receptionRepository = relaxedMock<ReceptionRepository>()
        val fileSavingCommandHandler = relaxedMock<FileSavingCommandHandler>()
        val fileByUrlQueryHandler = relaxedMock<FileByUrlQueryHandler>()
        val encryptor = LocalEncryption.LocalEncryptor
        val decryptor = LocalEncryption.LocalDecryptor
        val patientInfoEncryptor = PatientInfoEncryptor(encryptor, LocalEncryption.patientNameHasher)
        val receptionService =
            ReceptionService(
                receptionRepository,
                coverageByIdQueryHandler,
                fileSavingCommandHandler,
                fileByUrlQueryHandler,
                patientInfoEncryptor,
                decryptor,
                receptionApplicationBucket = "careinsurance-reception-application-dev"
            )

        beforeEach {
            with(receptionRepository) {
                val persistingEntitySlot = slot<Reception>()
                every {
                    save(capture(persistingEntitySlot))
                } answers {
                    persistingEntitySlot.captured
                }

                every { findByIdOrNull(any()) } returns null
                every { findByIdIn(any()) } returns listOf()
            }
        }

        afterEach { clearAllMocks() }

        `when`("creating reception") {
            beforeEach {
                mockkObject(ULID)
                every { ULID.random() } returns "01GPWNKNMTRT8PJXJHXVA921NQ"
            }

            afterEach { clearAllMocks() }

            val command = generateReceptionCreationCommand()

            fun behavior() = receptionService.createReception(command)

            then("persist reception entity") {
                behavior()

                verify {
                    receptionRepository.save(
                        withArg {
                            it.id shouldBe "01GPWNKNMTRT8PJXJHXVA921NQ"
                            it.insuranceInfo.insuranceNumber shouldBe "11111-1111"
                            it.insuranceInfo.subscriptionDate shouldBe LocalDate.of(2022, 9, 1)
                            it.insuranceInfo.coverageId shouldBe "01GPD5EE21TGK5A5VCYWQ9Z73W"
                            it.insuranceInfo.caregivingLimitPeriod shouldBe 180
                            it.patientInfo.nickname shouldBe "뽀리스"
                            it.patientInfo.age shouldBe 31
                            it.patientInfo.sex shouldBe Sex.MALE
                            it.patientInfo.primaryContact.relationshipWithPatient shouldBe "본인"
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
                            it.desiredCaregivingStartDate shouldBe LocalDate.of(2023, 1, 13)
                            it.urgency shouldBe Reception.Urgency.URGENT
                            it.desiredCaregivingPeriod shouldBe 180
                            it.additionalRequests shouldBe "몸무게가 많이 나가서 힘이 센분이여야 합니다."
                            it.notifyCaregivingProgress shouldBe true
                        }
                    )
                }
            }

            then("returns creation result") {
                val actualResult = behavior()

                actualResult.createdReceptionId shouldBe "01GPWNKNMTRT8PJXJHXVA921NQ"
            }

            and("but coverage not exists") {
                beforeEach {
                    every {
                        coverageByIdQueryHandler.ensureCoverageExists(
                            match {
                                it.coverageId == "01GPD5EE21TGK5A5VCYWQ9Z73W"
                            }
                        )
                    } throws CoverageNotFoundByIdException("01GPD5EE21TGK5A5VCYWQ9Z73W")
                }

                afterEach { clearAllMocks() }

                then("throws ReferenceCoverageNotExistsException") {
                    val thrownException = shouldThrow<ReferenceCoverageNotExistsException> { behavior() }

                    thrownException.referenceCoverageId shouldBe "01GPD5EE21TGK5A5VCYWQ9Z73W"
                    thrownException.cause!!.shouldBeInstanceOf<CoverageNotFoundByIdException>()
                }
            }
        }

        `when`("creating reception without internal user attribute") {
            val command = generateReceptionCreationCommand(subject = generateGuestSubject())

            fun behavior() = receptionService.createReception(command)

            then("throws AccessDeniedException") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }

        `when`("getting single reception") {
            val query = ReceptionByIdQuery(
                receptionId = "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                subject = generateInternalCaregivingManagerSubject()
            )

            fun behavior() = receptionService.getReception(query)

            then("should throws ReceptionNotFoundByIdException") {
                shouldThrow<ReceptionNotFoundByIdException> { behavior() }
            }
        }

        and("receptions") {
            val receptions = listOf<Reception>(relaxedMock(), relaxedMock())
            val receptionsInEncryptionContext =
                listOf<Reception.ReceptionInEncryptionContext>(relaxedMock(), relaxedMock())

            beforeEach {
                with(receptionRepository) {
                    val pageableSlot = slot<Pageable>()
                    every {
                        searchReceptions(any(), capture(pageableSlot))
                    } answers {
                        PageImpl(
                            receptions,
                            pageableSlot.captured,
                            receptions.size.toLong(),
                        )
                    }

                    every { findByIdOrNull("01GPJMK7ZPBBKTY3TP0NN5JWCJ") } returns receptions[0]

                    val idCollectionSlot = slot<Collection<String>>()
                    every { findByIdIn(capture(idCollectionSlot)) } answers {
                        val capturedIdCollection = idCollectionSlot.captured

                        val result = mutableListOf<Reception>()

                        if (capturedIdCollection.contains("01GPJMK7ZPBBKTY3TP0NN5JWCJ")) {
                            result.add(receptions[0])
                        }
                        if (capturedIdCollection.contains("01GSC4SKPGWMZDP7EKWQ0Z57NG")) {
                            result.add(receptions[1])
                        }

                        result
                    }
                }

                with(receptions[0]) {
                    every { id } returns "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                    val encryptionBlockSlot = slot<Reception.ReceptionInEncryptionContext.() -> Unit>()
                    every { inEncryptionContext(any(), any(), capture(encryptionBlockSlot)) } answers {
                        encryptionBlockSlot.captured(receptionsInEncryptionContext[0])
                    }

                    every { get(ObjectAttribute.ASSIGNED_ORGANIZATION_ID) } returns setOf("01GR34Z09TK5DZK27HCM0FEV54")
                }

                with(receptions[1]) {
                    every { id } returns "01GSC4SKPGWMZDP7EKWQ0Z57NG"
                }
            }

            afterEach { clearAllMocks() }

            `when`("getting receptions by filters") {
                val query = ReceptionsByFilterQuery(
                    from = LocalDate.of(2022, 1, 1),
                    until = LocalDate.of(2022, 1, 31),
                    urgency = Reception.Urgency.URGENT,
                    periodType = Reception.PeriodType.SHORT,
                    caregivingManagerAssigned = true,
                    organizationType = OrganizationType.AFFILIATED,
                    progressingStatuses = setOf(
                        ReceptionProgressingStatus.CANCELED,
                        ReceptionProgressingStatus.CANCELED_BY_MEDICAL_REQUEST,
                        ReceptionProgressingStatus.CANCELED_BY_PERSONAL_CAREGIVER
                    ),
                    searchCondition = SearchCondition(
                        searchingProperty = ReceptionsByFilterQuery.SearchingProperty.PATIENT_NAME,
                        keyword = "보리스",
                    ),
                    subject = generateInternalCaregivingManagerSubject(),
                )
                val pageRequest = PageRequest.of(0, 10)

                fun behavior() = receptionService.getReceptions(query, pageRequest)

                then("returns paged query result") {
                    val actualResult = behavior()

                    actualResult.totalElements shouldBe 2
                    actualResult.totalPages shouldBe 1
                    actualResult.pageable.pageNumber shouldBe 0
                    actualResult.content shouldContainExactly receptions
                }

                then("query receptions using reception repository") {
                    behavior()

                    verify {
                        receptionRepository.searchReceptions(
                            withArg {
                                it.from shouldBe LocalDate.of(2022, 1, 1)
                                it.until shouldBe LocalDate.of(2022, 1, 31)
                                it.urgency shouldBe Reception.Urgency.URGENT
                                it.periodType shouldBe Reception.PeriodType.SHORT
                                it.organizationType shouldBe OrganizationType.AFFILIATED
                                it.progressingStatuses shouldContainExactlyInAnyOrder setOf(
                                    ReceptionProgressingStatus.CANCELED,
                                    ReceptionProgressingStatus.CANCELED_BY_MEDICAL_REQUEST,
                                    ReceptionProgressingStatus.CANCELED_BY_PERSONAL_CAREGIVER,
                                )
                                it.patientNameContains shouldBe "보리스"
                            },
                            pageable = withArg {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 10
                            }
                        )
                    }
                }
            }

            `when`("searching receptions by caregiving manager name") {
                val query = ReceptionsByFilterQuery(
                    from = LocalDate.of(2022, 1, 1),
                    until = LocalDate.of(2022, 1, 31),
                    urgency = null,
                    periodType = null,
                    caregivingManagerAssigned = true,
                    organizationType = null,
                    progressingStatuses = setOf(),
                    searchCondition = SearchCondition(
                        searchingProperty = ReceptionsByFilterQuery.SearchingProperty.CAREGIVING_MANAGER_NAME,
                        keyword = "보리스",
                    ),
                    subject = generateInternalCaregivingManagerSubject(),
                )
                val pageRequest = PageRequest.of(0, 10)

                fun behavior() = receptionService.getReceptions(query, pageRequest)

                afterEach { clearAllMocks() }

                then("query receptions by caregiving manager name") {
                    behavior()

                    verify {
                        receptionRepository.searchReceptions(
                            withArg {
                                it.from shouldBe LocalDate.of(2022, 1, 1)
                                it.until shouldBe LocalDate.of(2022, 1, 31)
                                it.managerNameContains shouldBe "보리스"
                            },
                            pageable = withArg {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 10
                            }
                        )
                    }
                }
            }

            `when`("searching receptions by caregiving manager not assigned") {
                val query = ReceptionsByFilterQuery(
                    from = LocalDate.of(2022, 1, 1),
                    until = LocalDate.of(2022, 1, 31),
                    urgency = null,
                    periodType = null,
                    caregivingManagerAssigned = false,
                    organizationType = null,
                    progressingStatuses = setOf(),
                    searchCondition = null,
                    subject = generateInternalCaregivingManagerSubject(),
                )
                val pageRequest = PageRequest.of(0, 10)

                fun behavior() = receptionService.getReceptions(query, pageRequest)

                then("query receptions using reception repository") {
                    behavior()

                    verify {
                        receptionRepository.searchReceptions(
                            withArg {
                                it.from shouldBe LocalDate.of(2022, 1, 1)
                                it.until shouldBe LocalDate.of(2022, 1, 31)
                                it.caregivingManagerAssigned shouldBe false
                            },
                            pageable = withArg {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 10
                            }
                        )
                    }
                }
            }

            `when`("getting receptions by filters without internal user attribute") {
                val query = ReceptionsByFilterQuery(
                    from = LocalDate.of(2022, 1, 1),
                    until = LocalDate.of(2022, 1, 31),
                    urgency = Reception.Urgency.URGENT,
                    periodType = Reception.PeriodType.SHORT,
                    caregivingManagerAssigned = true,
                    organizationType = OrganizationType.AFFILIATED,
                    progressingStatuses = setOf(
                        ReceptionProgressingStatus.CANCELED,
                        ReceptionProgressingStatus.CANCELED_BY_MEDICAL_REQUEST,
                        ReceptionProgressingStatus.CANCELED_BY_PERSONAL_CAREGIVER
                    ),
                    searchCondition = null,
                    subject = generateGuestSubject(),
                )
                val pageRequest = PageRequest.of(0, 10)

                fun behavior() = receptionService.getReceptions(query, pageRequest)

                then("throws AccessDeniedException") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }

            `when`("getting single reception") {
                val query = ReceptionByIdQuery(
                    receptionId = "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                    subject = generateInternalCaregivingManagerSubject()
                )

                fun behavior() = receptionService.getReception(query)

                then("returns queried reception") {
                    val actualResult = behavior()

                    actualResult shouldBe receptions[0]
                }
            }

            `when`("시스템 사용자가 단일 접수를 조회하면") {
                val query = ReceptionByIdQuery(
                    receptionId = "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                    subject = generateSystemSubject()
                )

                fun behavior() = receptionService.getReception(query)

                then("아무런 예외도 발생하지 않습니다.") {
                    shouldNotThrowAny { behavior() }
                }
            }

            `when`("내부 사용자가 접수가 존재하는지 확인하면") {
                val query = ReceptionByIdQuery(
                    receptionId = "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                    subject = generateInternalCaregivingManagerSubject()
                )

                fun behavior() = receptionService.ensureReceptionExists(query)

                then("아무런 예외도 발생하지 않습니다.") {
                    shouldNotThrowAny { behavior() }
                }
            }

            `when`("시스템 사용자가 접수가 존재하는지 확인하면") {
                val query = ReceptionByIdQuery(
                    receptionId = "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                    subject = generateSystemSubject()
                )

                fun behavior() = receptionService.ensureReceptionExists(query)

                then("아무런 예외도 발생하지 않습니다.") {
                    shouldNotThrowAny { behavior() }
                }
            }

            `when`("접수를 관리하는 외부 협회 직원이 접수가 존재하는지 확인하면") {
                val query = ReceptionByIdQuery(
                    receptionId = "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                    subject = generateExternalCaregivingOrganizationManagerSubject("01GR34Z09TK5DZK27HCM0FEV54")
                )

                fun behavior() = receptionService.ensureReceptionExists(query)

                then("아무런 예외도 발생하지 않습니다.") {
                    shouldNotThrowAny { behavior() }
                }
            }

            `when`("접수와 관계 없는 외부 협회 직원이 접수가 존재하는지 확인하면") {
                val query = ReceptionByIdQuery(
                    receptionId = "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                    subject = generateExternalCaregivingOrganizationManagerSubject("01GVG3PH3S592T2CZFCV0TRQP0")
                )

                fun behavior() = receptionService.ensureReceptionExists(query)

                then("AccessDeniedException이 발생합니다.") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }

            `when`("editing reception that specified by id") {
                val query = ReceptionByIdQuery(
                    receptionId = "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                    subject = generateInternalCaregivingManagerSubject()
                )

                val command = ReceptionEditingCommand(
                    insuranceInfo = InsuranceInfo(
                        insuranceNumber = "11111-1111",
                        subscriptionDate = LocalDate.of(2022, 9, 1),
                        coverageId = "01GPD5EE21TGK5A5VCYWQ9Z73W",
                        caregivingLimitPeriod = 180,
                    ),
                    patientInfo = PatientInfo(
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
                    ),
                    accidentInfo = AccidentInfo(
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
                    ),
                    caregivingManagerInfo = CaregivingManagerInfo(
                        organizationType = OrganizationType.AFFILIATED,
                        organizationId = "01GR34Z09TK5DZK27HCM0FEV54",
                        managingUserId = "01GRAPC8T3AYFDWEJHZ7F3NQ2E",
                    ),
                    desiredCaregivingStartDate = LocalDate.of(2023, 1, 13),
                    desiredCaregivingPeriod = 180,
                    additionalRequests = "몸무게가 많이 나가서 힘이 센분이여야 합니다.",
                    expectedCaregivingLimitDate = LocalDate.of(2023, 7, 13),
                    progressingStatus = ReceptionProgressingStatus.PENDING,
                    reasonForCancellation = null,
                    notifyCaregivingProgress = true,
                    expectedCaregivingStartDate = null,
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = receptionService.editReception(query, command)

                then("edit reception entity using command") {
                    behavior()

                    verify {
                        receptionsInEncryptionContext[0].edit(
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
                                it.caregivingManagerInfo shouldBe CaregivingManagerInfo(
                                    organizationType = OrganizationType.AFFILIATED,
                                    organizationId = "01GR34Z09TK5DZK27HCM0FEV54",
                                    managingUserId = "01GRAPC8T3AYFDWEJHZ7F3NQ2E",
                                )
                                it.desiredCaregivingStartDate shouldBe LocalDate.of(2023, 1, 13)
                                it.desiredCaregivingPeriod shouldBe 180
                                it.additionalRequests shouldBe "몸무게가 많이 나가서 힘이 센분이여야 합니다."
                                it.expectedCaregivingLimitDate shouldBe LocalDate.of(2023, 7, 13)
                                it.notifyCaregivingProgress shouldBe true
                            },
                        )
                    }
                }
            }

            `when`("접수 아이디 목록으로 접수 목록을 조회하면") {
                val query = ReceptionsByIdsQuery(
                    receptionIds = setOf(
                        "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                        "01GSC4SKPGWMZDP7EKWQ0Z57NG",
                    ),
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = receptionService.getReceptions(query)

                then("리포지토리로부터 접수 목록을 조회합니다.") {
                    behavior()

                    verify {
                        receptionRepository.findByIdIn(
                            withArg {
                                it shouldContainAll setOf(
                                    "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                                    "01GSC4SKPGWMZDP7EKWQ0Z57NG",
                                )
                            }
                        )
                    }
                }

                then("조회된 접수 목록을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult shouldContainAll setOf(receptions[0], receptions[1])
                }
            }
        }

        `when`("접수 아이디 목록으로 접수 목록을 조회하면") {
            val query = ReceptionsByIdsQuery(
                receptionIds = setOf(
                    "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                ),
                subject = generateInternalCaregivingManagerSubject(),
            )

            fun behavior() = receptionService.getReceptions(query)

            then("ReceptionNotFoundByIdException이 발생합니다.") {
                val thrownException = shouldThrow<ReceptionNotFoundByIdException> { behavior() }

                thrownException.receptionId shouldBe "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
            }
        }

        `when`("간병인 신청서를 업로드하면") {
            val receptionId = "01H2YT3FWVFVZGW0AZPXS33G4J"
            val subject = generateInternalCaregivingManagerSubject()
            val bucketName = "careinsurance-reception-application-dev"

            val reception = relaxedMock<Reception>()
            val file = byteArrayOf(1, 2, 3).inputStream()

            beforeEach {
                mockkObject(ULID)
                every { ULID.random() } returns "01H2YZAAVAP2WG20BBE4VPSW94"

                every { receptionRepository.findByIdOrNull(match { it == receptionId }) } returns reception

                every { fileSavingCommandHandler.saveFile(any()) } returns relaxedMock {
                    every { savedFileUrl } returns "https://careinsurance-reception-application-dev.s3.ap-northeast-2.amazonaws.com/01H2YZAAVAP2WG20BBE4VPSW94"
                }
            }

            afterEach { clearAllMocks() }

            fun behavior() = receptionService.createReceptionApplication(
                ReceptionApplicationCreationCommand(
                    receptionId = receptionId,
                    fileName = "간병인 신청서.pdf",
                    file = file,
                    contentLength = file.available().toLong(),
                    mime = "application/pdf",
                    subject = subject
                )
            )
            then("간병 접수를 조회합니다.") {
                behavior()

                verify {
                    receptionRepository.findByIdOrNull(
                        withArg {
                            it shouldBe receptionId
                        }
                    )
                }
            }

            then("s3에 파일 업로드를 요청합니다.") {
                behavior()

                verify {
                    fileSavingCommandHandler.saveFile(
                        withArg {
                            it.bucketName shouldBe bucketName
                            it.path shouldBe "01H2YZAAVAP2WG20BBE4VPSW94"
                            it.fileStream shouldBe file
                            it.contentLength shouldBe 3
                            it.mime shouldBe "application/pdf"
                        }
                    )
                }
            }

            then("내용을 반영을 요청합니다.") {
                behavior()

                verify {
                    reception.updateReceptionApplicationFileInfo(
                        withArg {
                            it.receptionApplicationFileName shouldBe "간병인 신청서.pdf"
                            it.receptionApplicationFileUrl shouldBe "https://careinsurance-reception-application-dev.s3.ap-northeast-2.amazonaws.com/01H2YZAAVAP2WG20BBE4VPSW94"
                        },
                        subject,
                    )
                }
            }

            `when`("기존에 간병인 신청서가 있다면") {

                beforeEach {
                    every { reception.applicationFileInfo?.receptionApplicationFileUrl } returns "https://careinsurance-reception-application-dev.s3.ap-northeast-2.amazonaws.com/01H3E0D4RV0A672BMWWVEXZGTC"
                }

                afterEach { clearAllMocks() }

                then("간병 접수를 조회합니다.") {
                    behavior()

                    verify {
                        receptionRepository.findByIdOrNull(
                            withArg {
                                it shouldBe receptionId
                            }
                        )
                    }
                }

                then("s3에 파일 업로드를 요청합니다.") {
                    behavior()

                    verify {
                        fileSavingCommandHandler.saveFile(
                            withArg {
                                it.bucketName shouldBe bucketName
                                it.path shouldBe "01H2YZAAVAP2WG20BBE4VPSW94"
                                it.fileStream shouldBe file
                                it.contentLength shouldBe 3
                                it.mime shouldBe "application/pdf"
                            }
                        )
                    }
                }

                then("내용을 반영을 요청합니다.") {
                    behavior()

                    verify {
                        reception.updateReceptionApplicationFileInfo(
                            withArg {
                                it.receptionApplicationFileName shouldBe "간병인 신청서.pdf"
                                it.receptionApplicationFileUrl shouldBe "https://careinsurance-reception-application-dev.s3.ap-northeast-2.amazonaws.com/01H2YZAAVAP2WG20BBE4VPSW94"
                            },
                            subject,
                        )
                    }
                }

                then("s3에 기존 파일 삭제를 요청합니다.") {
                    behavior()

                    verify {
                        fileByUrlQueryHandler.deleteFile(
                            withArg {
                                it.url shouldBe "https://careinsurance-reception-application-dev.s3.ap-northeast-2.amazonaws.com/01H3E0D4RV0A672BMWWVEXZGTC"
                            }
                        )
                    }
                }
            }
        }

        `when`("간병인 신청서를 조회하면") {
            val receptionId = "01H3GSMTZPJ5S7ZR4CZK0JH6RZ"
            val subject = generateInternalCaregivingManagerSubject()
            val fileUrl =
                "https://careinsurance-reception-application-dev.s3.ap-northeast-2.amazonaws.com/01H3GTXJ4MJ21KTXS60YTQB2HP"

            val reception = relaxedMock<Reception>()
            beforeEach {
                every { receptionRepository.findByIdOrNull(match { it == receptionId }) } returns reception

                every { reception.applicationFileInfo } returns ReceptionApplicationFileInfo(
                    fileUrl,
                    "간병인 신청서.pdf",
                )
            }

            afterEach { clearAllMocks() }

            fun behavior() = receptionService.getReceptionApplication(
                ReceptionApplicationByReceptionIdQuery(
                    receptionId,
                    subject,
                )
            )
            then("간병 접수를 조회합니다.") {
                behavior()

                verify {
                    receptionRepository.findByIdOrNull(
                        withArg {
                            it shouldBe receptionId
                        }
                    )
                }
            }

            and("간병 접수가 없다면") {
                beforeEach {
                    every { receptionRepository.findByIdOrNull(match { it == receptionId }) } returns null
                }

                afterEach { clearAllMocks() }

                then("ReceptionNotFoundByIdException 발생합니다.") {
                    val thrownException = shouldThrow<ReceptionNotFoundByIdException> { behavior() }

                    thrownException.receptionId shouldBe receptionId
                }
            }

            and("간병인 신청서가 없다면") {
                beforeEach {
                    every { reception.applicationFileInfo } returns null
                }

                afterEach { clearAllMocks() }

                then("ReceptionApplicationNotFoundException 발생합니다.") {
                    val thrownException = shouldThrow<ReceptionApplicationNotFoundException> { behavior() }

                    thrownException.receptionId shouldBe receptionId
                }
            }
        }

        `when`("간병인 신청서를 삭제하면") {
            val receptionId = "01H3TC1KPB42R7Z88R3XFAV8CJ"
            val subject = generateInternalCaregivingManagerSubject()
            val fileUrl = "https://careinsurance-reception-application-dev.s3.ap-northeast-2.amazonaws.com/$receptionId"

            val reception = relaxedMock<Reception>()
            beforeEach {
                every { receptionRepository.findByIdOrNull(match { it == receptionId }) } returns reception

                every { reception.applicationFileInfo } returns ReceptionApplicationFileInfo(
                    "간병인 신청서.pdf",
                    fileUrl,
                )
            }

            afterEach { clearAllMocks() }

            fun behavior() = receptionService.deleteReceptionApplication(
                ReceptionApplicationByReceptionIdQuery(
                    receptionId,
                    subject,
                )
            )

            then("간병 접수를 조회합니다.") {
                behavior()

                verify {
                    receptionRepository.findByIdOrNull(
                        withArg {
                            it shouldBe receptionId
                        }
                    )
                }
            }

            then("간병인 신청서를 삭제를 요청합니다.") {
                behavior()

                verify {
                    fileByUrlQueryHandler.deleteFile(
                        withArg {
                            it.url shouldBe fileUrl
                        }
                    )
                }
            }

            and("간병 접수가 없다면") {
                beforeEach {
                    every { receptionRepository.findByIdOrNull(match { it == receptionId }) } returns null
                }

                afterEach { clearAllMocks() }

                then("ReceptionNotFoundByIdException 발생합니다.") {
                    val thrownException = shouldThrow<ReceptionNotFoundByIdException> { behavior() }

                    thrownException.receptionId shouldBe receptionId
                }
            }

            and("간병인 신청서가 없다면") {
                beforeEach {
                    every { reception.applicationFileInfo } returns null
                }

                afterEach { clearAllMocks() }

                then("ReceptionApplicationNotFoundException 발생합니다.") {
                    val thrownException = shouldThrow<ReceptionApplicationNotFoundException> { behavior() }

                    thrownException.receptionId shouldBe receptionId
                }
            }
        }

        and("엔티티 테스트 할 때") {
            val reception = Reception(
                id = "01HE45HH2P77GCWEXTWT0FH92Q",
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

            `when`("저장을 요청하면") {
                fun behavior() = cacheReceptionRepository.save(reception)
                then("저장이 됩니다.") {
                    behavior()
                }
            }

            `when`("조회를 요청하면") {
                fun behavior() = cacheReceptionRepository.findByIdOrNull("01HE45HH2P77GCWEXTWT0FH92Q")
                then("조회가 됩니다.") {
                    behavior()
                }
            }
        }
    }
})
