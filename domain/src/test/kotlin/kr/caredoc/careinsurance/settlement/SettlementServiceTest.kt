package kr.caredoc.careinsurance.settlement

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeSortedWith
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.slot
import io.mockk.verify
import kr.caredoc.careinsurance.LocalEncryption
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationByIdQueryHandler
import kr.caredoc.careinsurance.caregiving.CaregivingChargeCalculated
import kr.caredoc.careinsurance.caregiving.CaregivingChargeModified
import kr.caredoc.careinsurance.caregiving.CaregivingRoundsByIdsQueryHandler
import kr.caredoc.careinsurance.generateExternalCaregivingOrganizationManagerSubject
import kr.caredoc.careinsurance.generateGuestSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.patch.OverwritePatch
import kr.caredoc.careinsurance.patch.Patches
import kr.caredoc.careinsurance.patient.EncryptedPatientInfo
import kr.caredoc.careinsurance.reception.ReceptionByIdQueryHandler
import kr.caredoc.careinsurance.reception.ReceptionModified
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQueryHandler
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.search.SearchCondition
import kr.caredoc.careinsurance.security.accesscontrol.ObjectAttribute
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.security.encryption.Decryptor
import kr.caredoc.careinsurance.transaction.TransactionType
import kr.caredoc.careinsurance.user.InternalCaregivingManagerByIdQuery
import kr.caredoc.careinsurance.user.InternalCaregivingManagerByIdQueryHandler
import kr.caredoc.careinsurance.user.ReferenceInternalCaregivingManagerNotExistsException
import kr.caredoc.careinsurance.user.exception.InternalCaregivingManagerNotFoundByIdException
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
class SettlementServiceTest(
    @Autowired
    private val cacheSettlementRepository: SettlementRepository,
) : BehaviorSpec({
    given("정산 서비스가 주어졌을때") {
        val settlementRepository = relaxedMock<SettlementRepository>()
        val receptionByIdQueryHandler = relaxedMock<ReceptionByIdQueryHandler>()
        val internalCaregivingManagerByIdQueryHandler = relaxedMock<InternalCaregivingManagerByIdQueryHandler>()
        val caregivingRoundsByIdsQueryHandler = relaxedMock<CaregivingRoundsByIdsQueryHandler>()
        val receptionsByIdsQueryHandler = relaxedMock<ReceptionsByIdsQueryHandler>()
        val externalCaregivingOrganizationByIdQueryHandler = relaxedMock<ExternalCaregivingOrganizationByIdQueryHandler>()
        val decryptor = relaxedMock<Decryptor>()
        val service = SettlementService(
            settlementRepository = settlementRepository,
            receptionByIdQueryHandler = receptionByIdQueryHandler,
            internalCaregivingManagerByIdQueryHandler = internalCaregivingManagerByIdQueryHandler,
            caregivingRoundsByIdsQueryHandler = caregivingRoundsByIdsQueryHandler,
            receptionsByIdsQueryHandler = receptionsByIdsQueryHandler,
            externalCaregivingOrganizationByIdQueryHandler = externalCaregivingOrganizationByIdQueryHandler,
            decryptor = decryptor,
        )

        beforeEach {
            with(settlementRepository) {
                val savingSettlementSlot = slot<Settlement>()
                every {
                    save(capture(savingSettlementSlot))
                } answers {
                    savingSettlementSlot.captured
                }

                every {
                    findByIdOrNull(any())
                } returns null

                every {
                    findTopByCaregivingRoundId(any())
                } returns null
            }

            with(internalCaregivingManagerByIdQueryHandler) {
                justRun {
                    ensureInternalCaregivingManagerExists(match { it.internalCaregivingManagerId == "01GVCZ7W7MAYAC6C7JMJHSNEJR" })
                }
                val querySlot = slot<InternalCaregivingManagerByIdQuery>()
                every {
                    ensureInternalCaregivingManagerExists(capture(querySlot))
                } answers {
                    if (querySlot.captured.internalCaregivingManagerId != "01GVCZ7W7MAYAC6C7JMJHSNEJR") {
                        throw InternalCaregivingManagerNotFoundByIdException(querySlot.captured.internalCaregivingManagerId)
                    }
                }
            }
        }

        afterEach { clearAllMocks() }

        `when`("간병비가 산정되었음이 감지되면") {
            val event = relaxedMock<CaregivingChargeCalculated>()

            fun handling() = service.handleCaregivingChargeCalculated(event)

            beforeEach {
                with(event) {
                    every { receptionId } returns "01GVD2HS5FMX9012BN28VHDPW3"
                    every { caregivingRoundId } returns "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                    every { roundNumber } returns 2
                    every { dailyCaregivingCharge } returns 121000
                    every { basicAmount } returns 605000
                    every { additionalAmount } returns 20000
                    every { totalAmount } returns 625000
                    every { calculatedDateTime } returns LocalDateTime.of(2022, 1, 30, 5, 21, 31)
                    every { expectedSettlementDate } returns LocalDate.of(2022, 1, 30)
                }

                every {
                    receptionByIdQueryHandler.getReception(
                        match {
                            it.receptionId == "01GVD2HS5FMX9012BN28VHDPW3"
                        }
                    )
                } returns relaxedMock {
                    every { accidentInfo.accidentNumber } returns "2022-1111111"
                    every { patientInfo.name } returns EncryptedPatientInfo.EncryptedPatientName(
                        "임석민",
                        LocalEncryption.patientNameHasher,
                        LocalEncryption.LocalEncryptor,
                    )
                    every { patientInfo.nickname } returns "뽀리스"
                }
            }

            afterEach { clearAllMocks() }

            then("정산서가 소속할 간병 접수를 조회한다.") {
                handling()

                verify {
                    receptionByIdQueryHandler.getReception(
                        withArg {
                            it.receptionId shouldBe "01GVD2HS5FMX9012BN28VHDPW3"
                        }
                    )
                }
            }

            then("정산서를 생성한다.") {
                handling()

                verify {
                    settlementRepository.save(
                        withArg {
                            it.receptionId shouldBe "01GVD2HS5FMX9012BN28VHDPW3"
                            it.caregivingRoundId shouldBe "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                            it.caregivingRoundNumber shouldBe 2
                            it.progressingStatus shouldBe SettlementProgressingStatus.CONFIRMED
                            it.accidentNumber shouldBe "2022-1111111"
                            it.dailyCaregivingCharge shouldBe 121000
                            it.basicAmount shouldBe 605000
                            it.additionalAmount shouldBe 20000
                            it.totalAmount shouldBe 625000
                            it.lastCalculationDateTime shouldBe LocalDateTime.of(2022, 1, 30, 5, 21, 31)
                            it.expectedSettlementDate shouldBe LocalDate.of(2022, 1, 30)
                            it.settlementCompletionDateTime shouldBe null
                            it.settlementManagerId shouldBe null
                            it.totalDepositAmount shouldBe 0
                            it.totalWithdrawalAmount shouldBe 0
                            it.lastTransactionDatetime shouldBe null
                        }
                    )
                }
            }
        }

        `when`("정산의 입출금 내역을 조회하면") {
            val query = TransactionsBySettlementIdQuery(
                settlementId = "01GVCX47T2590S6RYTTFDGJQP6",
                subject = generateInternalCaregivingManagerSubject(),
            )

            fun behavior() = service.getTransactions(
                query,
                PageRequest.of(0, 2)
            )

            then("SettlementNotFoundByIdException이 발생합니다.") {
                val thrownException = shouldThrow<SettlementNotFoundByIdException> { behavior() }

                thrownException.settlementId shouldBe "01GVCX47T2590S6RYTTFDGJQP6"
            }
        }

        `when`("정산의 입출금 내역을 기록하면") {
            val subject = generateInternalCaregivingManagerSubject()
            val query = SettlementByIdQuery(
                settlementId = "01GVCX47T2590S6RYTTFDGJQP6",
                subject = subject,
            )
            val command = SettlementTransactionRecordingCommand(
                transactionType = TransactionType.WITHDRAWAL,
                amount = 5000,
                transactionDate = LocalDate.of(2022, 1, 30),
                transactionSubjectId = "01GW1160R5ZC9E3P5V57TYQX0E",
                subject = subject,
            )

            fun behavior() = service.recordTransaction(
                query,
                command
            )

            then("SettlementNotFoundByIdException이 발생합니다.") {
                val thrownException = shouldThrow<SettlementNotFoundByIdException> { behavior() }

                thrownException.settlementId shouldBe "01GVCX47T2590S6RYTTFDGJQP6"
            }
        }

        `when`("간병 회차 아이디로 정산을 조회하면") {
            val query = SettlementByCaregivingRoundIdQuery(
                caregivingRoundId = "01GSM0PQ5G8HW2GKYXH3VGGMZG",
                subject = generateInternalCaregivingManagerSubject(),
            )

            fun behavior() = service.getSettlement(query)

            then("SettlementNotFoundByCaregivingRoundIdException이 발생합니다.") {
                val thrownException = shouldThrow<SettlementNotFoundByCaregivingRoundIdException> { behavior() }

                thrownException.caregivingRoundId shouldBe "01GSM0PQ5G8HW2GKYXH3VGGMZG"
            }
        }

        and("그리고 이미 등록된 정산이 주어졌을때") {
            val settlements = listOf<Settlement>(
                relaxedMock(),
                relaxedMock(),
            )
            val settlementList = List<Settlement>(
                relaxedMock(),
                relaxedMock(),
            )

            beforeEach {
                with(settlementRepository) {
                    every {
                        findByReceptionId(
                            match { it == "01GVD2HS5FMX9012BN28VHDPW3" },
                            any(),
                        )
                    } returns settlements

                    every {
                        findByReceptionId(
                            match { it == "01GVD2HS5FMX9012BN28VHDPW3" },
                        )
                    } returns settlements

                    val pageableSlot = slot<Pageable>()
                    every {
                        searchSettlements(any(), capture(pageableSlot))
                    } answers {
                        PageImpl(
                            settlements,
                            pageableSlot.captured,
                            settlements.size.toLong(),
                        )
                    }
                    val sortSlot = slot<Sort>()
                    every {
                        searchSettlements(any(), capture(sortSlot))
                    } answers {
                        settlementList
                    }
                    val settlementIdsSlot = slot<Collection<String>>()
                    every {
                        findByIdIn(capture(settlementIdsSlot))
                    } answers {
                        val enteredSettlementIds = settlementIdsSlot.captured
                        val settlementByIds = mapOf(
                            "01GVCX47T2590S6RYTTFDGJQP6" to settlements[0],
                            "01GVCXJ4RX7A8KP4DPB7CDWRV8" to settlements[1],
                        )

                        enteredSettlementIds.mapNotNull { settlementByIds[it] }
                    }

                    val settlementIdSlot = slot<String>()
                    every { findByIdOrNull(capture(settlementIdSlot)) } answers {
                        val settlementByIds = mapOf(
                            "01GVCX47T2590S6RYTTFDGJQP6" to settlements[0],
                            "01GVCXJ4RX7A8KP4DPB7CDWRV8" to settlements[1],
                        )

                        settlementByIds[settlementIdSlot.captured]
                    }

                    every { findByCaregivingRoundId("01GSM0PQ5G8HW2GKYXH3VGGMZG") } returns listOf(settlements[0])
                    every { findTopByCaregivingRoundId("01GSM0PQ5G8HW2GKYXH3VGGMZG") } returns settlements[0]
                }

                with(settlements[0]) {
                    every { id } returns "01GVCX47T2590S6RYTTFDGJQP6"
                    every { caregivingRoundId } returns "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                    every { transactions } returns listOf(
                        relaxedMock {
                            every { transactionType } returns TransactionType.WITHDRAWAL
                            every { amount } returns 625000
                            every { transactionDate } returns LocalDate.of(2022, 1, 25)
                            every { enteredDateTime } returns LocalDateTime.of(2022, 1, 25, 14, 30, 45)
                            every { transactionSubjectId } returns "01GW1160R5ZC9E3P5V57TYQX0E"
                        },
                        relaxedMock {
                            every { transactionType } returns TransactionType.DEPOSIT
                            every { amount } returns 35000
                            every { transactionDate } returns LocalDate.of(2022, 1, 28)
                            every { enteredDateTime } returns LocalDateTime.of(2022, 1, 30, 14, 30, 24)
                            every { transactionSubjectId } returns "01GW118Y6KWZX0QYCBSKE5NZFB"
                        },
                        relaxedMock {
                            every { transactionType } returns TransactionType.WITHDRAWAL
                            every { amount } returns 5000
                            every { transactionDate } returns LocalDate.of(2022, 1, 30)
                            every { enteredDateTime } returns LocalDateTime.of(2022, 2, 1, 17, 50, 32)
                            every { transactionSubjectId } returns "01GW1160R5ZC9E3P5V57TYQX0E"
                        },
                    )
                }

                with(settlements[1]) {
                    every { id } returns "01GVCXJ4RX7A8KP4DPB7CDWRV8"
                }

                settlements.forEach {
                    every { it[ObjectAttribute.ASSIGNED_ORGANIZATION_ID] } returns setOf("01GVG25A2P5V6MVQG2SPKQ4D50")
                }
            }

            afterEach { clearAllMocks() }

            `when`("접수별 정산을 조회하면") {
                val query = SettlementsByReceptionIdQuery(
                    "01GVD2HS5FMX9012BN28VHDPW3",
                    generateInternalCaregivingManagerSubject()
                )

                fun behavior() = service.getSettlements(query)

                then("접수가 존재함을 확인합니다.") {
                    behavior()

                    verify {
                        receptionByIdQueryHandler.ensureReceptionExists(
                            withArg {
                                it.receptionId shouldBe "01GVD2HS5FMX9012BN28VHDPW3"
                            }
                        )
                    }
                }

                then("접수 아이디를 기준으로 정산을 조회합니다.") {
                    behavior()

                    verify {
                        settlementRepository.findByReceptionId(
                            withArg {
                                it shouldBe "01GVD2HS5FMX9012BN28VHDPW3"
                            },
                            withArg {
                                it.getOrderFor(Settlement::caregivingRoundNumber.name)?.direction shouldBe Sort.Direction.DESC
                            },
                        )
                    }
                }

                then("조회된 정산을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult shouldBe settlements
                }
            }

            `when`("정산을 할당받은 간병 업체에 소속된 관리자가 정산 목록을 조회하면") {
                val query = SettlementsByReceptionIdQuery(
                    "01GVD2HS5FMX9012BN28VHDPW3",
                    generateExternalCaregivingOrganizationManagerSubject("01GVG25A2P5V6MVQG2SPKQ4D50")
                )

                fun behavior() = service.getSettlements(query)

                then("아무 문제 없이 정산 목록을 조회할 수 있습니다.") {
                    val actualResult = behavior()

                    actualResult shouldBe settlements
                }
            }

            `when`("내부 사용자 권한 없이 정산을 조회하면") {
                val query = SettlementsByReceptionIdQuery(
                    "01GVD2HS5FMX9012BN28VHDPW3",
                    generateGuestSubject()
                )

                fun behavior() = service.getSettlements(query)

                then("AccessDeniedException이 발생합니다.") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }

            val expectedSettlementDateRange = DateRange(
                from = LocalDate.of(2023, 3, 1),
                until = LocalDate.of(2023, 3, 31),
            )
            val transactionDateRange = DateRange(
                from = LocalDate.of(2023, 3, 1),
                until = LocalDate.of(2023, 3, 7),
            )

            fun generateSettlementsSearchQuery(
                progressingStatus: SettlementProgressingStatus,
                expectedSettlementDate: DateRange?,
                transactionDate: DateRange?,
                searchCondition: SearchCondition<SettlementsSearchQuery.SearchingProperty>?,
                sorting: SettlementsSearchQuery.Sorting = SettlementsSearchQuery.Sorting.EXPECTED_SETTLEMENT_DATE_DESC_ACCIDENT_NUMBER_DESC,
                subject: Subject = generateInternalCaregivingManagerSubject(),
            ) = SettlementsSearchQuery(
                expectedSettlementDate = expectedSettlementDate,
                transactionDate = transactionDate,
                progressingStatus = progressingStatus,
                searchCondition = searchCondition,
                sorting = sorting,
                subject = subject,
            )

            `when`("정산 예정 일자를 기준으로 검색어 없이 정산 목록을 조회하면") {
                val query = generateSettlementsSearchQuery(
                    progressingStatus = SettlementProgressingStatus.WAITING,
                    expectedSettlementDate = expectedSettlementDateRange,
                    transactionDate = null,
                    searchCondition = null,
                    sorting = SettlementsSearchQuery.Sorting.EXPECTED_SETTLEMENT_DATE_DESC_ACCIDENT_NUMBER_DESC,
                )

                fun behavior() = service.getSettlements(query, PageRequest.of(0, 10))

                then("리포지토리에서 정산 목록을 조회한다.") {
                    behavior()

                    verify {
                        settlementRepository.searchSettlements(
                            withArg {
                                it.progressingStatus shouldBe SettlementProgressingStatus.WAITING
                                it.expectedSettlementDate shouldBe expectedSettlementDateRange
                            },
                            pageable = withArg {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 10
                                it.sort.getOrderFor(Settlement::expectedSettlementDate.name)?.direction shouldBe Sort.Direction.DESC
                                it.sort.getOrderFor(Settlement::accidentNumber.name)?.direction shouldBe Sort.Direction.DESC
                            }
                        )
                    }
                }

                then("조회된 정산 목록을 반환한다.") {
                    val actualResult = behavior()

                    actualResult shouldContainExactly settlements
                }
            }

            `when`("정산 예정 일자를 기준으로 검색어 없이 정산 목록을 조회하면") {
                val query = generateSettlementsSearchQuery(
                    progressingStatus = SettlementProgressingStatus.WAITING,
                    expectedSettlementDate = expectedSettlementDateRange,
                    transactionDate = null,
                    searchCondition = null,
                    sorting = SettlementsSearchQuery.Sorting.EXPECTED_SETTLEMENT_DATE_DESC_ACCIDENT_NUMBER_DESC,
                )

                fun behavior() = service.getSettlementsAsCsv(query)

                then("리포지토리에서 정산 목록을 조회한다.") {
                    behavior()

                    verify {
                        settlementRepository.searchSettlements(
                            withArg {
                                it.progressingStatus shouldBe SettlementProgressingStatus.WAITING
                                it.expectedSettlementDate shouldBe expectedSettlementDateRange
                            },
                            withArg<Sort> {
                                it.getOrderFor(Settlement::expectedSettlementDate.name)?.direction shouldBe Sort.Direction.DESC
                                it.getOrderFor(Settlement::accidentNumber.name)?.direction shouldBe Sort.Direction.DESC
                            }

                        )
                    }
                }
            }

            `when`("입출금 일자 기준으로 정렬 기준을 마지막 입출금 내림차순으로 정렬된 정산 목록을 조회하면") {
                val query = generateSettlementsSearchQuery(
                    progressingStatus = SettlementProgressingStatus.COMPLETED,
                    expectedSettlementDate = null,
                    transactionDate = transactionDateRange,
                    searchCondition = null,
                    sorting = SettlementsSearchQuery.Sorting.LAST_TRANSACTION_DATE_TIME_DESC,
                )

                fun behavior() = service.getSettlements(query, PageRequest.of(0, 10))

                then("리포지토리에서 정산 목록을 조회한다.") {
                    behavior()

                    verify {
                        settlementRepository.searchSettlements(
                            withArg {
                                it.progressingStatus shouldBe SettlementProgressingStatus.COMPLETED
                                it.lastTransactionDate shouldBe transactionDateRange
                            },
                            pageable = withArg {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 10
                                it.sort.getOrderFor(Settlement::lastTransactionDatetime.name)?.direction shouldBe Sort.Direction.DESC
                            }
                        )
                    }
                }
            }

            `when`("사고번호 검색 조건을 입력하여 정산 목록을 조회하면") {
                val query = generateSettlementsSearchQuery(
                    progressingStatus = SettlementProgressingStatus.WAITING,
                    expectedSettlementDate = expectedSettlementDateRange,
                    transactionDate = null,
                    searchCondition = SearchCondition(
                        searchingProperty = SettlementsSearchQuery.SearchingProperty.ACCIDENT_NUMBER,
                        keyword = "2022-"
                    )
                )

                fun behavior() = service.getSettlements(query, PageRequest.of(0, 10))

                then("리포지토리에서 정산 목록을 조회한다.") {
                    behavior()

                    verify {
                        settlementRepository.searchSettlements(
                            withArg {
                                it.progressingStatus shouldBe SettlementProgressingStatus.WAITING
                                it.expectedSettlementDate shouldBe expectedSettlementDateRange
                                it.accidentNumber shouldBe "2022-"
                            },
                            pageable = withArg {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 10
                                it.sort.getOrderFor(Settlement::expectedSettlementDate.name)?.direction shouldBe Sort.Direction.DESC
                                it.sort.getOrderFor(Settlement::accidentNumber.name)?.direction shouldBe Sort.Direction.DESC
                            }
                        )
                    }
                }

                // 반환 검증 생략
            }

            `when`("환자 이름 검색 조건을 입력하여 정산 목록을 조회하면") {
                val query = generateSettlementsSearchQuery(
                    expectedSettlementDate = expectedSettlementDateRange,
                    transactionDate = null,
                    progressingStatus = SettlementProgressingStatus.WAITING,
                    searchCondition = SearchCondition(
                        searchingProperty = SettlementsSearchQuery.SearchingProperty.PATIENT_NAME,
                        keyword = "박재병"
                    )
                )

                fun behavior() = service.getSettlements(query, PageRequest.of(0, 10))

                then("리포지토리에서 정산 목록을 조회한다.") {
                    behavior()

                    verify {
                        settlementRepository.searchSettlements(
                            withArg {
                                it.progressingStatus shouldBe SettlementProgressingStatus.WAITING
                                it.expectedSettlementDate shouldBe expectedSettlementDateRange
                                it.patientName shouldBe "박재병"
                            },
                            pageable = withArg {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 10
                                it.sort.getOrderFor(Settlement::expectedSettlementDate.name)?.direction shouldBe Sort.Direction.DESC
                                it.sort.getOrderFor(Settlement::accidentNumber.name)?.direction shouldBe Sort.Direction.DESC
                            }
                        )
                    }
                }

                // 반환 검증 생략
            }

            `when`("간병인 소속으로 검색 조건을 입력하여 정산 목록을 조회하면") {
                val query = generateSettlementsSearchQuery(
                    progressingStatus = SettlementProgressingStatus.WAITING,
                    expectedSettlementDate = expectedSettlementDateRange,
                    transactionDate = null,
                    searchCondition = SearchCondition(
                        searchingProperty = SettlementsSearchQuery.SearchingProperty.ORGANIZATION_NAME,
                        keyword = "협회"
                    )
                )

                fun behavior() = service.getSettlements(query, PageRequest.of(0, 10))

                then("리포지토리에서 정산 목록을 조회한다.") {
                    behavior()

                    verify {
                        settlementRepository.searchSettlements(
                            withArg {
                                it.progressingStatus shouldBe SettlementProgressingStatus.WAITING
                                it.expectedSettlementDate shouldBe expectedSettlementDateRange
                                it.organizationName shouldBe "협회"
                            },
                            pageable = withArg {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 10
                                it.sort.getOrderFor(Settlement::expectedSettlementDate.name)?.direction shouldBe Sort.Direction.DESC
                                it.sort.getOrderFor(Settlement::accidentNumber.name)?.direction shouldBe Sort.Direction.DESC
                            }
                        )
                    }
                }

                // 반환 검증 생략
            }

            `when`("내부 사용자 권한 없이 정산 목록을 조회하면") {
                val query = generateSettlementsSearchQuery(
                    progressingStatus = SettlementProgressingStatus.WAITING,
                    expectedSettlementDate = expectedSettlementDateRange,
                    transactionDate = null,
                    searchCondition = SearchCondition(
                        searchingProperty = SettlementsSearchQuery.SearchingProperty.ORGANIZATION_NAME,
                        keyword = "협회"
                    ),
                    subject = generateExternalCaregivingOrganizationManagerSubject("01GVMQXE9SFT6F199WG99T0R7X")
                )

                fun behavior() = service.getSettlements(query, PageRequest.of(0, 10))

                then("AccessDeniedException이 발생합니다.") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }

            `when`("정산들을 수정하면") {
                val subject = generateInternalCaregivingManagerSubject()
                val commands = listOf(
                    SettlementByIdQuery(
                        settlementId = "01GVCX47T2590S6RYTTFDGJQP6",
                        subject = subject,
                    ) to SettlementEditingCommand(
                        progressingStatus = Patches.ofValue(SettlementProgressingStatus.COMPLETED),
                        settlementManagerId = Patches.ofValue("01GVCZ7W7MAYAC6C7JMJHSNEJR"),
                        subject = subject,
                    ),
                    SettlementByIdQuery(
                        settlementId = "01GVCXJ4RX7A8KP4DPB7CDWRV8",
                        subject = subject,
                    ) to SettlementEditingCommand(
                        progressingStatus = Patches.ofValue(SettlementProgressingStatus.COMPLETED),
                        settlementManagerId = Patches.ofValue("01GVCZ7W7MAYAC6C7JMJHSNEJR"),
                        subject = subject,
                    ),
                )

                fun behavior() = service.editSettlements(commands)

                then("수정할 정산 목록을 조회합니다.") {
                    behavior()

                    verify {
                        settlementRepository.findByIdIn(
                            withArg {
                                it shouldContainExactly listOf(
                                    "01GVCX47T2590S6RYTTFDGJQP6",
                                    "01GVCXJ4RX7A8KP4DPB7CDWRV8"
                                )
                            }
                        )
                    }
                }

                then("정산 관리자로 지정할 내부 사용자가 존재함을 확인합니다.") {
                    behavior()

                    verify {
                        internalCaregivingManagerByIdQueryHandler.ensureInternalCaregivingManagerExists(
                            withArg {
                                it.internalCaregivingManagerId shouldBe "01GVCZ7W7MAYAC6C7JMJHSNEJR"
                            }
                        )
                    }
                }

                then("정산을 수정합니다.") {
                    behavior()

                    verify {
                        settlements[0].edit(
                            withArg {
                                it.progressingStatus shouldBe OverwritePatch(SettlementProgressingStatus.COMPLETED)
                                it.settlementManagerId shouldBe OverwritePatch("01GVCZ7W7MAYAC6C7JMJHSNEJR")
                            }
                        )
                        settlements[1].edit(
                            withArg {
                                it.progressingStatus shouldBe OverwritePatch(SettlementProgressingStatus.COMPLETED)
                                it.settlementManagerId shouldBe OverwritePatch("01GVCZ7W7MAYAC6C7JMJHSNEJR")
                            }
                        )
                    }
                }
            }

            `when`("존재하지 않는 정산을 수정하면") {
                val subject = generateInternalCaregivingManagerSubject()
                val commands = listOf(
                    SettlementByIdQuery(
                        settlementId = "01GVCX47T2590S6RYTTFDGJQP6",
                        subject = subject,
                    ) to SettlementEditingCommand(
                        progressingStatus = Patches.ofValue(SettlementProgressingStatus.COMPLETED),
                        settlementManagerId = Patches.ofValue("01GVCZ7W7MAYAC6C7JMJHSNEJR"),
                        subject = subject,
                    ),
                    SettlementByIdQuery(
                        settlementId = "01GVQPB2CBGYY5GWE6QC2V5R24",
                        subject = subject,
                    ) to SettlementEditingCommand(
                        progressingStatus = Patches.ofValue(SettlementProgressingStatus.COMPLETED),
                        settlementManagerId = Patches.ofValue("01GVCZ7W7MAYAC6C7JMJHSNEJR"),
                        subject = subject,
                    ),
                )

                fun behavior() = service.editSettlements(commands)
                then("ReferenceSettlementNotExistsException이 발생합니다.") {
                    val thrownException = shouldThrow<ReferenceSettlementNotExistsException> { behavior() }

                    thrownException.referenceSettlementId shouldBe "01GVQPB2CBGYY5GWE6QC2V5R24"
                }
            }

            `when`("정산의 정산 담당자로 존재하지 않는 내부 사용자를 지정하여 수정하면") {
                val subject = generateInternalCaregivingManagerSubject()
                val commands = listOf(
                    SettlementByIdQuery(
                        settlementId = "01GVCX47T2590S6RYTTFDGJQP6",
                        subject = subject,
                    ) to SettlementEditingCommand(
                        progressingStatus = Patches.ofValue(SettlementProgressingStatus.COMPLETED),
                        settlementManagerId = Patches.ofValue("01GVQN21EDXQ6NSJJ50WGR2JHE"),
                        subject = subject,
                    ),
                    SettlementByIdQuery(
                        settlementId = "01GVCXJ4RX7A8KP4DPB7CDWRV8",
                        subject = subject,
                    ) to SettlementEditingCommand(
                        progressingStatus = Patches.ofValue(SettlementProgressingStatus.COMPLETED),
                        settlementManagerId = Patches.ofValue("01GVQN21EDXQ6NSJJ50WGR2JHE"),
                        subject = subject,
                    ),
                )

                fun behavior() = service.editSettlements(commands)

                then("ReferenceInternalCaregivingManagerNotExistsException이 발생합니다.") {
                    val thrownException =
                        shouldThrow<ReferenceInternalCaregivingManagerNotExistsException> { behavior() }

                    thrownException.referenceInternalCaregivingManagerId shouldBe "01GVQN21EDXQ6NSJJ50WGR2JHE"
                }
            }

            `when`("간병비가 재산정되었음이 감지되면") {
                val event = relaxedMock<CaregivingChargeModified>()

                beforeEach {
                    with(event) {
                        every { caregivingRoundId } returns "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                    }
                }

                afterEach { clearAllMocks() }

                fun handling() = service.handleCaregivingChargeModified(event)

                then("재산정된 간병비의 정산을 조회합니다.") {
                    handling()

                    verify {
                        settlementRepository.findByCaregivingRoundId("01GSM0PQ5G8HW2GKYXH3VGGMZG")
                    }
                }

                then("간병비가 재정산되었음을 정산에 알립니다.") {
                    handling()

                    verify {
                        settlements[0].handleCaregivingChargeModified(event)
                    }
                }
            }

            `when`("정산의 입출금 내역을 조회하면") {
                val query = TransactionsBySettlementIdQuery(
                    settlementId = "01GVCX47T2590S6RYTTFDGJQP6",
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = service.getTransactions(
                    query,
                    PageRequest.of(0, 2)
                )

                then("입출금 내역을 조회할 정산을 조회합니다.") {
                    behavior()

                    verify {
                        settlementRepository.findById("01GVCX47T2590S6RYTTFDGJQP6")
                    }
                }

                then("정산의 입출금 내역을 페이징하여 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.totalElements shouldBe 3
                    actualResult.pageable.pageNumber shouldBe 0
                    actualResult.pageable.pageSize shouldBe 2
                    actualResult.content.map { it.transactionDate } shouldContainExactlyInAnyOrder setOf(
                        LocalDate.of(2022, 1, 28),
                        LocalDate.of(2022, 1, 30),
                    )
                }

                then("반환된 정산은 반드시 입출금일, 입력일 기준으로 내림차순 정렬되어있어야 합니다.") {
                    val actualResult = behavior()

                    actualResult.content shouldBeSortedWith compareByDescending<Settlement.TransactionRecord> {
                        it.transactionDate
                    }.thenByDescending {
                        it.enteredDateTime
                    }
                }
            }

            `when`("정산의 입출금 내역을 내부 관리자 권한 없이 조회하면") {
                val query = TransactionsBySettlementIdQuery(
                    settlementId = "01GVCX47T2590S6RYTTFDGJQP6",
                    subject = generateGuestSubject(),
                )

                fun behavior() = service.getTransactions(
                    query,
                    PageRequest.of(0, 2)
                )

                then("AccessDeniedException이 발생합니다.") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }

            `when`("정산의 입출금 내역을 기록하면") {
                val subject = generateInternalCaregivingManagerSubject()
                val query = SettlementByIdQuery(
                    settlementId = "01GVCX47T2590S6RYTTFDGJQP6",
                    subject = subject,
                )
                val command = SettlementTransactionRecordingCommand(
                    transactionType = TransactionType.WITHDRAWAL,
                    amount = 5000,
                    transactionDate = LocalDate.of(2022, 1, 30),
                    transactionSubjectId = "01GW1160R5ZC9E3P5V57TYQX0E",
                    subject = subject,
                )

                fun behavior() = service.recordTransaction(
                    query,
                    command
                )

                then("입출금을 기록할 정산을 조회합니다.") {
                    behavior()

                    verify {
                        settlementRepository.findById("01GVCX47T2590S6RYTTFDGJQP6")
                    }
                }

                then("대상이 되는 정산에 입출금 내역 기록을 전달합니다.") {
                    behavior()

                    verify {
                        settlements[0].recordTransaction(command)
                    }
                }
            }

            `when`("정산의 입출금 내역을 내부 관리자 권한 없이 기록하면") {
                val subject = generateGuestSubject()
                val query = SettlementByIdQuery(
                    settlementId = "01GVCX47T2590S6RYTTFDGJQP6",
                    subject = subject,
                )
                val command = SettlementTransactionRecordingCommand(
                    transactionType = TransactionType.WITHDRAWAL,
                    amount = 5000,
                    transactionDate = LocalDate.of(2022, 1, 30),
                    transactionSubjectId = "01GW1160R5ZC9E3P5V57TYQX0E",
                    subject = subject,
                )

                fun behavior() = service.recordTransaction(
                    query,
                    command
                )

                then("AccessDeniedException이 발생합니다.") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }

            `when`("간병 회차 아이디로 간병을 조회하면") {
                val query = SettlementByCaregivingRoundIdQuery(
                    caregivingRoundId = "01GSM0PQ5G8HW2GKYXH3VGGMZG",
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = service.getSettlement(query)

                then("리포지토리로부터 간병 회차 아이디로 정산을 조회합니다.") {
                    behavior()

                    verify {
                        settlementRepository.findTopByCaregivingRoundId("01GSM0PQ5G8HW2GKYXH3VGGMZG")
                    }
                }

                then("조회한 정산을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult shouldBe settlements[0]
                }
            }

            `when`("시스템 사용자 권한으로 간병 회차 아이디로 정산을 조회하면") {
                val query = SettlementByCaregivingRoundIdQuery(
                    caregivingRoundId = "01GSM0PQ5G8HW2GKYXH3VGGMZG",
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = service.getSettlement(query)

                then("문제없이 응답합니다.") {
                    shouldNotThrowAny { behavior() }
                }
            }

            `when`("아무런 관련 없는 외부 간병 업체 사용자가 간병 회차 아이디로 정산을 조회하면") {
                val query = SettlementByCaregivingRoundIdQuery(
                    caregivingRoundId = "01GSM0PQ5G8HW2GKYXH3VGGMZG",
                    subject = generateGuestSubject(),
                )

                fun behavior() = service.getSettlement(query)

                then("AccessDeniedException이 발생합니다.") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }

            `when`("접수정보가 변경되었음이 감지되면") {
                val event = relaxedMock<ReceptionModified>()

                beforeEach {
                    with(event) {
                        every { receptionId } returns "01GVD2HS5FMX9012BN28VHDPW3"
                        every { accidentInfo } returns Modification(
                            relaxedMock {
                                every { accidentNumber } returns "2022-1111111"
                            },
                            relaxedMock {
                                every { accidentNumber } returns "2022-3333333"
                            }
                        )
                        every { patientInfo } returns Modification(
                            relaxedMock {
                                every { nickname } returns "뽀리스"
                            },
                            relaxedMock {
                                every { nickname } returns "포리스"
                            }
                        )
                    }
                }

                afterEach { clearAllMocks() }

                fun handling() = service.handleReceptionModified(event)

                then("정산에 접수정보가 변경되었음을 알립니다.") {
                    handling()

                    verify {
                        settlements[0].handleReceptionModified(event)
                        settlements[1].handleReceptionModified(event)
                    }
                }
            }

            `when`("접수정보가 변경되었음이 감지되었지만 정산에 영향을 주는 요소가 없다면") {
                val event = relaxedMock<ReceptionModified>()

                beforeEach {
                    with(event) {
                        every { receptionId } returns "01GVD2HS5FMX9012BN28VHDPW3"
                        every { accidentInfo } returns Modification(
                            relaxedMock {
                                every { accidentNumber } returns "2022-1111111"
                            },
                            relaxedMock {
                                every { accidentNumber } returns "2022-1111111"
                            }
                        )
                        every { patientInfo } returns Modification(
                            relaxedMock {
                                every { nickname } returns "뽀리스"
                            },
                            relaxedMock {
                                every { nickname } returns "뽀리스"
                            }
                        )
                    }
                }

                afterEach { clearAllMocks() }

                fun handling() = service.handleReceptionModified(event)

                then("아무것도 안합니다.") {
                    handling()

                    verify(exactly = 0) {
                        settlementRepository.findByReceptionId(any())
                        settlements[0].handleReceptionModified(any())
                        settlements[1].handleReceptionModified(any())
                    }
                }
            }
        }

        and("엔티티 테스트할 떄") {
            val settlement = Settlement(
                id = "01GVCX47T2590S6RYTTFDGJQP6",
                receptionId = "01GVD2HS5FMX9012BN28VHDPW3",
                caregivingRoundId = "01GSM0PQ5G8HW2GKYXH3VGGMZG",
                caregivingRoundNumber = 2,
                accidentNumber = "2022-1111111",
                dailyCaregivingCharge = 121000,
                basicAmount = 605000,
                additionalAmount = 20000,
                totalAmount = 625000,
                lastCalculationDateTime = LocalDateTime.of(2022, 1, 30, 5, 21, 31),
                expectedSettlementDate = LocalDate.of(2022, 1, 30),
            )
            `when`("저장을 요청하면") {
                fun behavior() = cacheSettlementRepository.save(settlement)
                then("저장이 됩니다.") {
                    behavior()
                }
            }
            `when`("조회를 요청하면") {
                fun behavior() = cacheSettlementRepository.findByIdOrNull("01GVCX47T2590S6RYTTFDGJQP6")
                then("조회가 됩니다.") {
                    behavior()
                }
            }
        }
    }
})
