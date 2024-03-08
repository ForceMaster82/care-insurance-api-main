package kr.caredoc.careinsurance.reception.caregivingsatisfactionsurvey

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import kr.caredoc.careinsurance.LocalEncryption
import kr.caredoc.careinsurance.bizcall.BizcallReservation
import kr.caredoc.careinsurance.bizcall.BizcallReservationRepository
import kr.caredoc.careinsurance.bizcall.BizcallSender
import kr.caredoc.careinsurance.bizcall.Voice
import kr.caredoc.careinsurance.caregiving.LastCaregivingRoundFinished
import kr.caredoc.careinsurance.caregiving.LastCaregivingRoundModified
import kr.caredoc.careinsurance.decryptableMockReception
import kr.caredoc.careinsurance.generateExternalCaregivingOrganizationManagerSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.patient.EncryptedPatientInfo
import kr.caredoc.careinsurance.patient.PatientInfo
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQueryHandler
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.search.SearchCondition
import kr.caredoc.careinsurance.withFixedClock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.AccessDeniedException
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
class CaregivingSatisfactionSurveyServiceTest(
    @Autowired
    private val cacheCaregivingSatisfactionSurveyStatusRepository: CaregivingSatisfactionSurveyStatusRepository,
) : BehaviorSpec({
    given("간병 만족도 조사 서비스가 주어졌을때") {
        val caregivingSatisfactionSurveyStatusRepository = relaxedMock<CaregivingSatisfactionSurveyStatusRepository>()
        val patientNameHasher = LocalEncryption.patientNameHasher
        val receptionsByIdsQueryHandler = relaxedMock<ReceptionsByIdsQueryHandler>()
        val bizcallSender = relaxedMock<BizcallSender>()
        val bizcallReservationRepository = relaxedMock<BizcallReservationRepository>()
        val service = CaregivingSatisfactionSurveyService(
            caregivingSatisfactionSurveyStatusRepository = caregivingSatisfactionSurveyStatusRepository,
            patientNameHasher = patientNameHasher,
            receptionsByIdsQueryHandler = receptionsByIdsQueryHandler,
            bizcallReservationRepository = bizcallReservationRepository,
            bizcallSender = bizcallSender,
            satisfactionSurveyBizcallScenarioId = "bizcall_4c2e0e66-3255-427e-9a10-162eaf4c58fa",
            satisfactionSurveyBizcallCallerNumber = "01047009007",
            satisfactionSurveyBizcallDisplayNumber = "01011112222",
            decryptor = LocalEncryption.LocalDecryptor,
        )

        beforeEach {
            with(caregivingSatisfactionSurveyStatusRepository) {
                val savingEntitySlot = slot<CaregivingSatisfactionSurveyStatus>()
                every {
                    save(capture(savingEntitySlot))
                } answers { savingEntitySlot.captured }
                every {
                    existsByReceptionId(any())
                } returns false
            }
            with(bizcallReservationRepository) {
                val savingEntitySlot = slot<BizcallReservation>()
                every {
                    save(capture(savingEntitySlot))
                } answers { savingEntitySlot.captured }
            }
        }

        afterEach { clearAllMocks() }

        and("또한 간병 만족도 조사 상태가 등록되어 있을때") {
            val registeredSurveyStatuses = listOf<CaregivingSatisfactionSurveyStatus>(
                relaxedMock(),
                relaxedMock(),
            )

            beforeEach {
                with(caregivingSatisfactionSurveyStatusRepository) {
                    val pageableSlot = slot<Pageable>()
                    every {
                        findByExpectedSendingDate(
                            LocalDate.of(2023, 1, 3),
                            capture(pageableSlot)
                        )
                    } answers {
                        PageImpl(
                            registeredSurveyStatuses,
                            pageableSlot.captured,
                            registeredSurveyStatuses.size.toLong(),
                        )
                    }

                    every {
                        findByExpectedSendingDateAndHashedPatientName(
                            LocalDate.of(2023, 1, 3),
                            patientNameHasher.hashAsHex("홍길동"),
                            capture(pageableSlot)
                        )
                    } answers {
                        PageImpl(
                            listOf(registeredSurveyStatuses[0]),
                            pageableSlot.captured,
                            1,
                        )
                    }

                    every {
                        findByExpectedSendingDateAndAccidentNumberContaining(
                            LocalDate.of(2023, 1, 3),
                            "1917",
                            capture(pageableSlot)
                        )
                    } answers {
                        PageImpl(
                            listOf(registeredSurveyStatuses[1]),
                            pageableSlot.captured,
                            1,
                        )
                    }

                    every {
                        existsByReceptionId("01GVFY259Y6Z3Y5TZRVTAQD8T0")
                    } returns true

                    every {
                        findByReceptionId("01GVFY259Y6Z3Y5TZRVTAQD8T0")
                    } returns listOf(registeredSurveyStatuses[0])

                    every {
                        findByReceptionIdIn(match { it.contains("01GVFY259Y6Z3Y5TZRVTAQD8T0") })
                    } returns listOf(registeredSurveyStatuses[0])
                }

                every {
                    bizcallReservationRepository.findBySendingDateTime(LocalDateTime.of(2023, 6, 1, 16, 0, 0))
                } returns relaxedMock {
                    every { bizcallId } returns "646c750ea3f77579b3b1c64"
                }
            }

            afterEach { clearAllMocks() }

            `when`("간병 만족도 조사 상태 목록을 날짜로 필터링하여 조회하면") {
                val query = CaregivingSatisfactionSurveyStatusesByFilterQuery(
                    filter = CaregivingSatisfactionSurveyStatusFilter(
                        date = LocalDate.of(2023, 1, 3)
                    ),
                    subject = generateInternalCaregivingManagerSubject()
                )

                fun behavior() = service.getCaregivingSatisfactionSurveyStatuses(query, PageRequest.of(0, 10))

                then("리포지토리로부터 간병 만족도 조사 상태 목록을 조회합니다.") {
                    behavior()

                    verify {
                        caregivingSatisfactionSurveyStatusRepository.findByExpectedSendingDate(
                            withArg { it shouldBe LocalDate.of(2023, 1, 3) },
                            withArg {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 10
                                it.sort shouldBe Sort.by(Sort.Order.desc(CaregivingSatisfactionSurveyStatus::expectedSendingDate.name))
                            }
                        )
                    }
                }

                then("조회한 간병 만족도 조사 상태 목록을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.content shouldBe registeredSurveyStatuses
                }
            }

            `when`("내부 사용자 권한 없이 간병 만족도 조사 상태 목록을 날짜로 필터링하여 조회하면") {
                val query = CaregivingSatisfactionSurveyStatusesByFilterQuery(
                    filter = CaregivingSatisfactionSurveyStatusFilter(
                        date = LocalDate.of(2023, 1, 3)
                    ),
                    subject = generateExternalCaregivingOrganizationManagerSubject("01GSEKXGET5JKFST29B5K0N4XH")
                )

                fun behavior() = service.getCaregivingSatisfactionSurveyStatuses(query, PageRequest.of(0, 10))

                then("AccessDeniedException이 발생합니다.") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }

            `when`("간병 만족도 조사 상태를 환자 이름 기준으로 조회하면") {
                val query = CaregivingSatisfactionSurveyStatusSearchQuery(
                    filter = CaregivingSatisfactionSurveyStatusFilter(
                        date = LocalDate.of(2023, 1, 3)
                    ),
                    searchCondition = SearchCondition(
                        CaregivingSatisfactionSurveyStatusSearchQuery.SearchingProperty.PATIENT_NAME,
                        "홍길동"
                    ),
                    subject = generateInternalCaregivingManagerSubject()
                )

                fun behavior() = service.searchCaregivingSatisfactionSurveyStatus(query, PageRequest.of(0, 10))

                then("리포지토리로부터 간병 만족도 조사 상태 목록을 검색합니다.") {
                    behavior()

                    verify {
                        caregivingSatisfactionSurveyStatusRepository.findByExpectedSendingDateAndHashedPatientName(
                            withArg { it shouldBe LocalDate.of(2023, 1, 3) },
                            withArg { it shouldBe patientNameHasher.hashAsHex("홍길동") },
                            withArg {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 10
                                it.sort shouldBe Sort.by(Sort.Order.desc(CaregivingSatisfactionSurveyStatus::expectedSendingDate.name))
                            }
                        )
                    }
                }

                then("검색된 간병 만족도 조사 상태 목록을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.content shouldContainExactly setOf(registeredSurveyStatuses[0])
                }
            }

            `when`("간병 만족도 조사 상태를 사고번호 기준으로 조회하면") {
                val query = CaregivingSatisfactionSurveyStatusSearchQuery(
                    filter = CaregivingSatisfactionSurveyStatusFilter(
                        date = LocalDate.of(2023, 1, 3)
                    ),
                    searchCondition = SearchCondition(
                        CaregivingSatisfactionSurveyStatusSearchQuery.SearchingProperty.ACCIDENT_NUMBER,
                        "1917"
                    ),
                    subject = generateInternalCaregivingManagerSubject()
                )

                fun behavior() = service.searchCaregivingSatisfactionSurveyStatus(query, PageRequest.of(0, 10))

                then("리포지토리로부터 간병 만족도 조사 상태 목록을 검색합니다.") {
                    behavior()

                    verify {
                        caregivingSatisfactionSurveyStatusRepository.findByExpectedSendingDateAndAccidentNumberContaining(
                            withArg { it shouldBe LocalDate.of(2023, 1, 3) },
                            withArg { it shouldBe "1917" },
                            withArg {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 10
                                it.sort shouldBe Sort.by(Sort.Order.desc(CaregivingSatisfactionSurveyStatus::expectedSendingDate.name))
                            }
                        )
                    }
                }

                then("검색된 간병 만족도 조사 상태 목록을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.content shouldContainExactly setOf(registeredSurveyStatuses[1])
                }
            }

            `when`("이미 종료된 간병의 마지막 회차가 종료되면") {
                val event = relaxedMock<LastCaregivingRoundFinished>()

                beforeEach {
                    with(event) {
                        every { receptionId } returns "01GVFY259Y6Z3Y5TZRVTAQD8T0"
                        every { lastCaregivingRoundId } returns "01GVD2HS5FMX9012BN28VHDPW3"
                        every { endDateTime } returns LocalDateTime.of(2023, 1, 2, 13, 21, 17)
                    }
                }

                afterEach { clearAllMocks() }

                fun handling() = service.handleLastCaregivingRoundFinished(event)

                then("아무것도 안합니다.") {
                    handling()

                    verify(exactly = 0) {
                        caregivingSatisfactionSurveyStatusRepository.save(any())
                    }
                }
            }

            `when`("마지막 간병 회차가 종료된 이후에 종료일이 수정되면") {
                val event = relaxedMock<LastCaregivingRoundModified>()

                beforeEach {
                    with(event) {
                        every { receptionId } returns "01GVFY259Y6Z3Y5TZRVTAQD8T0"
                    }
                }

                afterEach { clearAllMocks() }

                fun handling() = service.handleLastCaregivingRoundModified(event)

                then("해당 접수의 간병 만족도 조사 상태에 이벤트를 전달합니다.") {
                    handling()

                    verify {
                        registeredSurveyStatuses[0].handleLastCaregivingRoundModified(event)
                    }
                }
            }

            `when`("당일 최초로 간병 만족도 조사를 예약하면") {
                val command = CaregivingSatisfactionSurveyReserveCommand(
                    targetReceptionIds = setOf(
                        "01GVFY259Y6Z3Y5TZRVTAQD8T0"
                    ),
                    subject = generateInternalCaregivingManagerSubject(),
                )

                beforeEach {
                    every {
                        bizcallReservationRepository.findBySendingDateTime(LocalDateTime.of(2023, 6, 1, 16, 0, 0))
                    } returns null

                    every {
                        receptionsByIdsQueryHandler.getReceptions(
                            match {
                                it.receptionIds.contains("01GVFY259Y6Z3Y5TZRVTAQD8T0")
                            }
                        )
                    } returns listOf(
                        decryptableMockReception {
                            every { id } returns "01GVFY259Y6Z3Y5TZRVTAQD8T0"
                            every { patientInfo.primaryContact } returns LocalEncryption.patientInfoEncryptor.encrypt(
                                PatientInfo.Contact("01011112222", "본인")
                            )
                            every { patientInfo.name } returns EncryptedPatientInfo.EncryptedPatientName(
                                "김환자",
                                LocalEncryption.patientNameHasher,
                                LocalEncryption.LocalEncryptor,
                            )
                        }
                    )

                    every {
                        bizcallSender.reserve(any())
                    } returns relaxedMock {
                        every { bizcallId } returns "646c750ea3f77579b3b1c645"
                    }
                }

                afterEach { clearAllMocks() }

                fun behavior() = withFixedClock(LocalDateTime.of(2023, 6, 1, 15, 0, 0)) {
                    service.reserveSatisfactionSurvey(command)
                }

                then("간병 대상이 되는 접수 목록을 조회합니다.") {
                    behavior()

                    verify {
                        receptionsByIdsQueryHandler.getReceptions(
                            withArg {
                                it.receptionIds shouldContainExactlyInAnyOrder setOf("01GVFY259Y6Z3Y5TZRVTAQD8T0")
                            }
                        )
                    }
                }

                then("비즈콜을 예약합니다.") {
                    behavior()

                    verify {
                        bizcallSender.reserve(
                            withArg {
                                it.scenarioId shouldBe "bizcall_4c2e0e66-3255-427e-9a10-162eaf4c58fa"
                                it.originalMdn shouldBe "01047009007"
                                it.changedMdn shouldBe "01011112222"
                                it.priority shouldBe 2
                                it.voiceSpeed shouldBe 90
                                it.bizcallName shouldBe "간병 서비스 만족도 조사"
                                it.voice shouldBe Voice.ARIA
                                it.reservationInfo.reservationDateTime shouldBe "2023-06-01T16:00:00"
                                it.retry.retryCount shouldBe 0
                                it.retry.retryInterval shouldBe 0
                                it.retry.replacedRetry shouldBe false
                            }
                        )
                    }
                }

                then("비즈콜 예약 내역을 저장합니다.") {
                    behavior()

                    verify {
                        bizcallReservationRepository.save(
                            withArg {
                                it.bizcallId shouldBe "646c750ea3f77579b3b1c645"
                                it.sendingDateTime shouldBe LocalDateTime.of(2023, 6, 1, 16, 0, 0)
                            }
                        )
                    }
                }

                then("예약한 비즈콜에 발신 대상을 추가합니다.") {
                    behavior()

                    verify {
                        bizcallSender.additionalRecipientByReservation(
                            withArg {
                                it shouldBe "646c750ea3f77579b3b1c645"
                            },
                            withArg {
                                it[0].mdn shouldBe "01011112222"
                                it[0].taskInfo shouldBe mapOf("대상자" to "김환자")
                            },
                        )
                    }
                }

                then("간병 만족도 조사 상태를 갱신합니다.") {
                    behavior()

                    verify {
                        registeredSurveyStatuses[0].markAsReserved(command.subject)
                    }
                }
            }

            `when`("당일 최초 예약 이후로 간병 만족도 조사를 예약하면") {
                val command = CaregivingSatisfactionSurveyReserveCommand(
                    targetReceptionIds = setOf(
                        "01GVFY259Y6Z3Y5TZRVTAQD8T0"
                    ),
                    subject = generateInternalCaregivingManagerSubject(),
                )

                beforeEach {
                    every {
                        receptionsByIdsQueryHandler.getReceptions(
                            match {
                                it.receptionIds.contains("01GVFY259Y6Z3Y5TZRVTAQD8T0")
                            }
                        )
                    } returns listOf(
                        decryptableMockReception {
                            every { id } returns "01GVFY259Y6Z3Y5TZRVTAQD8T0"
                            every { patientInfo.primaryContact } returns LocalEncryption.patientInfoEncryptor.encrypt(
                                PatientInfo.Contact("01011112222", "본인")
                            )
                            every { patientInfo.name } returns EncryptedPatientInfo.EncryptedPatientName(
                                "김환자",
                                LocalEncryption.patientNameHasher,
                                LocalEncryption.LocalEncryptor,
                            )
                        }
                    )
                }

                afterEach { clearAllMocks() }

                fun behavior() = withFixedClock(LocalDateTime.of(2023, 6, 1, 11, 23, 32)) {
                    service.reserveSatisfactionSurvey(command)
                }

                then("간병 대상이 되는 접수 목록을 조회합니다.") {
                    behavior()

                    verify {
                        receptionsByIdsQueryHandler.getReceptions(
                            withArg {
                                it.receptionIds shouldContainExactlyInAnyOrder setOf("01GVFY259Y6Z3Y5TZRVTAQD8T0")
                            }
                        )
                    }
                }

                then("예약한 비즈콜에 발신 대상을 추가합니다.") {
                    behavior()

                    verify {
                        bizcallSender.additionalRecipientByReservation(
                            withArg {
                                it shouldBe "646c750ea3f77579b3b1c64"
                            },
                            withArg {
                                it[0].mdn shouldBe "01011112222"
                                it[0].taskInfo shouldBe mapOf("대상자" to "김환자")
                            },
                        )
                    }
                }

                then("간병 만족도 조사 상태를 갱신합니다.") {
                    behavior()

                    verify {
                        registeredSurveyStatuses[0].markAsReserved(command.subject)
                    }
                }
            }

            `when`("내부 사용자 권한 없이 간병 만족도 조사를 예약하면") {
                val command = CaregivingSatisfactionSurveyReserveCommand(
                    targetReceptionIds = setOf(
                        "01GVFY259Y6Z3Y5TZRVTAQD8T0"
                    ),
                    subject = generateExternalCaregivingOrganizationManagerSubject("01GSEKXGET5JKFST29B5K0N4XH")
                )

                beforeEach {
                    every {
                        receptionsByIdsQueryHandler.getReceptions(
                            match {
                                it.receptionIds.contains("01GVFY259Y6Z3Y5TZRVTAQD8T0")
                            }
                        )
                    } returns listOf(
                        decryptableMockReception {
                            every { id } returns "01GVFY259Y6Z3Y5TZRVTAQD8T0"
                            every { patientInfo.primaryContact } returns LocalEncryption.patientInfoEncryptor.encrypt(
                                PatientInfo.Contact("01011112222", "본인")
                            )
                        }
                    )
                }

                afterEach { clearAllMocks() }

                fun behavior() = service.reserveSatisfactionSurvey(command)

                then("AccessDeniedException이 발생합니다.") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }
        }

        `when`("간병의 마지막 회차가 종료되면") {
            val event = relaxedMock<LastCaregivingRoundFinished>()

            beforeEach {
                with(event) {
                    every { receptionId } returns "01GVFY259Y6Z3Y5TZRVTAQD8T0"
                    every { lastCaregivingRoundId } returns "01GVD2HS5FMX9012BN28VHDPW3"
                    every { endDateTime } returns LocalDateTime.of(2023, 1, 2, 13, 21, 17)
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = service.handleLastCaregivingRoundFinished(event)

            then("간병 만족도 조사 상태를 초기화하여 저장합니다.") {
                handling()

                verify {
                    caregivingSatisfactionSurveyStatusRepository.save(
                        withArg {
                            it.caregivingRoundId shouldBe "01GVD2HS5FMX9012BN28VHDPW3"
                            it.receptionId shouldBe "01GVFY259Y6Z3Y5TZRVTAQD8T0"
                            it.expectedSendingDate shouldBe LocalDate.of(2023, 1, 3)
                            it.reservationStatus shouldBe ReservationStatus.READY
                        }
                    )
                }
            }
        }

        and("엔티티 테스트할 때") {
            val caregivingSatisfactionSurveyStatus = CaregivingSatisfactionSurveyStatus(
                id = "01HE47T9WPAHKQDHB78HQ3G0SH",
                receptionId = "01HE47TH98QCJJAQ27KYREXT67",
                caregivingRoundId = "01HE47VDC80AY7AGSJGYKVYR2D",
                caregivingRoundEndDate = LocalDate.of(2023, 10, 31)
            )
            `when`("저장을 요청하면") {
                fun behavior() = cacheCaregivingSatisfactionSurveyStatusRepository.save(caregivingSatisfactionSurveyStatus)
                then("저장이 됩니다.") {
                    behavior()
                }
            }

            `when`("조회를 요청하면") {
                fun behavior() = cacheCaregivingSatisfactionSurveyStatusRepository.findByIdOrNull("01HE47T9WPAHKQDHB78HQ3G0SH")
                then("조회가 됩니다.") {
                    behavior()
                }
            }
        }
    }
})
