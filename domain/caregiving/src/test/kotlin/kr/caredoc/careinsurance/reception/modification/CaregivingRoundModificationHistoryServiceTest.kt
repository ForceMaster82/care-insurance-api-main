package kr.caredoc.careinsurance.reception.modification

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyAll
import kr.caredoc.careinsurance.caregiving.CaregivingRoundModified
import kr.caredoc.careinsurance.generateExternalCaregivingOrganizationManagerSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.patient.Sex
import kr.caredoc.careinsurance.reception.ReceptionByIdQueryHandler
import kr.caredoc.careinsurance.reception.ReceptionReceived
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.security.accesscontrol.SubjectAttribute
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
class CaregivingRoundModificationHistoryServiceTest(
    @Autowired
    private val cacheCaregivingRoundModificationHistoryRepository: CaregivingRoundModificationHistoryRepository,
) : BehaviorSpec({
    given("간병 회차 수정 내역 서비스가 주어졌을때") {
        val receptionByIdQueryHandler = relaxedMock<ReceptionByIdQueryHandler>()
        val caregivingRoundModificationSummaryRepository = relaxedMock<CaregivingRoundModificationSummaryRepository>()
        val caregivingRoundModificationHistoryRepository = relaxedMock<CaregivingRoundModificationHistoryRepository>()
        val service = CaregivingRoundModificationHistoryService(
            caregivingRoundModificationSummaryRepository = caregivingRoundModificationSummaryRepository,
            receptionByIdQueryHandler = receptionByIdQueryHandler,
            caregivingRoundModificationHistoryRepository = caregivingRoundModificationHistoryRepository,
        )

        beforeEach {
            justRun {
                receptionByIdQueryHandler.ensureReceptionExists(
                    match { it.receptionId == "01GPJMK7ZPBBKTY3TP0NN5JWCJ" }
                )
            }

            with(caregivingRoundModificationSummaryRepository) {
                every { findTopByReceptionId(any()) } returns null
                val savingEntitySlot = slot<CaregivingRoundModificationSummary>()
                every { save(capture(savingEntitySlot)) } answers { savingEntitySlot.captured }
            }
            with(caregivingRoundModificationHistoryRepository) {
                val savingEntitySlot = slot<CaregivingRoundModificationHistory>()
                every { save(capture(savingEntitySlot)) } answers { savingEntitySlot.captured }
            }
        }

        afterEach { clearAllMocks() }

        and("이미 등록된 간병 회차 수정 내역이 존재할때") {
            val registeredModificationSummary = relaxedMock<CaregivingRoundModificationSummary>()
            val registeredModificationHistories = listOf<CaregivingRoundModificationHistory>(
                relaxedMock(),
                relaxedMock(),
                relaxedMock(),
            )

            beforeEach {
                every {
                    caregivingRoundModificationSummaryRepository.findTopByReceptionId("01GPJMK7ZPBBKTY3TP0NN5JWCJ")
                } returns registeredModificationSummary
                val pageableSlot = slot<Pageable>()
                every {
                    caregivingRoundModificationHistoryRepository.findByReceptionId(
                        match { it == "01GPJMK7ZPBBKTY3TP0NN5JWCJ" },
                        capture(pageableSlot),
                    )
                } answers {
                    PageImpl(
                        registeredModificationHistories,
                        pageableSlot.captured,
                        registeredModificationHistories.size.toLong(),
                    )
                }
            }

            afterEach { clearAllMocks() }

            `when`("접수 아이디로 간병 회차 수정 개요를 조회하면") {
                val query = CaregivingRoundModificationSummaryByReceptionIdQuery(
                    receptionId = "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = service.getCaregivingRoundModificationSummary(query)

                then("접수가 존재하며 접수에 접근 가능한지 검증합니다.") {
                    behavior()

                    verify {
                        receptionByIdQueryHandler.ensureReceptionExists(
                            withArg {
                                it.receptionId shouldBe "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                            }
                        )
                    }
                }

                then("리포지토리로부터 접수 아이디를 기준으로 수정 개요를 조회합니다.") {
                    behavior()

                    verify {
                        caregivingRoundModificationSummaryRepository.findTopByReceptionId("01GPJMK7ZPBBKTY3TP0NN5JWCJ")
                    }
                }

                then("조회한 수정 개요를 반환합니다.") {
                    val actualResult = behavior()

                    actualResult shouldBe registeredModificationSummary
                }
            }

            `when`("내부 사용자 권한 없이 접수 아이디로 간병 회차 수정 개요를 조회하면") {
                val query = CaregivingRoundModificationSummaryByReceptionIdQuery(
                    receptionId = "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                    subject = generateExternalCaregivingOrganizationManagerSubject("01H1972AQ7M5MMMWBKVZ4G1DQV"),
                )

                fun behavior() = service.getCaregivingRoundModificationSummary(query)

                then("throws AccessDeniedException") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }
            `when`("간병 회차에 간병인이 최초 등록되면") {
                val event = relaxedMock<CaregivingRoundModified>()

                beforeEach {
                    every { event.editingSubject[SubjectAttribute.USER_ID] } returns setOf("01GDYB3M58TBBXG1A0DJ1B866V")
                    every { event.receptionId } returns "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                    every { event.caregivingRoundNumber } returns 1
                    every { event.cause } returns CaregivingRoundModified.Cause.DIRECT_EDIT
                    every { event.caregiverInfo } returns Modification(
                        null,
                        relaxedMock {
                            every { caregiverOrganizationId } returns "01GPWXVJB2WPDNXDT5NE3B964N"
                            every { name } returns "박간병"
                            every { sex } returns Sex.FEMALE
                            every { birthDate } returns LocalDate.of(1975, 4, 28)
                            every { phoneNumber } returns "01011113333"
                            every { dailyCaregivingCharge } returns 110000
                            every { commissionFee } returns 11000
                            every { insured } returns true
                            every { accountInfo.bank } returns "농협"
                            every { accountInfo.accountHolder } returns "박간병"
                            every { accountInfo.accountNumber } returns "2222222222222"
                        },
                    )
                    every { event.startDateTime } returns Modification(
                        LocalDateTime.of(2023, 4, 28, 17, 23, 0),
                        LocalDateTime.of(2023, 4, 28, 17, 23, 0),
                    )
                    every { event.endDateTime } returns Modification(
                        LocalDateTime.of(2023, 5, 5, 17, 23, 0),
                        LocalDateTime.of(2023, 5, 5, 17, 23, 0),
                    )
                    every { event.remarks } returns Modification("", "")
                    every { event.modifiedDateTime } returns LocalDateTime.of(2023, 4, 16, 12, 3, 4)
                }

                afterEach { clearAllMocks() }

                fun handling() = service.handleCaregivingRoundModified(event)

                then("아무런 기록도 남기지 않습니다.") {
                    handling()

                    verify(exactly = 0) {
                        caregivingRoundModificationHistoryRepository.save(any())
                    }
                }
            }

            `when`("간병 회차가 직접 수정되면") {
                val event = relaxedMock<CaregivingRoundModified>()

                beforeEach {
                    every { event.editingSubject[SubjectAttribute.USER_ID] } returns setOf("01GDYB3M58TBBXG1A0DJ1B866V")
                    every { event.receptionId } returns "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                    every { event.caregivingRoundNumber } returns 1
                    every { event.cause } returns CaregivingRoundModified.Cause.DIRECT_EDIT
                    every { event.caregiverInfo } returns Modification(
                        relaxedMock {
                            every { caregiverOrganizationId } returns null
                            every { name } returns "김간병"
                            every { sex } returns Sex.MALE
                            every { birthDate } returns LocalDate.of(2023, 4, 28)
                            every { phoneNumber } returns "01011112222"
                            every { dailyCaregivingCharge } returns 100000
                            every { commissionFee } returns 10000
                            every { insured } returns false
                            every { accountInfo.bank } returns "신한"
                            every { accountInfo.accountHolder } returns "김간병"
                            every { accountInfo.accountNumber } returns "1111111111111"
                        },
                        relaxedMock {
                            every { caregiverOrganizationId } returns "01GPWXVJB2WPDNXDT5NE3B964N"
                            every { name } returns "박간병"
                            every { sex } returns Sex.FEMALE
                            every { birthDate } returns LocalDate.of(1975, 4, 28)
                            every { phoneNumber } returns "01011113333"
                            every { dailyCaregivingCharge } returns 110000
                            every { commissionFee } returns 11000
                            every { insured } returns true
                            every { accountInfo.bank } returns "농협"
                            every { accountInfo.accountHolder } returns "박간병"
                            every { accountInfo.accountNumber } returns "2222222222222"
                        },
                    )
                    every { event.startDateTime } returns Modification(
                        LocalDateTime.of(2023, 4, 28, 17, 23, 51),
                        LocalDateTime.of(2023, 4, 28, 17, 23, 0),
                    )
                    every { event.endDateTime } returns Modification(
                        LocalDateTime.of(2023, 5, 5, 17, 23, 51),
                        LocalDateTime.of(2023, 5, 5, 17, 23, 0),
                    )
                    every { event.remarks } returns Modification("", "1회차 해보고 만족해서 계속한다고 함.")
                    every { event.modifiedDateTime } returns LocalDateTime.of(2023, 4, 16, 12, 3, 4)
                }

                afterEach { clearAllMocks() }

                fun handling() = service.handleCaregivingRoundModified(event)

                then("수정 개요에 간병 회차가 수정되었음을 알립니다.") {
                    handling()

                    verify {
                        registeredModificationSummary.handleCaregivingRoundModified(event)
                    }
                }

                then("수정된 항목들을 기록으로 저장합니다.") {
                    handling()

                    fun verifyModification(
                        modifiedProperty: CaregivingRoundModificationHistory.ModifiedProperty,
                        previous: String?,
                        modified: String?,
                    ) {
                        verify {
                            caregivingRoundModificationHistoryRepository.save(
                                withArg {
                                    it.modifiedProperty shouldBe modifiedProperty
                                    it.previous shouldBe previous
                                    it.modified shouldBe modified
                                }
                            )
                        }
                    }

                    verifyAll {
                        caregivingRoundModificationHistoryRepository.save(
                            withArg {
                                it.receptionId shouldBe "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                                it.caregivingRoundNumber shouldBe 1
                                it.modifierId shouldBe "01GDYB3M58TBBXG1A0DJ1B866V"
                                it.modifiedDateTime shouldBe LocalDateTime.of(2023, 4, 16, 12, 3, 4)
                            }
                        )
                    }
                    verifyModification(
                        CaregivingRoundModificationHistory.ModifiedProperty.CAREGIVER_ORGANIZATION_ID,
                        null,
                        "01GPWXVJB2WPDNXDT5NE3B964N",
                    )
                    verifyModification(
                        CaregivingRoundModificationHistory.ModifiedProperty.CAREGIVER_NAME,
                        "김간병",
                        "박간병",
                    )
                    verifyModification(
                        CaregivingRoundModificationHistory.ModifiedProperty.CAREGIVER_SEX,
                        "MALE",
                        "FEMALE",
                    )
                    verifyModification(
                        CaregivingRoundModificationHistory.ModifiedProperty.CAREGIVER_BIRTH_DATE,
                        "2023-04-28",
                        "1975-04-28",
                    )
                    verifyModification(
                        CaregivingRoundModificationHistory.ModifiedProperty.CAREGIVER_PHONE_NUMBER,
                        "01011112222",
                        "01011113333",
                    )
                    verifyModification(
                        CaregivingRoundModificationHistory.ModifiedProperty.DAILY_CAREGIVING_CHARGE,
                        "100000",
                        "110000",
                    )
                    verifyModification(
                        CaregivingRoundModificationHistory.ModifiedProperty.COMMISSION_FEE,
                        "10000",
                        "11000",
                    )
                    verifyModification(
                        CaregivingRoundModificationHistory.ModifiedProperty.CAREGIVER_INSURED,
                        "false",
                        "true",
                    )
                    verifyModification(
                        CaregivingRoundModificationHistory.ModifiedProperty.CAREGIVER_ACCOUNT_BANK,
                        "신한",
                        "농협",
                    )
                    verifyModification(
                        CaregivingRoundModificationHistory.ModifiedProperty.CAREGIVER_ACCOUNT_HOLDER,
                        "김간병",
                        "박간병",
                    )
                    verifyModification(
                        CaregivingRoundModificationHistory.ModifiedProperty.CAREGIVER_ACCOUNT_NUMBER,
                        "1111111111111",
                        "2222222222222",
                    )
                    verifyModification(
                        CaregivingRoundModificationHistory.ModifiedProperty.START_DATE_TIME,
                        "2023-04-28T17:23:51",
                        "2023-04-28T17:23",
                    )
                    verifyModification(
                        CaregivingRoundModificationHistory.ModifiedProperty.END_DATE_TIME,
                        "2023-05-05T17:23:51",
                        "2023-05-05T17:23",
                    )
                    verifyModification(
                        CaregivingRoundModificationHistory.ModifiedProperty.REMARKS,
                        "",
                        "1회차 해보고 만족해서 계속한다고 함.",
                    )
                }
            }

            `when`("간병 회차가 직접 수정 외의 다른 이유로 수정되면") {
                val event = relaxedMock<CaregivingRoundModified>()

                beforeEach {
                    every { event.receptionId } returns "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                    every { event.cause } returns CaregivingRoundModified.Cause.ETC
                }

                afterEach { clearAllMocks() }

                fun handling() = service.handleCaregivingRoundModified(event)

                then("아무것도 안합니다.") {
                    handling()

                    verify(exactly = 0) {
                        registeredModificationSummary.handleCaregivingRoundModified(any())
                        caregivingRoundModificationHistoryRepository.save(any())
                    }
                }
            }

            `when`("간병 회차 수정 내역을 조회하면") {
                val query = CaregivingRoundModificationHistoriesByReceptionIdQuery(
                    receptionId = "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = service.getCaregivingRoundModificationHistories(
                    query = query,
                    pageRequest = PageRequest.of(0, 10),
                )

                then("접수가 존재하며 접수에 접근 가능한지 검증합니다.") {
                    behavior()

                    verify {
                        receptionByIdQueryHandler.ensureReceptionExists(
                            withArg {
                                it.receptionId shouldBe "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                            }
                        )
                    }
                }

                then("간병 회차 수정 내역을 리포지토리로부터 조회합니다.") {
                    behavior()

                    verify {
                        caregivingRoundModificationHistoryRepository.findByReceptionId(
                            withArg { it shouldBe "01GPJMK7ZPBBKTY3TP0NN5JWCJ" },
                            withArg {
                                it.pageSize shouldBe 10
                                it.pageNumber shouldBe 0
                            }
                        )
                    }
                }

                then("조회한 수정 내역을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.content shouldContainExactlyInAnyOrder registeredModificationHistories
                }
            }

            `when`("내부 사용자 권한 없이 간병 회차 수정 내역을 조회하면") {
                val query = CaregivingRoundModificationHistoriesByReceptionIdQuery(
                    receptionId = "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                    subject = generateExternalCaregivingOrganizationManagerSubject("01H1972AQ7M5MMMWBKVZ4G1DQV"),
                )

                fun behavior() = service.getCaregivingRoundModificationHistories(
                    query = query,
                    pageRequest = PageRequest.of(0, 10),
                )

                then("throws AccessDeniedException") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }
        }

        `when`("접수 아이디로 간병 회차 수정 개요를 조회하면") {
            val query = CaregivingRoundModificationSummaryByReceptionIdQuery(
                receptionId = "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                subject = generateInternalCaregivingManagerSubject(),
            )

            fun behavior() = service.getCaregivingRoundModificationSummary(query)

            then("CaregivingRoundModificationSummaryNotFoundByReceptionIdException이 발생합니다.") {
                shouldThrow<CaregivingRoundModificationSummaryNotFoundByReceptionIdException> { behavior() }
            }
        }

        `when`("접수가 등록되면") {
            val event = relaxedMock<ReceptionReceived>()

            beforeEach {
                every { event.receptionId } returns "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
            }

            afterEach { clearAllMocks() }

            fun handling() = service.handleReceptionReceived(event)

            then("수정 개요를 저장합니다.") {
                handling()

                verify {
                    caregivingRoundModificationSummaryRepository.save(
                        withArg {
                            it.receptionId shouldBe "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                            it.lastModifiedDateTime shouldBe null
                            it.lastModifierId shouldBe null
                            it.modificationCount shouldBe 0
                        }
                    )
                }
            }
        }

        and("엔티티 테스트 할 때") {
            val caregivingRoundModificationHistory = CaregivingRoundModificationHistory(
                id = "01HFN8TKHD9G75SCDGAX8J9W4Q",
                receptionId = "01HFN8WNG61DP0J0P4NWA2A08M",
                caregivingRoundNumber = 1,
                modifiedProperty = CaregivingRoundModificationHistory.ModifiedProperty.CAREGIVER_NAME,
                previous = "간병인",
                modified = "헬퍼",
                modifierId = "01HFN8Y9MR0FS331SFK67QW8B6",
                modifiedDateTime = LocalDateTime.of(2023, 11, 20, 9, 50, 4)
            )
            `when`("저장을 요청하면") {
                fun behavior() = cacheCaregivingRoundModificationHistoryRepository.save(caregivingRoundModificationHistory)
                then("저장이 됩니다.") {
                    behavior()
                }
            }
            `when`("조회르 요청하면") {
                fun behavior() = cacheCaregivingRoundModificationHistoryRepository.findByIdOrNull("01HFN8TKHD9G75SCDGAX8J9W4Q")
                then("조회가 됩니다.") {
                    behavior()
                }
            }
        }
    }
})
