package kr.caredoc.careinsurance.billing.statistics

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import kr.caredoc.careinsurance.billing.BillingTransactionRecorded
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.transaction.TransactionType
import java.time.LocalDate

class DailyBillingTransactionStatisticsTest : BehaviorSpec({
    given("날짜별 청구금 입출금 통계가 주어졌을때") {
        lateinit var dailyBillingTransactionStatistics: DailyBillingTransactionStatistics

        beforeEach {
            dailyBillingTransactionStatistics = DailyBillingTransactionStatistics(
                id = "01GXN9NE9XYR7BC62FWVFNCYPP",
                date = LocalDate.of(2023, 1, 30),
            )
        }

        afterEach { /* nothing to do */ }

        `when`("청구금 입금이 확인되면") {
            val event = relaxedMock<BillingTransactionRecorded>()

            beforeEach {
                with(event) {
                    every { transactionType } returns TransactionType.DEPOSIT
                    every { amount } returns 5000
                }
            }

            afterEach {
                clearAllMocks()
            }

            fun handling() = dailyBillingTransactionStatistics.handleBillingTransactionRecorded(event)

            then("청구금 입금 총액이 입금 금액만큼 증가합니다.") {
                dailyBillingTransactionStatistics.totalDepositAmount shouldBe 0

                handling()

                dailyBillingTransactionStatistics.totalDepositAmount shouldBe 5000
            }
        }

        `when`("청구금 출금이 확인되면") {
            val event = relaxedMock<BillingTransactionRecorded>()

            beforeEach {
                with(event) {
                    every { transactionType } returns TransactionType.WITHDRAWAL
                    every { amount } returns 5000
                }
            }

            afterEach {
                clearAllMocks()
            }

            fun handling() = dailyBillingTransactionStatistics.handleBillingTransactionRecorded(event)

            then("청구금 출금 총액이 출금 금액만큼 증가합니다.") {
                dailyBillingTransactionStatistics.totalWithdrawalAmount shouldBe 0

                handling()

                dailyBillingTransactionStatistics.totalWithdrawalAmount shouldBe 5000
            }
        }
    }
})
