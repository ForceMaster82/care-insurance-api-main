package kr.caredoc.careinsurance.reception.modification

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import kr.caredoc.careinsurance.caregiving.CaregivingChargeModified
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.security.accesscontrol.SubjectAttribute
import java.time.LocalDateTime

class CaregivingChargeModificationSummaryTest : BehaviorSpec({
    given("간병비 산정 수정 개요가 주어졌을때") {
        lateinit var summary: CaregivingChargeModificationSummary

        beforeEach {
            summary = CaregivingChargeModificationSummary(
                id = "01GYS8E7J23SHM0X1V3DF32DGC",
                receptionId = "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
            )
        }

        afterEach { /* nothing to do */ }

        `when`("간병비 산정이 수정되었음이 확인되면") {
            val event = relaxedMock<CaregivingChargeModified>()

            beforeEach {
                with(event) {
                    every { editingSubject[SubjectAttribute.USER_ID] } returns setOf("01GDYB3M58TBBXG1A0DJ1B866V")
                    every { calculatedDateTime } returns LocalDateTime.of(2023, 4, 16, 12, 3, 4)
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = summary.handleCaregivingChargeModified(event)

            then("간병 개요를 수정합니다.") {
                handling()

                summary.lastModifierId shouldBe "01GDYB3M58TBBXG1A0DJ1B866V"
                summary.lastModifiedDateTime shouldBe LocalDateTime.of(2023, 4, 16, 12, 3, 4)
                summary.modificationCount shouldBe 1
            }
        }
    }
})
