package kr.caredoc.careinsurance.user

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.patch.Patches
import kr.caredoc.careinsurance.security.accesscontrol.ObjectAttribute
import kr.caredoc.careinsurance.security.accesscontrol.SubjectAttribute

class ExternalCaregivingManagerTest : BehaviorSpec({
    given("external caregiving manager") {
        val externalCaregivingManager = ExternalCaregivingManager(
            id = "01GSF0T1GVR133F39WDQFQA368",
            userId = "01GSF0TB2HAYQ79RTK4TXCD0WJ",
            email = "eddy@caredoc.kr",
            name = "eddy",
            phoneNumber = "01012345678",
            externalCaregivingOrganizationId = "01GSF0VXWB8QN9XWPJFAPG1XQ5"
        )

        `when`("getting USER_TYPE attribute") {
            fun behavior() = externalCaregivingManager[SubjectAttribute.USER_TYPE]

            then("returns set contains EXTERNAL user type") {
                val actualResult = behavior()

                actualResult shouldContain UserType.EXTERNAL
            }
        }

        `when`("ORGANIZATION_ID 접근 주체 속성을 조회하면") {
            fun behavior() = externalCaregivingManager[SubjectAttribute.ORGANIZATION_ID]

            then("외부 간병 업체 사용자의 업체 아이디를 포함한 셋을 반환한다.") {
                val actualResult = behavior()

                actualResult shouldContain "01GSF0VXWB8QN9XWPJFAPG1XQ5"
            }
        }

        `when`("BELONGING_ORGANIZATION_ID 접근 대상 속성을 조회하면") {
            fun behavior() = externalCaregivingManager[ObjectAttribute.BELONGING_ORGANIZATION_ID]

            then("외부 간병 업체 사용자의 업체 아이디를 포함한 셋을 반환한다.") {
                val actualResult = behavior()

                actualResult shouldContain "01GSF0VXWB8QN9XWPJFAPG1XQ5"
            }
        }

        `when`("external caregiving manager 를 수정합니다.") {
            val externalCaregivingManagerEditCommand = ExternalCaregivingManagerEditCommand(
                email = Patches.ofValue("jerry@caredoc.kr"),
                name = Patches.ofValue("jerry"),
                phoneNumber = Patches.ofValue("01012345678"),
                remarks = Patches.ofValue("수정 해주세요."),
                suspended = Patches.ofValue(false),
                externalCaregivingOrganizationId = Patches.ofValue("01GSF0VXWB8QN9XWPJFAPG1XQ5"),
                subject = generateInternalCaregivingManagerSubject(),
            )
            fun behavior() = externalCaregivingManager.edit(externalCaregivingManagerEditCommand)
            then("커멘드에 포함된 property 대응되는 external caregiving manager property 를 수정 합니다.") {
                behavior()

                externalCaregivingManager.email shouldBe "jerry@caredoc.kr"
                externalCaregivingManager.name shouldBe "jerry"
                externalCaregivingManager.remarks shouldBe "수정 해주세요."
            }
        }
    }
})
