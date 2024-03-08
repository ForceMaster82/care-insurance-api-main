package kr.caredoc.careinsurance.billing.revision

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import kr.caredoc.careinsurance.billing.BillingByIdQueryHandler
import kr.caredoc.careinsurance.billing.BillingGenerated
import kr.caredoc.careinsurance.billing.BillingModified
import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import kr.caredoc.careinsurance.billing.BillingTransactionRecorded
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.transaction.TransactionType
import java.time.LocalDate
import java.time.LocalDateTime

class BillingRevisionServiceTest : BehaviorSpec({
    given("청구 리비젼 서비스가 주어졌을때") {
        val billingRevisionRepository = relaxedMock<BillingRevisionRepository>()
        val billingByIdQueryHandler = relaxedMock<BillingByIdQueryHandler>()
        val service = BillingRevisionService(
            billingRevisionRepository,
            billingByIdQueryHandler,
        )

        beforeEach {
            val savingEntitySlot = slot<BillingRevision>()
            every {
                billingRevisionRepository.save(capture(savingEntitySlot))
            } answers {
                savingEntitySlot.captured
            }
        }

        afterEach { clearAllMocks() }

        `when`("청구가 생성 되었을 때") {
            val event = relaxedMock<BillingGenerated>()
            beforeEach {
                with(event) {
                    every { billingId } returns "01H45F4GR2F1NVSVZXNYNC62TY"
                    every { caregivingRoundId } returns "01H45F4ZJFD2GD94W5TEDZ3RER"
                    every { progressingStatus } returns BillingProgressingStatus.WAITING_DEPOSIT
                    every { billingAmount } returns 540000
                    every { issuedDateTime } returns LocalDateTime.of(2023, 6, 30, 15, 15, 15)
                }
                every {
                    billingByIdQueryHandler.getBilling(
                        match { it.billingId == "01H45F4GR2F1NVSVZXNYNC62TY" }
                    )
                } returns relaxedMock {
                    every { id } returns "01H45F4GR2F1NVSVZXNYNC62TY"
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = service.handleBillingGenerated(event)

            then("청구 리비젼를 기록합니다.") {
                handling()

                verify {
                    billingRevisionRepository.save(
                        withArg {
                            it.billingId shouldBe "01H45F4GR2F1NVSVZXNYNC62TY"
                            it.billingProgressingStatus shouldBe BillingProgressingStatus.WAITING_DEPOSIT
                            it.billingAmount shouldBe 540000
                            it.issuedDateTime shouldBe LocalDateTime.of(2023, 6, 30, 15, 15, 15)
                        }
                    )
                }
            }
        }

        `when`("청구의 상태가 수정 되었을 때") {
            val event = relaxedMock<BillingModified>()
            beforeEach {
                with(event) {
                    every { billingId } returns "01H45F4GR2F1NVSVZXNYNC62TY"
                    every { caregivingRoundId } returns "01H45F4ZJFD2GD94W5TEDZ3RER"
                    every { progressingStatus } returns Modification(
                        BillingProgressingStatus.WAITING_DEPOSIT,
                        BillingProgressingStatus.COMPLETED_DEPOSIT
                    )
                    every { totalAmount } returns Modification(
                        540000,
                        540000
                    )
                    every { totalDepositAmount } returns Modification(
                        540000,
                        540000
                    )
                    every { totalWithdrawalAmount } returns Modification(
                        0,
                        0
                    )
                    every { modifiedDateTime } returns LocalDateTime.of(2023, 6, 30, 16, 15, 15)
                }
                every {
                    billingByIdQueryHandler.getBilling(
                        match { it.billingId == "01H45F4GR2F1NVSVZXNYNC62TY" }
                    )
                } returns relaxedMock {
                    every { id } returns "01H45F4GR2F1NVSVZXNYNC62TY"
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = service.handleBillingModified(event)

            then("청구 리비젼를 기록합니다.") {
                handling()

                verify {
                    billingRevisionRepository.save(
                        withArg {
                            it.billingId shouldBe "01H45F4GR2F1NVSVZXNYNC62TY"
                            it.billingProgressingStatus shouldBe BillingProgressingStatus.COMPLETED_DEPOSIT
                            it.billingAmount shouldBe 540000
                            it.totalDepositAmount shouldBe 540000
                            it.totalWithdrawalAmount shouldBe 0
                            it.issuedDateTime shouldBe LocalDateTime.of(2023, 6, 30, 16, 15, 15)
                        }
                    )
                }
            }
        }

        `when`("청구의 트랜잭션 레코드가 생성 되었을 때") {
            val event = relaxedMock<BillingTransactionRecorded>()
            beforeEach {
                with(event) {
                    every { billingId } returns "01H45F4GR2F1NVSVZXNYNC62TY"
                    every { caregivingRoundId } returns "01H45F4ZJFD2GD94W5TEDZ3RER"
                    every { receptionId } returns "01H4DABX5Y7C48KE787PPP8TVB"
                    every { progressingStatus } returns BillingProgressingStatus.OVER_DEPOSIT
                    every { totalAmount } returns 540000
                    every { totalDepositAmount } returns 550000
                    every { totalWithdrawalAmount } returns 0
                    every { transactionDate } returns LocalDate.of(2023, 7, 3)
                    every { transactionType } returns TransactionType.DEPOSIT
                    every { amount } returns 10000
                    every { enteredDateTime } returns LocalDateTime.of(2023, 7, 3, 12, 15, 15)
                }
                every {
                    billingByIdQueryHandler.getBilling(
                        match { it.billingId == "01H45F4GR2F1NVSVZXNYNC62TY" }
                    )
                } returns relaxedMock {
                    every { id } returns "01H45F4GR2F1NVSVZXNYNC62TY"
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = service.handleBillingTransactionRecorded(event)

            then("청구 리비젼를 기록합니다.") {
                handling()

                verify {
                    billingRevisionRepository.save(
                        withArg {
                            it.billingId shouldBe "01H45F4GR2F1NVSVZXNYNC62TY"
                            it.billingProgressingStatus shouldBe BillingProgressingStatus.OVER_DEPOSIT
                            it.billingAmount shouldBe 540000
                            it.totalDepositAmount shouldBe 550000
                            it.totalWithdrawalAmount shouldBe 0
                            it.issuedDateTime shouldBe LocalDateTime.of(2023, 7, 3, 12, 15, 15)
                        }
                    )
                }
            }
        }
    }
})
