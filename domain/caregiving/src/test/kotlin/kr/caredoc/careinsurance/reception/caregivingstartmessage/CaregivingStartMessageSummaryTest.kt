package kr.caredoc.careinsurance.reception.caregivingstartmessage

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import kr.caredoc.careinsurance.caregiving.CaregivingRoundModified
import kr.caredoc.careinsurance.message.SendingStatus
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.relaxedMock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
class CaregivingStartMessageSummaryTest(
    @Autowired
    private val cacheCaregivingStartMessageSummaryRepository: CaregivingStartMessageSummaryRepository,
) : BehaviorSpec({
    given("간병 시작 메시지 요약이 주어졌을때") {
        lateinit var summary: CaregivingStartMessageSummary

        beforeEach {
            summary = CaregivingStartMessageSummary(
                id = "01H04AN2EMMFX29V3DSB3J4D4K",
                receptionId = "01GWK30517ZTHWDW1QQ22V6QZC",
                firstCaregivingRoundId = "01H047ASNAG016RRP89C5A7F57",
                caregivingRoundStartDate = LocalDate.of(2022, 1, 29),
            )
        }

        afterEach { clearAllMocks() }

        `when`("간병 시작일이 변경되면") {
            val event = relaxedMock<CaregivingRoundModified> {
                every { startDateTime } returns Modification(
                    LocalDateTime.of(2022, 1, 29, 14, 51, 20),
                    LocalDateTime.of(2022, 1, 28, 13, 0, 0),
                )
            }

            fun handling() = summary.handleCaregivingRoundModified(event)

            then("간병 시작 메시지 발송 예정일을 변경합니다.") {
                summary.expectedSendingDate shouldBe LocalDate.of(2022, 1, 30)

                handling()

                summary.expectedSendingDate shouldBe LocalDate.of(2022, 1, 29)
            }
        }

        `when`("간병이 변경되었으나 간병 시작일이 변경되지 않았다면") {
            val event = relaxedMock<CaregivingRoundModified> {
                every { startDateTime } returns Modification(
                    LocalDateTime.of(2022, 1, 29, 14, 51, 20),
                    LocalDateTime.of(2022, 1, 29, 13, 0, 0),
                )
            }

            fun handling() = summary.handleCaregivingRoundModified(event)

            then("메시지 발송 변경일이 변경되지 않습니다.") {
                summary.expectedSendingDate shouldBe LocalDate.of(2022, 1, 30)

                handling()

                summary.expectedSendingDate shouldBe LocalDate.of(2022, 1, 30)
            }
        }

        `when`("간병 시작 메시지 발송 성공으로 기록을 갱신하면") {
            fun behavior() = summary.updateSendingResult(SendingStatus.SENT, LocalDateTime.of(2022, 1, 30, 16, 30, 0))

            then("최근 발송 상태가 성공으로 기록됩니다.") {
                behavior()

                summary.sendingStatus shouldBe SendingStatus.SENT
            }

            then("최근 발송 성공일이 발송일로 기록됩니다.") {
                behavior()

                summary.sentDate shouldBe LocalDate.of(2022, 1, 30)
            }
        }

        `when`("간병 시작 메시지 발송 실패로 기록을 갱신하면") {
            fun behavior() = summary.updateSendingResult(SendingStatus.FAILED, null)

            then("최근 발송 상태가 실패로 기록됩니다.") {
                behavior()

                summary.sendingStatus shouldBe SendingStatus.FAILED
            }

            then("최근 발송 성공일이 갱신되지 않습니다.") {
                behavior()

                summary.sentDate shouldBe null
            }
        }

        `when`("간병 시작 메시지 발송 성공으로 기록 후 발송 실패로 기록을 다시 갱신하면") {
            fun sentBehavior() = summary.updateSendingResult(SendingStatus.SENT, LocalDateTime.of(2022, 1, 30, 16, 30, 0))
            fun failedBehavior() = summary.updateSendingResult(SendingStatus.FAILED, null)

            then("최근 발송 상태가 실패로 기록됩니다.") {
                sentBehavior()
                failedBehavior()

                summary.sendingStatus shouldBe SendingStatus.FAILED
            }

            then("최근 발송 성공일이 갱신되지 않습니다.") {
                sentBehavior()
                failedBehavior()

                summary.sentDate shouldBe LocalDate.of(2022, 1, 30)
            }
        }

        and("엔티티 테스트할 때") {
            val caregivingStartMessageSummary = CaregivingStartMessageSummary(
                id = "01H04AN2EMMFX29V3DSB3J4D4K",
                receptionId = "01GWK30517ZTHWDW1QQ22V6QZC",
                firstCaregivingRoundId = "01H047ASNAG016RRP89C5A7F57",
                caregivingRoundStartDate = LocalDate.of(2022, 1, 29),
            )
            `when`("저장을 요청하면") {
                fun behavior() = cacheCaregivingStartMessageSummaryRepository.save(caregivingStartMessageSummary)
                then("저장이 됩니다.") {
                    behavior()
                }
            }
            `when`("조회를 요청하면") {
                fun behavior() = cacheCaregivingStartMessageSummaryRepository.findByIdOrNull("01H04AN2EMMFX29V3DSB3J4D4K")
                then("조회가 됩니다.") {
                    behavior()
                }
            }
        }
    }
})
