package kr.caredoc.careinsurance.reception.history

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import kr.caredoc.careinsurance.reception.ReceptionModified
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.security.accesscontrol.SubjectAttribute
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDateTime

@SpringBootTest
class ReceptionModificationSummaryTest(
    @Autowired
    private val cacheReceptionModificationSummaryRepository: ReceptionModificationSummaryRepository,
) : BehaviorSpec({
    given("접수 수정 개요가 주어졌을때") {
        lateinit var summary: ReceptionModificationSummary

        beforeEach {
            summary = ReceptionModificationSummary(
                id = "01GZE09S6RYR6J9WH262Z5EPE1",
                receptionId = "01GYXEQ9FS8K3PCFXWG09FM0F1"
            )
        }

        afterEach { /* nothing to do */ }

        `when`("접수가 수정되었음이 확인되면") {
            val event = relaxedMock<ReceptionModified>()

            beforeEach {
                with(event) {
                    every { editingSubject[SubjectAttribute.USER_ID] } returns setOf("01GZE0F4WX680E9VK0YYPS8A6K")
                    every { modifiedDateTime } returns LocalDateTime.of(2023, 5, 2, 19, 37, 22)
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = summary.handleReceptionModified(event)

            then("접수 수정 개요를 수정합니다.") {
                handling()

                summary.lastModifierId shouldBe "01GZE0F4WX680E9VK0YYPS8A6K"
                summary.lastModifiedDateTime shouldBe LocalDateTime.of(2023, 5, 2, 19, 37, 22)
                summary.modificationCount shouldBe 1
            }
        }

        `when`("접수가 수정되었음이 확인되었으나 그 주체를 특정할 수 없다면") {
            val event = relaxedMock<ReceptionModified>()

            beforeEach {
                with(event) {
                    every { editingSubject[SubjectAttribute.USER_ID] } returns setOf()
                    every { modifiedDateTime } returns LocalDateTime.of(2023, 5, 2, 19, 37, 22)
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = summary.handleReceptionModified(event)

            then("아무런 수정도 하지 않습니다.") {
                handling()

                summary.lastModifierId shouldBe null
                summary.lastModifiedDateTime shouldBe null
                summary.modificationCount shouldBe 0
            }
        }

        and("엔티티 테스트할 때") {
            val receptionModificationSummary = ReceptionModificationSummary(
                id = "01HF3W6YBHPK13VJ76S5RA9VKP",
                receptionId = "01HF3W779VYCWKFKQ4VY8QB6C8",
            )

            `when`("저장을 요청하면") {
                fun behavior() = cacheReceptionModificationSummaryRepository.save(receptionModificationSummary)
                then("저장이 됩니다.") {
                    behavior()
                }
            }

            `when`("조회를 요청하면") {
                fun behavior() = cacheReceptionModificationSummaryRepository.findByIdOrNull("01HF3W6YBHPK13VJ76S5RA9VKP")
                then("조회가 됩니다.") {
                    behavior()
                }
            }
        }
    }
})
