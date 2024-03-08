package kr.caredoc.careinsurance.caregiving.progressmessage

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import kr.caredoc.careinsurance.LocalEncryption
import kr.caredoc.careinsurance.alimtalk.AlimtalkSender
import kr.caredoc.careinsurance.alimtalk.BulkAlimtalkMessage
import kr.caredoc.careinsurance.alimtalk.SendingResultOfMessage
import kr.caredoc.careinsurance.caregiving.CaregivingProgressingStatus
import kr.caredoc.careinsurance.caregiving.CaregivingRound
import kr.caredoc.careinsurance.caregiving.CaregivingRoundStarted
import kr.caredoc.careinsurance.caregiving.CaregivingRoundsByIdsQuery
import kr.caredoc.careinsurance.caregiving.CaregivingRoundsByIdsQueryHandler
import kr.caredoc.careinsurance.decryptableMockReception
import kr.caredoc.careinsurance.generateExternalCaregivingOrganizationManagerSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.message.SendingStatus
import kr.caredoc.careinsurance.patient.EncryptedPatientInfo
import kr.caredoc.careinsurance.patient.PatientInfo
import kr.caredoc.careinsurance.reception.ReceptionByIdQueryHandler
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.search.SearchCondition
import kr.caredoc.careinsurance.security.personaldata.PatientInfoEncryptor
import kr.caredoc.careinsurance.withFixedClock
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.access.AccessDeniedException
import java.time.LocalDate
import java.time.LocalDateTime

