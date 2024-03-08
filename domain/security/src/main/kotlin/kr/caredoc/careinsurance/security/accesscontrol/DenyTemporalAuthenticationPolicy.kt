package kr.caredoc.careinsurance.security.accesscontrol

import kr.caredoc.careinsurance.security.authentication.AuthenticationMethod
import org.springframework.security.access.AccessDeniedException

object DenyTemporalAuthenticationPolicy : AccessPolicy {
    override fun check(sub: Subject, act: Action, obj: Object) {
        if (sub[SubjectAttribute.AUTHENTICATION_METHOD].contains(AuthenticationMethod.TEMPORAL_CODE.name)) {
            throw AccessDeniedException("임시 인증 권한으로는 접근할 수 없습니다.")
        }
    }
}
