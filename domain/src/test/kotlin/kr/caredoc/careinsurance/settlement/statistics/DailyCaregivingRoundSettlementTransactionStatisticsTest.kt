package kr.caredoc.careinsurance.settlement.statistics

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.settlement.SettlementTransactionRecorded
import kr.caredoc.careinsurance.transaction.TransactionType
import kr.caredoc.careinsurance.withFixedClock
import java.time.LocalDate
import java.time.LocalDateTime

class DailyCaregivingRoundSettlementTransactionStatisticsTest : BehaviorSpec({
    given("날짜/간병회차별 간병비 정산 입출금 통계가 주어졌을때") {
        lateinit var statistics: DailyCaregivingRoundSettlementTransactionStatistics

        beforeEach {
            statistics = withFixedClock(LocalDateTime.of(2023, 1, 25, 16, 5, 21)) {
                DailyCaregivingRoundSettlementTransactionStatistics(
                    id = "01GXQY49TAX390D6QB5W308PJJ",
                    receptionId = "01gvd2hs5fmx9012bn28vhdpw3",
                    caregivingRoundId = "01GSM0PQ5G8HW2GKYXH3VGGMZG",
                    date = LocalDate.of(2023, 1, 30),
                )
            }
        }

        afterEach { /* nothing to do */ }

        `when`("간병비 정산으로 인한 출금이 발생하면") {
            val event = relaxedMock<SettlementTransactionRecorded>()

            beforeEach {
                with(event) {
                    every { transactionType } returns TransactionType.WITHDRAWAL
                    every { amount } returns 5000
                    every { enteredDateTime } returns LocalDateTime.of(2023, 1, 30, 16, 5, 21)
                }
            }

            afterEach {
                clearAllMocks()
            }

            fun handling() = statistics.handleSettlementTransactionRecorded(event)

            then("총 출금 금액이 증가합니다.") {
                statistics.totalWithdrawalAmount shouldBe 0

                handling()

                statistics.totalWithdrawalAmount shouldBe 5000
            }

            then("마지막 간병비 입출금 기록일시가 갱신됩니다.") {
                statistics.lastEnteredDateTime shouldBe LocalDateTime.of(2023, 1, 25, 16, 5, 21)

                withFixedClock(LocalDateTime.of(2023, 1, 30, 16, 5, 21)) {
                    handling()
                }

                statistics.lastEnteredDateTime shouldBe LocalDateTime.of(2023, 1, 30, 16, 5, 21)
            }
        }

        `when`("간병비 정산으로 인한 입금이 발생하면") {
            val event = relaxedMock<SettlementTransactionRecorded>()

            beforeEach {
                with(event) {
                    every { transactionType } returns TransactionType.DEPOSIT
                    every { amount } returns 5000
                    every { enteredDateTime } returns LocalDateTime.of(2023, 1, 30, 16, 5, 21)
                }
            }

            afterEach {
                clearAllMocks()
            }

            fun handling() = statistics.handleSettlementTransactionRecorded(event)

            then("총 입금 금액이 증가합니다.") {
                statistics.totalDepositAmount shouldBe 0

                handling()

                statistics.totalDepositAmount shouldBe 5000
            }

            then("마지막 간병비 입출금 기록일시가 갱신됩니다.") {
                handling()

                statistics.lastEnteredDateTime shouldBe LocalDateTime.of(2023, 1, 30, 16, 5, 21)
            }
        }
    }
})
