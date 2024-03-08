package kr.caredoc.careinsurance.user

import kr.caredoc.careinsurance.security.accesscontrol.AccessPolicy
import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.DenyExpiredCredentialPolicy
import kr.caredoc.careinsurance.security.accesscontrol.DenyTemporalAuthenticationPolicy
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.ObjectAttribute
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import org.springframework.security.access.AccessDeniedException

object ExternalCaregivingManagerAccessPolicy : AccessPolicy {
    override fun check(sub: Subject, act: Action, obj: Object) {
        val actionTypes = act[ActionAttribute.ACTION_TYPE]
        if (actionTypes.contains(ActionType.CREATE)) {
            DenyTemporalAuthenticationPolicy.check(sub, act, obj)
            DenyExpiredCredentialPolicy.check(sub, act, obj)
            controlCreateAccess(sub)
        }
        if (actionTypes.contains(ActionType.READ_ONE)) {
            controlReadOneAccess(sub, obj)
        }
        if (actionTypes.contains(ActionType.MODIFY)) {
            DenyTemporalAuthenticationPolicy.check(sub, act, obj)
            DenyExpiredCredentialPolicy.check(sub, act, obj)
            controlModifyAccess(sub, obj)
        }
        if (actionTypes.contains(ActionType.READ_ALL)) {
            DenyTemporalAuthenticationPolicy.check(sub, act, obj)
            DenyExpiredCredentialPolicy.check(sub, act, obj)
            controlReadAllAccess(sub)
        }
    }

    private fun controlCreateAccess(sub: Subject) {
        if (!sub.isInternalUser) {
            throw AccessDeniedException("ExternalCaregivingManager를 생성하기 위해서 내부 사용자 권한이 필요합니다.")
        }
    }

    private fun controlReadOneAccess(sub: Subject, obj: Object) {
        if (sub.isInternalUser) {
            return
        }

        if (sub.isBelongToOrganizationIn(obj[ObjectAttribute.BELONGING_ORGANIZATION_ID])) {
            return
        }

        if (sub.isOwning(obj)) {
            return
        }

        throw AccessDeniedException("ExternalCaregivingManager 조회하기 위해서 내부 사용자 권한 혹은 같은 조직에 속한 사용자 권한이 필요합니다.")
    }

    private fun controlModifyAccess(sub: Subject, obj: Object) {
        if (sub.isInternalUser) {
            return
        }
        if (sub.isOwning(obj)) {
            return
        }

        throw AccessDeniedException("ExternalCaregivingManager 수정하기 위해서 내부 사용자 권한이 필요합니다.")
    }

    private fun controlReadAllAccess(sub: Subject) {
        if (sub.isInternalUser) {
            return
        }

        throw AccessDeniedException("ExternalCaregivingManager 목록을 조회하기 위해서 내부 사용자 권한이 필요합니다.")
    }
}
