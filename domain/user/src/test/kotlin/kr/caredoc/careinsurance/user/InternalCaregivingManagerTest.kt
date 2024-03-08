package kr.caredoc.careinsurance.user

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import kr.caredoc.careinsurance.security.accesscontrol.SubjectAttribute

class InternalCaregivingManagerTest : BehaviorSpec({
    given("internal caregiving manager") {
        val internalCaregivingManager = InternalCaregivingManager(
            id = "01GQ72A8BW8G5MA9G7CX1FKBE5",
            userId = "01GQ72AFBFD0YPD88MD2M5DCRV",
            name = "임석민",
            nickname = "보리스",
            phoneNumber = "01011112222",
            role = "케어닥 백앤드 프로그래머",
            remarks = "살날이 얼마 안남았음",
        )
        `when`("getting USER_TYPE attribute") {
            fun behavior() = internalCaregivingManager[SubjectAttribute.USER_TYPE]

            then("returns set contains INTERNAL user type") {
                val actualResult = behavior()

                actualResult shouldContain UserType.INTERNAL
            }
        }
    }
})
