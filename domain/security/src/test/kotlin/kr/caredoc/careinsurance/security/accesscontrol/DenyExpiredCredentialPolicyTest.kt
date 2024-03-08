package kr.caredoc.careinsurance.security.accesscontrol

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import kr.caredoc.careinsurance.security.authentication.CredentialExpiredException

class DenyExpiredCredentialPolicyTest : BehaviorSpec({
    given("CREDENTIAL_EXPIRED 속성에 true를 포함한 접근 주체가 주어졌을때") {
        val subject = Subject.fromAttributes(
            SubjectAttribute.CREDENTIAL_EXPIRED to setOf("true"),
        )

        `when`("DenyExpiredCredentialPolicy를 적용하면") {
            fun behavior() = DenyExpiredCredentialPolicy.check(subject, object : Action {}, Object.Empty)

            then("CredentialExpiredException이 발생합니다.") {
                shouldThrow<CredentialExpiredException> { behavior() }
            }
        }
    }

    given("CREDENTIAL_EXPIRED 속성에 true를 포함하지 않은 접근 주체가 주어졌을때") {
        val subject = Subject.fromAttributes(
            SubjectAttribute.CREDENTIAL_EXPIRED to setOf("false"),
        )

        `when`("DenyExpiredCredentialPolicy를 적용하면") {
            fun behavior() = DenyExpiredCredentialPolicy.check(subject, object : Action {}, Object.Empty)

            then("아무일도 일어나지 않습니다.") {
                shouldNotThrowAny { behavior() }
            }
        }
    }
})
