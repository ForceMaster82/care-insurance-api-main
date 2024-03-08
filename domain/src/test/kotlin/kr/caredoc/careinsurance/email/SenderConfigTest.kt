package kr.caredoc.careinsurance.email

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class SenderConfigTest : BehaviorSpec({
    given("어플리케이션 프로퍼티로부터 주입된 데이터와 SenderConfig 이 주어졌을때") {
        val config = SenderConfig(
            infoProfileAddress = "info@caredoc.kr",
        )

        `when`("발신 프로필 목록 Bean 을 요구하면") {
            fun behavior() = config.senders()

            then("주입된 데이터로부터 생성된 Senders 를 제공한다.") {
                val actualResult = behavior()

                actualResult[SenderProfile.INFO] shouldBe "info@caredoc.kr"
            }
        }
    }
})
