package kr.caredoc.careinsurance.reception.statistics

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import kr.caredoc.careinsurance.billing.BillingModified
import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import kr.caredoc.careinsurance.billing.BillingTransactionRecorded
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.reception.Reception
import kr.caredoc.careinsurance.reception.ReceptionModified
import kr.caredoc.careinsurance.reception.ReceptionProgressingStatus
import kr.caredoc.careinsurance.reception.ReceptionReceived
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.transaction.TransactionType
import java.time.LocalDate
import java.time.LocalDateTime

class DailyReceptionStatisticsTest : BehaviorSpec({
    given("일자별 간병 접수 통계가 주어졌을때") {
        lateinit var statistics: DailyReceptionStatistics

        beforeEach {
            statistics = DailyReceptionStatistics(
                id = "01GWVSH6RE0HBF78EYFEDA3X73",
                receivedDate = LocalDate.of(2022, 11, 1)
            )
        }

        afterEach { /* nothing to do */ }

        `when`("간병 신청이 접수되었음이 감지되면") {
            val event = ReceptionReceived(
                receptionId = "01GWVQRD04F6213P3QS3VCWJPA",
                receivedDateTime = LocalDateTime.of(2022, 11, 1, 20, 3, 12),
                desiredCaregivingStartDate = LocalDate.of(2022, 11, 2),
                urgency = Reception.Urgency.NORMAL,
                periodType = Reception.PeriodType.NORMAL,
            )

            fun handling() = statistics.handleReceptionReceived(event)

            then("접수 개수를 증가시킵니다.") {
                statistics.receptionCount shouldBe 0

                handling()

                statistics.receptionCount shouldBe 1
            }
        }

        `when`("당일 처리 되어야 하는 간병 신청이 접수되었음이 확인되면") {
            val event = ReceptionReceived(
                receptionId = "01GWVQRD04F6213P3QS3VCWJPA",
                receivedDateTime = LocalDateTime.of(2022, 11, 1, 20, 3, 12),
                desiredCaregivingStartDate = LocalDate.of(2022, 11, 1),
                urgency = Reception.Urgency.URGENT,
                periodType = Reception.PeriodType.NORMAL,
            )

            fun handling() = statistics.handleReceptionReceived(event)

            then("접수 개수와 당일 처리 접수 개수를 증가시킵니다.") {
                statistics.receptionCount shouldBe 0
                statistics.sameDayAssignmentReceptionCount shouldBe 0

                handling()

                statistics.receptionCount shouldBe 1
                statistics.sameDayAssignmentReceptionCount shouldBe 1
            }
        }

        `when`("단기 간병 신청이 접수되었음이 확인되면") {
            val event = ReceptionReceived(
                receptionId = "01GWVQRD04F6213P3QS3VCWJPA",
                receivedDateTime = LocalDateTime.of(2022, 11, 1, 20, 3, 12),
                desiredCaregivingStartDate = LocalDate.of(2022, 11, 2),
                urgency = Reception.Urgency.NORMAL,
                periodType = Reception.PeriodType.SHORT,
            )

            fun handling() = statistics.handleReceptionReceived(event)

            then("접수 개수와 단기 간병 접수 개수를 증가시킵니다.") {
                statistics.receptionCount shouldBe 0
                statistics.shortTermReceptionCount shouldBe 0

                handling()

                statistics.receptionCount shouldBe 1
                statistics.shortTermReceptionCount shouldBe 1
            }
        }
    }

    fun ReceptionModified.mockReceptionModifiedEvent(
        origin: ReceptionReceived,
        desiredCaregivingStartDate: LocalDate = origin.desiredCaregivingStartDate,
        periodType: Reception.PeriodType = origin.periodType,
        urgency: Reception.Urgency = origin.urgency,
        progressingStatus: ReceptionProgressingStatus = ReceptionProgressingStatus.RECEIVED,
    ) {
        every { receptionId } returns "01GWVSH6RE0HBF78EYFEDA3X73"
        every { receivedDateTime } returns origin.receivedDateTime
        every { this@mockReceptionModifiedEvent.desiredCaregivingStartDate } returns Modification(
            origin.desiredCaregivingStartDate,
            desiredCaregivingStartDate,
        )
        every { this@mockReceptionModifiedEvent.urgency } returns urgency
        every { this@mockReceptionModifiedEvent.periodType } returns Modification(origin.periodType, periodType)
        every { this@mockReceptionModifiedEvent.progressingStatus } returns Modification(
            ReceptionProgressingStatus.RECEIVED,
            progressingStatus,
        )
    }

    given("특이하지 않은 간병 접수의 통계를 포함한 일자별 간병 접수 통계가 주어졌을때") {
        lateinit var statistics: DailyReceptionStatistics
        val appliedReceptionReceivedEvent = ReceptionReceived(
            receptionId = "01GWVQRD04F6213P3QS3VCWJPA",
            receivedDateTime = LocalDateTime.of(2022, 11, 1, 20, 3, 12),
            desiredCaregivingStartDate = LocalDate.of(2022, 11, 2),
            urgency = Reception.Urgency.URGENT,
            periodType = Reception.PeriodType.NORMAL,
        )

        beforeEach {
            statistics = DailyReceptionStatistics(
                id = "01GWVSH6RE0HBF78EYFEDA3X73",
                receivedDate = LocalDate.of(2022, 11, 1)
            )
            statistics.handleReceptionReceived(appliedReceptionReceivedEvent)
        }

        afterEach { /* nothing to do */ }

        `when`("간병 접수가 취소되었음이 감지되면") {
            val event = relaxedMock<ReceptionModified>()

            beforeEach {
                event.mockReceptionModifiedEvent(
                    origin = appliedReceptionReceivedEvent,
                    progressingStatus = ReceptionProgressingStatus.CANCELED,
                )
            }

            afterEach {
                clearAllMocks()
            }

            fun behavior() = statistics.handleReceptionModified(event)

            then("취소된 간병 접수의 개수를 증가시킵니다.") {
                statistics.canceledReceptionCount shouldBe 0

                behavior()

                statistics.canceledReceptionCount shouldBe 1
            }
        }

        `when`("개인 구인으로 인해 간병 접수가 취소되었음이 감지되면") {
            val event = relaxedMock<ReceptionModified>()

            beforeEach {
                event.mockReceptionModifiedEvent(
                    origin = appliedReceptionReceivedEvent,
                    progressingStatus = ReceptionProgressingStatus.CANCELED_BY_PERSONAL_CAREGIVER,
                )
            }

            afterEach {
                clearAllMocks()
            }

            fun behavior() = statistics.handleReceptionModified(event)

            then("개인 구인으로 인해 취소된 간병 접수의 개수를 증가시킵니다.") {
                statistics.canceledByPersonalCaregiverReceptionCount shouldBe 0

                behavior()

                statistics.canceledByPersonalCaregiverReceptionCount shouldBe 1
            }

            then("취소된 간병 접수의 개수를 증가시킵니다.") {
                statistics.canceledReceptionCount shouldBe 0

                behavior()

                statistics.canceledReceptionCount shouldBe 1
            }
        }

        `when`("의료 행위 요구로 인해 간병 접수가 취소되었음이 감지되면") {
            val event = relaxedMock<ReceptionModified>()

            beforeEach {
                event.mockReceptionModifiedEvent(
                    origin = appliedReceptionReceivedEvent,
                    progressingStatus = ReceptionProgressingStatus.CANCELED_BY_MEDICAL_REQUEST,
                )
            }

            afterEach {
                clearAllMocks()
            }

            fun behavior() = statistics.handleReceptionModified(event)

            then("의료 행위 요구로 인해 취소된 간병 접수의 개수를 증가시킵니다.") {
                statistics.canceledByMedicalRequestReceptionCount shouldBe 0

                behavior()

                statistics.canceledByMedicalRequestReceptionCount shouldBe 1
            }

            then("취소된 간병 접수의 개수를 증가시킵니다.") {
                statistics.canceledReceptionCount shouldBe 0

                behavior()

                statistics.canceledReceptionCount shouldBe 1
            }
        }

        `when`("간병 접수가 당일 처리 접수로 변경되었음이 감지되면") {
            val event = relaxedMock<ReceptionModified>()

            beforeEach {
                event.mockReceptionModifiedEvent(
                    origin = appliedReceptionReceivedEvent,
                    desiredCaregivingStartDate = appliedReceptionReceivedEvent.receivedDateTime.toLocalDate(),
                )
            }

            afterEach {
                clearAllMocks()
            }

            fun behavior() = statistics.handleReceptionModified(event)

            then("당일 처리 접수의 개수를 증가시킵니다.") {
                statistics.sameDayAssignmentReceptionCount shouldBe 0

                behavior()

                statistics.sameDayAssignmentReceptionCount shouldBe 1
            }
        }

        `when`("간병 접수가 당일 처리 접수로 변경되었고 시작되었다면") {
            val event = relaxedMock<ReceptionModified>()

            beforeEach {
                event.mockReceptionModifiedEvent(
                    origin = appliedReceptionReceivedEvent,
                    desiredCaregivingStartDate = appliedReceptionReceivedEvent.receivedDateTime.toLocalDate(),
                    progressingStatus = ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS,
                )
            }

            afterEach {
                clearAllMocks()
            }

            fun behavior() = statistics.handleReceptionModified(event)

            then("당일 처리 접수의 개수를 증가시킵니다.") {
                statistics.sameDayAssignmentReceptionCount shouldBe 0

                behavior()

                statistics.sameDayAssignmentReceptionCount shouldBe 1
            }

            then("시작된 당일 처리 접수의 개수를 증가시킵니다.") {
                statistics.startedSameDayAssignmentReceptionCount shouldBe 0

                behavior()

                statistics.startedSameDayAssignmentReceptionCount shouldBe 1
            }
        }

        `when`("간병 접수가 단기 간병 접수로 변경되었음이 감지되면") {
            val event = relaxedMock<ReceptionModified>()

            beforeEach {
                event.mockReceptionModifiedEvent(
                    origin = appliedReceptionReceivedEvent,
                    periodType = Reception.PeriodType.SHORT,
                )
            }

            afterEach {
                clearAllMocks()
            }

            fun behavior() = statistics.handleReceptionModified(event)

            then("단기 간병 접수의 개수를 증가시킵니다.") {
                statistics.shortTermReceptionCount shouldBe 0

                behavior()

                statistics.shortTermReceptionCount shouldBe 1
            }
        }

        `when`("간병 접수가 단기 간병 접수로 변경되었고 시작되었다면") {
            val event = relaxedMock<ReceptionModified>()

            beforeEach {
                event.mockReceptionModifiedEvent(
                    origin = appliedReceptionReceivedEvent,
                    periodType = Reception.PeriodType.SHORT,
                    progressingStatus = ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS,
                )
            }

            afterEach {
                clearAllMocks()
            }

            fun behavior() = statistics.handleReceptionModified(event)

            then("단기 간병 접수의 개수를 증가시킵니다.") {
                statistics.shortTermReceptionCount shouldBe 0

                behavior()

                statistics.shortTermReceptionCount shouldBe 1
            }

            then("시작된 단기 간병 접수의 개수를 증가시킵니다.") {
                statistics.startedShortTermReceptionCount shouldBe 0

                behavior()

                statistics.startedShortTermReceptionCount shouldBe 1
            }
        }

        `when`("간병비 청구가 진행되어 입금 대기 상태가 된다면") {
            val event = relaxedMock<BillingModified>()

            beforeEach {
                with(event) {
                    every { progressingStatus } returns Modification(
                        BillingProgressingStatus.WAITING_FOR_BILLING,
                        BillingProgressingStatus.WAITING_DEPOSIT,
                    )
                    every { totalAmount } returns Modification(140000, 140000)
                }
            }

            afterEach {
                clearAllMocks()
            }

            fun handling() = statistics.handleBillingModified(event)

            then("지급 요청건수가 증가합니다.") {
                statistics.requestedBillingCount shouldBe 0

                handling()

                statistics.requestedBillingCount shouldBe 1
            }

            then("지급 요청금액이 증가합니다.") {
                statistics.requestedBillingAmount shouldBe 0

                handling()

                statistics.requestedBillingAmount shouldBe 140000
            }
        }
    }

    given("간병비 청구 관련 수치를 포함한 접수 통계가 주어졌을때") {
        lateinit var statistics: DailyReceptionStatistics

        beforeEach {
            statistics = DailyReceptionStatistics(
                id = "01GWVSH6RE0HBF78EYFEDA3X73",
                receivedDate = LocalDate.of(2022, 11, 1)
            )
            statistics.handleReceptionReceived(
                ReceptionReceived(
                    receptionId = "01GWVQRD04F6213P3QS3VCWJPA",
                    receivedDateTime = LocalDateTime.of(2022, 11, 1, 20, 3, 12),
                    desiredCaregivingStartDate = LocalDate.of(2022, 11, 2),
                    urgency = Reception.Urgency.URGENT,
                    periodType = Reception.PeriodType.NORMAL,
                )
            )
            statistics.handleBillingModified(
                relaxedMock {
                    every { progressingStatus } returns Modification(
                        BillingProgressingStatus.WAITING_FOR_BILLING,
                        BillingProgressingStatus.WAITING_DEPOSIT,
                    )
                    every { totalAmount } returns Modification(140000, 140000)
                }
            )
        }

        afterEach { /* nothing to do */ }

        `when`("간병비 청구 금액이 변경된다면") {
            val event = relaxedMock<BillingModified>()

            beforeEach {
                with(event) {
                    every { progressingStatus } returns Modification(
                        BillingProgressingStatus.COMPLETED_DEPOSIT,
                        BillingProgressingStatus.UNDER_DEPOSIT,
                    )
                    every { totalAmount } returns Modification(140000, 145000)
                }
            }

            afterEach {
                clearAllMocks()
            }

            fun handling() = statistics.handleBillingModified(event)

            then("지급 요청건수는 유지합니다.") {
                statistics.requestedBillingCount shouldBe 1

                handling()

                statistics.requestedBillingCount shouldBe 1
            }

            then("변경된 차액만큼 지급요청 금액을 변경합니다.") {
                statistics.requestedBillingAmount shouldBe 140000

                handling()

                statistics.requestedBillingAmount shouldBe 145000
            }
        }

        `when`("간병비가 입금된다면") {
            val event = relaxedMock<BillingTransactionRecorded>()

            beforeEach {
                with(event) {
                    every { transactionType } returns TransactionType.DEPOSIT
                    every { amount } returns 140000
                }
            }

            afterEach {
                clearAllMocks()
            }

            fun handling() = statistics.handleBillingTransactionRecorded(event)

            then("지급건수가 증가합니다.") {
                statistics.depositCount shouldBe 0

                handling()

                statistics.depositCount shouldBe 1
            }

            then("지급금액이 증가합니다.") {
                statistics.depositAmount shouldBe 0

                handling()

                statistics.depositAmount shouldBe 140000
            }
        }
    }

    given("간병비 청구와 지급된 금액 관련 수치를 포함한 접수 통계가 주어졌을때") {

        lateinit var statistics: DailyReceptionStatistics

        beforeEach {
            statistics = DailyReceptionStatistics(
                id = "01GWVSH6RE0HBF78EYFEDA3X73",
                receivedDate = LocalDate.of(2022, 11, 1)
            )
            statistics.handleReceptionReceived(
                ReceptionReceived(
                    receptionId = "01GWVQRD04F6213P3QS3VCWJPA",
                    receivedDateTime = LocalDateTime.of(2022, 11, 1, 20, 3, 12),
                    desiredCaregivingStartDate = LocalDate.of(2022, 11, 2),
                    urgency = Reception.Urgency.URGENT,
                    periodType = Reception.PeriodType.NORMAL,
                )
            )
            statistics.handleBillingModified(
                relaxedMock {
                    every { progressingStatus } returns Modification(
                        BillingProgressingStatus.WAITING_FOR_BILLING,
                        BillingProgressingStatus.WAITING_DEPOSIT,
                    )
                    every { totalAmount } returns Modification(140000, 140000)
                }
            )
            statistics.handleBillingTransactionRecorded(
                relaxedMock {
                    every { transactionType } returns TransactionType.DEPOSIT
                    every { amount } returns 140000
                }
            )
        }

        afterEach { /* nothing to do */ }

        `when`("지금된 간병비가 환수된다면") {
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

            fun handling() = statistics.handleBillingTransactionRecorded(event)

            then("환수건수가 증가합니다.") {
                statistics.withdrawalCount shouldBe 0

                handling()

                statistics.withdrawalCount shouldBe 1
            }

            then("환수금액이 증가합니다.") {
                statistics.withdrawalAmount shouldBe 0

                handling()

                statistics.withdrawalAmount shouldBe 5000
            }
        }
    }

    given("당일 처리 접수의 통계를 포함한 일자별 간병 접수 통계가 주어졌을때") {
        lateinit var statistics: DailyReceptionStatistics
        val appliedReceptionReceivedEvent = ReceptionReceived(
            receptionId = "01GWVQRD04F6213P3QS3VCWJPA",
            receivedDateTime = LocalDateTime.of(2022, 11, 1, 20, 3, 12),
            desiredCaregivingStartDate = LocalDate.of(2022, 11, 1),
            urgency = Reception.Urgency.URGENT,
            periodType = Reception.PeriodType.NORMAL,
        )

        beforeEach {
            statistics = DailyReceptionStatistics(
                id = "01GWVSH6RE0HBF78EYFEDA3X73",
                receivedDate = LocalDate.of(2022, 11, 1)
            )
            statistics.handleReceptionReceived(appliedReceptionReceivedEvent)
        }

        afterEach { /* nothing to do */ }

        `when`("간병 접수가 변경되었으나 당일 처리 접수 상태를 계속 유지한다면") {
            val event = relaxedMock<ReceptionModified>()

            beforeEach {
                event.mockReceptionModifiedEvent(
                    origin = appliedReceptionReceivedEvent,
                    periodType = Reception.PeriodType.SHORT,
                )
            }

            afterEach {
                clearAllMocks()
            }

            fun behavior() = statistics.handleReceptionModified(event)

            then("당일 처리 접수의 개수를 유지합니다.") {
                statistics.sameDayAssignmentReceptionCount shouldBe 1

                behavior()

                statistics.sameDayAssignmentReceptionCount shouldBe 1
            }
        }

        `when`("간병 접수가 변경되어 당일 처리할 필요가 없는 접수로 변경되었다면") {
            val event = relaxedMock<ReceptionModified>()

            beforeEach {
                event.mockReceptionModifiedEvent(
                    origin = appliedReceptionReceivedEvent,
                    desiredCaregivingStartDate = appliedReceptionReceivedEvent.desiredCaregivingStartDate.plusDays(1),
                )
            }

            afterEach {
                clearAllMocks()
            }

            fun behavior() = statistics.handleReceptionModified(event)

            then("당일 처리 접수의 개수를 감소시킵니다.") {
                statistics.sameDayAssignmentReceptionCount shouldBe 1

                behavior()

                statistics.sameDayAssignmentReceptionCount shouldBe 0
            }
        }

        `when`("당일 처리 상태인채로 간병이 시작되었다면") {
            val event = relaxedMock<ReceptionModified>()

            beforeEach {
                event.mockReceptionModifiedEvent(
                    origin = appliedReceptionReceivedEvent,
                    progressingStatus = ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS,
                )
            }

            afterEach {
                clearAllMocks()
            }

            fun behavior() = statistics.handleReceptionModified(event)

            then("시작된 당일 처리 접수의 개수를 증가시킵니다.") {
                statistics.startedSameDayAssignmentReceptionCount shouldBe 0

                behavior()

                statistics.startedSameDayAssignmentReceptionCount shouldBe 1
            }
        }
    }

    given("단기 접수의 통계를 포함한 일자별 간병 접수 통계가 주어졌을때") {
        lateinit var statistics: DailyReceptionStatistics
        val appliedReceptionReceivedEvent = ReceptionReceived(
            receptionId = "01GWVQRD04F6213P3QS3VCWJPA",
            receivedDateTime = LocalDateTime.of(2022, 11, 1, 20, 3, 12),
            desiredCaregivingStartDate = LocalDate.of(2022, 11, 2),
            urgency = Reception.Urgency.NORMAL,
            periodType = Reception.PeriodType.SHORT,
        )

        beforeEach {
            statistics = DailyReceptionStatistics(
                id = "01GWVSH6RE0HBF78EYFEDA3X73",
                receivedDate = LocalDate.of(2022, 11, 1)
            )
            statistics.handleReceptionReceived(appliedReceptionReceivedEvent)
        }

        afterEach { /* nothing to do */ }

        `when`("간병 접수가 변경되었으나 단기 접수 상태를 계속 유지한다면") {
            val event = relaxedMock<ReceptionModified>()

            beforeEach {
                event.mockReceptionModifiedEvent(
                    origin = appliedReceptionReceivedEvent,
                    desiredCaregivingStartDate = appliedReceptionReceivedEvent.receivedDateTime.toLocalDate(),
                )
            }

            afterEach {
                clearAllMocks()
            }

            fun behavior() = statistics.handleReceptionModified(event)

            then("단기 접수의 개수를 유지합니다.") {
                statistics.shortTermReceptionCount shouldBe 1

                behavior()

                statistics.shortTermReceptionCount shouldBe 1
            }
        }

        `when`("간병 접수가 변경되어 단기 간병 접수가 아닌 접수로 변경되었다면") {
            val event = relaxedMock<ReceptionModified>()

            beforeEach {
                event.mockReceptionModifiedEvent(
                    origin = appliedReceptionReceivedEvent,
                    periodType = Reception.PeriodType.NORMAL,
                )
            }

            afterEach {
                clearAllMocks()
            }

            fun behavior() = statistics.handleReceptionModified(event)

            then("단기 간병 접수의 개수를 감소시킵니다.") {
                statistics.shortTermReceptionCount shouldBe 1

                behavior()

                statistics.shortTermReceptionCount shouldBe 0
            }
        }

        `when`("단기 간병 상태인채로 간병이 시작되었다면") {
            val event = relaxedMock<ReceptionModified>()

            beforeEach {
                event.mockReceptionModifiedEvent(
                    origin = appliedReceptionReceivedEvent,
                    progressingStatus = ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS,
                )
            }

            afterEach {
                clearAllMocks()
            }

            fun behavior() = statistics.handleReceptionModified(event)

            then("시작된 단기 간병 접수의 개수를 증가시킵니다.") {
                statistics.startedShortTermReceptionCount shouldBe 0

                behavior()

                statistics.startedShortTermReceptionCount shouldBe 1
            }
        }
    }
})
