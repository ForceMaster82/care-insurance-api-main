package kr.caredoc.careinsurance.caregiving.progressmessage

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import kr.caredoc.careinsurance.caregiving.CaregivingProgressingStatus
import kr.caredoc.careinsurance.caregiving.CaregivingRoundModified
import kr.caredoc.careinsurance.message.SendingStatus
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.relaxedMock
import java.time.LocalDate
import java.time.LocalDateTime

class CaregivingProgressMessageSummaryTest : BehaviorSpec({
    given("간병 진행 메시지 요약이 주어졌을때") {
        lateinit var firstRoundSummary: CaregivingProgressMessageSummary
        lateinit var nonFirstRoundSummary: CaregivingProgressMessageSummary
        beforeEach {
            firstRoundSummary = CaregivingProgressMessageSummary(
                id = "01H0HGFVPC08SCYMVYM5FQG1D3",
                caregivingRoundId = "01H0CZP7YKA0YPSX9JMSGWJBNW",
                caregivingRoundNumber = 1,
                startDateTime = LocalDateTime.of(2023, 5, 8, 14, 20, 0),
                receptionId = "01H0D0NB66HJ8V2YZ0XKTKBG15",
            )
            nonFirstRoundSummary = CaregivingProgressMessageSummary(
                id = "01H0HGMYED6NNRS8GQDB0N23AA",
                caregivingRoundId = "01H0CZNTEGESA5HRBF9ZP8NV4X",
                caregivingRoundNumber = 2,
                startDateTime = LocalDateTime.of(2023, 5, 13, 14, 20, 0),
                receptionId = "01H0D0NB66HJ8V2YZ0XKTKBG16",
            )
        }

        afterEach { clearAllMocks() }

        `when`("첫번째 간병 회차의 간병 시작일이 변경되면") {
            val event = relaxedMock<CaregivingRoundModified> {
                every { caregivingRoundNumber } returns 1
                every { startDateTime } returns Modification(
                    LocalDateTime.of(2023, 5, 8, 14, 20, 0),
                    LocalDateTime.of(2023, 5, 9, 14, 0, 0),
                )
            }

            fun handling() = firstRoundSummary.handleCaregivingRoundModified(event)

            then("간병 진행 메시지 발송 예정일을 변경합니다.") {
                firstRoundSummary.expectedSendingDate shouldBe LocalDate.of(2023, 5, 13)

                handling()

                firstRoundSummary.expectedSendingDate shouldBe LocalDate.of(2023, 5, 14)
            }
        }

        `when`("N회차 간병 회차의 간병 시작일이 변경되면") {
            val event = relaxedMock<CaregivingRoundModified> {
                every { caregivingRoundNumber } returns 2
                every { startDateTime } returns Modification(
                    LocalDateTime.of(2023, 5, 13, 14, 20, 0),
                    LocalDateTime.of(2023, 5, 15, 14, 0, 0),
                )
            }

            fun handling() = nonFirstRoundSummary.handleCaregivingRoundModified(event)

            then("간병 진행 메시지 발송 예정일을 변경합니다.") {
                nonFirstRoundSummary.expectedSendingDate shouldBe LocalDate.of(2023, 5, 23)

                handling()

                nonFirstRoundSummary.expectedSendingDate shouldBe LocalDate.of(2023, 5, 25)
            }
        }

        `when`("간병 상태만 변경되고 간병 시작일이 변경되지 않았다면") {
            val event = relaxedMock<CaregivingRoundModified> {
                every { caregivingRoundNumber } returns 2
                every { startDateTime } returns Modification(
                    LocalDateTime.of(2023, 5, 13, 14, 20, 0),
                    LocalDateTime.of(2023, 5, 13, 16, 0, 0),
                )
                every { caregivingProgressingStatus } returns Modification(
                    CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS,
                    CaregivingProgressingStatus.COMPLETED,
                )
            }

            fun handling() = nonFirstRoundSummary.handleCaregivingRoundModified(event)

            then("간병 상태만 변경되고 진행 메시지 발송 예정일은 변경되지 않습니다.") {
                nonFirstRoundSummary.expectedSendingDate shouldBe LocalDate.of(2023, 5, 23)
                nonFirstRoundSummary.caregivingProgressingStatus shouldBe CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS

                handling()

                nonFirstRoundSummary.expectedSendingDate shouldBe LocalDate.of(2023, 5, 23)
                nonFirstRoundSummary.caregivingProgressingStatus shouldBe CaregivingProgressingStatus.COMPLETED
            }
        }

        `when`("간병 진행 메시지 발송 성공으로 기록을 갱신하면") {
            fun behavior() = firstRoundSummary.updateSendingResult(
                SendingStatus.SENT,
                LocalDateTime.of(2023, 5, 13, 17, 30, 0)
            )

            then("최신 발송 성공일이 발송일로 기록됩니다.") {
                behavior()

                firstRoundSummary.sentDate shouldBe LocalDate.of(2023, 5, 13)
            }
        }

        `when`("간병 진행 메시지 발송 실패으로 기록을 갱신하면") {
            fun behavior() = firstRoundSummary.updateSendingResult(
                SendingStatus.FAILED,
                null
            )

            then("최신 발송 성공일이 갱신되지 않습니다.") {
                behavior()

                firstRoundSummary.sentDate shouldBe null
            }
        }

        `when`("간병 진행 메시지 발송 성공으로 기록 후 발송 실패로 기록을 다시 갱신하면") {
            fun sentBehavior() = firstRoundSummary.updateSendingResult(
                SendingStatus.SENT,
                LocalDateTime.of(2023, 5, 13, 17, 30, 0)
            )
            fun failedBehavior() = firstRoundSummary.updateSendingResult(
                SendingStatus.FAILED,
                null
            )

            then("최신 발송 상태가 실패로 기록됩니다.") {
                sentBehavior()
                failedBehavior()

                firstRoundSummary.sendingStatus shouldBe SendingStatus.FAILED
            }

            then("최신 발송 성공일이 갱신되지 않습니다.") {
                sentBehavior()
                failedBehavior()

                firstRoundSummary.sentDate shouldBe LocalDate.of(2023, 5, 13)
            }
        }
    }
})
