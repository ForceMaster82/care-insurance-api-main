package kr.caredoc.careinsurance.settlement.statistics

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.settlement.SettlementTransactionRecorded
import kr.caredoc.careinsurance.transaction.TransactionType
import java.time.LocalDate

class DailySettlementTransactionStatisticsTest : BehaviorSpec({
    given("날짜별 정산 입출금 통계가 주어졌을때") {
        lateinit var dailySettlementTransactionStatistics: DailySettlementTransactionStatistics

        beforeEach {
            dailySettlementTransactionStatistics = DailySettlementTransactionStatistics(
                id = "01GXN9NE9XYR7BC62FWVFNCYPP",
                date = LocalDate.of(2023, 1, 30),
            )
        }

        afterEach { /* nothing to do */ }

        `when`("정산 입금이 확인되면") {
            val event = relaxedMock<SettlementTransactionRecorded>()

            beforeEach {
                with(event) {
                    every { transactionType } returns TransactionType.DEPOSIT
                    every { amount } returns 5000
                }
            }

            afterEach {
                clearAllMocks()
            }

            fun handling() = dailySettlementTransactionStatistics.handleSettlementTransactionRecorded(event)

            then("정산 입금 총액이 입금 금액만큼 증가합니다.") {
                dailySettlementTransactionStatistics.totalDepositAmount shouldBe 0

                handling()

                dailySettlementTransactionStatistics.totalDepositAmount shouldBe 5000
            }
        }

        `when`("정산 출금이 확인되면") {
            val event = relaxedMock<SettlementTransactionRecorded>()

            beforeEach {
                with(event) {
                    every { transactionType } returns TransactionType.WITHDRAWAL
                    every { amount } returns 5000
                }
            }

            afterEach {
                clearAllMocks()
            }

            fun handling() = dailySettlementTransactionStatistics.handleSettlementTransactionRecorded(event)

            then("정산 출금 총액이 출금 금액만큼 증가합니다.") {
                dailySettlementTransactionStatistics.totalWithdrawalAmount shouldBe 0

                handling()

                dailySettlementTransactionStatistics.totalWithdrawalAmount shouldBe 5000
            }
        }
    }
})
