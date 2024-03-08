package kr.caredoc.careinsurance.security.accesscontrol

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import kr.caredoc.careinsurance.security.authentication.AuthenticationMethod
import org.springframework.security.access.AccessDeniedException

class DenyTemporalAuthenticationPolicyTest : BehaviorSpec({
    given("ID, PW로 인증한 주체가 주어졌을때") {
        val subject = Subject.fromAttributes(
            SubjectAttribute.AUTHENTICATION_METHOD to setOf(AuthenticationMethod.ID_PW_LOGIN.name),
        )
        `when`("DenyTemporalAuthenticationPolicy로 접근제어를 컨트롤하면") {
            fun behavior() = DenyTemporalAuthenticationPolicy.check(
                subject,
                object : Action {
                    override fun get(attribute: ActionAttribute): Set<String> {
                        return setOf()
                    }
                },
                Object.Empty,
            )

            then("아무 일도 일어나지 않습니다.") {
                shouldNotThrowAny { behavior() }
            }
        }
    }

    given("임시 인증코드로 인증한 주체가 주어졌을때") {
        val subject = Subject.fromAttributes(
            SubjectAttribute.AUTHENTICATION_METHOD to setOf(AuthenticationMethod.TEMPORAL_CODE.name),
        )

        `when`("DenyTemporalAuthenticationPolicy로 접근제어를 컨트롤하면") {
            fun behavior() = DenyTemporalAuthenticationPolicy.check(
                subject,
                object : Action {
                    override fun get(attribute: ActionAttribute): Set<String> {
                        return setOf()
                    }
                },
                Object.Empty,
            )

            then("AccessDeniedException이 발생합니다.") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }
    }
})
