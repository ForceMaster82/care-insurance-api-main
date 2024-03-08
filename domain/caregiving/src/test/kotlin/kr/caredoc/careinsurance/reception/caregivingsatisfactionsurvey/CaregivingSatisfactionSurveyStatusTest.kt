package kr.caredoc.careinsurance.reception.caregivingsatisfactionsurvey

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import kr.caredoc.careinsurance.caregiving.LastCaregivingRoundModified
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.relaxedMock
import java.time.LocalDate
import java.time.LocalDateTime

class CaregivingSatisfactionSurveyStatusTest : BehaviorSpec({
    given("간병 만족도 조사 상태가 주어졌을때") {
        val surveyStatus = CaregivingSatisfactionSurveyStatus(
            id = "01H0S82JVG8DRCRNHCGAM092RW",
            receptionId = "01GVFY259Y6Z3Y5TZRVTAQD8T0",
            caregivingRoundId = "01GVD2HS5FMX9012BN28VHDPW3",
            caregivingRoundEndDate = LocalDate.of(2023, 1, 2),
        )

        `when`("마지막 간병의 종료일이 수정되면") {
            val event = relaxedMock<LastCaregivingRoundModified>()

            beforeEach {
                with(event) {
                    every { receptionId } returns "01GVFY259Y6Z3Y5TZRVTAQD8T0"
                    every { lastCaregivingRoundId } returns "01GVD2HS5FMX9012BN28VHDPW3"
                    every { endDateTime } returns Modification(
                        LocalDateTime.of(2023, 1, 2, 13, 21, 17),
                        LocalDateTime.of(2023, 1, 3, 13, 21, 17),
                    )
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = surveyStatus.handleLastCaregivingRoundModified(event)

            then("간병 만족도 조사의 발송 예정일을 갱신합니다.") {
                surveyStatus.expectedSendingDate shouldBe LocalDate.of(2023, 1, 3)

                handling()

                surveyStatus.expectedSendingDate shouldBe LocalDate.of(2023, 1, 4)
            }
        }
    }
})
