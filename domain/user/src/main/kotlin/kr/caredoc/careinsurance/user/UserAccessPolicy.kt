package kr.caredoc.careinsurance.user

import kr.caredoc.careinsurance.security.accesscontrol.AccessPolicy
import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import org.springframework.security.access.AccessDeniedException

object UserAccessPolicy : AccessPolicy {
    override fun check(sub: Subject, act: Action, obj: Object) {
        val actionTypes = act[ActionAttribute.ACTION_TYPE]

        if (actionTypes.contains(ActionType.READ_ALL)) {
            controlReadAllAccess(sub)
        }
        if (actionTypes.contains(ActionType.READ_ONE)) {
            controlReadOneAccess(sub, obj)
        }
        if (actionTypes.contains(ActionType.MODIFY)) {
            controlModifyAccess(sub, obj)
        }
    }

    private fun controlReadAllAccess(sub: Subject) {
        if (!sub.isInternalUser) {
            throw AccessDeniedException("User 를 조회하기 위해선 내부 사용자 권한이 필요합니다.")
        }
    }

    private fun controlReadOneAccess(sub: Subject, obj: Object) {
        if (sub.isInternalUser) {
            return
        }

        if (sub.isOwning(obj)) {
            return
        }

        throw AccessDeniedException("User 를 조회하기 위해선 내부 사용자 권한이 필요합니다.")
    }

    private fun controlModifyAccess(sub: Subject, obj: Object) {
        if (sub.isInternalUser) {
            return
        }

        if (sub.isOwning(obj)) {
            return
        }

        throw AccessDeniedException("본인이 아닌 User 를 수정하기 위해선 내부 사용자 권한이 필요합니다.")
    }
}
