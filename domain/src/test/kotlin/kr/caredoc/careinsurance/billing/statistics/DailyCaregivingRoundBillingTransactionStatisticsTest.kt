package kr.caredoc.careinsurance.billing.statistics

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import kr.caredoc.careinsurance.billing.BillingTransactionRecorded
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.transaction.TransactionType
import java.time.LocalDate
import java.time.LocalDateTime

class DailyCaregivingRoundBillingTransactionStatisticsTest : BehaviorSpec({
    given("일자별 청구 입/출금 통계가 주어졌을때") {
        lateinit var statistics: DailyCaregivingRoundBillingTransactionStatistics

        beforeEach {
            statistics = DailyCaregivingRoundBillingTransactionStatistics(
                id = "01GZG95050YE9DX5BBP5NF9XTE",
                receptionId = "01GZG94VSCHKFED7KTWWQEX408",
                caregivingRoundId = "01GZG957CX58ESFNGGXZ016Z69",
                date = LocalDate.of(2023, 4, 30),
            )
        }

        afterEach { /* nothing to do */ }

        `when`("청구의 입금이 발생하면") {
            val event = relaxedMock<BillingTransactionRecorded>()

            beforeEach {
                with(event) {
                    every { transactionType } returns TransactionType.DEPOSIT
                    every { amount } returns 645000
                    every { enteredDateTime } returns LocalDateTime.of(2023, 4, 30, 9, 15, 0)
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = statistics.handleBillingTransactionRecorded(event)

            then("총 출금 금액이 증가합니다.") {
                statistics.totalDepositAmount shouldBe 0

                handling()

                statistics.totalDepositAmount shouldBe 645000
            }

            then("마지막 입/출금 기록일시가 갱신됩니다.") {
                handling()

                statistics.lastEnteredDateTime shouldBe LocalDateTime.of(2023, 4, 30, 9, 15, 0)
            }
        }

        `when`("청구의 출금이 발생하면") {
            val event = relaxedMock<BillingTransactionRecorded>()

            beforeEach {
                with(event) {
                    every { transactionType } returns TransactionType.WITHDRAWAL
                    every { amount } returns 124000
                    every { enteredDateTime } returns LocalDateTime.of(2023, 4, 30, 9, 15, 0)
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = statistics.handleBillingTransactionRecorded(event)

            then("총 입금 금액이 증가합니다.") {
                statistics.totalWithdrawalAmount shouldBe 0

                handling()

                statistics.totalWithdrawalAmount shouldBe 124000
            }

            then("마지막 입/출금 기록일시가 갱신됩니다.") {
                handling()

                statistics.lastEnteredDateTime shouldBe LocalDateTime.of(2023, 4, 30, 9, 15, 0)
            }
        }
    }
})
