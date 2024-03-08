package kr.caredoc.careinsurance.aws.ses

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.types.shouldBeTypeOf

class SesConfigTest : BehaviorSpec({
    given("SES 설정이 주어졌을때") {
        val config = SesConfig()
        `when`("SES 이메일 클라이언트 빈을 요구하면") {
            fun behavior() = config.sesEmailClient()

            then("AmazonSimpleEmailService 구현체를 구성한다.") {
                val actualResult = behavior()

                actualResult.shouldBeTypeOf<AmazonSimpleEmailServiceClient>()
            }
        }
    }
})
