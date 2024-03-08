package kr.caredoc.careinsurance.caregiving

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kr.caredoc.careinsurance.account.AccountInfo
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationByIdQueryHandler
import kr.caredoc.careinsurance.billing.BillingGenerated
import kr.caredoc.careinsurance.billing.BillingModified
import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import kr.caredoc.careinsurance.caregiving.exception.CaregivingChargeEditingDeniedException
import kr.caredoc.careinsurance.caregiving.exception.CaregivingRoundNotFoundByIdException
import kr.caredoc.careinsurance.caregiving.exception.InvalidCaregivingChargeActionableStatusException
import kr.caredoc.careinsurance.caregiving.exception.UnknownCaregivingRoundInfoException
import kr.caredoc.careinsurance.caregiving.state.CancellationReason
import kr.caredoc.careinsurance.caregiving.state.FinishingReason
import kr.caredoc.careinsurance.generateExternalCaregivingOrganizationManagerSubject
import kr.caredoc.careinsurance.generateGuestSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.patch.OverwritePatch
import kr.caredoc.careinsurance.patch.Patch
import kr.caredoc.careinsurance.patient.Sex
import kr.caredoc.careinsurance.reception.CaregivingManagerInfo
import kr.caredoc.careinsurance.reception.OrganizationType
import kr.caredoc.careinsurance.reception.ReceptionByIdQueryHandler
import kr.caredoc.careinsurance.reception.ReceptionModified
import kr.caredoc.careinsurance.reception.ReceptionProgressingStatus
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQueryHandler
import kr.caredoc.careinsurance.reconciliation.IssuedType
import kr.caredoc.careinsurance.reconciliation.ReconciliationClosed
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.search.SearchCondition
import kr.caredoc.careinsurance.security.accesscontrol.ObjectAttribute
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.security.accesscontrol.SystemUser
import kr.caredoc.careinsurance.security.encryption.Decryptor
import kr.caredoc.careinsurance.settlement.SettlementGenerated
import kr.caredoc.careinsurance.settlement.SettlementModified
import kr.caredoc.careinsurance.settlement.SettlementProgressingStatus
import kr.caredoc.careinsurance.withFixedClock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.SimpleApplicationEventMulticaster
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.AccessDeniedException
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
class CaregivingRoundServiceTest(
    @Autowired
    private val cacheCaregivingRoundRepository: CaregivingRoundRepository,
    @Autowired
    private val simpleApplicationEventMulticaster: SimpleApplicationEventMulticaster,
) : BehaviorSpec({
    given("caregiving round service") {
        val caregivingRoundRepository = relaxedMock<CaregivingRoundRepository>()
        val receptionByIdQueryHandler = relaxedMock<ReceptionByIdQueryHandler>()
        val caregivingChargeRepository = relaxedMock<CaregivingChargeRepository>()
        val eventPublisher = relaxedMock<ApplicationEventPublisher>()
        val receptionsByIdsQueryHandler = relaxedMock<ReceptionsByIdsQueryHandler>()
        val externalCaregivingOrganizationByIdQueryHandler = relaxedMock<ExternalCaregivingOrganizationByIdQueryHandler>()
        val decryptor = relaxedMock<Decryptor>()
        val caregivingRoundService = CaregivingRoundService(
            caregivingRoundRepository,
            receptionByIdQueryHandler,
            caregivingChargeRepository,
            eventPublisher,
            receptionsByIdsQueryHandler,
            externalCaregivingOrganizationByIdQueryHandler,
            decryptor,
        )

        beforeEach {
            val savingCaregivingChargeSlot = slot<CaregivingCharge>()
            every {
                caregivingChargeRepository.save(capture(savingCaregivingChargeSlot))
            } answers {
                savingCaregivingChargeSlot.captured
            }

            with(caregivingRoundRepository) {
                val savingEntitySlot = slot<CaregivingRound>()
                every {
                    save(capture(savingEntitySlot))
                } answers { savingEntitySlot.captured }
            }
        }

        afterEach { clearAllMocks() }

        and("caregiving rounds") {
            val caregivingRounds = listOf<CaregivingRound>(relaxedMock(), relaxedMock())

            beforeEach {
                with(caregivingRoundRepository) {
                    val pageableSlot = slot<Pageable>()
                    every {
                        searchCaregivingRounds(any(), capture(pageableSlot))
                    } answers {
                        PageImpl(
                            caregivingRounds,
                            pageableSlot.captured,
                            caregivingRounds.size.toLong(),
                        )
                    }

                    every { findByIdOrNull("01GRTRSB5H4CBFDMHAA231FYPF") } returns caregivingRounds[0]
                }

                with(caregivingRounds[0]) {
                    every { id } returns "01GRTRSB5H4CBFDMHAA231FYPF"
                }
            }

            afterEach { clearAllMocks() }

            `when`("getting caregiving rounds by filters") {
                val query = CaregivingRoundsByFilterQuery(
                    from = LocalDate.of(2023, 2, 1),
                    until = LocalDate.of(2023, 2, 6),
                    organizationId = null,
                    expectedCaregivingStartDate = LocalDate.of(2023, 2, 1),
                    receptionProgressingStatuses = setOf(
                        ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                    ),
                    caregivingProgressingStatuses = setOf(
                        CaregivingProgressingStatus.NOT_STARTED,
                        CaregivingProgressingStatus.REMATCHING,
                        CaregivingProgressingStatus.PENDING_REMATCHING,
                    ),
                    settlementProgressingStatuses = setOf(
                        SettlementProgressingStatus.NOT_STARTED
                    ),
                    billingProgressingStatuses = setOf(
                        BillingProgressingStatus.NOT_STARTED
                    ),
                    searchCondition = SearchCondition(
                        searchingProperty = CaregivingRoundsByFilterQuery.SearchingProperty.ACCIDENT_NUMBER,
                        keyword = "2022-",
                    ),
                    subject = generateInternalCaregivingManagerSubject(),
                )
                val pageRequest = PageRequest.of(0, 20)

                fun behavior() = caregivingRoundService.getCaregivingRounds(query, pageRequest)

                then("returns paged query result") {
                    val actualResult = behavior()
                    actualResult.totalElements shouldBe 2
                    actualResult.totalPages shouldBe 1
                    actualResult.pageable.pageNumber shouldBe 0
                    actualResult.content shouldContainExactly caregivingRounds
                }

                then("query caregiving rounds using repository") {
                    withFixedClock(LocalDateTime.of(2023, 7, 25, 14, 20, 1)) {
                        behavior()
                    }

                    verify {
                        caregivingRoundRepository.searchCaregivingRounds(
                            withArg {
                                it.caregivingStartDateFrom shouldBe LocalDate.of(2023, 2, 1)
                                it.caregivingStartDateUntil shouldBe LocalDate.of(2023, 2, 6)
                                it.organizationId shouldBe null
                                it.expectedCaregivingStartDate shouldBe LocalDate.of(2023, 2, 1)
                                it.receptionProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                                    ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                                )
                                it.caregivingProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                                    CaregivingProgressingStatus.NOT_STARTED,
                                    CaregivingProgressingStatus.REMATCHING,
                                    CaregivingProgressingStatus.PENDING_REMATCHING,
                                )
                                it.settlementProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                                    SettlementProgressingStatus.NOT_STARTED
                                )
                                it.billingProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                                    BillingProgressingStatus.NOT_STARTED
                                )
                                it.accidentNumberContains shouldBe "2022-"
                                it.patientName shouldBe null
                                it.insuranceNumberContains shouldBe null
                                it.receptionReceivedDateFrom shouldBe LocalDate.of(2023, 1, 6)
                            },
                            pageable = withArg {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 20
                            }
                        )
                    }
                }
            }

            `when`("필터링하여 간병 회차 목록을 CSV 형식으로 조회하면") {
                val query = CaregivingRoundsByFilterQuery(
                    from = LocalDate.of(2023, 7, 1),
                    until = LocalDate.of(2023, 9, 1),
                    organizationId = null,
                    expectedCaregivingStartDate = LocalDate.of(2023, 8, 1),
                    receptionProgressingStatuses = setOf(
                        ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                    ),
                    caregivingProgressingStatuses = setOf(
                        CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
                    ),
                    settlementProgressingStatuses = setOf(
                        SettlementProgressingStatus.NOT_STARTED
                    ),
                    billingProgressingStatuses = setOf(
                        BillingProgressingStatus.NOT_STARTED
                    ),
                    searchCondition = SearchCondition(
                        searchingProperty = CaregivingRoundsByFilterQuery.SearchingProperty.ACCIDENT_NUMBER,
                        keyword = "2023-",
                    ),
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = caregivingRoundService.getCaregivingRoundsAsCsv(query)

                then("리포지토리에서 간병 회차 목록을 조회합니다.") {
                    withFixedClock(LocalDateTime.of(2023, 10, 10, 0, 0, 0)) {
                        behavior()
                    }

                    verify {
                        caregivingRoundRepository.searchCaregivingRounds(
                            withArg {
                                it.expectedCaregivingStartDate shouldBe LocalDate.of(2023, 8, 1)
                                it.receptionProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                                    ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                                )
                                it.caregivingProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                                    CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
                                )
                                it.settlementProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                                    SettlementProgressingStatus.NOT_STARTED
                                )
                                it.billingProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                                    BillingProgressingStatus.NOT_STARTED
                                )
                                it.accidentNumberContains shouldBe "2023-"
                            }
                        )
                    }
                }
            }

            `when`("getting organization caregiving rounds by filters") {
                val query = CaregivingRoundsByFilterQuery(
                    from = LocalDate.of(2023, 2, 1),
                    until = LocalDate.of(2023, 2, 6),
                    organizationId = "01GRWDRWTM6ENFSQXTDN9HDDWK",
                    expectedCaregivingStartDate = LocalDate.of(2023, 2, 1),
                    receptionProgressingStatuses = setOf(
                        ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                    ),
                    caregivingProgressingStatuses = setOf(
                        CaregivingProgressingStatus.NOT_STARTED,
                        CaregivingProgressingStatus.REMATCHING,
                        CaregivingProgressingStatus.PENDING_REMATCHING,
                    ),
                    settlementProgressingStatuses = setOf(
                        SettlementProgressingStatus.NOT_STARTED
                    ),
                    billingProgressingStatuses = setOf(
                        BillingProgressingStatus.NOT_STARTED
                    ),
                    searchCondition = SearchCondition(
                        searchingProperty = CaregivingRoundsByFilterQuery.SearchingProperty.ACCIDENT_NUMBER,
                        keyword = "2022-1234567",
                    ),
                    subject = generateInternalCaregivingManagerSubject(),
                )
                val pageRequest = PageRequest.of(0, 20)

                fun behavior() = caregivingRoundService.getCaregivingRounds(query, pageRequest)

                then("returns paged query result") {
                    val actualResult = behavior()
                    actualResult.totalElements shouldBe 2
                    actualResult.totalPages shouldBe 1
                    actualResult.pageable.pageNumber shouldBe 0
                    actualResult.content shouldContainExactly caregivingRounds
                }

                then("query caregiving rounds using repository") {
                    withFixedClock(LocalDateTime.of(2023, 7, 25, 14, 20, 1)) {
                        behavior()
                    }

                    verify {
                        caregivingRoundRepository.searchCaregivingRounds(
                            withArg {
                                it.caregivingStartDateFrom shouldBe LocalDate.of(2023, 2, 1)
                                it.caregivingStartDateUntil shouldBe LocalDate.of(2023, 2, 6)
                                it.organizationId shouldBe "01GRWDRWTM6ENFSQXTDN9HDDWK"
                                it.expectedCaregivingStartDate shouldBe LocalDate.of(2023, 2, 1)
                                it.receptionProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                                    ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                                )
                                it.caregivingProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                                    CaregivingProgressingStatus.NOT_STARTED,
                                    CaregivingProgressingStatus.REMATCHING,
                                    CaregivingProgressingStatus.PENDING_REMATCHING,
                                )
                                it.settlementProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                                    SettlementProgressingStatus.NOT_STARTED
                                )
                                it.billingProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                                    BillingProgressingStatus.NOT_STARTED
                                )
                                it.accidentNumberContains shouldBe "2022-1234567"
                                it.receptionReceivedDateFrom shouldBe LocalDate.of(2023, 1, 6)
                            },
                            pageable = withArg {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 20
                            }
                        )
                    }
                }
            }

            `when`("필터링하여 제휴사 간병 회차 목록을 CSV 형식으로 조회하면") {
                val query = CaregivingRoundsByFilterQuery(
                    from = LocalDate.of(2023, 7, 1),
                    until = LocalDate.of(2023, 9, 1),
                    organizationId = "01GRWDRWTM6ENFSQXTDN9HDDWK",
                    expectedCaregivingStartDate = LocalDate.of(2023, 8, 1),
                    receptionProgressingStatuses = setOf(
                        ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                    ),
                    caregivingProgressingStatuses = setOf(
                        CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
                    ),
                    settlementProgressingStatuses = setOf(
                        SettlementProgressingStatus.NOT_STARTED
                    ),
                    billingProgressingStatuses = setOf(
                        BillingProgressingStatus.NOT_STARTED
                    ),
                    searchCondition = SearchCondition(
                        searchingProperty = CaregivingRoundsByFilterQuery.SearchingProperty.ACCIDENT_NUMBER,
                        keyword = "2023-",
                    ),
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = caregivingRoundService.getCaregivingRoundsAsCsv(query)

                then("리포지토리에서 간병 회차 목록을 조회합니다.") {
                    withFixedClock(LocalDateTime.of(2023, 10, 10, 0, 0, 0)) {
                        behavior()
                    }

                    verify {
                        caregivingRoundRepository.searchCaregivingRounds(
                            withArg {
                                it.organizationId shouldBe "01GRWDRWTM6ENFSQXTDN9HDDWK"
                                it.expectedCaregivingStartDate shouldBe LocalDate.of(2023, 8, 1)
                                it.receptionProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                                    ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                                )
                                it.caregivingProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                                    CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
                                )
                                it.settlementProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                                    SettlementProgressingStatus.NOT_STARTED
                                )
                                it.billingProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                                    BillingProgressingStatus.NOT_STARTED
                                )
                                it.accidentNumberContains shouldBe "2023-"
                            }
                        )
                    }
                }
            }
        }

        `when`("getting organization caregiving rounds by filters by guest user") {
            val query = CaregivingRoundsByFilterQuery(
                from = LocalDate.of(2023, 2, 1),
                until = LocalDate.of(2023, 2, 6),
                organizationId = "01GRWDRWTM6ENFSQXTDN9HDDWK",
                expectedCaregivingStartDate = null,
                receptionProgressingStatuses = setOf(
                    ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                ),
                caregivingProgressingStatuses = setOf(
                    CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
                ),
                settlementProgressingStatuses = setOf(
                    SettlementProgressingStatus.NOT_STARTED
                ),
                billingProgressingStatuses = setOf(
                    BillingProgressingStatus.NOT_STARTED
                ),
                searchCondition = null,
                subject = generateGuestSubject(),
            )
            val pageRequest = PageRequest.of(0, 20)

            fun behavior() = caregivingRoundService.getCaregivingRounds(query, pageRequest)

            then("throws AccessDeniedException") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }

        `when`("getting organization caregiving rounds by filters with external organization") {
            val query = CaregivingRoundsByFilterQuery(
                from = LocalDate.of(2023, 2, 1),
                until = LocalDate.of(2023, 2, 6),
                organizationId = "01GRWDRWTM6ENFSQXTDN9HDDWK",
                expectedCaregivingStartDate = null,
                receptionProgressingStatuses = setOf(
                    ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                ),
                caregivingProgressingStatuses = setOf(
                    CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
                ),
                settlementProgressingStatuses = setOf(
                    SettlementProgressingStatus.NOT_STARTED
                ),
                billingProgressingStatuses = setOf(
                    BillingProgressingStatus.NOT_STARTED
                ),
                searchCondition = null,
                subject = generateExternalCaregivingOrganizationManagerSubject("01GRWDRWTM6ENFSQXTDN9HDDWK")
            )
            val pageRequest = PageRequest.of(0, 20)

            fun behavior() = caregivingRoundService.getCaregivingRounds(query, pageRequest)

            then("query caregiving rounds using repository") {
                withFixedClock(LocalDateTime.of(2023, 7, 25, 14, 20, 1)) {
                    behavior()
                }

                verify {
                    caregivingRoundRepository.searchCaregivingRounds(
                        withArg {
                            it.caregivingStartDateFrom shouldBe LocalDate.of(2023, 2, 1)
                            it.caregivingStartDateUntil shouldBe LocalDate.of(2023, 2, 6)
                            it.organizationId shouldBe "01GRWDRWTM6ENFSQXTDN9HDDWK"

                            it.receptionProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                                ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                            )
                            it.caregivingProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                                CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
                            )
                            it.settlementProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                                SettlementProgressingStatus.NOT_STARTED
                            )
                            it.billingProgressingStatuses shouldContainExactlyInAnyOrder setOf(
                                BillingProgressingStatus.NOT_STARTED
                            )
                            it.receptionReceivedDateFrom shouldBe LocalDate.of(2023, 1, 6)
                        },
                        pageable = withArg {
                            it.pageNumber shouldBe 0
                            it.pageSize shouldBe 20
                        }
                    )
                }
            }
        }

        and("접수의 간병 목록") {
            val receptionCaregivingRounds = listOf<CaregivingRound>(relaxedMock(), relaxedMock())

            beforeEach {
                with(caregivingRoundRepository) {
                    every {
                        findByReceptionInfoReceptionIdOrderByCaregivingRoundNumberDesc(any())
                    } answers {
                        receptionCaregivingRounds
                    }

                    val caregivingRoundIdsSlot = slot<Collection<String>>()
                    every {
                        findByIdIn(capture(caregivingRoundIdsSlot))
                    } answers {
                        val capturedCaregivingRoundIds = caregivingRoundIdsSlot.captured

                        val queriedCaregivingRounds = mutableListOf<CaregivingRound>()
                        if (capturedCaregivingRoundIds.contains("01GSEM051HE80DCJ4ETEX6X5VD")) {
                            queriedCaregivingRounds.add(receptionCaregivingRounds[0])
                        }
                        if (capturedCaregivingRoundIds.contains("01GSEKE6HDDM2F25H3FP481DZW")) {
                            queriedCaregivingRounds.add(receptionCaregivingRounds[1])
                        }

                        queriedCaregivingRounds.toList()
                    }
                }
                with(receptionCaregivingRounds[0]) {
                    every { id } returns "01GSEM051HE80DCJ4ETEX6X5VD"
                }
                with(receptionCaregivingRounds[1]) {
                    every { id } returns "01GSEKE6HDDM2F25H3FP481DZW"
                }
            }

            afterEach { clearAllMocks() }

            `when`("내부 직원으로서 접수의 간병 목록을 조회하면") {
                fun behavior() = caregivingRoundService.getReceptionCaregivingRounds(
                    CaregivingRoundsByReceptionIdQuery(
                        receptionId = "01GSC4SKPGWMZDP7EKWQ0Z57NG",
                        subject = generateInternalCaregivingManagerSubject()
                    )
                )

                then("입력된 쿼리의 Reception Id로 간병 회차 목록을 조회한다.") {
                    behavior()

                    verify {
                        caregivingRoundRepository.findByReceptionInfoReceptionIdOrderByCaregivingRoundNumberDesc(
                            withArg {
                                it shouldBe "01GSC4SKPGWMZDP7EKWQ0Z57NG"
                            }
                        )
                    }
                }
            }

            `when`("게스트로서 접수의 간병 목록을 조회하면") {
                fun behavior() = caregivingRoundService.getReceptionCaregivingRounds(
                    CaregivingRoundsByReceptionIdQuery(
                        receptionId = "01GSC4SKPGWMZDP7EKWQ0Z57NG",
                        subject = generateGuestSubject()
                    )
                )

                then("AccessDeniedException 이 발생한다.") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }

            `when`("외부 소속 직원으로서 접수의 간병 목록을 조회하면") {
                beforeEach {
                    with(caregivingRoundRepository) {
                        every { findByIdOrNull("01GSEM051HE80DCJ4ETEX6X5VD") } returns receptionCaregivingRounds[0]
                        every { findByIdOrNull("01GSEKE6HDDM2F25H3FP481DZW") } returns receptionCaregivingRounds[1]
                    }
                    with(receptionCaregivingRounds[0]) {
                        every { id } returns "01GSEM051HE80DCJ4ETEX6X5VD"
                        every { receptionInfo.receptionId } returns "01GSC4SKPGWMZDP7EKWQ0Z57NG"
                        every { receptionInfo.caregivingManagerInfo.organizationId } returns "01GSEKXGET5JKFST29B5K0N4XH"
                        every { get(ObjectAttribute.ASSIGNED_ORGANIZATION_ID) } returns setOf("01GSEKXGET5JKFST29B5K0N4XH")
                    }
                    with(receptionCaregivingRounds[1]) {
                        every { id } returns "01GSEKE6HDDM2F25H3FP481DZW"
                        every { receptionInfo.receptionId } returns "01GSEKDW5ZZ8G119F7ZEC8VR78"
                        every { receptionInfo.caregivingManagerInfo.organizationId } returns "01GSEKXGET5JKFST29B5K0N4XH"
                        every { get(ObjectAttribute.ASSIGNED_ORGANIZATION_ID) } returns setOf("01GSEKXGET5JKFST29B5K0N4XH")
                    }
                }
                afterEach { clearAllMocks() }

                fun behavior() = caregivingRoundService.getReceptionCaregivingRounds(
                    CaregivingRoundsByReceptionIdQuery(
                        receptionId = "01GSC4SKPGWMZDP7EKWQ0Z57NG",
                        subject = generateExternalCaregivingOrganizationManagerSubject("01GSEKXGET5JKFST29B5K0N4XH")
                    )
                )

                then("입력된 쿼리의 Reception Id로 간병 회차 목록을 조회한다.") {
                    behavior()

                    verify {
                        caregivingRoundRepository.findByReceptionInfoReceptionIdOrderByCaregivingRoundNumberDesc(
                            withArg {
                                it shouldBe "01GSC4SKPGWMZDP7EKWQ0Z57NG"
                            }
                        )
                    }
                }
            }

            `when`("접수 데이터가 변경되면") {
                val event = relaxedMock<ReceptionModified> {
                    every { receptionId } returns "01GVAJRX4TXCDTV9J4WY0Y89HB"
                }
                val caregivingChargesInReception = listOf<CaregivingCharge>(
                    relaxedMock(),
                    relaxedMock()
                )

                beforeEach {
                    every {
                        caregivingChargeRepository.findByCaregivingRoundInfoCaregivingRoundIdIn(
                            match {
                                it.containsAll(setOf("01GSEM051HE80DCJ4ETEX6X5VD", "01GSEKE6HDDM2F25H3FP481DZW"))
                            }
                        )
                    } returns caregivingChargesInReception
                }

                fun handling() = caregivingRoundService.handleReceptionModified(event)

                then("접수에 속한 간병 회차를 조회한다.") {
                    handling()

                    verify {
                        caregivingRoundRepository.findByReceptionInfoReceptionIdOrderByCaregivingRoundNumberDesc("01GVAJRX4TXCDTV9J4WY0Y89HB")
                    }
                }

                then("접수에 속한 간병 회차에 접수 데이터가 변경되었음을 알린다.") {
                    handling()

                    receptionCaregivingRounds.forEach {
                        verify {
                            it.handleReceptionModified(event)
                        }
                    }
                }

                then("간병 회차에 속한 간병비 산정을 조회한다.") {
                    handling()

                    verify {
                        caregivingChargeRepository.findByCaregivingRoundInfoCaregivingRoundIdIn(
                            withArg {
                                it shouldContainExactly setOf(
                                    "01GSEM051HE80DCJ4ETEX6X5VD",
                                    "01GSEKE6HDDM2F25H3FP481DZW"
                                )
                            }
                        )
                    }
                }

                then("간병 회차에 속한 간병비에 접수 데이터가 변경되었음을 알린다.") {
                    handling()

                    caregivingChargesInReception.forEach {
                        verify {
                            it.handleReceptionModified(event)
                        }
                    }
                }
            }

            `when`("간병 회차 아이디 목록으로 간병 목록을 조회하면") {
                val query = CaregivingRoundsByIdsQuery(
                    caregivingRoundIds = listOf(
                        "01GSEM051HE80DCJ4ETEX6X5VD",
                        "01GSEKE6HDDM2F25H3FP481DZW",
                    ),
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = caregivingRoundService.getCaregivingRounds(query)

                then("리포지토리로부터 간병회차 목록을 조회합니다.") {
                    behavior()

                    verify {
                        caregivingRoundRepository.findByIdIn(
                            withArg {
                                it shouldContainExactlyInAnyOrder setOf(
                                    "01GSEM051HE80DCJ4ETEX6X5VD",
                                    "01GSEKE6HDDM2F25H3FP481DZW",
                                )
                            }
                        )
                    }
                }

                then("조회된 간병 회차 목록을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult shouldContainExactlyInAnyOrder receptionCaregivingRounds
                }
            }

            `when`("존재하지 않는 간병 회차 아이디 목록으로 간병 목록을 조회하면") {
                val query = CaregivingRoundsByIdsQuery(
                    caregivingRoundIds = setOf(
                        "01H0FHGX8KWG6C1PA5NBVSWHH7",
                    ),
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = caregivingRoundService.getCaregivingRounds(query)

                then("CaregivingRoundNotFoundByIdException이 발생합니다.") {
                    val thrownException = shouldThrow<CaregivingRoundNotFoundByIdException> { behavior() }

                    thrownException.caregivingRoundId shouldBe "01H0FHGX8KWG6C1PA5NBVSWHH7"
                }
            }
        }

        fun generateCaregivingRoundByIdQuery(subject: Subject = generateInternalCaregivingManagerSubject()) =
            CaregivingRoundByIdQuery(
                caregivingRoundId = "01GSEM051HE80DCJ4ETEX6X5VD",
                subject = subject
            )

        fun generateCaregivingRoundEditingCommand(
            subject: Subject = generateInternalCaregivingManagerSubject(),
            caregivingProgressingStatus: Patch<CaregivingProgressingStatus>,
            startDateTime: LocalDateTime? = LocalDateTime.of(2023, 2, 17, 14, 0, 0),
            endDateTime: LocalDateTime?,
            caregivingRoundClosingReasonType: ClosingReasonType?,
            caregivingRoundClosingReasonDetail: String?,
            remarks: String = "1회차 해보고 만족해서 계속한다고 함.",
        ) = CaregivingRoundEditingCommand(
            caregiverInfo = CaregiverInfo(
                caregiverOrganizationId = null,
                name = "정만길",
                sex = Sex.MALE,
                phoneNumber = "01012341234",
                birthDate = LocalDate.of(1964, 2, 14),
                insured = true,
                dailyCaregivingCharge = 150000,
                commissionFee = 3000,
                accountInfo = AccountInfo(
                    bank = "국민은행",
                    accountNumber = "110-110-1111111",
                    accountHolder = "정만길",
                )
            ),
            startDateTime = startDateTime,
            caregivingProgressingStatus = caregivingProgressingStatus,
            caregivingRoundClosingReasonType = caregivingRoundClosingReasonType,
            caregivingRoundClosingReasonDetail = caregivingRoundClosingReasonDetail,
            endDateTime = endDateTime,
            remarks = remarks,
            subject = subject
        )

        and("간병 회차가 주어졌을때") {
            val caregivingRounds = listOf<CaregivingRound>(relaxedMock(), relaxedMock())

            beforeEach {
                with(caregivingRoundRepository) {
                    every { findByIdOrNull("01GSEM051HE80DCJ4ETEX6X5VD") } returns caregivingRounds[0]
                }
            }

            afterEach { clearAllMocks() }

            `when`("단일 간병 회차를 조회하면") {
                val query = generateCaregivingRoundByIdQuery()

                fun behavior() = caregivingRoundService.getCaregivingRound(query)

                then("간병 회차가 조회된다.") {
                    val actualResult = behavior()

                    actualResult shouldBe caregivingRounds[0]
                }
            }

            `when`("시스템 사용자로 단일 간병 회차를 조회하면") {
                val query = generateCaregivingRoundByIdQuery(subject = SystemUser)

                fun behavior() = caregivingRoundService.getCaregivingRound(query)

                then("아무 문제없이 응답한다.") {
                    shouldNotThrowAny { behavior() }
                }
            }

            `when`("시작 일자를 수정하면") {
                val query = generateCaregivingRoundByIdQuery()
                val command = generateCaregivingRoundEditingCommand(
                    caregivingProgressingStatus = OverwritePatch(CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS),
                    startDateTime = LocalDateTime.of(2023, 2, 17, 14, 0, 0),
                    endDateTime = null,
                    caregivingRoundClosingReasonType = null,
                    caregivingRoundClosingReasonDetail = null,
                )

                fun behavior() = caregivingRoundService.editCaregivingRound(query, command)

                then("간병 회차가 수정된다.") {
                    behavior()

                    verify {
                        caregivingRounds[0].editCaregivingStartDateTime(
                            LocalDateTime.of(2023, 2, 17, 14, 0, 0),
                            command.subject
                        )
                    }
                }
            }

            `when`("진행중 상태에서 간병을 중단하면") {
                val query = generateCaregivingRoundByIdQuery()
                val command = generateCaregivingRoundEditingCommand(
                    caregivingProgressingStatus = OverwritePatch(CaregivingProgressingStatus.COMPLETED_RESTARTING),
                    startDateTime = LocalDateTime.of(2023, 2, 17, 14, 0, 0),
                    endDateTime = LocalDateTime.of(2023, 2, 25, 14, 0, 0),
                    caregivingRoundClosingReasonType = null,
                    caregivingRoundClosingReasonDetail = null,
                )

                fun behavior() = caregivingRoundService.editCaregivingRound(query, command)

                then("간병 회차가 수정된다.") {
                    behavior()

                    verify {
                        caregivingRounds[0].stop(
                            LocalDateTime.of(2023, 2, 25, 14, 0, 0),
                            command.subject
                        )
                    }
                }
            }

            `when`("취소 정보를 수정하면") {
                val query = generateCaregivingRoundByIdQuery()
                val command = generateCaregivingRoundEditingCommand(
                    caregivingProgressingStatus = OverwritePatch(CaregivingProgressingStatus.CANCELED_WHILE_REMATCHING),
                    startDateTime = null,
                    endDateTime = null,
                    caregivingRoundClosingReasonType = ClosingReasonType.CANCELED_WHILE_REMATCHING,
                    caregivingRoundClosingReasonDetail = "리매칭 중 취소 처리를 합니다. ",
                )

                fun behavior() = caregivingRoundService.editCaregivingRound(query, command)

                then("간병 회차를 취소합니다.") {
                    behavior()

                    verify {
                        caregivingRounds[0].cancel(
                            CancellationReason.CANCELED_WHILE_REMATCHING,
                            "리매칭 중 취소 처리를 합니다. ",
                            command.subject
                        )
                    }
                }
            }

            `when`("정상 종료 정보로 수정하면") {
                val query = generateCaregivingRoundByIdQuery()
                val command = generateCaregivingRoundEditingCommand(
                    caregivingProgressingStatus = OverwritePatch(CaregivingProgressingStatus.COMPLETED),
                    startDateTime = LocalDateTime.of(2023, 2, 17, 14, 0, 0),
                    endDateTime = LocalDateTime.of(2023, 2, 22, 14, 0, 0),
                    caregivingRoundClosingReasonType = ClosingReasonType.FINISHED,
                    caregivingRoundClosingReasonDetail = null,
                    remarks = "해당 간병 회차를 정상 종료 처리합니다. ",
                )

                fun behavior() = caregivingRoundService.editCaregivingRound(query, command)

                then("간병 회차를 종료합니다.") {
                    behavior()

                    verify {
                        caregivingRounds[0].finish(
                            LocalDateTime.of(2023, 2, 22, 14, 0, 0),
                            FinishingReason.FINISHED,
                            command.subject
                        )
                    }
                }
            }

            `when`("개인구인 이용으로 인한 종료로 수정하면") {
                val query = generateCaregivingRoundByIdQuery()
                val command = generateCaregivingRoundEditingCommand(
                    caregivingProgressingStatus = OverwritePatch(CaregivingProgressingStatus.COMPLETED_USING_PERSONAL_CAREGIVER),
                    startDateTime = LocalDateTime.of(2023, 2, 17, 14, 0, 0),
                    endDateTime = LocalDateTime.of(2023, 2, 20, 10, 0, 0),
                    caregivingRoundClosingReasonType = ClosingReasonType.FINISHED_USING_PERSONAL_CAREGIVER,
                    caregivingRoundClosingReasonDetail = null,
                    remarks = "해당 간병 회차를 개인구인 이용으로 종료 처리합니다. ",
                )

                beforeEach {
                    with(caregivingRounds[0]) {
                        every { caregivingRoundClosingReasonType } returns null
                    }
                }

                afterEach { clearAllMocks() }

                fun behavior() = caregivingRoundService.editCaregivingRound(query, command)

                then("간병 회차를 종료합니다.") {
                    behavior()

                    verify {
                        caregivingRounds[0].finish(
                            LocalDateTime.of(2023, 2, 20, 10, 0, 0),
                            FinishingReason.FINISHED_USING_PERSONAL_CAREGIVER,
                            any()
                        )
                    }
                }
            }

            `when`("중단 계속 상태에서 중단 계속 사유로 간병을 종료하면") {
                val query = generateCaregivingRoundByIdQuery()
                val command = generateCaregivingRoundEditingCommand(
                    caregivingProgressingStatus = OverwritePatch(CaregivingProgressingStatus.COMPLETED),
                    startDateTime = LocalDateTime.of(2023, 2, 17, 14, 0, 0),
                    endDateTime = LocalDateTime.of(2023, 2, 22, 14, 0, 0),
                    caregivingRoundClosingReasonType = ClosingReasonType.FINISHED_CHANGING_CAREGIVER,
                    caregivingRoundClosingReasonDetail = null,
                    remarks = "해당 간병 회차를 중단 계속 상태에서 중단 계속 사유(간병인 교체, 병원 교체 등)로 종료 처리합니다. ",
                )
                val newCaregivingRound = relaxedMock<CaregivingRound>()

                beforeEach {
                    every {
                        caregivingRounds[0].finish(
                            any(),
                            FinishingReason.FINISHED_CHANGING_CAREGIVER,
                            any()
                        )
                    } returns CaregivingRound.FinishingResult(newCaregivingRound)

                    with(caregivingRoundRepository) {
                        every { save(any()) } returns newCaregivingRound
                    }
                }

                afterEach { clearAllMocks() }

                fun behavior() = caregivingRoundService.editCaregivingRound(query, command)

                then("간병을 종료한다.") {
                    behavior()

                    verify {
                        caregivingRounds[0].finish(
                            LocalDateTime.of(2023, 2, 22, 14, 0, 0),
                            FinishingReason.FINISHED_CHANGING_CAREGIVER,
                            any()
                        )
                    }
                }

                then("다음 회차가 자동 생성된다.") {
                    behavior()

                    caregivingRoundRepository.save(newCaregivingRound)
                }
            }

            `when`("진행중 상태에서 계속건 추가로 간병을 종료하면") {
                val query = generateCaregivingRoundByIdQuery()
                val command = generateCaregivingRoundEditingCommand(
                    caregivingProgressingStatus = OverwritePatch(CaregivingProgressingStatus.COMPLETED),
                    startDateTime = LocalDateTime.of(2023, 2, 17, 14, 0, 0),
                    endDateTime = LocalDateTime.of(2023, 2, 22, 14, 0, 0),
                    caregivingRoundClosingReasonType = ClosingReasonType.FINISHED_CONTINUE,
                    caregivingRoundClosingReasonDetail = null,
                    remarks = "해당 간병 회차를 행중인 상태에서 계속건 추가로 종료 처리합니다. ",
                )
                val newCaregivingRound = relaxedMock<CaregivingRound>()

                beforeEach {
                    every {
                        caregivingRounds[0].finish(
                            any(),
                            FinishingReason.FINISHED_CONTINUE,
                            any()
                        )
                    } returns CaregivingRound.FinishingResult(newCaregivingRound)

                    with(caregivingRoundRepository) {
                        every { save(any()) } returns newCaregivingRound
                    }
                }

                afterEach { clearAllMocks() }

                fun behavior() = caregivingRoundService.editCaregivingRound(query, command)

                then("간병을 종료한다.") {
                    behavior()

                    verify {
                        caregivingRounds[0].finish(
                            LocalDateTime.of(2023, 2, 22, 14, 0, 0),
                            FinishingReason.FINISHED_CONTINUE,
                            any()
                        )
                    }
                }

                then("다음 회차가 자동 생성된다.") {
                    behavior()

                    caregivingRoundRepository.save(newCaregivingRound)
                }
            }

            `when`("중단계속 상태에서 계속건 추가 사유로 종료하면") {
                val query = generateCaregivingRoundByIdQuery()
                val command = generateCaregivingRoundEditingCommand(
                    caregivingProgressingStatus = OverwritePatch(CaregivingProgressingStatus.COMPLETED),
                    startDateTime = LocalDateTime.of(2023, 2, 17, 14, 0, 0),
                    endDateTime = LocalDateTime.of(2023, 2, 22, 14, 0, 0),
                    caregivingRoundClosingReasonType = ClosingReasonType.FINISHED_RESTARTING,
                    caregivingRoundClosingReasonDetail = null,
                    remarks = "해당 간병 회차를 중단계속 상태에서 계속건 추가 사유로 종료 처리합니다. ",
                )
                val generatedNextRound = relaxedMock<CaregivingRound>()

                beforeEach {
                    val newCaregivingRound = mockk<CaregivingRound>(relaxed = true)

                    with(caregivingRoundRepository) {
                        every { save(any()) } returns newCaregivingRound
                    }

                    every {
                        caregivingRounds[0].finish(
                            any(),
                            FinishingReason.FINISHED_RESTARTING,
                            any()
                        )
                    } returns relaxedMock {
                        every { nextRound } returns generatedNextRound
                    }
                }

                afterEach { clearAllMocks() }

                fun behavior() = caregivingRoundService.editCaregivingRound(query, command)

                then("간병 회차를 종료합니다.") {
                    behavior()

                    verify {
                        caregivingRounds[0].finish(
                            LocalDateTime.of(2023, 2, 22, 14, 0, 0),
                            FinishingReason.FINISHED_RESTARTING,
                            command.subject
                        )
                    }
                }

                then("다음 간병 회차를 저장합니다.") {
                    behavior()

                    verify {
                        caregivingRoundRepository.save(generatedNextRound)
                    }
                }
            }

            `when`("청구의 진행 상태가 변경되었을때 간병 회차가 변경사항에 영향을 받는다면") {
                val event = relaxedMock<BillingModified>()

                beforeEach {
                    with(event) {
                        every { caregivingRoundId } returns "01GSEM051HE80DCJ4ETEX6X5VD"
                        every { progressingStatus } returns Modification(
                            BillingProgressingStatus.NOT_STARTED,
                            BillingProgressingStatus.WAITING_FOR_BILLING,
                        )
                    }
                    every { caregivingRounds[0].willBeAffectedBy(event) } returns true
                }

                afterEach { clearAllMocks() }

                fun handling() = caregivingRoundService.handleBillingModified(event)

                then("청구 상태 변경을 간병 회차에 알립니다.") {
                    handling()

                    verify {
                        caregivingRounds[0].handleBillingModified(event)
                    }
                }
            }

            `when`("청구가 변경되었지만 간병 회차가 변경사항에 영향을 받지 않는다면") {
                val event = relaxedMock<BillingModified>()

                beforeEach {
                    with(event) {
                        every { caregivingRoundId } returns "01GSEM051HE80DCJ4ETEX6X5VD"
                        every { progressingStatus } returns Modification(
                            BillingProgressingStatus.NOT_STARTED,
                            BillingProgressingStatus.NOT_STARTED,
                        )
                    }
                    every { caregivingRounds[0].willBeAffectedBy(event) } returns false
                }

                afterEach { clearAllMocks() }

                fun handling() = caregivingRoundService.handleBillingModified(event)

                then("청구 상태 변경을 간병 회차에 알리지 않습니다.") {
                    handling()

                    verify(exactly = 0) {
                        caregivingRounds[0].handleBillingModified(event)
                    }
                }
            }

            `when`("정산의 진행 상태가 변경되었을때 간병 회차가 변경사항에 영향을 받는다면") {
                val event = relaxedMock<SettlementModified>()

                beforeEach {
                    with(event) {
                        every { caregivingRoundId } returns "01GSEM051HE80DCJ4ETEX6X5VD"
                        every { progressingStatus } returns Modification(
                            SettlementProgressingStatus.WAITING,
                            SettlementProgressingStatus.COMPLETED,
                        )
                    }
                    every { caregivingRounds[0].willBeAffectedBy(event) } returns true
                }

                afterEach { clearAllMocks() }

                fun handling() = caregivingRoundService.handleSettlementModified(event)

                then("정산 상태 변경을 간병 회차에 알립니다.") {
                    handling()

                    verify {
                        caregivingRounds[0].handleSettlementModified(event)
                    }
                }
            }

            `when`("정산의 진행 상태가 변경되었을때 간병 회차가 변경사항에 영향을 받지 않는다면") {
                val event = relaxedMock<SettlementModified>()

                beforeEach {
                    with(event) {
                        every { caregivingRoundId } returns "01GSEM051HE80DCJ4ETEX6X5VD"
                        every { progressingStatus } returns Modification(
                            SettlementProgressingStatus.WAITING,
                            SettlementProgressingStatus.WAITING,
                        )
                    }
                    every { caregivingRounds[0].willBeAffectedBy(event) } returns false
                }

                afterEach { clearAllMocks() }

                fun handling() = caregivingRoundService.handleSettlementModified(event)

                then("정산 상태 변경을 간병 회차에 알리지 않습니다.") {
                    handling()

                    verify(exactly = 0) {
                        caregivingRounds[0].handleSettlementModified(event)
                    }
                }
            }

            `when`("정산이 생성 되었을때") {
                val event = relaxedMock<SettlementGenerated>()

                beforeEach {
                    with(event) {
                        every { caregivingRoundId } returns "01GSEM051HE80DCJ4ETEX6X5VD"
                        every { progressingStatus } returns SettlementProgressingStatus.CONFIRMED
                    }
                }

                afterEach { clearAllMocks() }

                fun handling() = caregivingRoundService.handleSettlementGenerated(event)

                then("정산생성을 간병 회차에 알립니다.") {
                    handling()

                    verify {
                        caregivingRounds[0].handleSettlementGenerated(event)
                    }
                }
            }

            `when`("청구가 생성 되었을때") {
                val event = relaxedMock<BillingGenerated>()

                beforeEach {
                    with(event) {
                        every { caregivingRoundId } returns "01GSEM051HE80DCJ4ETEX6X5VD"
                        every { progressingStatus } returns BillingProgressingStatus.WAITING_FOR_BILLING
                    }
                }

                afterEach { clearAllMocks() }

                fun handling() = caregivingRoundService.handleBillingGenerated(event)

                then("청구 생성을 간병 회차에 알립니다.") {
                    handling()

                    verify {
                        caregivingRounds[0].handleBillingGenerated(event)
                    }
                }
            }

            `when`("대사의 간병회차가 월마감 처리 되었을 때") {
                val event = relaxedMock<ReconciliationClosed>()

                beforeEach {
                    with(event) {
                        every { reconciliationId } returns "01H2T20NTXM528366MGGGK0ZZH"
                        every { caregivingRoundId } returns "01GSEM051HE80DCJ4ETEX6X5VD"
                        every { receptionId } returns "01H2WFS2WQQPFR15T8PXP8NHED"
                        every { issuedType } returns IssuedType.FINISH
                        every { subject } returns generateInternalCaregivingManagerSubject()
                    }
                    with(caregivingRoundRepository) {
                        every { findByIdOrNull("01GSEM051HE80DCJ4ETEX6X5VD") } returns caregivingRounds[0]

                        every {
                            findByReceptionInfoReceptionIdOrderByCaregivingRoundNumberDesc(match { it == "01H2WFS2WQQPFR15T8PXP8NHED" })
                        } returns listOf(
                            relaxedMock {
                                every { id } returns "01GSEM051HE80DCJ4ETEX6X5VD"
                                every { receptionInfo.receptionId } returns "01H2WFS2WQQPFR15T8PXP8NHED"
                                every { caregivingProgressingStatus } returns CaregivingProgressingStatus.RECONCILIATION_COMPLETED
                            },
                            relaxedMock {
                                every { id } returns "01H2WFH9HH54NJYFGV0DBK9GHB"
                                every { receptionInfo.receptionId } returns "01H2WFS2WQQPFR15T8PXP8NHED"
                                every { caregivingProgressingStatus } returns CaregivingProgressingStatus.CANCELED_WHILE_REMATCHING
                            },
                            relaxedMock {
                                every { id } returns "01H2WFK76BTEK3GV4VK0ZW9AFJ"
                                every { receptionInfo.receptionId } returns "01H2WFS2WQQPFR15T8PXP8NHED"
                                every { caregivingProgressingStatus } returns CaregivingProgressingStatus.RECONCILIATION_COMPLETED
                            }
                        )
                    }
                }

                afterEach { clearAllMocks() }

                fun handling() = caregivingRoundService.handleReconciliationClosed(event)

                then("대사 완료 되었음을 간병 회차에 알립니다.") {
                    handling()

                    verify {
                        caregivingRounds[0].handleReconciliationClosed(event)
                    }
                }

                then("모든 간병회차의 상태가 대사완료(리매칭중 취소 포함) 이벤트를 게시합니다.") {
                    handling()

                    verify {
                        eventPublisher.publishEvent(
                            withArg<AllCaregivingRoundReconciliationCompleted> {
                                it.receptionId shouldBe "01H2WFS2WQQPFR15T8PXP8NHED"
                            }
                        )
                    }
                }
            }

            `when`("대사 발생 구분이 종료가 아닌 간병회차가 월마감 처리 되었을 때") {
                val event = relaxedMock<ReconciliationClosed>()

                beforeEach {
                    with(event) {
                        every { reconciliationId } returns "01H2T20NTXM528366MGGGK0ZZH"
                        every { caregivingRoundId } returns "01GSEM051HE80DCJ4ETEX6X5VD"
                        every { issuedType } returns IssuedType.TRANSACTION
                    }
                }

                afterEach { clearAllMocks() }

                fun handling() = caregivingRoundService.handleReconciliationClosed(event)

                then("아무것도 안합니다.") {
                    handling()

                    verify(exactly = 0) {
                        caregivingRoundRepository.findByIdOrNull(any())
                    }

                    verify(exactly = 0) {
                        caregivingRoundRepository.findByReceptionInfoReceptionIdOrderByCaregivingRoundNumberDesc(any())
                    }
                }
            }
        }

        fun generateCaregivingChargeEditingCommand(subject: Subject = generateInternalCaregivingManagerSubject()) =
            CaregivingChargeEditingCommand(
                additionalHoursCharge = 10000,
                mealCost = 0,
                transportationFee = 0,
                holidayCharge = 0,
                caregiverInsuranceFee = 0,
                commissionFee = -5000,
                vacationCharge = 0,
                patientConditionCharge = 50000,
                covid19TestingCost = 4500,
                outstandingAmount = 0,
                additionalCharges = listOf(
                    CaregivingCharge.AdditionalCharge(
                        name = "특별 보상비",
                        amount = 5000,
                    ),
                    CaregivingCharge.AdditionalCharge(
                        name = "고객 보상비",
                        amount = -10000,
                    ),
                ),
                isCancelAfterArrived = true,
                expectedSettlementDate = LocalDate.of(2023, 4, 17),
                caregivingChargeConfirmStatus = CaregivingChargeConfirmStatus.NOT_STARTED,
                subject = subject
            )

        and("간병비가 아직 산정되지 않았을때") {
            val caregivingRounds = listOf<CaregivingRound>(relaxedMock(), relaxedMock())

            beforeEach {
                with(caregivingChargeRepository) {
                    every { findByCaregivingRoundInfoCaregivingRoundId("01GSEM051HE80DCJ4ETEX6X5VD") } returns null
                }
                with(caregivingRoundRepository) {
                    every { findByIdOrNull("01GSEM051HE80DCJ4ETEX6X5VD") } returns caregivingRounds[0]
                }
            }

            afterEach { clearAllMocks() }

            `when`("산정된 간병비를 조회하면") {
                val query = CaregivingChargeByCaregivingRoundIdQuery(
                    caregivingRoundId = "01GSEM051HE80DCJ4ETEX6X5VD",
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = caregivingRoundService.getCaregivingCharge(query)

                then("CaregivingChargeNotEnteredException이 발생합니다.") {
                    val thrownException = shouldThrow<CaregivingChargeNotEnteredException> { behavior() }
                    thrownException.enteredCaregivingRoundId shouldBe "01GSEM051HE80DCJ4ETEX6X5VD"
                }
            }
        }

        and("간병비 산정을 위한 인자들이 주어졌을 때") {
            val caregivingCharges = listOf<CaregivingCharge>(relaxedMock(), relaxedMock())
            val caregivingRounds = listOf<CaregivingRound>(relaxedMock(), relaxedMock())

            beforeEach {
                with(caregivingChargeRepository) {
                    every { findByCaregivingRoundInfoCaregivingRoundId("01GSEM051HE80DCJ4ETEX6X5VD") } returns null
                    every { findByIdOrNull("01GV002Y0AE5GS8GSKBHA9Q922") } returns caregivingCharges[0]
                    every { save(any()) } returns caregivingCharges[1]
                }
                with(caregivingRoundRepository) {
                    every { findByIdOrNull("01GSEM051HE80DCJ4ETEX6X5VD") } returns caregivingRounds[0]
                }
                with(caregivingRounds[0]) {
                    every { caregivingRounds[0].caregivingProgressingStatus } returns CaregivingProgressingStatus.COMPLETED
                }
            }

            afterEach { clearAllMocks() }

            `when`("내부 사용자가 간병비 산정을 하면") {
                val query = generateCaregivingRoundByIdQuery()
                val command = generateCaregivingChargeEditingCommand()

                fun behavior() = caregivingRoundService.createOrEditCaregivingCharge(query, command)

                then("간병비 산정은 생성된다.") {
                    val actualResult = behavior()
                    actualResult shouldBe Unit
                }
            }

            `when`("간병비 산정 처리가 불가능한 상태로 간병비 산정을 하면") {
                beforeEach {
                    with(caregivingRounds[0]) {
                        every { caregivingRounds[0].caregivingProgressingStatus } returns CaregivingProgressingStatus.COMPLETED_RESTARTING
                    }
                }

                afterEach { clearAllMocks() }

                val query = generateCaregivingRoundByIdQuery()
                val command = generateCaregivingChargeEditingCommand()

                fun behavior() = caregivingRoundService.createOrEditCaregivingCharge(query, command)

                then("InvalidCaregivingChargeActionableStatusException 발생 한다.") {
                    shouldThrow<InvalidCaregivingChargeActionableStatusException> { behavior() }
                }
            }

            `when`("간병 회차의 정보가 없는 상태로 간병비 산정을 하면") {
                beforeEach {
                    with(caregivingRounds[0]) {
                        every { caregivingRounds[0].id } returns "01GSEM051HE80DCJ4ETEX6X5VD"
                        every { caregivingRounds[0].startDateTime } returns null
                        every { caregivingRounds[0].endDateTime } returns null
                        every { caregivingRounds[0].caregiverInfo } returns null
                    }
                }

                afterEach { clearAllMocks() }

                val query = generateCaregivingRoundByIdQuery()
                val command = generateCaregivingChargeEditingCommand()

                fun behavior() = caregivingRoundService.createOrEditCaregivingCharge(query, command)

                then("UnknownCaregivingRoundInfoException 발생 한다.") {
                    shouldThrow<UnknownCaregivingRoundInfoException> { behavior() }
                }
            }
        }

        and("간병비가 이미 산정되어 있을 때") {
            val caregivingCharges = listOf<CaregivingCharge>(relaxedMock(), relaxedMock())
            val caregivingRounds = listOf<CaregivingRound>(relaxedMock(), relaxedMock())

            beforeEach {
                with(caregivingChargeRepository) {
                    every { findByCaregivingRoundInfoCaregivingRoundId("01GSEM051HE80DCJ4ETEX6X5VD") } returns caregivingCharges[0]
                }

                with(caregivingCharges[0]) {
                    every { caregivingCharges[0].caregivingChargeConfirmStatus } returns CaregivingChargeConfirmStatus.NOT_STARTED
                }
                with(caregivingRoundRepository) {
                    every { findByIdOrNull("01GSEM051HE80DCJ4ETEX6X5VD") } returns caregivingRounds[0]
                }
            }

            afterEach { clearAllMocks() }

            `when`("단일 산정된 간병비를 조회하면") {
                val query = CaregivingChargeByCaregivingRoundIdQuery(
                    caregivingRoundId = "01GSEM051HE80DCJ4ETEX6X5VD",
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = caregivingRoundService.getCaregivingCharge(query)

                then("산정된 간병비가 조회된다.") {
                    val actualResult = behavior()

                    actualResult shouldBe caregivingCharges[0]
                }
            }

            `when`("내부 사용자가 간병비 산정을 수정하면") {
                val query = generateCaregivingRoundByIdQuery()
                val command = generateCaregivingChargeEditingCommand()

                fun behavior() = caregivingRoundService.createOrEditCaregivingCharge(query, command)

                then("간병비 산정은 수정된다.") {
                    behavior()

                    verify {
                        caregivingCharges[0].edit(
                            withArg {
                                it.additionalHoursCharge shouldBe 10000
                                it.mealCost shouldBe 0
                                it.transportationFee shouldBe 0
                                it.holidayCharge shouldBe 0
                                it.caregiverInsuranceFee shouldBe 0
                                it.commissionFee shouldBe -5000
                                it.vacationCharge shouldBe 0
                                it.patientConditionCharge shouldBe 50000
                                it.covid19TestingCost shouldBe 4500
                                it.outstandingAmount shouldBe 0
                                it.additionalCharges shouldBe listOf(
                                    CaregivingCharge.AdditionalCharge(
                                        name = "특별 보상비",
                                        amount = 5000,
                                    ),
                                    CaregivingCharge.AdditionalCharge(
                                        name = "고객 보상비",
                                        amount = -10000,
                                    ),
                                )
                                it.isCancelAfterArrived shouldBe true
                                it.expectedSettlementDate shouldBe LocalDate.of(2023, 4, 17)
                                it.caregivingChargeConfirmStatus shouldBe CaregivingChargeConfirmStatus.NOT_STARTED
                            }
                        )
                    }
                }
            }

            `when`("간병 회차 정보 없이 간병비 산정을 수정하면") {
                beforeEach {
                    with(caregivingRoundRepository) {
                        every { findByIdOrNull("01GSEM051HE80DCJ4ETEX6X5VD") } returns null
                    }
                }

                afterEach { clearAllMocks() }

                val query = generateCaregivingRoundByIdQuery()
                val command = generateCaregivingChargeEditingCommand()

                fun behavior() = caregivingRoundService.createOrEditCaregivingCharge(query, command)

                then("CaregivingRoundNotFoundByIdException 발생 한다.") {
                    shouldThrow<CaregivingRoundNotFoundByIdException> { behavior() }
                }
            }

            `when`("간병비 산정이 확정된 상태에서 간병비 산정을 수정하면") {
                beforeEach {
                    with(caregivingCharges[0]) {
                        every { caregivingCharges[0].caregivingChargeConfirmStatus } returns CaregivingChargeConfirmStatus.CONFIRMED
                    }
                }

                afterEach { clearAllMocks() }

                val query = generateCaregivingRoundByIdQuery()
                val command = generateCaregivingChargeEditingCommand()

                fun behavior() = caregivingRoundService.createOrEditCaregivingCharge(query, command)

                then("CaregivingChargeEditingDeniedException 발생 한다.") {
                    shouldThrow<CaregivingChargeEditingDeniedException> { behavior() }
                }
            }
        }

        and("엔티티 테스트할 때") {
            beforeEach {
                simpleApplicationEventMulticaster.removeAllListeners()
            }

            afterEach {
                clearAllMocks()
            }

            val id = "01HA97EVW7ZR6H3G92427MMGNJ"
            `when`("저장을 요청하면") {
                val caregivingRound = CaregivingRound(
                    id = "01GVATYRYEPXMNDP9MBC540T1E",
                    caregivingRoundNumber = 1,
                    receptionInfo = CaregivingRound.ReceptionInfo(
                        receptionId = "01GVAV4T8AP2SWE71SXZMWB1Z9",
                        insuranceNumber = "12345-12345",
                        accidentNumber = "2023-1234567",
                        maskedPatientName = "김*자",
                        receptionProgressingStatus = ReceptionProgressingStatus.RECEIVED,
                        expectedCaregivingStartDate = null,
                        caregivingManagerInfo = CaregivingManagerInfo(
                            organizationType = OrganizationType.INTERNAL,
                            organizationId = null,
                            managingUserId = "01GQ23MVTBAKS526S0WGS9CS0A"
                        )
                    ),
                )

                fun behavior() = cacheCaregivingRoundRepository.save(caregivingRound)
                then("저장이 됩니다.") {
                    behavior()
                }
            }

            `when`("조회를 요청하면") {
                fun behavior() = cacheCaregivingRoundRepository.findById(id)
                then("조회됩니다.") {
                    behavior()
                }
            }
        }
    }
})
