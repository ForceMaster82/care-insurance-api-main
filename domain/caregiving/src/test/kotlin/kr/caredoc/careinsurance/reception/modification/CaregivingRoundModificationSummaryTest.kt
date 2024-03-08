package kr.caredoc.careinsurance.reception.modification

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import kr.caredoc.careinsurance.caregiving.CaregivingRoundModified
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.security.accesscontrol.SubjectAttribute
import java.time.LocalDateTime

class CaregivingRoundModificationSummaryTest : BehaviorSpec({
    given("간병비 산정 수정 개요가 주어졌을때") {
        lateinit var summary: CaregivingRoundModificationSummary

        beforeEach {
            summary = CaregivingRoundModificationSummary(
                id = "01GYS8E7J23SHM0X1V3DF32DGC",
                receptionId = "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
            )
        }

        afterEach { /* nothing to do */ }

        `when`("간병 회차가 수정되었음이 확인되면") {
            val event = relaxedMock<CaregivingRoundModified>()

            beforeEach {
                with(event) {
                    every { editingSubject[SubjectAttribute.USER_ID] } returns setOf("01GDYB3M58TBBXG1A0DJ1B866V")
                    every { modifiedDateTime } returns LocalDateTime.of(2023, 4, 16, 12, 3, 4)
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = summary.handleCaregivingRoundModified(event)

            then("간병 회차 수정 개요를 수정합니다.") {
                handling()

                summary.lastModifierId shouldBe "01GDYB3M58TBBXG1A0DJ1B866V"
                summary.lastModifiedDateTime shouldBe LocalDateTime.of(2023, 4, 16, 12, 3, 4)
                summary.modificationCount shouldBe 1
            }
        }

        `when`("간병 회차가 수정되었음이 확인되었으나 그 주체를 특정할 수 없다면") {
            val event = relaxedMock<CaregivingRoundModified>()

            beforeEach {
                with(event) {
                    every { editingSubject[SubjectAttribute.USER_ID] } returns setOf()
                    every { modifiedDateTime } returns LocalDateTime.of(2023, 4, 16, 12, 3, 4)
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = summary.handleCaregivingRoundModified(event)

            then("아무런 수정도 하지 않습니다.") {
                handling()

                summary.lastModifierId shouldBe null
                summary.lastModifiedDateTime shouldBe null
                summary.modificationCount shouldBe 0
            }
        }
    }
})
