package kr.caredoc.careinsurance.settlement

import com.github.guepardoapps.kulid.ULID
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.settlement.revision.SettlementRevision
import kr.caredoc.careinsurance.settlement.revision.SettlementRevisionRepository
import kr.caredoc.careinsurance.settlement.revision.SettlementRevisionService

class SettlementRevisionServiceTest : BehaviorSpec({
    given("정산 리비전 서비스가 주어졌을 떄") {
        val settlementRevisionRepository = relaxedMock<SettlementRevisionRepository>()
        val settlementRevisionService = SettlementRevisionService(
            settlementRevisionRepository,
        )

        val settlementRevisionId = "01H4FRG29X235PDD56TWMT0DY4"
        val eventSettlementId = "01H4FRRMNBG9F5W39XP1MS59Q3"
        beforeEach {
            mockkObject(ULID)
            every { ULID.random() } returns settlementRevisionId
        }

        afterEach { clearAllMocks() }

        `when`("SettlementGenerated 이벤트를 감지하면") {
            val event = relaxedMock<SettlementGenerated>()

            beforeEach {
                with(event) {
                    every { totalAmount } returns 234500
                    every { settlementId } returns eventSettlementId
                    every { progressingStatus } returns SettlementProgressingStatus.NOT_STARTED
                }

                val slot = slot<SettlementRevision>()
                with(settlementRevisionRepository) {
                    every { save(capture(slot)) } answers {
                        slot.captured
                    }
                }
            }

            afterEach { clearAllMocks() }

            fun handler() = settlementRevisionService.handleSettlementGenerated(event)

            then("SettlementRevision 을 생성합니다.") {
                handler()

                verify {
                    settlementRevisionRepository.save(
                        withArg {
                            it.id shouldBe settlementRevisionId
                            it.settlementId shouldBe eventSettlementId
                            it.progressingStatus shouldBe SettlementProgressingStatus.NOT_STARTED
                            it.totalAmount shouldBe 234500
                            it.totalDepositAmount shouldBe 0
                            it.totalWithdrawalAmount shouldBe 0
                        }
                    )
                }
            }
        }

        `when`("SettlementModified 이벤트를 감지하면") {
            val event = relaxedMock<SettlementModified>()
            beforeEach {
                with(event) {
                    every { settlementId } returns eventSettlementId
                    every { totalAmount } returns 77500
                    every { totalDepositAmount } returns 0
                    every { totalWithdrawalAmount } returns 0
                    every { progressingStatus } returns Modification(SettlementProgressingStatus.CONFIRMED, SettlementProgressingStatus.WAITING)
                }

                val slot = slot<SettlementRevision>()
                with(settlementRevisionRepository) {
                    every { save(capture(slot)) } answers {
                        slot.captured
                    }
                }
            }

            afterEach { clearAllMocks() }

            fun handler() = settlementRevisionService.handleSettlementModified(event)

            then("SettlementRevision 을 생성합니다.") {
                handler()

                verify {
                    settlementRevisionRepository.save(
                        withArg {
                            it.id shouldBe settlementRevisionId
                            it.settlementId shouldBe eventSettlementId
                            it.progressingStatus shouldBe SettlementProgressingStatus.WAITING
                            it.totalAmount shouldBe 77500
                            it.totalDepositAmount shouldBe 0
                            it.totalWithdrawalAmount shouldBe 0
                        }
                    )
                }
            }
        }

        `when`("SettlementTransactionRecorded 이벤트를 감지하면") {
            val event = relaxedMock<SettlementTransactionRecorded>()

            beforeEach {
                with(event) {
                    every { settlementId } returns eventSettlementId
                    every { totalAmount } returns 350000
                    every { totalDepositAmount } returns 400000
                    every { totalWithdrawalAmount } returns 50000
                    every { progressingStatus } returns SettlementProgressingStatus.WAITING
                }

                val slot = slot<SettlementRevision>()
                with(settlementRevisionRepository) {
                    every { save(capture(slot)) } answers {
                        slot.captured
                    }
                }
            }

            afterEach { clearAllMocks() }

            fun handler() = settlementRevisionService.handleSettlementTransactionRecorded(event)

            then("SettlementRevision 을 생성합니다.") {
                handler()

                verify {
                    settlementRevisionRepository.save(
                        withArg {
                            it.id shouldBe settlementRevisionId
                            it.settlementId shouldBe eventSettlementId
                            it.progressingStatus shouldBe SettlementProgressingStatus.WAITING
                            it.totalAmount shouldBe 350000
                            it.totalDepositAmount shouldBe 400000
                            it.totalWithdrawalAmount shouldBe 50000
                        }
                    )
                }
            }
        }
    }
})
