package kr.caredoc.careinsurance.coverage

import kr.caredoc.careinsurance.security.accesscontrol.AccessPolicy
import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.DenyExpiredCredentialPolicy
import kr.caredoc.careinsurance.security.accesscontrol.DenyTemporalAuthenticationPolicy
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.user.isInternalUser
import org.springframework.security.access.AccessDeniedException

object CoverageAccessPolicy : AccessPolicy {
    override fun check(sub: Subject, act: Action, obj: Object) {
        DenyTemporalAuthenticationPolicy.check(sub, act, obj)
        DenyExpiredCredentialPolicy.check(sub, act, obj)
        val actionTypes = act[ActionAttribute.ACTION_TYPE]

        // 단일 조회는 모든 사용자에게 허가됩니다.
        if (actionTypes.contains(ActionType.READ_ALL)) {
            controlReadAllAccess(sub)
        }
        if (actionTypes.contains(ActionType.CREATE)) {
            controlCreateAccess(sub)
        }
        if (actionTypes.contains(ActionType.MODIFY)) {
            controlModificationAccess(sub)
        }
    }

    private fun controlModificationAccess(sub: Subject) {
        if (!sub.isInternalUser) {
            throw AccessDeniedException("Coverage 를 수정하기 위해선 내부 사용자 권한이 필요합니다.")
        }
    }

    private fun controlReadAllAccess(sub: Subject) {
        if (!sub.isInternalUser) {
            throw AccessDeniedException("모든 Coverage 를 확인하기 위해선 내부 사용자 권한이 필요합니다.")
        }
    }

    private fun controlCreateAccess(sub: Subject) {
        if (!sub.isInternalUser) {
            throw AccessDeniedException("Coverage 를 생성하기 위해선 내부 사용자 권한이 필요합니다.")
        }
    }
}
