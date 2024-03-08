package kr.caredoc.careinsurance.reconciliation

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import kr.caredoc.careinsurance.LocalEncryption
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationByIdQueryHandler
import kr.caredoc.careinsurance.billing.BillingByCaregivingRoundIdQueryHandler
import kr.caredoc.careinsurance.billing.BillingModified
import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import kr.caredoc.careinsurance.caregiving.CaregivingRoundByIdQueryHandler
import kr.caredoc.careinsurance.caregiving.CaregivingRoundModified
import kr.caredoc.careinsurance.generateExternalCaregivingOrganizationManagerSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.search.SearchCondition
import kr.caredoc.careinsurance.settlement.SettlementByCaregivingRoundIdQueryHandler
import kr.caredoc.careinsurance.settlement.SettlementProgressingStatus
import kr.caredoc.careinsurance.settlement.SettlementTransactionRecorded
import kr.caredoc.careinsurance.transaction.TransactionType
import kr.caredoc.careinsurance.withFixedClock
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.security.access.AccessDeniedException
import java.time.LocalDate
import java.time.LocalDateTime

class ReconciliationServiceTest : BehaviorSpec({
    given("대사 서비스가 주어졌을때") {
        val reconciliationRepository = relaxedMock<ReconciliationRepository>()
        val settlementByCaregivingRoundIdQueryHandler = relaxedMock<SettlementByCaregivingRoundIdQueryHandler>()
        val billingByCaregivingRoundIdQueryHandler = relaxedMock<BillingByCaregivingRoundIdQueryHandler>()
        val caregivingRoundByIdQueryHandler = relaxedMock<CaregivingRoundByIdQueryHandler>()
        val externalCaregivingOrganizationByIdQueryHandler =
            relaxedMock<ExternalCaregivingOrganizationByIdQueryHandler>()
        val patientNameHasher = LocalEncryption.patientNameHasher
        val reconciliationCsvTemplate = relaxedMock<ReconciliationCsvTemplate>()

        val service = ReconciliationService(
            reconciliationRepository = reconciliationRepository,
            caregivingRoundByIdQueryHandler = caregivingRoundByIdQueryHandler,
            externalCaregivingOrganizationByIdQueryHandler = externalCaregivingOrganizationByIdQueryHandler,
            settlementByCaregivingRoundIdQueryHandler = settlementByCaregivingRoundIdQueryHandler,
            billingByCaregivingRoundIdQueryHandler = billingByCaregivingRoundIdQueryHandler,
            patientNameHasher = patientNameHasher,
            reconciliationCsvTemplate = reconciliationCsvTemplate,
        )

        beforeEach {
            val savingEntitySlot = slot<Reconciliation>()
            every {
                reconciliationRepository.save(capture(savingEntitySlot))
            } answers {
                savingEntitySlot.captured
            }
        }

        and("대사 자료 또한 주어졌을때") {
            val openReconciliations = listOf<Reconciliation>(
                relaxedMock(),
                relaxedMock(),
            )
            val closedReconciliations = listOf<Reconciliation>(
                relaxedMock(),
                relaxedMock(),
            )

            beforeEach {
                every { openReconciliations[0].id } returns "01GXX57HXJY77ND6G57PDM3R1D"
                every { openReconciliations[1].id } returns "01GXX57HXKV884RPBVGF4CXRT8"
                every { closedReconciliations[0].id } returns "01GXX57HXK1VCZ6SQPSTWP16N5"
                every { closedReconciliations[1].id } returns "01GXX57HXKVSBNBF72DS8CT0S2"

                with(reconciliationRepository) {
                    val pageableSlot = slot<Pageable>()
                    every {
                        findByIssuedDateBetweenAndClosingStatus(
                            any(),
                            any(),
                            match { it == ClosingStatus.OPEN },
                            capture(pageableSlot),
                        )
                    } answers {
                        PageImpl(
                            openReconciliations,
                            pageableSlot.captured,
                            2,
                        )
                    }
                    every {
                        findByIssuedDateBetweenAndClosingStatus(
                            any(),
                            any(),
                            match { it == ClosingStatus.OPEN },
                        )
                    } returns openReconciliations
                    every {
                        findByIssuedDateBetweenAndClosingStatusAndAccidentNumberLike(
                            any(),
                            any(),
                            match { it == ClosingStatus.OPEN },
                            match { "2022-1111111".contains(it) },
                            capture(pageableSlot),
                        )
                    } answers {
                        PageImpl(
                            listOf(openReconciliations[0]),
                            pageableSlot.captured,
                            1,
                        )
                    }
                    every {
                        findByIssuedDateBetweenAndClosingStatusAndHashedPatientName(
                            any(),
                            any(),
                            match { it == ClosingStatus.OPEN },
                            match { it == patientNameHasher.hashAsHex("홍길동") },
                            capture(pageableSlot),
                        )
                    } answers {
                        PageImpl(
                            listOf(openReconciliations[1]),
                            pageableSlot.captured,
                            1,
                        )
                    }
                    every {
                        findByReconciledYearAndReconciledMonthAndClosingStatusOrderByIdDesc(
                            any(),
                            any(),
                            match { it == ClosingStatus.CLOSED },
                            capture(pageableSlot),
                        )
                    } answers {
                        PageImpl(
                            closedReconciliations,
                            pageableSlot.captured,
                            2,
                        )
                    }
                    every {
                        findByReconciledYearAndReconciledMonthAndClosingStatusOrderByIdDesc(
                            any(),
                            any(),
                            match { it == ClosingStatus.CLOSED },
                        )
                    } returns closedReconciliations

                    val idsSlot = slot<Collection<String>>()
                    every {
                        findByIdIn(capture(idsSlot))
                    } answers {
                        (openReconciliations + closedReconciliations).filter {
                            idsSlot.captured.contains(it.id)
                        }
                    }
                }
            }

            afterEach { clearAllMocks() }

            `when`("기간 범위 내의 마감하지 않은 대사 자료를 조회하면") {
                val query = OpenReconciliationsByFilterQuery(
                    from = LocalDate.of(2023, 10, 1),
                    until = LocalDate.of(2223, 11, 30),
                    searchCondition = null,
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = service.getOpenReconciliations(query, PageRequest.of(0, 20))

                then("리포지토리로부터 대사 자료를 조회합니다.") {
                    behavior()

                    verify {
                        reconciliationRepository.findByIssuedDateBetweenAndClosingStatus(
                            from = LocalDate.of(2023, 10, 1),
                            until = LocalDate.of(2223, 11, 30),
                            closingStatus = ClosingStatus.OPEN,
                            pageable = PageRequest.of(
                                0,
                                20,
                            ),
                        )
                    }
                }

                then("조회한 대사 자료를 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.content shouldContainExactlyInAnyOrder openReconciliations
                }
            }

            `when`("기간 범위 내의 마감하지 않은 대사 자료를 사고 번호 단위로 조회하면") {
                val query = OpenReconciliationsByFilterQuery(
                    from = LocalDate.of(2023, 10, 1),
                    until = LocalDate.of(2223, 11, 30),
                    searchCondition = SearchCondition(
                        searchingProperty = OpenReconciliationsByFilterQuery.SearchingProperty.ACCIDENT_NUMBER,
                        keyword = "1111111",
                    ),
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = service.getOpenReconciliations(query, PageRequest.of(0, 20))

                then("리포지토리로부터 대사 자료를 검색합니다.") {
                    behavior()

                    verify {
                        reconciliationRepository.findByIssuedDateBetweenAndClosingStatusAndAccidentNumberLike(
                            from = LocalDate.of(2023, 10, 1),
                            until = LocalDate.of(2223, 11, 30),
                            closingStatus = ClosingStatus.OPEN,
                            accidentNumber = "1111111",
                            pageable = PageRequest.of(
                                0,
                                20,
                            ),
                        )
                    }
                }

                then("검색한 대사 자료를 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.content shouldContainExactlyInAnyOrder listOf(openReconciliations[0])
                }
            }

            `when`("기간 범위 내의 마감하지 않은 대사 자료를 환자 이름으로 조회하면") {
                val query = OpenReconciliationsByFilterQuery(
                    from = LocalDate.of(2023, 10, 1),
                    until = LocalDate.of(2223, 11, 30),
                    searchCondition = SearchCondition(
                        searchingProperty = OpenReconciliationsByFilterQuery.SearchingProperty.PATIENT_NAME,
                        keyword = "홍길동",
                    ),
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = service.getOpenReconciliations(query, PageRequest.of(0, 20))

                then("리포지토리로부터 대사 자료를 검색합니다.") {
                    behavior()

                    verify {
                        reconciliationRepository.findByIssuedDateBetweenAndClosingStatusAndHashedPatientName(
                            from = LocalDate.of(2023, 10, 1),
                            until = LocalDate.of(2223, 11, 30),
                            closingStatus = ClosingStatus.OPEN,
                            hashedPatientName = patientNameHasher.hashAsHex("홍길동"),
                            pageable = PageRequest.of(
                                0,
                                20,
                            ),
                        )
                    }
                }

                then("검색한 대사 자료를 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.content shouldContainExactlyInAnyOrder listOf(openReconciliations[1])
                }
            }

            `when`("내부 사용자 권한 없이 마감하지 않은 대사 자료를 조회하면") {
                val query = OpenReconciliationsByFilterQuery(
                    from = LocalDate.of(2023, 10, 1),
                    until = LocalDate.of(2223, 11, 30),
                    searchCondition = null,
                    subject = generateExternalCaregivingOrganizationManagerSubject("01GSEKXGET5JKFST29B5K0N4XH")
                )

                fun behavior() = service.getOpenReconciliations(query, PageRequest.of(0, 20))

                then("AccessDeniedExeption이 발생합니다.") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }

            `when`("대사 연/월을 기준으로 마감된 대사 자료를 조회하면") {
                val query = ClosedReconciliationsByFilterQuery(
                    year = 2023,
                    month = 11,
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = service.getClosedReconciliations(query, PageRequest.of(0, 20))

                then("리포지토리로부터 대사 자료를 검색합니다.") {
                    behavior()

                    verify {
                        reconciliationRepository.findByReconciledYearAndReconciledMonthAndClosingStatusOrderByIdDesc(
                            reconciledYear = 2023,
                            reconciledMonth = 11,
                            closingStatus = ClosingStatus.CLOSED,
                            pageable = PageRequest.of(
                                0,
                                20,
                            ),
                        )
                    }
                }

                then("검색한 대사 자료를 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.content shouldContainExactlyInAnyOrder closedReconciliations
                }
            }

            `when`("내부 사용자 권한 없이 마감하지 않은 대사 자료를 조회하면") {
                val query = OpenReconciliationsByFilterQuery(
                    from = LocalDate.of(2023, 10, 1),
                    until = LocalDate.of(2223, 11, 30),
                    searchCondition = null,
                    subject = generateExternalCaregivingOrganizationManagerSubject("01GSEKXGET5JKFST29B5K0N4XH")
                )

                fun behavior() = service.getOpenReconciliations(query, PageRequest.of(0, 20))

                then("AccessDeniedExeption이 발생합니다.") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }

            `when`("대사들을 수정하면") {
                val subject = generateInternalCaregivingManagerSubject()
                val commands = listOf(
                    ReconciliationByIdQuery(
                        reconciliationId = "01GXX57HXJY77ND6G57PDM3R1D",
                        subject = subject,
                    ) to ReconciliationEditingCommand(
                        closingStatus = ClosingStatus.CLOSED,
                        subject = subject,
                    ),
                    ReconciliationByIdQuery(
                        reconciliationId = "01GXX57HXK1VCZ6SQPSTWP16N5",
                        subject = subject,
                    ) to ReconciliationEditingCommand(
                        closingStatus = ClosingStatus.OPEN,
                        subject = subject,
                    ),
                )

                fun behavior() = service.editReconciliations(commands)

                then("지정한 대사들을 수정합니다.") {
                    behavior()

                    verify {
                        openReconciliations[0].edit(
                            withArg {
                                it.closingStatus shouldBe ClosingStatus.CLOSED
                            }
                        )
                        closedReconciliations[0].edit(
                            withArg {
                                it.closingStatus shouldBe ClosingStatus.OPEN
                            }
                        )
                    }
                }
            }

            `when`("존재하지 않은 대사를 포함하여 대사들을 수정하면") {
                val subject = generateInternalCaregivingManagerSubject()
                val commands = listOf(
                    ReconciliationByIdQuery(
                        reconciliationId = "01GXX57HXJY77ND6G57PDM3R1D",
                        subject = subject,
                    ) to ReconciliationEditingCommand(
                        closingStatus = ClosingStatus.CLOSED,
                        subject = subject,
                    ),
                    ReconciliationByIdQuery(
                        reconciliationId = "01GXX57HXK1VCZ6SQPSTWP16N6",
                        subject = subject,
                    ) to ReconciliationEditingCommand(
                        closingStatus = ClosingStatus.OPEN,
                        subject = subject,
                    ),
                )

                fun behavior() = service.editReconciliations(commands)

                then("ReferenceReconciliationNotExistsException이 발생합니다.") {
                    val thrownException = shouldThrow<ReferenceReconciliationNotExistsException> { behavior() }

                    thrownException.referenceReconciliationId shouldBe "01GXX57HXK1VCZ6SQPSTWP16N6"
                }
            }

            `when`("마감되지 않은 대사들을 CSV형식으로 조회하면") {
                val query = OpenReconciliationsByFilterQuery(
                    from = LocalDate.of(2023, 10, 1),
                    until = LocalDate.of(2223, 11, 30),
                    searchCondition = null,
                    subject = generateInternalCaregivingManagerSubject(),
                )

                beforeEach {
                    every {
                        reconciliationCsvTemplate.generate(
                            match { it == openReconciliations },
                            any(),
                        )
                    } returns """
                        사고번호,고객명,간병차수,청구금액,간병인명,정산금액,출금액,입금액,회차수익,케어닥수익,제휴사,분배수익
                        2022-1234567,홍*동,3,625000,오간병,590000,0,0,35000,14000,케어라인,21000
                        2022-1234567,홍*동,3,-70000,오간병,0,0,0,-75000,-28000,케어라인,-42000
                    """.trimIndent()
                }

                afterEach { clearAllMocks() }

                fun behavior() = service.getOpenReconciliationsAsCsv(query)

                then("마감되지 않은 대사 목록을 조회합니다.") {
                    behavior()

                    verify {
                        reconciliationRepository.findByIssuedDateBetweenAndClosingStatus(
                            LocalDate.of(2023, 10, 1),
                            LocalDate.of(2223, 11, 30),
                            ClosingStatus.OPEN,
                        )
                    }
                }

                then("마감되지 않은 대사 목록을 CSV 템플릿으로 템플리팅합니다.") {
                    behavior()

                    verify {
                        reconciliationCsvTemplate.generate(
                            openReconciliations,
                            query.subject,
                        )
                    }
                }

                then("템플리팅한 대사 목록을 응답합니다.") {
                    val actualResult = behavior()

                    actualResult shouldBe """
                        사고번호,고객명,간병차수,청구금액,간병인명,정산금액,출금액,입금액,회차수익,케어닥수익,제휴사,분배수익
                        2022-1234567,홍*동,3,625000,오간병,590000,0,0,35000,14000,케어라인,21000
                        2022-1234567,홍*동,3,-70000,오간병,0,0,0,-75000,-28000,케어라인,-42000
                    """.trimIndent()
                }
            }

            `when`("마감된 대사들을 CSV형식으로 조회하면") {
                val query = ClosedReconciliationsByFilterQuery(
                    year = 2023,
                    month = 11,
                    subject = generateInternalCaregivingManagerSubject(),
                )

                beforeEach {
                    every {
                        reconciliationCsvTemplate.generate(
                            match { it == closedReconciliations },
                            any(),
                        )
                    } returns """
                        사고번호,고객명,간병차수,청구금액,간병인명,정산금액,출금액,입금액,회차수익,케어닥수익,제휴사,분배수익
                        2022-1234567,홍*동,3,625000,오간병,590000,0,0,35000,14000,케어라인,21000
                        2022-1234567,홍*동,3,-70000,오간병,0,0,0,-75000,-28000,케어라인,-42000
                    """.trimIndent()
                }

                afterEach { clearAllMocks() }

                fun behavior() = service.getClosedReconciliationsAsCsv(query)

                then("마감된 대사 목록을 조회합니다.") {
                    behavior()

                    verify {
                        reconciliationRepository.findByReconciledYearAndReconciledMonthAndClosingStatusOrderByIdDesc(
                            2023,
                            11,
                            ClosingStatus.CLOSED,
                        )
                    }
                }

                then("마감된 대사 목록을 CSV 템플릿으로 템플리팅합니다.") {
                    behavior()

                    verify {
                        reconciliationCsvTemplate.generate(
                            closedReconciliations,
                            query.subject,
                        )
                    }
                }

                then("템플리팅한 대사 목록을 응답합니다.") {
                    val actualResult = behavior()

                    actualResult shouldBe """
                        사고번호,고객명,간병차수,청구금액,간병인명,정산금액,출금액,입금액,회차수익,케어닥수익,제휴사,분배수익
                        2022-1234567,홍*동,3,625000,오간병,590000,0,0,35000,14000,케어라인,21000
                        2022-1234567,홍*동,3,-70000,오간병,0,0,0,-75000,-28000,케어라인,-42000
                    """.trimIndent()
                }
            }
        }

        `when`("간병 회차의 정산이 완료되고 청구가 미수 상태가 되었을때") {
            val event = relaxedMock<CaregivingRoundModified>()

            beforeEach {
                with(event) {
                    every { caregivingRoundId } returns "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                    every { settlementProgressingStatus } returns Modification(
                        SettlementProgressingStatus.COMPLETED,
                        SettlementProgressingStatus.COMPLETED,
                    )
                    every { billingProgressingStatus } returns Modification(
                        BillingProgressingStatus.WAITING_FOR_BILLING,
                        BillingProgressingStatus.WAITING_DEPOSIT,
                    )
                }

                every {
                    caregivingRoundByIdQueryHandler.getCaregivingRound(
                        match { it.caregivingRoundId == "01GSM0PQ5G8HW2GKYXH3VGGMZG" }
                    )
                } returns relaxedMock {
                    every { id } returns "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                    every { receptionInfo.receptionId } returns "01GVD2HS5FMX9012BN28VHDPW3"
                    every { receptionInfo.caregivingManagerInfo.organizationId } returns "01GQ4PE5J0SHCS5BQTJBBKTX8Q"
                    every { endDateTime } returns LocalDateTime.of(2023, 11, 3, 14, 21, 32)
                }

                every {
                    settlementByCaregivingRoundIdQueryHandler.getSettlement(
                        match { it.caregivingRoundId == "01GSM0PQ5G8HW2GKYXH3VGGMZG" }
                    )
                } returns relaxedMock {
                    every { totalAmount } returns 590000
                }

                every {
                    billingByCaregivingRoundIdQueryHandler.getBilling(
                        match { it.caregivingRoundId == "01GSM0PQ5G8HW2GKYXH3VGGMZG" }
                    )
                } returns relaxedMock {
                    every { totalAmount } returns 625000
                }

                every {
                    externalCaregivingOrganizationByIdQueryHandler.getExternalCaregivingOrganization(
                        match { it.id == "01GQ4PE5J0SHCS5BQTJBBKTX8Q" }
                    )
                } returns relaxedMock {
                    every { profitAllocationRatio } returns 0.6f
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = service.handleCaregivingRoundModified(event)

            then("해당 간병의 회차를 조회합니다.") {
                handling()

                verify {
                    caregivingRoundByIdQueryHandler.getCaregivingRound(
                        withArg {
                            it.caregivingRoundId shouldBe "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                        }
                    )
                }
            }

            then("해당 간병 회차를 담당한 협회를 조회합니다.") {
                handling()

                verify {
                    externalCaregivingOrganizationByIdQueryHandler.getExternalCaregivingOrganization(
                        withArg {
                            it.id shouldBe "01GQ4PE5J0SHCS5BQTJBBKTX8Q"
                        }
                    )
                }
            }

            then("해당 간병 회차의 정산을 조회합니다.") {
                handling()

                verify {
                    settlementByCaregivingRoundIdQueryHandler.getSettlement(
                        withArg {
                            it.caregivingRoundId shouldBe "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                        }
                    )
                }
            }

            then("해당 간병 회차의 청구를 조회합니다.") {
                handling()

                verify {
                    billingByCaregivingRoundIdQueryHandler.getBilling(
                        withArg {
                            it.caregivingRoundId shouldBe "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                        }
                    )
                }
            }

            then("정산과 청구를 바탕으로 대사자료를 생성합니다.") {
                handling()

                verify {
                    reconciliationRepository.save(
                        withArg {
                            it.receptionId shouldBe "01GVD2HS5FMX9012BN28VHDPW3"
                            it.caregivingRoundId shouldBe "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                            it.billingAmount shouldBe 625000
                            it.settlementAmount shouldBe 590000
                            it.settlementDepositAmount shouldBe 0
                            it.settlementWithdrawalAmount shouldBe 0
                            it.profit shouldBe 35000
                            it.distributedProfit shouldBe 21000
                            it.reconciledYear shouldBe 2023
                            it.reconciledMonth shouldBe 11
                            it.issuedDate shouldBe LocalDate.of(2023, 11, 3)
                            it.issuedType shouldBe IssuedType.FINISH
                        }
                    )
                }
            }
        }

        `when`("간병 회차의 정산이 완료되었지만 청구가 미수 상태에 접어들지 않았을때") {
            val event = relaxedMock<CaregivingRoundModified>()

            beforeEach {
                with(event) {
                    every { caregivingRoundId } returns "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                    every { settlementProgressingStatus } returns Modification(
                        SettlementProgressingStatus.WAITING,
                        SettlementProgressingStatus.COMPLETED,
                    )
                    every { billingProgressingStatus } returns Modification(
                        BillingProgressingStatus.WAITING_FOR_BILLING,
                        BillingProgressingStatus.WAITING_FOR_BILLING,
                    )
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = service.handleCaregivingRoundModified(event)

            then("아무것도 안합니다.") {
                handling()

                verify(exactly = 0) { caregivingRoundByIdQueryHandler.getCaregivingRound(any()) }
                verify(exactly = 0) {
                    externalCaregivingOrganizationByIdQueryHandler.getExternalCaregivingOrganization(
                        any()
                    )
                }
                verify(exactly = 0) { settlementByCaregivingRoundIdQueryHandler.getSettlement(any()) }
                verify(exactly = 0) { billingByCaregivingRoundIdQueryHandler.getBilling(any()) }
                verify(exactly = 0) { reconciliationRepository.save(any()) }
            }
        }

        `when`("간병 회차의 청구가 미수 상태가 되었지만 정산이 완료되지 않았을때") {
            val event = relaxedMock<CaregivingRoundModified>()

            beforeEach {
                with(event) {
                    every { caregivingRoundId } returns "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                    every { settlementProgressingStatus } returns Modification(
                        SettlementProgressingStatus.WAITING,
                        SettlementProgressingStatus.WAITING,
                    )
                    every { billingProgressingStatus } returns Modification(
                        BillingProgressingStatus.WAITING_FOR_BILLING,
                        BillingProgressingStatus.WAITING_DEPOSIT,
                    )
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = service.handleCaregivingRoundModified(event)

            then("아무것도 안합니다.") {
                handling()

                verify(exactly = 0) { caregivingRoundByIdQueryHandler.getCaregivingRound(any()) }
                verify(exactly = 0) {
                    externalCaregivingOrganizationByIdQueryHandler.getExternalCaregivingOrganization(
                        any()
                    )
                }
                verify(exactly = 0) { settlementByCaregivingRoundIdQueryHandler.getSettlement(any()) }
                verify(exactly = 0) { billingByCaregivingRoundIdQueryHandler.getBilling(any()) }
                verify(exactly = 0) { reconciliationRepository.save(any()) }
            }
        }

        `when`("간병 회차의 정산이 완료되고 청구가 미수 상태에서 더욱 진행된다면") {
            val event = relaxedMock<CaregivingRoundModified>()

            beforeEach {
                with(event) {
                    every { caregivingRoundId } returns "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                    every { settlementProgressingStatus } returns Modification(
                        SettlementProgressingStatus.COMPLETED,
                        SettlementProgressingStatus.COMPLETED,
                    )
                    every { billingProgressingStatus } returns Modification(
                        BillingProgressingStatus.WAITING_DEPOSIT,
                        BillingProgressingStatus.OVER_DEPOSIT,
                    )
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = service.handleCaregivingRoundModified(event)

            then("아무것도 안합니다.") {
                handling()

                verify(exactly = 0) { caregivingRoundByIdQueryHandler.getCaregivingRound(any()) }
                verify(exactly = 0) {
                    externalCaregivingOrganizationByIdQueryHandler.getExternalCaregivingOrganization(
                        any()
                    )
                }
                verify(exactly = 0) { settlementByCaregivingRoundIdQueryHandler.getSettlement(any()) }
                verify(exactly = 0) { billingByCaregivingRoundIdQueryHandler.getBilling(any()) }
                verify(exactly = 0) { reconciliationRepository.save(any()) }
            }
        }

        `when`("정산금이 입금되면") {
            val event = relaxedMock<SettlementTransactionRecorded>()

            beforeEach {
                with(event) {
                    every { receptionId } returns "01GVD2HS5FMX9012BN28VHDPW3"
                    every { caregivingRoundId } returns "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                    every { settlementId } returns "01GVCX47T2590S6RYTTFDGJQP6"
                    every { amount } returns 5000
                    every { transactionDate } returns LocalDate.of(2022, 1, 30)
                    every { transactionType } returns TransactionType.DEPOSIT
                    every { enteredDateTime } returns LocalDateTime.of(2022, 1, 30, 16, 5, 21)
                    every { order } returns 1
                }

                every {
                    caregivingRoundByIdQueryHandler.getCaregivingRound(
                        match { it.caregivingRoundId == "01GSM0PQ5G8HW2GKYXH3VGGMZG" }
                    )
                } returns relaxedMock {
                    every { id } returns "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                    every { receptionInfo.receptionId } returns "01GVD2HS5FMX9012BN28VHDPW3"
                    every { receptionInfo.caregivingManagerInfo.organizationId } returns "01GQ4PE5J0SHCS5BQTJBBKTX8Q"
                }

                every {
                    externalCaregivingOrganizationByIdQueryHandler.getExternalCaregivingOrganization(
                        match { it.id == "01GQ4PE5J0SHCS5BQTJBBKTX8Q" }
                    )
                } returns relaxedMock {
                    every { profitAllocationRatio } returns 0.6f
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = service.handleSettlementTransactionRecorded(event)

            then("해당 간병의 회차를 조회합니다.") {
                handling()

                verify {
                    caregivingRoundByIdQueryHandler.getCaregivingRound(
                        withArg {
                            it.caregivingRoundId shouldBe "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                        }
                    )
                }
            }

            then("해당 간병 회차를 담당한 협회를 조회합니다.") {
                handling()

                verify {
                    externalCaregivingOrganizationByIdQueryHandler.getExternalCaregivingOrganization(
                        withArg {
                            it.id shouldBe "01GQ4PE5J0SHCS5BQTJBBKTX8Q"
                        }
                    )
                }
            }

            then("새로운 대사를 입력합니다.") {
                withFixedClock(LocalDateTime.of(2023, 1, 30, 0, 0, 0)) {
                    handling()
                }

                verify {
                    reconciliationRepository.save(
                        withArg {
                            it.receptionId shouldBe "01GVD2HS5FMX9012BN28VHDPW3"
                            it.caregivingRoundId shouldBe "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                            it.billingAmount shouldBe 0
                            it.settlementAmount shouldBe 0
                            it.settlementWithdrawalAmount shouldBe 0
                            it.settlementDepositAmount shouldBe 5000
                            it.profit shouldBe 5000
                            it.distributedProfit shouldBe 3000
                            it.issuedDate shouldBe LocalDate.of(2023, 1, 30)
                            it.issuedType shouldBe IssuedType.TRANSACTION
                        }
                    )
                }
            }
        }

        `when`("정산금이 출금되면") {
            val event = relaxedMock<SettlementTransactionRecorded>()

            beforeEach {
                with(event) {
                    every { receptionId } returns "01GVD2HS5FMX9012BN28VHDPW3"
                    every { caregivingRoundId } returns "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                    every { settlementId } returns "01GVCX47T2590S6RYTTFDGJQP6"
                    every { amount } returns 5000
                    every { transactionDate } returns LocalDate.of(2022, 1, 30)
                    every { transactionType } returns TransactionType.WITHDRAWAL
                    every { enteredDateTime } returns LocalDateTime.of(2022, 1, 30, 16, 5, 21)
                    every { order } returns 1
                }

                every {
                    caregivingRoundByIdQueryHandler.getCaregivingRound(
                        match { it.caregivingRoundId == "01GSM0PQ5G8HW2GKYXH3VGGMZG" }
                    )
                } returns relaxedMock {
                    every { id } returns "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                    every { receptionInfo.receptionId } returns "01GVD2HS5FMX9012BN28VHDPW3"
                    every { receptionInfo.caregivingManagerInfo.organizationId } returns "01GQ4PE5J0SHCS5BQTJBBKTX8Q"
                }

                every {
                    externalCaregivingOrganizationByIdQueryHandler.getExternalCaregivingOrganization(
                        match { it.id == "01GQ4PE5J0SHCS5BQTJBBKTX8Q" }
                    )
                } returns relaxedMock {
                    every { profitAllocationRatio } returns 0.6f
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = service.handleSettlementTransactionRecorded(event)

            then("해당 간병의 회차를 조회합니다.") {
                handling()

                verify {
                    caregivingRoundByIdQueryHandler.getCaregivingRound(
                        withArg {
                            it.caregivingRoundId shouldBe "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                        }
                    )
                }
            }

            then("해당 간병 회차를 담당한 협회를 조회합니다.") {
                handling()

                verify {
                    externalCaregivingOrganizationByIdQueryHandler.getExternalCaregivingOrganization(
                        withArg {
                            it.id shouldBe "01GQ4PE5J0SHCS5BQTJBBKTX8Q"
                        }
                    )
                }
            }

            then("새로운 대사를 입력합니다.") {
                withFixedClock(LocalDateTime.of(2022, 1, 30, 0, 0, 0)) {
                    handling()
                }

                verify {
                    reconciliationRepository.save(
                        withArg {
                            it.receptionId shouldBe "01GVD2HS5FMX9012BN28VHDPW3"
                            it.caregivingRoundId shouldBe "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                            it.billingAmount shouldBe 0
                            it.settlementAmount shouldBe 0
                            it.settlementWithdrawalAmount shouldBe 5000
                            it.settlementDepositAmount shouldBe 0
                            it.profit shouldBe -5000
                            it.distributedProfit shouldBe -3000
                            it.issuedDate shouldBe LocalDate.of(2022, 1, 30)
                            it.issuedType shouldBe IssuedType.TRANSACTION
                        }
                    )
                }
            }
        }

        `when`("정산금이 입금되었으나 입금된 정산이 최초 정산처리로 인한 입금이라면") {
            val event = relaxedMock<SettlementTransactionRecorded>()

            beforeEach {
                with(event) {
                    every { order } returns 0
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = service.handleSettlementTransactionRecorded(event)

            then("아무것도 안합니다.") {
                handling()

                verify(exactly = 0) {
                    reconciliationRepository.save(any())
                }
            }
        }

        `when`("청구 총액이 변경된다면") {
            val event = relaxedMock<BillingModified>()

            beforeEach {
                with(event) {
                    every { receptionId } returns "01GVD2HS5FMX9012BN28VHDPW3"
                    every { caregivingRoundId } returns "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                    every { totalAmount } returns Modification(100000, 50000)
                }

                every {
                    caregivingRoundByIdQueryHandler.getCaregivingRound(
                        match { it.caregivingRoundId == "01GSM0PQ5G8HW2GKYXH3VGGMZG" }
                    )
                } returns relaxedMock {
                    every { id } returns "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                    every { receptionInfo.receptionId } returns "01GVD2HS5FMX9012BN28VHDPW3"
                    every { receptionInfo.caregivingManagerInfo.organizationId } returns "01GQ4PE5J0SHCS5BQTJBBKTX8Q"
                }

                every {
                    externalCaregivingOrganizationByIdQueryHandler.getExternalCaregivingOrganization(
                        match { it.id == "01GQ4PE5J0SHCS5BQTJBBKTX8Q" }
                    )
                } returns relaxedMock {
                    every { profitAllocationRatio } returns 0.6f
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = service.handleBillingModified(event)

            then("해당 간병의 회차를 조회합니다.") {
                handling()

                verify {
                    caregivingRoundByIdQueryHandler.getCaregivingRound(
                        withArg {
                            it.caregivingRoundId shouldBe "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                        }
                    )
                }
            }

            then("해당 간병 회차를 담당한 협회를 조회합니다.") {
                handling()

                verify {
                    externalCaregivingOrganizationByIdQueryHandler.getExternalCaregivingOrganization(
                        withArg {
                            it.id shouldBe "01GQ4PE5J0SHCS5BQTJBBKTX8Q"
                        }
                    )
                }
            }

            then("새로운 대사를 입력합니다.") {
                withFixedClock(LocalDateTime.of(2023, 1, 30, 0, 0, 0)) {
                    handling()
                }

                verify {
                    reconciliationRepository.save(
                        withArg {
                            it.receptionId shouldBe "01GVD2HS5FMX9012BN28VHDPW3"
                            it.caregivingRoundId shouldBe "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                            it.billingAmount shouldBe -50000
                            it.settlementAmount shouldBe 0
                            it.settlementWithdrawalAmount shouldBe 0
                            it.settlementDepositAmount shouldBe 0
                            it.profit shouldBe -50000
                            it.distributedProfit shouldBe -30000
                            it.issuedDate shouldBe LocalDate.of(2023, 1, 30)
                            it.issuedType shouldBe IssuedType.ADDITIONAL
                        }
                    )
                }
            }
        }

        `when`("청구가 변경됐지만 청구 금액이 변경되지 않았다면") {
            val event = relaxedMock<BillingModified>()

            fun handling() = service.handleBillingModified(event)

            beforeEach {
                with(event) {
                    every { receptionId } returns "01GVD2HS5FMX9012BN28VHDPW3"
                    every { caregivingRoundId } returns "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                    every { totalAmount } returns Modification(100000, 100000)
                }
            }

            afterEach { clearAllMocks() }

            then("아무것도 안합니다.") {
                handling()

                verify(exactly = 0) {
                    reconciliationRepository.save(any())
                }
            }
        }
    }
})