class CaregivingProgressMessageServiceTest : BehaviorSpec({
    given("진행 메시지 서비스가 주어졌을 때") {
        val caregivingProgressMessageSummaryRepository = relaxedMock<CaregivingProgressMessageSummaryRepository>()
        val caregivingProgressMessageSendingHistoryRepository =
            relaxedMock<CaregivingProgressMessageSendingHistoryRepository>()
        val receptionByIdQueryHandler = relaxedMock<ReceptionByIdQueryHandler>()
        val caregivingRoundsByIdsQueryHandler = relaxedMock<CaregivingRoundsByIdsQueryHandler>()
        val alimtalkSender = relaxedMock<AlimtalkSender>()
        val patientNameHasher = LocalEncryption.patientNameHasher
        val service = CaregivingProgressMessageService(
            caregivingProgressMessageSummaryRepository = caregivingProgressMessageSummaryRepository,
            caregivingProgressMessageSendingHistoryRepository = caregivingProgressMessageSendingHistoryRepository,
            receptionByIdQueryHandler = receptionByIdQueryHandler,
            caregivingRoundsByIdsQueryHandler = caregivingRoundsByIdsQueryHandler,
            alimtalkSender = alimtalkSender,
            decryptor = LocalEncryption.LocalDecryptor,
            patientNameHasher = patientNameHasher,
        )

        beforeEach {
            with(caregivingProgressMessageSummaryRepository) {
                val savingEntitySlot = slot<CaregivingProgressMessageSummary>()
                every { save(capture(savingEntitySlot)) } answers {
                    savingEntitySlot.captured
                }
            }

            with(caregivingProgressMessageSendingHistoryRepository) {
                val savingEntitySlot = slot<CaregivingProgressMessageSendingHistory>()
                every { save(capture(savingEntitySlot)) } answers {
                    savingEntitySlot.captured
                }
            }

            with(caregivingRoundsByIdsQueryHandler) {
                val caregivingRoundsByIds = mapOf<String, CaregivingRound>(
                    "01H0CZP7YKA0YPSX9JMSGWJBNW" to relaxedMock {
                        every { id } returns "01H0CZP7YKA0YPSX9JMSGWJBNW"
                        every { caregivingRoundNumber } returns 1
                        every { receptionInfo.receptionId } returns "01H0D0NB66HJ8V2YZ0XKTKBG15"
                        every { caregivingProgressingStatus } returns CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
                    },
                    "01H0CZNTEGESA5HRBF9ZP8NV4X" to relaxedMock {
                        every { id } returns "01H0CZNTEGESA5HRBF9ZP8NV4X"
                        every { caregivingRoundNumber } returns 2
                        every { receptionInfo.receptionId } returns "01H0D0NB66HJ8V2YZ0XKTKBG16"
                        every { caregivingProgressingStatus } returns CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
                    },
                )
                val caregivingRoundsByIdsQuerySlot = slot<CaregivingRoundsByIdsQuery>()
                every {
                    getCaregivingRounds(capture(caregivingRoundsByIdsQuerySlot))
                } answers {
                    val capturedCaregivingRoundIds = caregivingRoundsByIdsQuerySlot.captured.caregivingRoundIds

                    capturedCaregivingRoundIds.mapNotNull { caregivingRoundsByIds[it] }
                }
            }

            val receptions = listOf(decryptableMockReception(), decryptableMockReception())
            every {
                receptionByIdQueryHandler.getReception(
                    match {
                        it.receptionId == "01H0D0NB66HJ8V2YZ0XKTKBG15"
                    }
                )
            } returns receptions[0]
            every {
                receptionByIdQueryHandler.getReception(
                    match {
                        it.receptionId == "01H0D0NB66HJ8V2YZ0XKTKBG16"
                    }
                )
            } returns receptions[1]

            val encryptor = PatientInfoEncryptor(LocalEncryption.LocalEncryptor, LocalEncryption.patientNameHasher)

            with(receptions[0]) {
                every { id } returns "01H0D0NB66HJ8V2YZ0XKTKBG15"
                every { patientInfo.primaryContact } returns encryptor.encrypt(
                    PatientInfo.Contact(
                        phoneNumber = "01033334444",
                        relationshipWithPatient = "본인",
                    )
                )
                every { patientInfo.name } returns EncryptedPatientInfo.EncryptedPatientName(
                    "김환자",
                    LocalEncryption.patientNameHasher,
                    LocalEncryption.LocalEncryptor,
                )
            }
            with(receptions[1]) {
                every { id } returns "01H0D0NB66HJ8V2YZ0XKTKBG16"
                every { patientInfo.primaryContact } returns encryptor.encrypt(
                    PatientInfo.Contact(
                        phoneNumber = "01033335555",
                        relationshipWithPatient = "본인",
                    )
                )
                every { patientInfo.name } returns EncryptedPatientInfo.EncryptedPatientName(
                    "홍길동",
                    LocalEncryption.patientNameHasher,
                    LocalEncryption.LocalEncryptor,
                )
            }

            with(alimtalkSender) {
                val bulkMessageSlot = slot<BulkAlimtalkMessage>()

                every {
                    send(capture(bulkMessageSlot))
                } answers {
                    bulkMessageSlot.captured.messageParameters.map {
                        SendingResultOfMessage(
                            messageId = it.id,
                            sentMessageId = if (it.recipient == "01033334444") {
                                "124c3425-1743-470c-843b-b175bd5f8cbf"
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }

        afterEach {
            clearAllMocks()
        }

        and("간병 진행 메시지 발송 대상 또한 주어졌을 때") {
            val registeredCaregivingProgressMessageSummaries = listOf(
                relaxedMock<CaregivingProgressMessageSummary>(),
                relaxedMock<CaregivingProgressMessageSummary>(),
            )
            val pageRequest = PageRequest.of(0, 30)
            val subject = generateInternalCaregivingManagerSubject()

            beforeEach {
                with(caregivingProgressMessageSummaryRepository) {
                    val pageableSlot = slot<Pageable>()
                    every {
                        getCaregivingProgressMessageSummaryByDate(
                            LocalDate.of(2023, 5, 8),
                            capture(pageableSlot)
                        )
                    } answers {
                        PageImpl(
                            registeredCaregivingProgressMessageSummaries,
                            pageableSlot.captured,
                            registeredCaregivingProgressMessageSummaries.size.toLong(),
                        )
                    }
                    every {
                        getCaregivingProgressMessageSummaryByDateAndSendingStatus(
                            expectedSendingDate = LocalDate.of(2023, 5, 8),
                            sendingStatus = SendingStatus.READY,
                            capture(pageableSlot)
                        )
                    } answers {
                        PageImpl(
                            registeredCaregivingProgressMessageSummaries,
                            pageableSlot.captured,
                            registeredCaregivingProgressMessageSummaries.size.toLong(),
                        )
                    }
                    every {
                        getCaregivingProgressMessageSummaryByDateAndAccidentNumber(
                            expectedSendingDate = LocalDate.of(2023, 5, 8),
                            accidentNumberKeyword = "2022",
                            capture(pageableSlot)
                        )
                    } answers {
                        PageImpl(
                            registeredCaregivingProgressMessageSummaries,
                            pageableSlot.captured,
                            registeredCaregivingProgressMessageSummaries.size.toLong(),
                        )
                    }
                    every {
                        getCaregivingProgressMessageSummaryByDateAndHashedPatientName(
                            expectedSendingDate = LocalDate.of(2023, 5, 8),
                            hashedPatientName = patientNameHasher.hashAsHex("홍길동"),
                            capture(pageableSlot)
                        )
                    } answers {
                        PageImpl(
                            registeredCaregivingProgressMessageSummaries,
                            pageableSlot.captured,
                            registeredCaregivingProgressMessageSummaries.size.toLong(),
                        )
                    }
                    every {
                        getCaregivingProgressMessageSummaryByDateAndSendingStatusAndAccidentNumber(
                            expectedSendingDate = LocalDate.of(2023, 5, 8),
                            accidentNumberKeyword = "2022",
                            sendingStatus = SendingStatus.SENT,
                            capture(pageableSlot)
                        )
                    } answers {
                        PageImpl(
                            registeredCaregivingProgressMessageSummaries,
                            pageableSlot.captured,
                            registeredCaregivingProgressMessageSummaries.size.toLong(),
                        )
                    }
                    every {
                        getCaregivingProgressMessageSummaryByDateAndSendingStatusAndHashedPatientName(
                            expectedSendingDate = LocalDate.of(2023, 5, 8),
                            hashedPatientName = patientNameHasher.hashAsHex("홍길동"),
                            sendingStatus = SendingStatus.FAILED,
                            capture(pageableSlot)
                        )
                    } answers {
                        PageImpl(
                            registeredCaregivingProgressMessageSummaries,
                            pageableSlot.captured,
                            registeredCaregivingProgressMessageSummaries.size.toLong(),
                        )
                    }
                    every {
                        existsByCaregivingRoundId("01H0CNRRQW3GVWCJEE08CGP7DA")
                    } returns true

                    every {
                        findByCaregivingRoundId("01H0CZP7YKA0YPSX9JMSGWJBNW")
                    } returns listOf(registeredCaregivingProgressMessageSummaries[0])
                    every {
                        findByCaregivingRoundId("01H0CZNTEGESA5HRBF9ZP8NV4X")
                    } returns listOf(registeredCaregivingProgressMessageSummaries[1])
                }
            }

            afterEach { clearAllMocks() }

            `when`("발송예정일 조건으로 간병 진행 메시지 목록을 조회하면") {
                val filter = CaregivingProgressMessageSummaryFilter(
                    date = LocalDate.of(2023, 5, 8),
                    sendingStatus = null
                )
                val query = CaregivingProgressMessageSummariesByFilterQuery(
                    filter = filter,
                    subject = subject,
                )

                fun behavior() = service.getCaregivingProgressMessageSummaries(query, pageRequest)

                then("레파지토리로부터 간병 진행 메시지 요약 목록을 조회합니다.") {
                    behavior()

                    verify {
                        caregivingProgressMessageSummaryRepository.getCaregivingProgressMessageSummaryByDate(
                            withArg {
                                it shouldBe LocalDate.of(2023, 5, 8)
                            },
                            withArg {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 30
                                it.sort.getOrderFor(
                                    CaregivingProgressMessageSummaryRepository.RECEPTION_RECEIVED_DATE_TIME_ORDERING
                                )?.direction shouldBe Sort.Direction.DESC
                            }
                        )
                    }
                }

                then("조회한 간병 진행 메시지 요약 목록을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.content shouldBe registeredCaregivingProgressMessageSummaries
                }
            }

            `when`("발송예정일과 발송상태를 조건으로 간병 진행 메시지 목록을 조회하면") {
                val filter = CaregivingProgressMessageSummaryFilter(
                    date = LocalDate.of(2023, 5, 8),
                    sendingStatus = SendingStatus.READY,
                )
                val query = CaregivingProgressMessageSummariesByFilterQuery(
                    filter = filter,
                    subject = subject,
                )

                fun behavior() = service.getCaregivingProgressMessageSummaries(query, pageRequest)

                then("레파지토리로부터 간병 진행 메시지 요약 목록을 조회합니다.") {
                    behavior()

                    verify {
                        caregivingProgressMessageSummaryRepository.getCaregivingProgressMessageSummaryByDateAndSendingStatus(
                            withArg {
                                it shouldBe LocalDate.of(2023, 5, 8)
                            },
                            withArg {
                                it shouldBe SendingStatus.READY
                            },
                            withArg {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 30
                                it.sort.getOrderFor(
                                    CaregivingProgressMessageSummaryRepository.RECEPTION_RECEIVED_DATE_TIME_ORDERING
                                )?.direction shouldBe Sort.Direction.DESC
                            }
                        )
                    }
                }

                then("조회한 간병 진행 메시지 요약 목록을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.content shouldBe registeredCaregivingProgressMessageSummaries
                }
            }

            `when`("발송예정일과 사고번호를 조건으로 간병 진행 메시지 목록을 조회하면") {
                val filter = CaregivingProgressMessageSummaryFilter(
                    date = LocalDate.of(2023, 5, 8),
                    sendingStatus = null
                )
                val query = CaregivingProgressMessageSummariesSearchQuery(
                    filter = filter,
                    searchCondition = SearchCondition(
                        searchingProperty = CaregivingProgressMessageSummariesSearchQuery.SearchingProperty.ACCIDENT_NUMBER,
                        keyword = "2022",
                    ),
                    subject = subject,
                )

                fun behavior() = service.searchCaregivingProgressMessageSummaries(query, pageRequest)

                then("레파지토리로부터 간병 진행 메시지 요약 목록을 조회합니다.") {
                    behavior()

                    verify {
                        caregivingProgressMessageSummaryRepository.getCaregivingProgressMessageSummaryByDateAndAccidentNumber(
                            withArg {
                                it shouldBe LocalDate.of(2023, 5, 8)
                            },
                            withArg {
                                it shouldBe "2022"
                            },
                            withArg {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 30
                                it.sort.getOrderFor(
                                    CaregivingProgressMessageSummaryRepository.RECEPTION_RECEIVED_DATE_TIME_ORDERING
                                )?.direction shouldBe Sort.Direction.DESC
                            }
                        )
                    }
                }

                then("조회한 간병 진행 메시지 요약 목록을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.content shouldBe registeredCaregivingProgressMessageSummaries
                }
            }

            `when`("발송예정일과 고객명을 조건으로 간병 진행 메시지 목록을 조회하면") {
                val filter = CaregivingProgressMessageSummaryFilter(
                    date = LocalDate.of(2023, 5, 8),
                    sendingStatus = null
                )
                val query = CaregivingProgressMessageSummariesSearchQuery(
                    filter = filter,
                    searchCondition = SearchCondition(
                        searchingProperty = CaregivingProgressMessageSummariesSearchQuery.SearchingProperty.PATIENT_NAME,
                        keyword = "홍길동",
                    ),
                    subject = subject,
                )

                fun behavior() = service.searchCaregivingProgressMessageSummaries(query, pageRequest)

                then("레파지토리로부터 간병 진행 메시지 요약 목록을 조회합니다.") {
                    behavior()

                    verify {
                        caregivingProgressMessageSummaryRepository.getCaregivingProgressMessageSummaryByDateAndHashedPatientName(
                            withArg {
                                it shouldBe LocalDate.of(2023, 5, 8)
                            },
                            withArg {
                                it shouldBe patientNameHasher.hashAsHex("홍길동")
                            },
                            withArg {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 30
                                it.sort.getOrderFor(
                                    CaregivingProgressMessageSummaryRepository.RECEPTION_RECEIVED_DATE_TIME_ORDERING
                                )?.direction shouldBe Sort.Direction.DESC
                            }
                        )
                    }
                }

                then("조회한 간병 진행 메시지 요약 목록을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.content shouldBe registeredCaregivingProgressMessageSummaries
                }
            }

            `when`("발송예정일과 발송상태 그리고 사고번호를 조건으로 간병 진행 메시지 목록을 조회하면") {
                val filter = CaregivingProgressMessageSummaryFilter(
                    date = LocalDate.of(2023, 5, 8),
                    sendingStatus = SendingStatus.SENT,
                )
                val query = CaregivingProgressMessageSummariesSearchQuery(
                    filter = filter,
                    searchCondition = SearchCondition(
                        searchingProperty = CaregivingProgressMessageSummariesSearchQuery.SearchingProperty.ACCIDENT_NUMBER,
                        keyword = "2022",
                    ),
                    subject = subject,
                )

                fun behavior() = service.searchCaregivingProgressMessageSummaries(query, pageRequest)

                then("레파지토리로부터 간병 진행 메시지 요약 목록을 조회합니다.") {
                    behavior()

                    verify {
                        caregivingProgressMessageSummaryRepository.getCaregivingProgressMessageSummaryByDateAndSendingStatusAndAccidentNumber(
                            withArg {
                                it shouldBe LocalDate.of(2023, 5, 8)
                            },
                            withArg {
                                it shouldBe "2022"
                            },
                            withArg {
                                it shouldBe SendingStatus.SENT
                            },
                            withArg {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 30
                                it.sort.getOrderFor(
                                    CaregivingProgressMessageSummaryRepository.RECEPTION_RECEIVED_DATE_TIME_ORDERING
                                )?.direction shouldBe Sort.Direction.DESC
                            }
                        )
                    }
                }

                then("조회한 간병 진행 메시지 요약 목록을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.content shouldBe registeredCaregivingProgressMessageSummaries
                }
            }

            `when`("발송예정일과 발송상태 그리고 고객명을 조건으로 간병 진행 메시지 목록을 조회하면") {
                val filter = CaregivingProgressMessageSummaryFilter(
                    date = LocalDate.of(2023, 5, 8),
                    sendingStatus = SendingStatus.FAILED,
                )
                val query = CaregivingProgressMessageSummariesSearchQuery(
                    filter = filter,
                    searchCondition = SearchCondition(
                        searchingProperty = CaregivingProgressMessageSummariesSearchQuery.SearchingProperty.PATIENT_NAME,
                        keyword = "홍길동",
                    ),
                    subject = subject,
                )

                fun behavior() = service.searchCaregivingProgressMessageSummaries(query, pageRequest)

                then("레파지토리로부터 간병 진행 메시지 요약 목록을 조회합니다.") {
                    behavior()

                    verify {
                        caregivingProgressMessageSummaryRepository.getCaregivingProgressMessageSummaryByDateAndSendingStatusAndHashedPatientName(
                            withArg {
                                it shouldBe LocalDate.of(2023, 5, 8)
                            },
                            withArg {
                                it shouldBe patientNameHasher.hashAsHex("홍길동")
                            },
                            withArg {
                                it shouldBe SendingStatus.FAILED
                            },
                            withArg {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 30
                                it.sort.getOrderFor(
                                    CaregivingProgressMessageSummaryRepository.RECEPTION_RECEIVED_DATE_TIME_ORDERING
                                )?.direction shouldBe Sort.Direction.DESC
                            }
                        )
                    }
                }

                then("조회한 간병 시작 메시지 요약 목록을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.content shouldBe registeredCaregivingProgressMessageSummaries
                }
            }

            `when`("내부 사용자 권한 없이 간병 진행 메시지 목록을 조회하면") {
                val filter = CaregivingProgressMessageSummaryFilter(
                    date = LocalDate.of(2023, 5, 8),
                    sendingStatus = null,
                )
                val query = CaregivingProgressMessageSummariesByFilterQuery(
                    filter = filter,
                    subject = generateExternalCaregivingOrganizationManagerSubject("01H0BXC6G3PB0RGK6P3ZWXPWK4"),
                )

                fun behavior() = service.getCaregivingProgressMessageSummaries(query, pageRequest)

                then("AccessDeniedException이 발생합니다.") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }
            `when`("간병 진행 안내 메시지를 발송하도록 요청하면") {
                val command = CaregivingProgressMessageSendingCommand(
                    targetCaregivingRoundIds = setOf(
                        "01H0CZP7YKA0YPSX9JMSGWJBNW",
                        "01H0CZNTEGESA5HRBF9ZP8NV4X",
                    ),
                    subject = generateInternalCaregivingManagerSubject()
                )

                fun behavior() = service.sendCaregivingProgressMessages(command)

                then("대상이 되는 간병 회차 목록을 조회합니다.") {
                    behavior()

                    verify {
                        caregivingRoundsByIdsQueryHandler.getCaregivingRounds(
                            withArg {
                                it.caregivingRoundIds shouldContainAll setOf(
                                    "01H0CZNTEGESA5HRBF9ZP8NV4X",
                                    "01H0CZP7YKA0YPSX9JMSGWJBNW",
                                )
                            }
                        )
                    }
                }

                then("진행 메시지를 접수에 입력된 전화번호로 알림톡 발송을 요청합니다.") {
                    behavior()

                    verify {
                        alimtalkSender.send(
                            withArg {
                                it.templateCode shouldBe "M-20230823"
                                it.messageParameters[0].recipient shouldBe "01033334444"
                                it.messageParameters[0].templateData shouldBe mutableListOf(Pair("고객명", "김환자"))
                            }
                        )
                        alimtalkSender.send(
                            withArg {
                                it.templateCode shouldBe "M-20230823-1"
                                it.messageParameters[0].recipient shouldBe "01033335555"
                                it.messageParameters[0].templateData shouldBe mutableListOf(Pair("고객명", "홍길동"))
                            }
                        )
                    }
                }

                then("간병 진행 안내 메시지를 보낸 기록을 남깁니다.") {
                    withFixedClock(LocalDateTime.of(2023, 5, 15, 18, 50, 0)) {
                        behavior()
                    }

                    verify {
                        caregivingProgressMessageSendingHistoryRepository.save(
                            withArg {
                                it.caregivingRoundId shouldBe "01H0CZP7YKA0YPSX9JMSGWJBNW"
                                it.messageId shouldBe "124c3425-1743-470c-843b-b175bd5f8cbf"
                                it.result shouldBe CaregivingProgressMessageSendingHistory.SendingResult.SENT
                                it.attemptDateTime shouldBe LocalDateTime.of(2023, 5, 15, 18, 50, 0)
                            }
                        )
                        caregivingProgressMessageSendingHistoryRepository.save(
                            withArg {
                                it.caregivingRoundId shouldBe "01H0CZNTEGESA5HRBF9ZP8NV4X"
                                it.messageId shouldBe null
                                it.result shouldBe CaregivingProgressMessageSendingHistory.SendingResult.FAILED
                                it.attemptDateTime shouldBe LocalDateTime.of(2023, 5, 15, 18, 50, 0)
                            }
                        )
                    }
                }

                then("발송 대상이 된 진행 메시지의 발송 요약을 갱신합니다.") {
                    withFixedClock(LocalDateTime.of(2023, 5, 15, 18, 50, 0)) {
                        behavior()
                    }

                    verify {
                        registeredCaregivingProgressMessageSummaries[0].updateSendingResult(
                            SendingStatus.SENT,
                            LocalDateTime.of(2023, 5, 15, 18, 50, 0),
                        )
                        registeredCaregivingProgressMessageSummaries[1].updateSendingResult(
                            SendingStatus.FAILED,
                            null,
                        )
                    }
                }
            }
        }

        `when`("간병 회차의 시작 일시가 입력되면") {
            val event = relaxedMock<CaregivingRoundStarted>()

            beforeEach {
                with(event) {
                    every { caregivingRoundId } returns "01H0BXC6G3PB0RGK6P3ZWXPWK4"
                    every { caregivingRoundNumber } returns 2
                    every { startDateTime } returns LocalDateTime.of(2023, 5, 10, 14, 44, 22)
                    every { receptionId } returns "01H0CQKJX1C9WA9BNAHY31F8K5"
                }
            }

            afterEach { clearAllMocks() }

            fun handing() = service.handleCaregivingRoundStarted(event)

            then("간병 회차의 간병 진행 메시지 요약의 존재여부를 조회합니다. ") {
                handing()

                verify {
                    caregivingProgressMessageSummaryRepository.existsByCaregivingRoundId("01H0BXC6G3PB0RGK6P3ZWXPWK4")
                }
            }
        }

        `when`("첫번째 간병 회차의 시작 일시가 입력되면") {
            val event = relaxedMock<CaregivingRoundStarted>()

            beforeEach {
                with(event) {
                    every { caregivingRoundId } returns "01H0BXC6G3PB0RGK6P3ZWXPWK4"
                    every { caregivingRoundNumber } returns 1
                    every { receptionId } returns "01H0CQKJX1C9WA9BNAHY31F8K5"
                    every { startDateTime } returns LocalDateTime.of(2023, 5, 10, 14, 44, 22)
                }
            }

            afterEach { clearAllMocks() }

            fun handing() = service.handleCaregivingRoundStarted(event)

            then("간병 회차의 간병 진행 메시지 요약을 생성합니다.") {
                handing()

                verify {
                    caregivingProgressMessageSummaryRepository.save(
                        withArg {
                            it.caregivingRoundId shouldBe "01H0BXC6G3PB0RGK6P3ZWXPWK4"
                            it.caregivingProgressingStatus shouldBe CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
                            it.caregivingRoundId shouldBe "01H0BXC6G3PB0RGK6P3ZWXPWK4"
                            it.startDateTime shouldBe LocalDateTime.of(2023, 5, 10, 14, 44, 22)
                            it.expectedSendingDate shouldBe LocalDate.of(2023, 5, 15)
                            it.receptionId shouldBe "01H0CQKJX1C9WA9BNAHY31F8K5"
                            it.sentDate shouldBe null
                        }
                    )
                }
            }
        }

        `when`("첫번째가 아닌 간병 회차의 시작 일시가 입력되면") {
            val event = relaxedMock<CaregivingRoundStarted>()

            beforeEach {
                with(event) {
                    every { caregivingRoundId } returns "01H0BXC6G3PB0RGK6P3ZWXPWK4"
                    every { caregivingRoundNumber } returns 2
                    every { receptionId } returns "01H0CQKJX1C9WA9BNAHY31F8K5"
                    every { startDateTime } returns LocalDateTime.of(2023, 5, 10, 14, 44, 22)
                }
            }

            afterEach { clearAllMocks() }

            fun handing() = service.handleCaregivingRoundStarted(event)

            then("간병 회차의 간병 진행 메시지 요약을 생성합니다.") {
                handing()

                verify {
                    caregivingProgressMessageSummaryRepository.save(
                        withArg {
                            it.caregivingRoundId shouldBe "01H0BXC6G3PB0RGK6P3ZWXPWK4"
                            it.caregivingProgressingStatus shouldBe CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
                            it.caregivingRoundId shouldBe "01H0BXC6G3PB0RGK6P3ZWXPWK4"
                            it.startDateTime shouldBe LocalDateTime.of(2023, 5, 10, 14, 44, 22)
                            it.expectedSendingDate shouldBe LocalDate.of(2023, 5, 20)
                            it.receptionId shouldBe "01H0CQKJX1C9WA9BNAHY31F8K5"
                            it.sentDate shouldBe null
                        }
                    )
                }
            }
        }

        `when`("내부 사용자 권한 없이 간병 진행 메시지를 발송하도록 요청하면") {
            val command = CaregivingProgressMessageSendingCommand(
                targetCaregivingRoundIds = setOf(
                    "01H0CZNTEGESA5HRBF9ZP8NV4X",
                ),
                subject = generateExternalCaregivingOrganizationManagerSubject("01H0D2WK7A6RR6XNY6ZC5A55AA")
            )

            fun behavior() = service.sendCaregivingProgressMessages(command)

            then("AccessDeniedException이 발생합니다.") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }
    }
})
