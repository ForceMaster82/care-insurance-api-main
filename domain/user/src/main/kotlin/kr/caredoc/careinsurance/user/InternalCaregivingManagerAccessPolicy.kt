package kr.caredoc.careinsurance.user

import kr.caredoc.careinsurance.security.accesscontrol.AccessPolicy
import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.DenyExpiredCredentialPolicy
import kr.caredoc.careinsurance.security.accesscontrol.DenyTemporalAuthenticationPolicy
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import org.springframework.security.access.AccessDeniedException

object InternalCaregivingManagerAccessPolicy : AccessPolicy {
    override fun check(sub: Subject, act: Action, obj: Object) {
        val actionTypes = act[ActionAttribute.ACTION_TYPE]
        if (actionTypes.contains(ActionType.CREATE)) {
            DenyTemporalAuthenticationPolicy.check(sub, act, obj)
            DenyExpiredCredentialPolicy.check(sub, act, obj)
            controlCreateAccess(sub)
        }
        if (actionTypes.contains(ActionType.MODIFY)) {
            DenyTemporalAuthenticationPolicy.check(sub, act, obj)
            DenyExpiredCredentialPolicy.check(sub, act, obj)
            controlModifyAccess(sub)
        }
        if (actionTypes.contains(ActionType.READ_ALL)) {
            DenyTemporalAuthenticationPolicy.check(sub, act, obj)
            DenyExpiredCredentialPolicy.check(sub, act, obj)
            controlReadAllAccess(sub)
        }
        if (actionTypes.contains(ActionType.READ_ONE)) {
            controlReadOneAccess(sub, obj)
        }
    }

    private fun controlReadAllAccess(sub: Subject) {
        if (!sub.isInternalUser) {
            throw AccessDeniedException("InternalCaregivingManager 를 조회하기 위해선 내부 사용자 권한이 필요합니다.")
        }
    }

    private fun controlCreateAccess(sub: Subject) {
        if (!sub.isInternalUser) {
            throw AccessDeniedException("InternalCaregivingManager 를 생성하기 위해선 내부 사용자 권한이 필요합니다.")
        }
    }

    private fun controlReadOneAccess(sub: Subject, obj: Object) {
        if (sub.isInternalUser) {
            return
        }

        if (sub.isOwning(obj)) {
            return
        }

        throw AccessDeniedException("본인 외의 InternalCaregivingManager 를 조회하기 위해선 내부 사용자 권한이 필요합니다.")
    }

    private fun controlModifyAccess(sub: Subject) {
        if (!sub.isInternalUser) {
            throw AccessDeniedException("InternalCaregivingManager 를 수정하기 위해선 내부 사용자 권한이 필요합니다.")
        }
    }
}
