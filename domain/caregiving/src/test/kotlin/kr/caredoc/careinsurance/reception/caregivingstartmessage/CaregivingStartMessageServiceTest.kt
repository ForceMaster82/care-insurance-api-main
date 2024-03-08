package kr.caredoc.careinsurance.reception.caregivingstartmessage

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
import kr.caredoc.careinsurance.caregiving.CaregivingRoundModified
import kr.caredoc.careinsurance.caregiving.CaregivingRoundStarted
import kr.caredoc.careinsurance.decryptableMockReception
import kr.caredoc.careinsurance.generateExternalCaregivingOrganizationManagerSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.message.SendingStatus
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.patient.PatientInfo
import kr.caredoc.careinsurance.reception.Reception
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQuery
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQueryHandler
import kr.caredoc.careinsurance.reception.ReferenceReceptionNotExistsException
import kr.caredoc.careinsurance.reception.exception.ReceptionNotFoundByIdException
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

class CaregivingStartMessageServiceTest : BehaviorSpec({
    given("간병 시작 메시지 서비스가 주어졌을때") {
        val receptionsByIdsQueryHandler = relaxedMock<ReceptionsByIdsQueryHandler>()
        val alimtalkSender = relaxedMock<AlimtalkSender>()
        val caregivingStartMessageSummaryRepository = relaxedMock<CaregivingStartMessageSummaryRepository>()
        val caregivingStartMessageSendingHistoryRepository =
            relaxedMock<CaregivingStartMessageSendingHistoryRepository>()
        val patientNameHasher = LocalEncryption.patientNameHasher
        val service = CaregivingStartMessageService(
            receptionsByIdsQueryHandler = receptionsByIdsQueryHandler,
            caregivingStartMessageSummaryRepository = caregivingStartMessageSummaryRepository,
            caregivingStartMessageSendingHistoryRepository = caregivingStartMessageSendingHistoryRepository,
            alimtalkSender = alimtalkSender,
            decryptor = LocalEncryption.LocalDecryptor,
            patientNameHasher = patientNameHasher,
        )

        beforeEach {
            with(caregivingStartMessageSummaryRepository) {
                val savingEntitySlot = slot<CaregivingStartMessageSummary>()
                every { save(capture(savingEntitySlot)) } answers {
                    savingEntitySlot.captured
                }
            }

            with(caregivingStartMessageSendingHistoryRepository) {
                val savingEntitySlot = slot<CaregivingStartMessageSendingHistory>()
                every { save(capture(savingEntitySlot)) } answers {
                    savingEntitySlot.captured
                }
            }

            with(receptionsByIdsQueryHandler) {
                val encryptor = PatientInfoEncryptor(LocalEncryption.LocalEncryptor, LocalEncryption.patientNameHasher)

                val receptionsByIds = mapOf<String, Reception>(
                    "01GPJMK7ZPBBKTY3TP0NN5JWCJ" to decryptableMockReception {
                        every { id } returns "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                        every { patientInfo.primaryContact } returns encryptor.encrypt(
                            PatientInfo.Contact(
                                phoneNumber = "01011112222",
                                relationshipWithPatient = "본인",
                            )
                        )
                    },
                    "01GSC4SKPGWMZDP7EKWQ0Z57NG" to decryptableMockReception {
                        every { id } returns "01GSC4SKPGWMZDP7EKWQ0Z57NG"
                        every { patientInfo.primaryContact } returns encryptor.encrypt(
                            PatientInfo.Contact(
                                phoneNumber = "01011113333",
                                relationshipWithPatient = "본인",
                            )
                        )
                    }
                )
                val receptionsByIdsQuerySlot = slot<ReceptionsByIdsQuery>()
                every {
                    getReceptions(capture(receptionsByIdsQuerySlot))
                } answers {
                    val capturedReceptionIds = receptionsByIdsQuerySlot.captured.receptionIds

                    capturedReceptionIds.map { receptionsByIds[it] ?: throw ReceptionNotFoundByIdException(it) }
                }
            }

            with(alimtalkSender) {
                val bulkMessageSlot = slot<BulkAlimtalkMessage>()

                every {
                    send(capture(bulkMessageSlot))
                } answers {
                    bulkMessageSlot.captured.messageParameters.map {
                        SendingResultOfMessage(
                            messageId = it.id,
                            sentMessageId = if (it.recipient == "01011112222") {
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

        and("또한 이미 등록된 간병 시작 메시지 요약이 주어졌을때") {
            val registeredCaregivingStartMessageSummaries = listOf(
                relaxedMock<CaregivingStartMessageSummary>(),
                relaxedMock<CaregivingStartMessageSummary>(),
            )

            beforeEach {
                with(caregivingStartMessageSummaryRepository) {
                    val pageableSlot = slot<Pageable>()
                    every {
                        findByExpectedSendingDate(
                            LocalDate.of(2023, 1, 30),
                            capture(pageableSlot),
                        )
                    } answers {
                        PageImpl(
                            registeredCaregivingStartMessageSummaries,
                            pageableSlot.captured,
                            registeredCaregivingStartMessageSummaries.size.toLong(),
                        )
                    }

                    every {
                        caregivingStartMessageSummaryRepository.findByExpectedSendingDateAndSendingStatus(
                            LocalDate.of(2023, 1, 30),
                            SendingStatus.READY,
                            capture(pageableSlot),
                        )
                    } answers {
                        PageImpl(
                            registeredCaregivingStartMessageSummaries,
                            pageableSlot.captured,
                            registeredCaregivingStartMessageSummaries.size.toLong(),
                        )
                    }

                    every {
                        searchByExpectedSendingDateAndAccidentNumberKeyword(
                            LocalDate.of(2023, 1, 30),
                            "1997",
                            capture(pageableSlot),
                        )
                    } answers {
                        PageImpl(
                            registeredCaregivingStartMessageSummaries,
                            pageableSlot.captured,
                            registeredCaregivingStartMessageSummaries.size.toLong(),
                        )
                    }

                    every {
                        searchByExpectedSendingDateAndHashedPatientName(
                            LocalDate.of(2023, 1, 30),
                            patientNameHasher.hashAsHex("박재병"),
                            capture(pageableSlot),
                        )
                    } answers {
                        PageImpl(
                            registeredCaregivingStartMessageSummaries,
                            pageableSlot.captured,
                            registeredCaregivingStartMessageSummaries.size.toLong(),
                        )
                    }

                    every {
                        searchByExpectedSendingDateAndSendingStatusAndHashedPatientName(
                            LocalDate.of(2023, 1, 30),
                            SendingStatus.SENT,
                            patientNameHasher.hashAsHex("박재병"),
                            capture(pageableSlot),
                        )
                    } answers {
                        PageImpl(
                            registeredCaregivingStartMessageSummaries,
                            pageableSlot.captured,
                            registeredCaregivingStartMessageSummaries.size.toLong(),
                        )
                    }

                    every {
                        searchByExpectedSendingDateAndSendingStatusAndAccidentNumberKeyword(
                            LocalDate.of(2023, 1, 30),
                            SendingStatus.FAILED,
                            "1997",
                            capture(pageableSlot),
                        )
                    } answers {
                        PageImpl(
                            registeredCaregivingStartMessageSummaries,
                            pageableSlot.captured,
                            registeredCaregivingStartMessageSummaries.size.toLong(),
                        )
                    }

                    every {
                        findByReceptionId("01GPJMK7ZPBBKTY3TP0NN5JWCJ")
                    } returns listOf(registeredCaregivingStartMessageSummaries[0])
                    every {
                        findByReceptionId("01GSC4SKPGWMZDP7EKWQ0Z57NG")
                    } returns listOf(registeredCaregivingStartMessageSummaries[1])
                }
            }

            afterEach { clearAllMocks() }

            `when`("발송 일자 기준으로 간병 시작 메시지 목록을 조회하면") {
                val query = CaregivingStartMessageSummariesByFilterQuery(
                    filter = CaregivingStartMessageSummaryFilter(
                        date = LocalDate.of(2023, 1, 30),
                        sendingStatus = null,
                    ),
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = service.getCaregivingStartMessageSummaries(
                    query,
                    PageRequest.of(0, 10),
                )

                then("리포지토리로부터 간병 시작 메시지 요약 목록을 조회합니다.") {
                    behavior()

                    verify {
                        caregivingStartMessageSummaryRepository.findByExpectedSendingDate(
                            LocalDate.of(2023, 1, 30),
                            PageRequest.of(
                                0,
                                10,
                                Sort.by(
                                    Sort.Order.desc(CaregivingStartMessageSummaryRepository.RECEPTION_RECEIVED_DATE_TIME_ORDERING),
                                ),
                            ),
                        )
                    }
                }

                then("조회한 간병 시작 메시지 요약 목록을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.content shouldBe registeredCaregivingStartMessageSummaries
                }
            }

            `when`("발송 상태로 필터링하여 발송 일자 기준으로 간병 시작 메시지 목록을 조회하면") {
                val query = CaregivingStartMessageSummariesByFilterQuery(
                    filter = CaregivingStartMessageSummaryFilter(
                        date = LocalDate.of(2023, 1, 30),
                        sendingStatus = SendingStatus.READY,
                    ),
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = service.getCaregivingStartMessageSummaries(
                    query,
                    PageRequest.of(0, 10),
                )

                then("리포지토리로부터 간병 시작 메시지 요약 목록을 조회합니다.") {
                    behavior()

                    verify {
                        caregivingStartMessageSummaryRepository.findByExpectedSendingDateAndSendingStatus(
                            LocalDate.of(2023, 1, 30),
                            SendingStatus.READY,
                            PageRequest.of(
                                0,
                                10,
                                Sort.by(
                                    Sort.Order.desc(CaregivingStartMessageSummaryRepository.RECEPTION_RECEIVED_DATE_TIME_ORDERING),
                                ),
                            ),
                        )
                    }
                }

                then("조회한 간병 시작 메시지 요약 목록을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.content shouldBe registeredCaregivingStartMessageSummaries
                }
            }

            `when`("내부 사용자 권한 없이 간병 시작 메시지 목록을 조회하면") {
                val query = CaregivingStartMessageSummariesByFilterQuery(
                    filter = CaregivingStartMessageSummaryFilter(
                        date = LocalDate.of(2023, 1, 30),
                        sendingStatus = SendingStatus.READY,
                    ),
                    subject = generateExternalCaregivingOrganizationManagerSubject("01GVG25A2P5V6MVQG2SPKQ4D50")
                )

                fun behavior() = service.getCaregivingStartMessageSummaries(
                    query,
                    PageRequest.of(0, 10),
                )

                then("AccessDeniedException이 발생합니다.") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }

            `when`("발송 일자 기준으로 간병 시작 메시지 목록을 사고 번호로 검색하면") {
                val query = CaregivingStartMessageSummarySearchQuery(
                    filter = CaregivingStartMessageSummaryFilter(
                        date = LocalDate.of(2023, 1, 30),
                        sendingStatus = null,
                    ),
                    searchCondition = SearchCondition(
                        searchingProperty = CaregivingStartMessageSummarySearchQuery.SearchingProperty.ACCIDENT_NUMBER,
                        keyword = "1997",
                    ),
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = service.searchCaregivingStartMessageSummary(
                    query,
                    PageRequest.of(0, 10),
                )

                then("리포지토리로부터 간병 시작 메시지 요약 목록을 조회합니다.") {
                    behavior()

                    verify {
                        caregivingStartMessageSummaryRepository.searchByExpectedSendingDateAndAccidentNumberKeyword(
                            LocalDate.of(2023, 1, 30),
                            "1997",
                            PageRequest.of(
                                0,
                                10,
                                Sort.by(
                                    Sort.Order.desc(CaregivingStartMessageSummaryRepository.RECEPTION_RECEIVED_DATE_TIME_ORDERING),
                                ),
                            ),
                        )
                    }
                }

                then("조회한 간병 시작 메시지 요약 목록을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.content shouldBe registeredCaregivingStartMessageSummaries
                }
            }

            `when`("발송 일자 기준으로 간병 시작 메시지 목록을 환자 이름으로 검색하면") {
                val query = CaregivingStartMessageSummarySearchQuery(
                    filter = CaregivingStartMessageSummaryFilter(
                        date = LocalDate.of(2023, 1, 30),
                        sendingStatus = null,
                    ),
                    searchCondition = SearchCondition(
                        searchingProperty = CaregivingStartMessageSummarySearchQuery.SearchingProperty.PATIENT_NAME,
                        keyword = "박재병",
                    ),
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = service.searchCaregivingStartMessageSummary(
                    query,
                    PageRequest.of(0, 10),
                )

                then("리포지토리로부터 간병 시작 메시지 요약 목록을 검색합니다.") {
                    behavior()

                    verify {
                        caregivingStartMessageSummaryRepository.searchByExpectedSendingDateAndHashedPatientName(
                            LocalDate.of(2023, 1, 30),
                            patientNameHasher.hashAsHex("박재병"),
                            PageRequest.of(
                                0,
                                10,
                                Sort.by(
                                    Sort.Order.desc(CaregivingStartMessageSummaryRepository.RECEPTION_RECEIVED_DATE_TIME_ORDERING),
                                ),
                            ),
                        )
                    }
                }

                then("조회한 간병 시작 메시지 요약 목록을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.content shouldBe registeredCaregivingStartMessageSummaries
                }
            }

            `when`("발송 일자 기준으로 발송 상태 필터를 적용하여 간병 시작 메시지 목록을 사고 번호로 검색하면") {
                val query = CaregivingStartMessageSummarySearchQuery(
                    filter = CaregivingStartMessageSummaryFilter(
                        date = LocalDate.of(2023, 1, 30),
                        sendingStatus = SendingStatus.FAILED,
                    ),
                    searchCondition = SearchCondition(
                        searchingProperty = CaregivingStartMessageSummarySearchQuery.SearchingProperty.ACCIDENT_NUMBER,
                        keyword = "1997",
                    ),
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = service.searchCaregivingStartMessageSummary(
                    query,
                    PageRequest.of(0, 10),
                )

                then("리포지토리로부터 간병 시작 메시지 요약 목록을 조회합니다.") {
                    behavior()

                    verify {
                        caregivingStartMessageSummaryRepository.searchByExpectedSendingDateAndSendingStatusAndAccidentNumberKeyword(
                            LocalDate.of(2023, 1, 30),
                            SendingStatus.FAILED,
                            "1997",
                            PageRequest.of(
                                0,
                                10,
                                Sort.by(
                                    Sort.Order.desc(CaregivingStartMessageSummaryRepository.RECEPTION_RECEIVED_DATE_TIME_ORDERING),
                                ),
                            ),
                        )
                    }
                }

                then("조회한 간병 시작 메시지 요약 목록을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.content shouldBe registeredCaregivingStartMessageSummaries
                }
            }

            `when`("발송 일자 기준으로 발송 상태 필터를 적용하여 간병 시작 메시지 목록을 환자 이름으로 검색하면") {
                val query = CaregivingStartMessageSummarySearchQuery(
                    filter = CaregivingStartMessageSummaryFilter(
                        date = LocalDate.of(2023, 1, 30),
                        sendingStatus = SendingStatus.SENT,
                    ),
                    searchCondition = SearchCondition(
                        searchingProperty = CaregivingStartMessageSummarySearchQuery.SearchingProperty.PATIENT_NAME,
                        keyword = "박재병",
                    ),
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = service.searchCaregivingStartMessageSummary(
                    query,
                    PageRequest.of(0, 10),
                )

                then("리포지토리로부터 간병 시작 메시지 요약 목록을 검색합니다.") {
                    behavior()

                    verify {
                        caregivingStartMessageSummaryRepository.searchByExpectedSendingDateAndSendingStatusAndHashedPatientName(
                            LocalDate.of(2023, 1, 30),
                            SendingStatus.SENT,
                            patientNameHasher.hashAsHex("박재병"),
                            PageRequest.of(
                                0,
                                10,
                                Sort.by(
                                    Sort.Order.desc(CaregivingStartMessageSummaryRepository.RECEPTION_RECEIVED_DATE_TIME_ORDERING),
                                ),
                            ),
                        )
                    }
                }

                then("조회한 간병 시작 메시지 요약 목록을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.content shouldBe registeredCaregivingStartMessageSummaries
                }
            }

            `when`("첫번째 간병 회차의 시작 시간이 변경되면") {
                val event = relaxedMock<CaregivingRoundModified>()

                beforeEach {
                    with(event) {
                        every { receptionId } returns "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                        every { caregivingRoundId } returns "01H047ASNAG016RRP89C5A7F57"
                        every { caregivingRoundNumber } returns 1
                        every { startDateTime } returns Modification(
                            LocalDateTime.of(2022, 1, 28, 14, 51, 20),
                            LocalDateTime.of(2022, 1, 29, 14, 51, 20),
                        )
                    }
                }

                afterEach { clearAllMocks() }

                fun handling() = service.handleCaregivingRoundModified(event)

                then("간병 회차의 간병 시작 메시지 요약을 조회합니다.") {
                    handling()

                    verify {
                        caregivingStartMessageSummaryRepository.findByReceptionId("01GPJMK7ZPBBKTY3TP0NN5JWCJ")
                    }
                }

                then("간병 회차가 변경되었음을 간병 시작 메시지 요약에 전달합니다.") {
                    handling()

                    verify {
                        registeredCaregivingStartMessageSummaries[0].handleCaregivingRoundModified(event)
                    }
                }
            }

            `when`("첫번째 간병이 아닌 간병 회차의 시작 시간이 변경되면") {
                val event = relaxedMock<CaregivingRoundModified>()

                beforeEach {
                    with(event) {
                        every { caregivingRoundId } returns "01H047ASNAG016RRP89C5A7F57"
                        every { caregivingRoundNumber } returns 2
                        every { startDateTime } returns Modification(
                            LocalDateTime.of(2022, 1, 28, 14, 51, 20),
                            LocalDateTime.of(2022, 1, 29, 14, 51, 20),
                        )
                    }
                }

                afterEach { clearAllMocks() }

                fun handling() = service.handleCaregivingRoundModified(event)

                then("아무것도 안합니다.") {
                    handling()

                    verify(exactly = 0) {
                        caregivingStartMessageSummaryRepository.findByReceptionId(any())
                    }
                }
            }

            `when`("첫번째 간병이 변경되었지만 시작시간이 날짜 수준으로 변경되지 않았다면") {
                val event = relaxedMock<CaregivingRoundModified>()

                beforeEach {
                    with(event) {
                        every { caregivingRoundId } returns "01H047ASNAG016RRP89C5A7F57"
                        every { caregivingRoundNumber } returns 1
                        every { startDateTime } returns Modification(
                            LocalDateTime.of(2022, 1, 28, 14, 51, 20),
                            LocalDateTime.of(2022, 1, 28, 13, 0, 0),
                        )
                    }
                }

                afterEach { clearAllMocks() }

                fun handling() = service.handleCaregivingRoundModified(event)

                then("아무것도 안합니다.") {
                    handling()

                    verify(exactly = 0) {
                        caregivingStartMessageSummaryRepository.findByReceptionId(any())
                    }
                }
            }

            `when`("간병 시작 안내 메시지를 발송하도록 요청하면") {
                val command = CaregivingStartMessageSendingCommand(
                    targetReceptionIds = setOf(
                        "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                        "01GSC4SKPGWMZDP7EKWQ0Z57NG",
                    ),
                    subject = generateInternalCaregivingManagerSubject()
                )

                fun behavior() = service.sendCaregivingStartMessage(command)

                then("발송 대상이 된 간병 접수 목록을 조회합니다.") {
                    behavior()

                    verify {
                        receptionsByIdsQueryHandler.getReceptions(
                            withArg {
                                it.receptionIds shouldContainAll setOf(
                                    "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                                    "01GSC4SKPGWMZDP7EKWQ0Z57NG",
                                )
                            }
                        )
                    }
                }

                then("간병 접수에 입력된 전화번호로 알림톡 발송을 요청합니다.") {
                    behavior()

                    verify {
                        alimtalkSender.send(
                            withArg {
                                it.templateCode shouldBe "M-20230823-3"
                                it.messageParameters.map { messageParameter -> messageParameter.recipient } shouldContainAll setOf(
                                    "01011112222",
                                    "01011113333",
                                )
                            }
                        )
                    }
                }

                then("간병 시작 안내 메시지를 보낸 기록을 남깁니다.") {
                    withFixedClock(LocalDateTime.of(2023, 1, 30, 16, 30, 0)) {
                        behavior()
                    }

                    verify {
                        caregivingStartMessageSendingHistoryRepository.save(
                            withArg {
                                it.receptionId shouldBe "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                                it.messageId shouldBe "124c3425-1743-470c-843b-b175bd5f8cbf"
                                it.result shouldBe CaregivingStartMessageSendingHistory.SendingResult.SENT
                                it.attemptDateTime shouldBe LocalDateTime.of(2023, 1, 30, 16, 30, 0)
                            }
                        )
                        caregivingStartMessageSendingHistoryRepository.save(
                            withArg {
                                it.receptionId shouldBe "01GSC4SKPGWMZDP7EKWQ0Z57NG"
                                it.messageId shouldBe null
                                it.result shouldBe CaregivingStartMessageSendingHistory.SendingResult.FAILED
                                it.attemptDateTime shouldBe LocalDateTime.of(2023, 1, 30, 16, 30, 0)
                            }
                        )
                    }
                }

                then("발송 대상이 된 접수의 발송 요약을 갱신합니다.") {
                    withFixedClock(LocalDateTime.of(2023, 1, 30, 16, 30, 0)) {
                        behavior()
                    }

                    verify {
                        registeredCaregivingStartMessageSummaries[0].updateSendingResult(
                            SendingStatus.SENT,
                            LocalDateTime.of(2023, 1, 30, 16, 30, 0),
                        )
                        registeredCaregivingStartMessageSummaries[1].updateSendingResult(
                            SendingStatus.FAILED,
                            null,
                        )
                    }
                }
            }

            `when`("존재하지 않는 접수에 대해서 간병 시작 안내 메시지를 발송하도록 요청하면") {
                val command = CaregivingStartMessageSendingCommand(
                    targetReceptionIds = setOf(
                        "01GPJMK7ZPBBKTY3TP0NN5JWCK",
                    ),
                    subject = generateInternalCaregivingManagerSubject()
                )

                fun behavior() = service.sendCaregivingStartMessage(command)

                then("ReferenceReceptionNotExistsException이 발생합니다.") {
                    val thrownException = shouldThrow<ReferenceReceptionNotExistsException> { behavior() }

                    thrownException.referenceReceptionId shouldBe "01GPJMK7ZPBBKTY3TP0NN5JWCK"
                }
            }

            `when`("내부 사용자 권한 없이 간병 시작 안내 메시지를 발송하도록 요청하면") {
                val command = CaregivingStartMessageSendingCommand(
                    targetReceptionIds = setOf(
                        "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                        "01GSC4SKPGWMZDP7EKWQ0Z57NG",
                    ),
                    subject = generateExternalCaregivingOrganizationManagerSubject("01GVG25A2P5V6MVQG2SPKQ4D50"),
                )

                fun behavior() = service.sendCaregivingStartMessage(command)

                then("AccessDeniedException이 발생합니다.") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }
        }

        `when`("첫번째 간병 회차의 시작시간이 생기면") {
            val event = relaxedMock<CaregivingRoundStarted>()

            beforeEach {
                with(event) {
                    every { receptionId } returns "01GWK30517ZTHWDW1QQ22V6QZC"
                    every { caregivingRoundId } returns "01H047ASNAG016RRP89C5A7F57"
                    every { caregivingRoundNumber } returns 1
                    every { startDateTime } returns LocalDateTime.of(2022, 1, 29, 14, 51, 20)
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = service.handleCaregivingRoundStarted(event)

            then("간병 회차의 간병 시작 메시지 요약을 생성합니다.") {
                handling()

                verify {
                    caregivingStartMessageSummaryRepository.save(
                        withArg {
                            it.receptionId shouldBe "01GWK30517ZTHWDW1QQ22V6QZC"
                            it.firstCaregivingRoundId shouldBe "01H047ASNAG016RRP89C5A7F57"
                            it.sendingStatus shouldBe SendingStatus.READY
                            it.sentDate shouldBe null
                            it.expectedSendingDate shouldBe LocalDate.of(2022, 1, 30)
                        }
                    )
                }
            }
        }

        `when`("첫번째 간병 회차의 시작 시간이 변경되면") {
            val event = relaxedMock<CaregivingRoundModified>()

            val caregivingStartMessageSummaries = listOf(
                relaxedMock<CaregivingStartMessageSummary>(),
                relaxedMock<CaregivingStartMessageSummary>(),
                relaxedMock<CaregivingStartMessageSummary>(),
            )

            beforeEach {
                with(event) {
                    every { receptionId } returns "01GWK30517ZTHWDW1QQ22V6QZC"
                    every { caregivingRoundId } returns "01H047ASNAG016RRP89C5A7F57"
                    every { caregivingRoundNumber } returns 1
                    every { startDateTime } returns Modification(
                        LocalDateTime.of(2022, 1, 28, 14, 51, 20),
                        LocalDateTime.of(2022, 1, 29, 14, 51, 20),
                    )
                }

                with(caregivingStartMessageSummaryRepository) {
                    every { findByReceptionId(match { it == "01GWK30517ZTHWDW1QQ22V6QZC" }) } returns caregivingStartMessageSummaries
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = service.handleCaregivingRoundModified(event)

            then("간병 시작 메시지의 예상 발송일 변경을 요청합니다.") {
                handling()

                caregivingStartMessageSummaries.forEach {
                    verify {
                        it.handleCaregivingRoundModified(event)
                    }
                }
            }
        }

        `when`("첫번째 간병이 아닌 간병 회차의 시작 시간이 변경되면") {
            val event = relaxedMock<CaregivingRoundModified>()

            beforeEach {
                with(event) {
                    every { receptionId } returns "01GWK30517ZTHWDW1QQ22V6QZC"
                    every { caregivingRoundId } returns "01H047ASNAG016RRP89C5A7F57"
                    every { caregivingRoundNumber } returns 2
                    every { startDateTime } returns Modification(
                        null,
                        LocalDateTime.of(2022, 1, 29, 14, 51, 20),
                    )
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = service.handleCaregivingRoundModified(event)

            then("아무것도 안합니다.") {
                handling()

                verify(exactly = 0) {
                    caregivingStartMessageSummaryRepository.save(any())
                }
            }
        }
    }
})
