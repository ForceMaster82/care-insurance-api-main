package kr.caredoc.careinsurance.billing

import com.github.guepardoapps.kulid.ULID
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import kr.caredoc.careinsurance.caregiving.CaregivingChargeCalculated
import kr.caredoc.careinsurance.caregiving.CaregivingChargeConfirmStatus
import kr.caredoc.careinsurance.caregiving.CaregivingChargeModified
import kr.caredoc.careinsurance.caregiving.CaregivingRoundByIdQueryHandler
import kr.caredoc.careinsurance.caregiving.CaregivingRoundModified
import kr.caredoc.careinsurance.caregiving.certificate.CertificateByCaregivingRoundIdQueryHandler
import kr.caredoc.careinsurance.coverage.Coverage
import kr.caredoc.careinsurance.coverage.CoverageByIdQueryHandler
import kr.caredoc.careinsurance.generateExternalCaregivingOrganizationManagerSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.generateSystemSubject
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.reception.ReceptionByIdQueryHandler
import kr.caredoc.careinsurance.reception.ReceptionModified
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.search.SearchCondition
import kr.caredoc.careinsurance.security.accesscontrol.ObjectAttribute
import kr.caredoc.careinsurance.settlement.SettlementProgressingStatus
import kr.caredoc.careinsurance.transaction.TransactionType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.AccessDeniedException
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
class BillingServiceTest(
    @Autowired
    private val cacheBillingRepository: BillingRepository,
) : BehaviorSpec({
    given("청구 서비스가 주어졌을 때") {
        val billingRepository = relaxedMock<BillingRepository>()
        val caregivingRoundByIdQueryHandler = relaxedMock<CaregivingRoundByIdQueryHandler>()
        val receptionByIdQueryHandler = relaxedMock<ReceptionByIdQueryHandler>()
        val coverageByIdQueryHandler = relaxedMock<CoverageByIdQueryHandler>()
        val certificateByCaregivingRoundIdQueryHandler = relaxedMock<CertificateByCaregivingRoundIdQueryHandler>()
        val billingService = BillingService(
            billingRepository = billingRepository,
            caregivingRoundByIdQueryHandler = caregivingRoundByIdQueryHandler,
            receptionByIdQueryHandler = receptionByIdQueryHandler,
            coverageByIdQueryHandler = coverageByIdQueryHandler,
            certificateByCaregivingRoundIdQueryHandler = certificateByCaregivingRoundIdQueryHandler,
        )

        beforeEach {
            every {
                billingRepository.findTopByCaregivingRoundInfoCaregivingRoundId(any())
            } returns null
        }

        afterEach { clearAllMocks() }

        `when`("간병비 산정이 감지되면") {
            val event = relaxedMock<CaregivingChargeCalculated>()
            fun handling() = billingService.handleCaregivingChargeCalculated(event)

            beforeEach {
                mockkObject(ULID)
                every { ULID.random() } returns "01GVQ925A0DZ0CCDSAYDBF4HD5"

                val billingSlot = slot<Billing>()
                with(billingRepository) {
                    every { save(capture(billingSlot)) } answers {
                        billingSlot.captured
                    }
                }

                with(event) {
                    every { receptionId } returns "01GVQ7A3RSQQ6J36EB2SFMS82P"
                    every { caregivingRoundId } returns "01GVQ7DT0NGQ7CBF1PA6TXWQ6C"
                    every { roundNumber } returns 3
                    every { isCancelAfterArrived } returns true
                }

                every { caregivingRoundByIdQueryHandler.getCaregivingRound(match { it.caregivingRoundId == "01GVQ7DT0NGQ7CBF1PA6TXWQ6C" }) } returns relaxedMock {
                    every { id } returns "01GVQ7DT0NGQ7CBF1PA6TXWQ6C"
                    every { startDateTime } returns LocalDateTime.of(2023, 3, 16, 9, 21, 31)
                    every { endDateTime } returns LocalDateTime.of(2023, 3, 17, 9, 21, 31)
                    every { receptionInfo.accidentNumber } returns "2023-1111111"
                }

                every { receptionByIdQueryHandler.getReception(match { it.receptionId == "01GVQ7A3RSQQ6J36EB2SFMS82P" }) } returns relaxedMock {
                    every { insuranceInfo.subscriptionDate } returns LocalDate.of(2022, 1, 17)
                    every { insuranceInfo.coverageId } returns "01GX8EP2CPAT4A9FMWM3RWV0WW"
                }

                every {
                    coverageByIdQueryHandler.getCoverage(
                        match { it.coverageId == "01GX8EP2CPAT4A9FMWM3RWV0WW" },
                        any<(Coverage) -> CoverageInfo>()
                    )
                } returns relaxedMock {
                    every { targetSubscriptionYear } returns 2023
                    every { renewalType } returns CoverageInfo.RenewalType.THREE_YEAR
                    every { annualCoveredCaregivingCharges } returns listOf(
                        relaxedMock {
                            every { targetAccidentYear } returns 2023
                            every { caregivingCharge } returns 100000
                        }
                    )
                }
            }

            afterEach {
                clearAllMocks()
            }

            then("청구 생성에 필요한 데이터인 caregivingRound 를 요청합니다.") {
                handling()

                verify {
                    caregivingRoundByIdQueryHandler.getCaregivingRound(match { it.caregivingRoundId == "01GVQ7DT0NGQ7CBF1PA6TXWQ6C" })
                }
            }

            then("청구를 생성합니다.") {
                handling()

                verify {
                    billingRepository.save(
                        withArg {
                            it.id shouldBe "01GVQ925A0DZ0CCDSAYDBF4HD5"
                            it.receptionInfo.receptionId shouldBe "01GVQ7A3RSQQ6J36EB2SFMS82P"
                            it.receptionInfo.accidentNumber shouldBe "2023-1111111"
                            it.receptionInfo.subscriptionDate shouldBe LocalDate.of(2022, 1, 17)
                            it.caregivingRoundInfo.caregivingRoundId shouldBe "01GVQ7DT0NGQ7CBF1PA6TXWQ6C"
                            it.caregivingRoundInfo.roundNumber shouldBe 3
                            it.caregivingRoundInfo.startDateTime shouldBe LocalDateTime.of(2023, 3, 16, 9, 21, 31)
                            it.caregivingRoundInfo.endDateTime shouldBe LocalDateTime.of(2023, 3, 17, 9, 21, 31)
                            it.billingProgressingStatus shouldBe BillingProgressingStatus.WAITING_FOR_BILLING
                            it.basicAmounts[0].targetAccidentYear shouldBe 2023
                            it.basicAmounts[0].caregivingDays shouldBe 1
                            it.basicAmounts[0].dailyCaregivingCharge shouldBe 100000
                            it.basicAmounts[0].totalAmount shouldBe 100000
                            it.additionalAmount shouldBe 0
                            it.additionalHours shouldBe 0
                            it.isCancelAfterArrived shouldBe true
                        }
                    )
                }
            }
        }

        `when`("청구 대기 상태에서 사용 확인서를 다운로드 받으면") {
            val billingId = "01GW4B47P95QP58254T5TSX4ZY"
            val subject = generateInternalCaregivingManagerSubject()

            val generatedCertificate = byteArrayOf()

            lateinit var billing: Billing
            beforeEach {
                billing = relaxedMock {
                    every { id } returns billingId
                    every { billingProgressingStatus } returns BillingProgressingStatus.WAITING_FOR_BILLING
                    every { caregivingRoundInfo.caregivingRoundId } returns "01GVQ7DT0NGQ7CBF1PA6TXWQ6C"
                }

                val billingSlot = slot<Billing>()
                with(billingRepository) {
                    every { save(capture(billingSlot)) } answers {
                        billingSlot.captured
                    }
                    every { findByIdOrNull(billingId) } returns billing
                }

                every {
                    certificateByCaregivingRoundIdQueryHandler.getCertificate(
                        match {
                            it.caregivingRoundId == "01GVQ7DT0NGQ7CBF1PA6TXWQ6C"
                        }
                    )
                } returns generatedCertificate
            }

            afterEach {
                clearAllMocks()
            }

            fun behavior() = billingService.downloadCertification(
                DownloadCertificateCommand(
                    billingId = billingId,
                    subject = subject,
                )
            )

            then("사용 확인서를 구성하는데 필요한 billing 을 조회합니다.") {
                behavior()

                verify {
                    billingRepository.findByIdOrNull(billingId)
                }
            }

            then("청구를 미수 상태로 수정합니다.") {
                behavior()

                verify(exactly = 1) {
                    billing.waitDeposit()
                }
            }

            then("간병인 사용 확인서 생성을 요청합니다.") {
                behavior()

                verify {
                    certificateByCaregivingRoundIdQueryHandler.getCertificate(
                        withArg {
                            it.caregivingRoundId shouldBe "01GVQ7DT0NGQ7CBF1PA6TXWQ6C"
                        }
                    )
                }
            }

            then("간병인 사용 확인서를 반환합니다.") {
                val actualResult = behavior()

                actualResult shouldBe generatedCertificate
            }
        }

        `when`("청구 대기 상태가 아닌 상태에서 사용 확인서를 다운로드 받으면") {
            val billingId = "01GW4B47P95QP58254T5TSX4ZY"
            val subject = generateInternalCaregivingManagerSubject()

            val generatedCertificate = byteArrayOf()

            lateinit var billing: Billing
            beforeEach {
                billing = relaxedMock {
                    every { id } returns billingId
                    every { billingProgressingStatus } returns BillingProgressingStatus.UNDER_DEPOSIT
                    every { caregivingRoundInfo.caregivingRoundId } returns "01GVQ7DT0NGQ7CBF1PA6TXWQ6C"
                }

                every { billingRepository.findByIdOrNull(billingId) } returns billing

                every {
                    certificateByCaregivingRoundIdQueryHandler.getCertificate(
                        match {
                            it.caregivingRoundId == "01GVQ7DT0NGQ7CBF1PA6TXWQ6C"
                        }
                    )
                } returns generatedCertificate
            }

            afterEach {
                clearAllMocks()
            }

            fun behavior() = billingService.downloadCertification(
                DownloadCertificateCommand(
                    billingId = billingId,
                    subject = subject,
                )
            )

            then("간병인 사용 확인서 생성을 요청합니다.") {
                behavior()

                verify {
                    certificateByCaregivingRoundIdQueryHandler.getCertificate(
                        withArg {
                            it.caregivingRoundId shouldBe "01GVQ7DT0NGQ7CBF1PA6TXWQ6C"
                        }
                    )
                }
            }

            then("간병인 사용 확인서를 반환합니다.") {
                val actualResult = behavior()

                actualResult shouldBe generatedCertificate
            }
        }

        `when`("입/출금 내역을 추가하면") {
            val billingId = "01GXQVV8RJ7WQ8M8V9T0PQ2F3T"
            val subject = generateInternalCaregivingManagerSubject()
            val transactionSubjectId = "01GXQW0NBYMFRBFRKWWCB1VVKV"

            val billing = relaxedMock<Billing>()
            beforeEach {
                val billingSlot = slot<Billing>()
                with(billingRepository) {
                    every { save(capture(billingSlot)) } answers {
                        billingSlot.captured
                    }

                    every { findByIdOrNull(billingId) } returns billing
                }
            }

            afterEach {
                clearAllMocks()
            }

            fun behavior() = billingService.recordTransaction(
                BillingByIdQuery(
                    billingId = billingId,
                    subject = subject,
                ),
                BillingTransactionRecordingCommand(
                    transactionType = TransactionType.WITHDRAWAL,
                    amount = 30000,
                    transactionDate = LocalDate.of(2023, 4, 11),
                    transactionSubjectId = transactionSubjectId,
                    subject = subject,
                )
            )

            then("입/출금 내역이 추가될 대상의 청구를 요청합니다.") {
                behavior()

                verify {
                    billingRepository.findByIdOrNull(billingId)
                }
            }

            then("입/출금 내역을 생성합니다.") {
                behavior()

                verify {
                    billing.recordTransaction(
                        withArg {
                            it.transactionType shouldBe TransactionType.WITHDRAWAL
                            it.amount shouldBe 30000
                            it.transactionDate shouldBe LocalDate.of(2023, 4, 11)
                            it.transactionSubjectId shouldBe transactionSubjectId
                            it.subject shouldBe subject
                        }
                    )
                }
            }
        }

        `when`("간병 회차 아이디로 청구를 조회하면") {
            val query = BillingByCaregivingRoundIdQuery(
                caregivingRoundId = "01GSM0PQ5G8HW2GKYXH3VGGMZG",
                subject = generateInternalCaregivingManagerSubject(),
            )

            fun behavior() = billingService.getBilling(query)

            then("BillingNotFoundByCaregivingRoundIdException이 발생합니다.") {
                val thrownException = shouldThrow<BillingNotFoundByCaregivingRoundIdException> { behavior() }

                thrownException.caregivingRoundId shouldBe "01GSM0PQ5G8HW2GKYXH3VGGMZG"
            }
        }

        and("또한 등록된 청구가 주어졌을때") {
            val billing = relaxedMock<Billing>()

            beforeEach {
                with(billing) {
                    every { caregivingRoundInfo.caregivingRoundId } returns "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                }

                every {
                    billingRepository.findTopByCaregivingRoundInfoCaregivingRoundId("01GSM0PQ5G8HW2GKYXH3VGGMZG")
                } returns billing

                every {
                    billingRepository.findByReceptionInfoReceptionId("01GSM0PQ5G8HW2GKYXH3VGGMZG")
                } returns listOf(billing)
            }

            afterEach { clearAllMocks() }

            `when`("간병 회차 아이디로 청구를 조회하면") {
                val query = BillingByCaregivingRoundIdQuery(
                    caregivingRoundId = "01GSM0PQ5G8HW2GKYXH3VGGMZG",
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = billingService.getBilling(query)

                then("리포지토리로부터 간병 회차 아이디로 청구를 조회합니다.") {
                    behavior()

                    verify {
                        billingRepository.findTopByCaregivingRoundInfoCaregivingRoundId("01GSM0PQ5G8HW2GKYXH3VGGMZG")
                    }
                }

                then("조회한 정산을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult shouldBe billing
                }
            }

            `when`("시스템 사용자 권한으로 간병 회차 아이디로 청구를 조회하면") {
                val query = BillingByCaregivingRoundIdQuery(
                    caregivingRoundId = "01GSM0PQ5G8HW2GKYXH3VGGMZG",
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = billingService.getBilling(query)

                then("문제없이 응답합니다.") {
                    shouldNotThrowAny { behavior() }
                }
            }

            `when`("아무런 관련 없는 외부 간병 업체 사용자가 간병 회차 아이디로 청구를 조회하면") {
                val query = BillingByCaregivingRoundIdQuery(
                    caregivingRoundId = "01GSM0PQ5G8HW2GKYXH3VGGMZG",
                    subject = generateExternalCaregivingOrganizationManagerSubject("01GVG25A2P5V6MVQG2SPKQ4D50")
                )

                fun behavior() = billingService.getBilling(query)

                then("AccessDeniedException이 발생합니다.") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }

            `when`("접수정보가 변경되었음이 감지되면") {
                val event = relaxedMock<ReceptionModified>()

                beforeEach {
                    with(event) {
                        every { receptionId } returns "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                        every { accidentInfo } returns Modification(
                            relaxedMock {
                                every { accidentNumber } returns "01GW6918R2H7NART1C8ACWCRD8"
                            },
                            relaxedMock {
                                every { accidentNumber } returns "2022-3333333"
                            }
                        )
                        every { insuranceInfo } returns Modification(
                            relaxedMock {
                                every { subscriptionDate } returns LocalDate.of(2012, 3, 24)
                            },
                            relaxedMock {
                                every { subscriptionDate } returns LocalDate.of(2012, 3, 25)
                            }
                        )
                    }
                }

                afterEach { clearAllMocks() }

                fun handling() = billingService.handleReceptionModified(event)

                then("청구에 변경사항을 전달합니다.") {
                    handling()

                    verify {
                        billing.handleReceptionModified(event)
                    }
                }
            }

            `when`("접수정보가 변경되었음이 감지되었지만 청구에 영향을 줄 내용이 없다면") {
                val event = relaxedMock<ReceptionModified>()

                beforeEach {
                    with(event) {
                        every { receptionId } returns "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                        every { accidentInfo } returns Modification(
                            relaxedMock {
                                every { accidentNumber } returns "01GW6918R2H7NART1C8ACWCRD8"
                            },
                            relaxedMock {
                                every { accidentNumber } returns "01GW6918R2H7NART1C8ACWCRD8"
                            }
                        )
                        every { insuranceInfo } returns Modification(
                            relaxedMock {
                                every { subscriptionDate } returns LocalDate.of(2012, 3, 24)
                            },
                            relaxedMock {
                                every { subscriptionDate } returns LocalDate.of(2012, 3, 24)
                            }
                        )
                    }
                }

                afterEach { clearAllMocks() }

                fun handling() = billingService.handleReceptionModified(event)

                then("아무것도 안합니다.") {
                    handling()

                    verify(exactly = 0) {
                        billingRepository.findByReceptionInfoReceptionId(any())
                        billing.handleReceptionModified(any())
                    }
                }
            }
        }

        and("조회하려는 billing 이 없다면") {
            val billingId = "01GW7JJN6C6QJF8V7D0Q2PSST7"
            val subject = generateInternalCaregivingManagerSubject()

            beforeEach {
                every { billingRepository.findByIdOrNull(billingId) } returns null
            }

            afterEach {
                clearAllMocks()
            }

            fun behavior() = billingService.downloadCertification(
                DownloadCertificateCommand(
                    billingId = billingId,
                    subject = subject,
                )
            )

            then("BillingNotExistsException 이 발생합니다.") {
                val exception = shouldThrow<BillingNotExistsException> {
                    behavior()
                }
                exception.billingId shouldBe billingId
            }
        }

        and("청구의 목록을 조회할 때") {
            val subject = generateInternalCaregivingManagerSubject()
            val pageRequest = PageRequest.of(0, 2)
            val searchQuery = SearchCondition(
                searchingProperty = BillingByFilterQuery.SearchingProperty.PATIENT_NAME,
                keyword = "김철수",
            )

            beforeEach {
                every { billingRepository.searchBillings(any(), any()) } returns relaxedMock()
            }

            afterEach {
                clearAllMocks()
            }

            `when`("사용기간을 조건으로 목록을 조회하면") {
                fun behavior() = billingService.getBillings(
                    BillingByFilterQuery(
                        progressingStatus = setOf(BillingProgressingStatus.WAITING_FOR_BILLING),
                        usedPeriodFrom = LocalDate.of(2023, 4, 1),
                        usedPeriodUntil = LocalDate.of(2023, 4, 30),
                        searchQuery = null,
                        sorting = null,
                        subject = subject,
                    ),
                    pageRequest = pageRequest
                )

                then("청구 목록 조회를 요청합니다.") {
                    behavior()

                    verify {
                        billingRepository.searchBillings(
                            withArg {
                                it.progressingStatus shouldBe setOf(BillingProgressingStatus.WAITING_FOR_BILLING)
                                it.usedPeriodFrom shouldBe LocalDate.of(2023, 4, 1)
                                it.usedPeriodUntil shouldBe LocalDate.of(2023, 4, 30)
                                it.billingDateFrom shouldBe null
                                it.billingDateUntil shouldBe null
                                it.transactionDateFrom shouldBe null
                                it.transactionDateUntil shouldBe null
                                it.accidentNumber shouldBe null
                                it.patientName shouldBe null
                            },
                            withArg<PageRequest> {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 2
                                it.sort shouldBe Sort.by(Sort.Order.desc("id"))
                            }
                        )
                    }
                }
            }
            and("사용기간을 조건으로 쿼리와 함께 목록을 조회하면") {
                fun behavior() = billingService.getBillings(
                    BillingByFilterQuery(
                        progressingStatus = setOf(BillingProgressingStatus.WAITING_FOR_BILLING),
                        usedPeriodFrom = LocalDate.of(2023, 4, 1),
                        usedPeriodUntil = LocalDate.of(2023, 4, 30),
                        searchQuery = searchQuery,
                        sorting = null,
                        subject = subject,
                    ),
                    pageRequest = pageRequest
                )

                then("청구 목록 조회를 요청합니다.") {
                    behavior()

                    verify {
                        billingRepository.searchBillings(
                            withArg {
                                it.progressingStatus shouldBe setOf(BillingProgressingStatus.WAITING_FOR_BILLING)
                                it.usedPeriodFrom shouldBe LocalDate.of(2023, 4, 1)
                                it.usedPeriodUntil shouldBe LocalDate.of(2023, 4, 30)
                                it.billingDateFrom shouldBe null
                                it.billingDateUntil shouldBe null
                                it.transactionDateFrom shouldBe null
                                it.transactionDateUntil shouldBe null
                                it.accidentNumber shouldBe null
                                it.patientName shouldBe "김철수"
                            },
                            withArg<PageRequest> {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 2
                                it.sort shouldBe Sort.by(Sort.Order.desc("id"))
                            }
                        )
                    }
                }
            }

            `when`("청구일자를 조건으로 목록을 조회하면") {
                fun behavior() = billingService.getBillings(
                    BillingByFilterQuery(
                        progressingStatus = setOf(
                            BillingProgressingStatus.WAITING_DEPOSIT,
                            BillingProgressingStatus.UNDER_DEPOSIT,
                            BillingProgressingStatus.OVER_DEPOSIT
                        ),
                        billingDateFrom = LocalDate.of(2023, 4, 1),
                        billingDateUntil = LocalDate.of(2023, 4, 30),
                        searchQuery = null,
                        sorting = null,
                        subject = subject,
                    ),
                    pageRequest = pageRequest
                )

                then("청구 목록 조회를 요청합니다.") {
                    behavior()

                    verify {
                        billingRepository.searchBillings(
                            withArg {
                                it.progressingStatus shouldBe setOf(
                                    BillingProgressingStatus.WAITING_DEPOSIT,
                                    BillingProgressingStatus.UNDER_DEPOSIT,
                                    BillingProgressingStatus.OVER_DEPOSIT,
                                )
                                it.usedPeriodFrom shouldBe null
                                it.usedPeriodUntil shouldBe null
                                it.billingDateFrom shouldBe LocalDate.of(2023, 4, 1)
                                it.billingDateUntil shouldBe LocalDate.of(2023, 4, 30)
                                it.transactionDateFrom shouldBe null
                                it.transactionDateUntil shouldBe null
                                it.accidentNumber shouldBe null
                                it.patientName shouldBe null
                            },
                            withArg<PageRequest> {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 2
                                it.sort shouldBe Sort.by(Sort.Order.desc("id"))
                            }
                        )
                    }
                }
            }

            and("청구일자를 조건으로 쿼리와 함께 목록을 조회하면") {
                fun behavior() = billingService.getBillings(
                    BillingByFilterQuery(
                        progressingStatus = setOf(
                            BillingProgressingStatus.WAITING_DEPOSIT,
                            BillingProgressingStatus.UNDER_DEPOSIT,
                            BillingProgressingStatus.OVER_DEPOSIT
                        ),
                        billingDateFrom = LocalDate.of(2023, 4, 1),
                        billingDateUntil = LocalDate.of(2023, 4, 30),
                        searchQuery = null,
                        sorting = null,
                        subject = subject,
                    ),
                    pageRequest = pageRequest
                )

                then("청구 목록 조회를 요청합니다.") {
                    behavior()

                    verify {
                        billingRepository.searchBillings(
                            withArg {
                                it.progressingStatus shouldBe setOf(
                                    BillingProgressingStatus.WAITING_DEPOSIT,
                                    BillingProgressingStatus.UNDER_DEPOSIT,
                                    BillingProgressingStatus.OVER_DEPOSIT,
                                )
                                it.billingDateFrom shouldBe LocalDate.of(2023, 4, 1)
                                it.billingDateUntil shouldBe LocalDate.of(2023, 4, 30)
                                it.transactionDateFrom shouldBe null
                                it.transactionDateUntil shouldBe null
                                it.usedPeriodFrom shouldBe null
                                it.usedPeriodUntil shouldBe null
                                it.accidentNumber shouldBe null
                                it.patientName shouldBe null
                            },
                            withArg<PageRequest> {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 2
                                it.sort shouldBe Sort.by(Sort.Order.desc("id"))
                            }
                        )
                    }
                }
            }

            `when`("입출금 일자를 조건으로 목록을 조회하면") {
                fun behavior() = billingService.getBillings(
                    BillingByFilterQuery(
                        progressingStatus = setOf(BillingProgressingStatus.COMPLETED_DEPOSIT),
                        transactionDateFrom = LocalDate.of(2023, 4, 1),
                        transactionDateUntil = LocalDate.of(2023, 4, 7),
                        searchQuery = null,
                        sorting = null,
                        subject = subject,
                    ),
                    pageRequest = pageRequest
                )

                then("청구 목록 조회를 요청합니다.") {
                    behavior()

                    verify {
                        billingRepository.searchBillings(
                            withArg {
                                it.progressingStatus shouldBe setOf(BillingProgressingStatus.COMPLETED_DEPOSIT)
                                it.usedPeriodFrom shouldBe null
                                it.usedPeriodUntil shouldBe null
                                it.billingDateFrom shouldBe null
                                it.billingDateUntil shouldBe null
                                it.transactionDateFrom shouldBe LocalDate.of(2023, 4, 1)
                                it.transactionDateUntil shouldBe LocalDate.of(2023, 4, 7)
                                it.accidentNumber shouldBe null
                                it.patientName shouldBe null
                            },
                            withArg<PageRequest> {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 2
                                it.sort shouldBe Sort.by(Sort.Order.desc("id"))
                            }
                        )
                    }
                }
            }
            and("입출금 일자를 조건으로 쿼리와 함께 목록을 조회하면") {
                fun behavior() = billingService.getBillings(
                    BillingByFilterQuery(
                        progressingStatus = setOf(BillingProgressingStatus.COMPLETED_DEPOSIT),
                        transactionDateFrom = LocalDate.of(2023, 4, 1),
                        transactionDateUntil = LocalDate.of(2023, 4, 7),
                        searchQuery = searchQuery,
                        sorting = null,
                        subject = subject,
                    ),
                    pageRequest = pageRequest
                )

                then("청구 목록 조회를 요청합니다.") {
                    behavior()

                    verify {
                        billingRepository.searchBillings(
                            withArg {
                                it.progressingStatus shouldBe setOf(BillingProgressingStatus.COMPLETED_DEPOSIT)
                                it.usedPeriodFrom shouldBe null
                                it.usedPeriodUntil shouldBe null
                                it.billingDateFrom shouldBe null
                                it.billingDateUntil shouldBe null
                                it.transactionDateFrom shouldBe LocalDate.of(2023, 4, 1)
                                it.transactionDateUntil shouldBe LocalDate.of(2023, 4, 7)
                                it.accidentNumber shouldBe null
                                it.patientName shouldBe "김철수"
                            },
                            withArg<PageRequest> {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 2
                            }
                        )
                    }
                }
            }
        }

        `when`("간병 회차 시작일과 종료일 수정이 감지되면") {
            val event = relaxedMock<CaregivingRoundModified>()
            fun handling() = billingService.handleCaregivingRoundModified(event)

            val billing = relaxedMock<Billing> {
                every { handleCaregivingRoundModified(any(), any()) } returns Unit
            }

            val coverageInfo = CoverageInfo(
                targetSubscriptionYear = 2023,
                renewalType = CoverageInfo.RenewalType.THREE_YEAR,
                annualCoveredCaregivingCharges = listOf(
                    CoverageInfo.AnnualCoveredCaregivingCharge(
                        targetAccidentYear = 2023,
                        caregivingCharge = 100000,
                    )
                )
            )

            beforeEach {
                with(billingRepository) {
                    val slot = slot<Billing>()
                    every { findTopByCaregivingRoundInfoCaregivingRoundId(match { it == "01GYFTSHHH5T8YYKCVMKMAZNT7" }) } returns billing
                    every { save(capture(slot)) } answers {
                        slot.captured
                    }
                }

                with(event) {
                    every { caregivingRoundId } returns "01GYFTSHHH5T8YYKCVMKMAZNT7"
                    every { receptionId } returns "01GYFTS2010GD7YVDRVE357Y1C"
                    every { settlementProgressingStatus } returns Modification(
                        SettlementProgressingStatus.COMPLETED,
                        SettlementProgressingStatus.COMPLETED
                    )
                    every { billingProgressingStatus } returns Modification(
                        BillingProgressingStatus.COMPLETED_DEPOSIT,
                        BillingProgressingStatus.COMPLETED_DEPOSIT
                    )
                    every { startDateTime.current } returns LocalDateTime.of(2023, 3, 14, 9, 21, 31)
                    every { endDateTime.current } returns LocalDateTime.of(2023, 3, 18, 9, 21, 31)
                }

                every { receptionByIdQueryHandler.getReception(match { it.receptionId == "01GYFTS2010GD7YVDRVE357Y1C" }) } returns relaxedMock {
                    every { insuranceInfo.subscriptionDate } returns LocalDate.of(2022, 1, 17)
                    every { insuranceInfo.coverageId } returns "01GYFTSVVC1X9DSE48G122Y8Y6"
                }

                every {
                    coverageByIdQueryHandler.getCoverage(
                        match { it.coverageId == "01GYFTSVVC1X9DSE48G122Y8Y6" },
                        any<(Coverage) -> CoverageInfo>()
                    )
                } returns coverageInfo

                every { billing.willBeAffectedBy(event) } returns true
            }

            afterEach { clearAllMocks() }

            then("청구에 필요한 데이터인 reception 을 요청합니다.") {
                handling()

                verify {
                    receptionByIdQueryHandler.getReception(match { it.receptionId == "01GYFTS2010GD7YVDRVE357Y1C" })
                }
            }

            then("청구에 필요한 데이터인 coverage 를 요청합니다.") {
                handling()

                verify {
                    coverageByIdQueryHandler.getCoverage(
                        match { it.coverageId == "01GYFTSVVC1X9DSE48G122Y8Y6" },
                        any<(Coverage) -> CoverageInfo>()
                    )
                }
            }

            then("변경된 시작일자와 종료일자를 반영하여 청구를 업데이트 하도록 요청합니다.") {
                handling()

                verify {
                    billing.handleCaregivingRoundModified(
                        event,
                        withArg {
                            it.targetSubscriptionYear shouldBe 2023
                            it.renewalType shouldBe CoverageInfo.RenewalType.THREE_YEAR
                            it.annualCoveredCaregivingCharges shouldBe listOf(
                                CoverageInfo.AnnualCoveredCaregivingCharge(
                                    targetAccidentYear = 2023,
                                    caregivingCharge = 100000,
                                )
                            )
                        }
                    )
                }
            }
        }

        `when`("간병 회차 시작일과 종료일이 수정되었으나 청구가 변경사항에 영향을 받지 않는다면") {
            val event = relaxedMock<CaregivingRoundModified>()
            fun handling() = billingService.handleCaregivingRoundModified(event)

            val billing = relaxedMock<Billing> {
                every { handleCaregivingRoundModified(any(), any()) } returns Unit
            }

            val coverageInfo = CoverageInfo(
                targetSubscriptionYear = 2023,
                renewalType = CoverageInfo.RenewalType.THREE_YEAR,
                annualCoveredCaregivingCharges = listOf(
                    CoverageInfo.AnnualCoveredCaregivingCharge(
                        targetAccidentYear = 2023,
                        caregivingCharge = 100000,
                    )
                )
            )

            beforeEach {
                with(billingRepository) {
                    val slot = slot<Billing>()
                    every { findTopByCaregivingRoundInfoCaregivingRoundId(match { it == "01GYFTSHHH5T8YYKCVMKMAZNT7" }) } returns billing
                    every { save(capture(slot)) } answers {
                        slot.captured
                    }
                }

                with(event) {
                    every { caregivingRoundId } returns "01GYFTSHHH5T8YYKCVMKMAZNT7"
                    every { receptionId } returns "01GYFTS2010GD7YVDRVE357Y1C"
                    every { settlementProgressingStatus } returns Modification(
                        SettlementProgressingStatus.COMPLETED,
                        SettlementProgressingStatus.COMPLETED
                    )
                    every { billingProgressingStatus } returns Modification(
                        BillingProgressingStatus.COMPLETED_DEPOSIT,
                        BillingProgressingStatus.COMPLETED_DEPOSIT
                    )
                    every { startDateTime.current } returns LocalDateTime.of(2023, 3, 14, 9, 21, 31)
                    every { endDateTime.current } returns LocalDateTime.of(2023, 3, 18, 9, 21, 31)
                }

                every { billing.willBeAffectedBy(event) } returns false
            }

            afterEach { clearAllMocks() }

            then("변경 사항을 청구에 전달하지 않습니다.") {
                handling()

                verify(exactly = 0) {
                    billing.handleCaregivingRoundModified(
                        event,
                        any(),
                    )
                }
            }
        }

        `when`("도착 후 취소의 변경이 감지되면") {
            val event = relaxedMock<CaregivingChargeModified>()
            fun handling() = billingService.handleCaregivingChargeModified(event)

            val billing = relaxedMock<Billing> {
                every { handleCaregivingChargeModified(any(), any()) } returns Unit
            }

            val coverageInfo = CoverageInfo(
                targetSubscriptionYear = 2023,
                renewalType = CoverageInfo.RenewalType.THREE_YEAR,
                annualCoveredCaregivingCharges = listOf(
                    CoverageInfo.AnnualCoveredCaregivingCharge(
                        targetAccidentYear = 2023,
                        caregivingCharge = 100000,
                    )
                )
            )

            beforeEach {
                with(billingRepository) {
                    val slot = slot<Billing>()
                    every { findTopByCaregivingRoundInfoCaregivingRoundId(match { it == "01GYY4H7FJNRFEVPPDXMGVX5YZ" }) } returns billing
                    every { save(capture(slot)) } answers {
                        slot.captured
                    }
                }

                with(event) {
                    every { receptionId } returns "01GYY4GXDTSHSH8MSRQH04012X"
                    every { caregivingRoundId } returns "01GYY4H7FJNRFEVPPDXMGVX5YZ"
                    every { basicAmount } returns 100000
                    every { additionalAmount } returns 20000
                    every { totalAmount } returns 120000
                    every { expectedSettlementDate } returns Modification(
                        LocalDate.of(2023, 4, 23),
                        LocalDate.of(2023, 4, 26)
                    )
                    every { confirmStatus } returns CaregivingChargeConfirmStatus.CONFIRMED
                    every { editingSubject } returns generateSystemSubject()
                    every { isCancelAfterArrived } returns Modification(previous = false, current = true)
                }

                every { receptionByIdQueryHandler.getReception(match { it.receptionId == "01GYY4GXDTSHSH8MSRQH04012X" }) } returns relaxedMock {
                    every { insuranceInfo.subscriptionDate } returns LocalDate.of(2022, 1, 17)
                    every { insuranceInfo.coverageId } returns "01GYY6HMAQ5V0KEJT548RBE9CD"
                }

                every {
                    coverageByIdQueryHandler.getCoverage(
                        match { it.coverageId == "01GYY6HMAQ5V0KEJT548RBE9CD" },
                        any<(Coverage) -> CoverageInfo>()
                    )
                } returns coverageInfo
            }

            afterEach { clearAllMocks() }

            then("청구에 필요한 데이터인 reception 을 요청합니다.") {
                handling()

                verify {
                    receptionByIdQueryHandler.getReception(match { it.receptionId == "01GYY4GXDTSHSH8MSRQH04012X" })
                }
            }

            then("청구에 필요한 데이터인 coverage 를 요청합니다.") {
                handling()

                verify {
                    coverageByIdQueryHandler.getCoverage(
                        match { it.coverageId == "01GYY6HMAQ5V0KEJT548RBE9CD" },
                        any<(Coverage) -> CoverageInfo>()
                    )
                }
            }

            then("변경된 도착 후 취소를 반영하여 청구를 업데이트 하도록 요청합니다.") {
                handling()

                verify {
                    billing.handleCaregivingChargeModified(
                        event,
                        withArg {
                            it.targetSubscriptionYear shouldBe 2023
                            it.renewalType shouldBe CoverageInfo.RenewalType.THREE_YEAR
                            it.annualCoveredCaregivingCharges shouldBe listOf(
                                CoverageInfo.AnnualCoveredCaregivingCharge(
                                    targetAccidentYear = 2023,
                                    caregivingCharge = 100000,
                                )
                            )
                        }
                    )
                }
            }
        }

        `when`("접수를 기준으로 청구를 조회하면") {
            val receptionId = "01GZZ814TTCA5GXX4B7H8QSP0A"
            val subject = generateInternalCaregivingManagerSubject()

            beforeEach {
                with(billingRepository) {
                    every { findByReceptionInfoReceptionIdAndBillingDateIsNotNull(match { it == receptionId }) } returns listOf(
                        relaxedMock(),
                        relaxedMock()
                    )
                }
            }

            afterEach { clearAllMocks() }

            fun behavior() = billingService.getBillingReception(
                BillingByReceptionIdQuery(
                    receptionId = receptionId,
                    subject = subject,
                )
            )

            then("청구 목록 조회를 요청합니다.") {
                behavior()

                verify {
                    billingRepository.findByReceptionInfoReceptionIdAndBillingDateIsNotNull(withArg { it shouldBe receptionId })
                }
            }
        }

        `when`("협회에 할당된 청구를 그 협회의 관리자가 조회하면") {
            val billingsOfReception = listOf<Billing>(
                relaxedMock {
                    every { get(ObjectAttribute.ASSIGNED_ORGANIZATION_ID) } returns setOf("01GSVWS32PWXHXD500V3FKRT6K")
                },
                relaxedMock {
                    every { get(ObjectAttribute.ASSIGNED_ORGANIZATION_ID) } returns setOf("01GSVWS32PWXHXD500V3FKRT6K")
                },
            )

            beforeEach {
                with(billingRepository) {
                    every {
                        findByReceptionInfoReceptionIdAndBillingDateIsNotNull(match { it == "01GZZ814TTCA5GXX4B7H8QSP0A" })
                    } returns billingsOfReception
                }
            }

            afterEach { clearAllMocks() }

            fun behavior() = billingService.getBillingReception(
                BillingByReceptionIdQuery(
                    receptionId = "01GZZ814TTCA5GXX4B7H8QSP0A",
                    subject = generateExternalCaregivingOrganizationManagerSubject("01GSVWS32PWXHXD500V3FKRT6K"),
                )
            )

            then("아무런 문제 없이 정산을 조회할 수 있습니다.") {
                val actualResult = behavior()

                actualResult shouldBe billingsOfReception
            }
        }

        `when`("협회에 할당된 청구를 그 협회와 관계 없는 관리자가 조회하면") {
            val billingsOfReception = listOf<Billing>(
                relaxedMock {
                    every { get(ObjectAttribute.ASSIGNED_ORGANIZATION_ID) } returns setOf("01GSVWS32PWXHXD500V3FKRT6K")
                },
                relaxedMock {
                    every { get(ObjectAttribute.ASSIGNED_ORGANIZATION_ID) } returns setOf("01GSVWS32PWXHXD500V3FKRT6K")
                },
            )

            beforeEach {
                with(billingRepository) {
                    every {
                        findByReceptionInfoReceptionIdAndBillingDateIsNotNull(match { it == "01GZZ814TTCA5GXX4B7H8QSP0A" })
                    } returns billingsOfReception
                }
            }

            afterEach { clearAllMocks() }

            fun behavior() = billingService.getBillingReception(
                BillingByReceptionIdQuery(
                    receptionId = "01GZZ814TTCA5GXX4B7H8QSP0A",
                    subject = generateExternalCaregivingOrganizationManagerSubject("01H5RJM1C0A4AXPJ7WCNXA3ESZ"),
                )
            )

            then("AccessDeniedException이 발생합니다.") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }

        `when`("협회에 할당되지 않은 청구를 외부 협회 관리자가 조회하면") {
            val billingsOfReception = listOf<Billing>(
                relaxedMock { every { get(ObjectAttribute.ASSIGNED_ORGANIZATION_ID) } returns setOf() },
                relaxedMock { every { get(ObjectAttribute.ASSIGNED_ORGANIZATION_ID) } returns setOf() },
            )

            beforeEach {
                with(billingRepository) {
                    every {
                        findByReceptionInfoReceptionIdAndBillingDateIsNotNull(match { it == "01GZZ814TTCA5GXX4B7H8QSP0A" })
                    } returns billingsOfReception
                }
            }

            afterEach { clearAllMocks() }

            fun behavior() = billingService.getBillingReception(
                BillingByReceptionIdQuery(
                    receptionId = "01GZZ814TTCA5GXX4B7H8QSP0A",
                    subject = generateExternalCaregivingOrganizationManagerSubject("01GSVWS32PWXHXD500V3FKRT6K"),
                )
            )

            then("AccessDeniedException이 발생합니다.") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }

        and("엔티티 테스트할 때") {
            val billing = Billing(
                id = "01GW68NM47FX1C8KE6GCFYBBDK",
                receptionInfo = Billing.ReceptionInfo(
                    receptionId = "01GW690V6Q14VKHH4PYE3M6FBY",
                    accidentNumber = "01GW6918R2H7NART1C8ACWCRD8",
                    subscriptionDate = LocalDate.of(2015, 3, 24),
                ),
                caregivingRoundInfo = Billing.CaregivingRoundInfo(
                    caregivingRoundId = "01GW692NXFNWT7S85RJPYR9WVZ",
                    roundNumber = 3,
                    startDateTime = LocalDateTime.of(2013, 3, 23, 9, 30, 15),
                    endDateTime = LocalDateTime.of(2013, 3, 25, 9, 30, 15),
                ),
                billingProgressingStatus = BillingProgressingStatus.WAITING_FOR_BILLING,
                coverageInfo = CoverageInfo(
                    targetSubscriptionYear = 2012,
                    renewalType = CoverageInfo.RenewalType.TEN_YEAR,
                    annualCoveredCaregivingCharges = listOf(
                        CoverageInfo.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2022, caregivingCharge = 100000
                        ),
                        CoverageInfo.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2023, caregivingCharge = 200000
                        ),
                    )
                ),
                isCancelAfterArrived = false,
                caregivingManagerInfo = null
            )
            `when`("저장을 요청하면") {
                fun behavior() = cacheBillingRepository.save(billing)
                then("저장이 됩니다.") {
                    behavior()
                }
            }
            `when`("조회를 요청하면") {
                fun behavior() = cacheBillingRepository.findByIdOrNull("01GW68NM47FX1C8KE6GCFYBBDK")
                then("조회가 됩니다.") {
                    behavior()
                }
            }
        }
    }
})
