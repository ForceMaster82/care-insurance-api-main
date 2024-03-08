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
import kr.caredoc.careinsurance.caregiving.CaregivingCharge
import kr.caredoc.careinsurance.caregiving.CaregivingChargeModified
import kr.caredoc.careinsurance.generateExternalCaregivingOrganizationManagerSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.modification.Modification
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
class CaregivingChargeModificationHistoryServiceTest(
    @Autowired
    private val cacheCaregivingChargeModificationHistoryRepository: CaregivingChargeModificationHistoryRepository,
) : BehaviorSpec({
    given("간병비 산정 수정 내역 서비스가 주어졌을때") {
        val receptionByIdQueryHandler = relaxedMock<ReceptionByIdQueryHandler>()
        val caregivingChargeModificationSummaryRepository = relaxedMock<CaregivingChargeModificationSummaryRepository>()
        val caregivingChargeModificationHistoryRepository = relaxedMock<CaregivingChargeModificationHistoryRepository>()
        val service = CaregivingChargeModificationHistoryService(
            caregivingChargeModificationSummaryRepository = caregivingChargeModificationSummaryRepository,
            receptionByIdQueryHandler = receptionByIdQueryHandler,
            caregivingChargeModificationHistoryRepository = caregivingChargeModificationHistoryRepository,
        )

        beforeEach {
            justRun {
                receptionByIdQueryHandler.ensureReceptionExists(
                    match { it.receptionId == "01GPJMK7ZPBBKTY3TP0NN5JWCJ" }
                )
            }

            with(caregivingChargeModificationSummaryRepository) {
                every { findTopByReceptionId(any()) } returns null

                val savingEntitySlot = slot<CaregivingChargeModificationSummary>()
                every { save(capture(savingEntitySlot)) } answers { savingEntitySlot.captured }
            }

            with(caregivingChargeModificationHistoryRepository) {
                val savingEntitySlot = slot<CaregivingChargeModificationHistory>()
                every { save(capture(savingEntitySlot)) } answers { savingEntitySlot.captured }
            }
        }

        afterEach { clearAllMocks() }

        and("이미 등록된 간병비 산정 수정 내역이 존재할때") {
            val registeredModificationSummary = relaxedMock<CaregivingChargeModificationSummary>()
            val registeredModificationHistories = listOf<CaregivingChargeModificationHistory>(
                relaxedMock(),
                relaxedMock(),
                relaxedMock(),
            )

            beforeEach {
                every {
                    caregivingChargeModificationSummaryRepository.findTopByReceptionId("01GPJMK7ZPBBKTY3TP0NN5JWCJ")
                } returns registeredModificationSummary
                val pageableSlot = slot<Pageable>()
                every {
                    caregivingChargeModificationHistoryRepository.findByReceptionId(
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

            `when`("접수 아이디로 간병비 산정 수정 개요를 조회하면") {
                val query = CaregivingChargeModificationSummaryByReceptionIdQuery(
                    receptionId = "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = service.getCaregivingChargeModificationSummary(query)

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
                        caregivingChargeModificationSummaryRepository.findTopByReceptionId("01GPJMK7ZPBBKTY3TP0NN5JWCJ")
                    }
                }

                then("조회한 수정 개요를 반환합니다.") {
                    val actualResult = behavior()

                    actualResult shouldBe registeredModificationSummary
                }
            }

            `when`("내부 사용자 권한 없이 접수 아이디로 간병비 산정 수정 개요를 조회하면") {
                val query = CaregivingChargeModificationSummaryByReceptionIdQuery(
                    receptionId = "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                    subject = generateExternalCaregivingOrganizationManagerSubject("01H1972AQ7M5MMMWBKVZ4G1DQV"),
                )

                fun behavior() = service.getCaregivingChargeModificationSummary(query)

                then("throws AccessDeniedException") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }

            `when`("산정된 간병비가 수정되면") {
                val event = relaxedMock<CaregivingChargeModified>()

                beforeEach {
                    every { event.receptionId } returns "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                    every { event.caregivingRoundNumber } returns 1
                    every { event.additionalHoursCharge } returns Modification(0, 10000)
                    every { event.mealCost } returns Modification(0, 9000)
                    every { event.transportationFee } returns Modification(0, 1500)
                    every { event.holidayCharge } returns Modification(0, 50000)
                    every { event.caregiverInsuranceFee } returns Modification(0, 15000)
                    every { event.commissionFee } returns Modification(0, -5000)
                    every { event.vacationCharge } returns Modification(0, 30000)
                    every { event.patientConditionCharge } returns Modification(0, 50000)
                    every { event.covid19TestingCost } returns Modification(0, 4500)
                    every { event.additionalCharges } returns Modification(
                        listOf(
                            CaregivingCharge.AdditionalCharge(
                                name = "특별 보상비",
                                amount = 5000,
                            ),
                            CaregivingCharge.AdditionalCharge(
                                name = "고객보상비",
                                amount = -10000,
                            ),
                        ),
                        listOf(
                            CaregivingCharge.AdditionalCharge(
                                name = "특별 보상비",
                                amount = 10000,
                            ),
                            CaregivingCharge.AdditionalCharge(
                                name = "고객 보상비",
                                amount = -10000,
                            ),
                            CaregivingCharge.AdditionalCharge(
                                name = "세탁비",
                                amount = 15000,
                            ),
                        ),
                    )
                    every { event.outstandingAmount } returns Modification(0, 5000)
                    every { event.expectedSettlementDate } returns Modification(
                        LocalDate.of(2023, 4, 17),
                        LocalDate.of(2023, 4, 18)
                    )
                    every { event.isCancelAfterArrived } returns Modification(previous = true, current = false)
                    every { event.editingSubject[SubjectAttribute.USER_ID] } returns setOf("01GDYB3M58TBBXG1A0DJ1B866V")
                    every { event.calculatedDateTime } returns LocalDateTime.of(2023, 4, 16, 12, 3, 4)
                }

                afterEach { clearAllMocks() }

                fun handling() = service.handleCaregivingChargeModified(event)

                then("수정 개요에 간병비 산정이 수정되었음을 알립니다..") {
                    handling()

                    verify {
                        registeredModificationSummary.handleCaregivingChargeModified(event)
                    }
                }

                then("수정된 항목들을 기록으로 저장합니다.") {
                    handling()

                    fun verifyModification(
                        modifiedProperty: CaregivingChargeModificationHistory.ModifiedProperty,
                        previous: String?,
                        modified: String?,
                    ) {
                        verify {
                            caregivingChargeModificationHistoryRepository.save(
                                withArg {
                                    it.modifiedProperty shouldBe modifiedProperty
                                    it.previous shouldBe previous
                                    it.modified shouldBe modified
                                }
                            )
                        }
                    }

                    verifyAll {
                        caregivingChargeModificationHistoryRepository.save(
                            withArg {
                                it.receptionId shouldBe "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                                it.caregivingRoundNumber shouldBe 1
                                it.modifierId shouldBe "01GDYB3M58TBBXG1A0DJ1B866V"
                                it.modifiedDateTime shouldBe LocalDateTime.of(2023, 4, 16, 12, 3, 4)
                            }
                        )
                    }
                    verifyModification(
                        CaregivingChargeModificationHistory.ModifiedProperty.ADDITIONAL_HOURS_CHARGE,
                        "0",
                        "10000",
                    )
                    verifyModification(
                        CaregivingChargeModificationHistory.ModifiedProperty.MEAL_COST,
                        "0",
                        "9000",
                    )
                    verifyModification(
                        CaregivingChargeModificationHistory.ModifiedProperty.TRANSPORTATION_FEE,
                        "0",
                        "1500",
                    )
                    verifyModification(
                        CaregivingChargeModificationHistory.ModifiedProperty.HOLIDAY_CHARGE,
                        "0",
                        "50000",
                    )
                    verifyModification(
                        CaregivingChargeModificationHistory.ModifiedProperty.CAREGIVER_INSURANCE_FEE,
                        "0",
                        "15000",
                    )
                    verifyModification(
                        CaregivingChargeModificationHistory.ModifiedProperty.COMMISSION_FEE,
                        "0",
                        "-5000",
                    )
                    verifyModification(
                        CaregivingChargeModificationHistory.ModifiedProperty.VACATION_CHARGE,
                        "0",
                        "30000",
                    )
                    verifyModification(
                        CaregivingChargeModificationHistory.ModifiedProperty.PATIENT_CONDITION_CHARGE,
                        "0",
                        "50000",
                    )
                    verifyModification(
                        CaregivingChargeModificationHistory.ModifiedProperty.COVID_19_TESTING_COST,
                        "0",
                        "4500",
                    )
                    verifyModification(
                        CaregivingChargeModificationHistory.ModifiedProperty.ADDITIONAL_CHARGE_1,
                        "(특별 보상비) 5,000",
                        "(특별 보상비) 10,000",
                    )
                    verifyModification(
                        CaregivingChargeModificationHistory.ModifiedProperty.ADDITIONAL_CHARGE_2,
                        "(고객보상비) -10,000",
                        "(고객 보상비) -10,000",
                    )
                    verifyModification(
                        CaregivingChargeModificationHistory.ModifiedProperty.ADDITIONAL_CHARGE_3,
                        null,
                        "(세탁비) 15,000",
                    )
                    verifyModification(
                        CaregivingChargeModificationHistory.ModifiedProperty.OUTSTANDING_AMOUNT,
                        "0",
                        "5000",
                    )
                    every { event.expectedSettlementDate } returns Modification(
                        LocalDate.of(2023, 4, 17),
                        LocalDate.of(2023, 4, 18)
                    )
                    verifyModification(
                        CaregivingChargeModificationHistory.ModifiedProperty.EXPECTED_SETTLEMENT_DATE,
                        "2023-04-17",
                        "2023-04-18",
                    )
                    verifyModification(
                        CaregivingChargeModificationHistory.ModifiedProperty.IS_CANCEL_AFTER_ARRIVED,
                        "true",
                        "false",
                    )
                }
            }

            `when`("간병비 산정 수정 내역을 조회하면") {
                val query = CaregivingChargeModificationHistoriesByReceptionIdQuery(
                    receptionId = "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = service.getCaregivingChargeModificationHistories(
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

                then("간병비 산정 수정 내역을 리포지토리로부터 조회합니다.") {
                    behavior()

                    verify {
                        caregivingChargeModificationHistoryRepository.findByReceptionId(
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
        }

        `when`("내부 사용자 권한 없이 접수 아이디로 간병비 산정 수정 내역을 조회하면") {
            val query = CaregivingChargeModificationHistoriesByReceptionIdQuery(
                receptionId = "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                subject = generateExternalCaregivingOrganizationManagerSubject("01H1972AQ7M5MMMWBKVZ4G1DQV"),
            )

            fun behavior() = service.getCaregivingChargeModificationHistories(
                query = query,
                pageRequest = PageRequest.of(0, 10),
            )

            then("throws AccessDeniedException") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }

        `when`("접수 아이디로 간병비 산정 수정 개요를 조회하면") {
            val query = CaregivingChargeModificationSummaryByReceptionIdQuery(
                receptionId = "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                subject = generateInternalCaregivingManagerSubject(),
            )

            fun behavior() = service.getCaregivingChargeModificationSummary(query)

            then("CaregivingChargeModificationSummaryNotFoundByReceptionIdException이 발생합니다.") {
                shouldThrow<CaregivingChargeModificationSummaryNotFoundByReceptionIdException> { behavior() }
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
                    caregivingChargeModificationSummaryRepository.save(
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

        and("엔티티 테스트할 때") {
            val caregivingChargeModificationHistory = CaregivingChargeModificationHistory(
                id = "01HF3XXE6RSX4VM9KXYESCYYE9",
                receptionId = "01HF3Y0FFD72S0VCMF5GK47C4K",
                caregivingRoundNumber = 1,
                modifiedProperty = CaregivingChargeModificationHistory.ModifiedProperty.COMMISSION_FEE,
                previous = "0.1",
                modified = "0.2",
                modifierId = "01HF3Y37NYAYFXMAPNYECENR5P",
                modifiedDateTime = LocalDateTime.of(2023, 11, 13, 10, 33, 50)
            )

            `when`("저장을 요청하면") {
                fun behavior() = cacheCaregivingChargeModificationHistoryRepository.save(caregivingChargeModificationHistory)
                then("저장이 됩니다") {
                    behavior()
                }
            }

            `when`("조회를 요청하면") {
                fun behavior() = cacheCaregivingChargeModificationHistoryRepository.findByIdOrNull("01HF3XXE6RSX4VM9KXYESCYYE9")
                then("조회가 됩니다") {
                    behavior()
                }
            }
        }
    }
})
