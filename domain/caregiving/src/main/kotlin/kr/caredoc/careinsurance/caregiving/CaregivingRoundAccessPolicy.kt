package kr.caredoc.careinsurance.caregiving

import kr.caredoc.careinsurance.security.accesscontrol.AccessPolicy
import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.DenyExpiredCredentialPolicy
import kr.caredoc.careinsurance.security.accesscontrol.DenyTemporalAuthenticationPolicy
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.ObjectAttribute
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.security.accesscontrol.SubjectAttribute
import kr.caredoc.careinsurance.security.accesscontrol.isScopedOrganizationWithIn
import kr.caredoc.careinsurance.user.isBelongToOrganizationIn
import kr.caredoc.careinsurance.user.isExternalUser
import kr.caredoc.careinsurance.user.isInternalUser
import kr.caredoc.careinsurance.user.isSystem
import org.springframework.security.access.AccessDeniedException

object CaregivingRoundAccessPolicy : AccessPolicy {
    override fun check(sub: Subject, act: Action, obj: Object) {
        DenyTemporalAuthenticationPolicy.check(sub, act, obj)
        DenyExpiredCredentialPolicy.check(sub, act, obj)
        val actionTypes = act[ActionAttribute.ACTION_TYPE]

        if (actionTypes.contains(ActionType.READ_ALL)) {
            controlReadAllAccess(sub, act)
        }
        if (actionTypes.contains(ActionType.READ_ONE)) {
            controlReadOneAccess(sub, obj)
        }
        if (actionTypes.contains(ActionType.MODIFY)) {
            controlModifyAccess(sub, obj)
        }
    }

    private fun controlReadAllAccess(sub: Subject, act: Action) {
        if (sub.isInternalUser) {
            return
        }
        if (
            sub.isExternalUser &&
            act.isScopedOrganizationWithIn(sub[SubjectAttribute.ORGANIZATION_ID])
        ) {
            return
        }
        throw AccessDeniedException("전체 간병 목록을 조회하기 위해선 내부 사용자 권한 혹은 할당된 간병 협회 사용자 권한이 필요합니다.")
    }

    private fun controlReadOneAccess(sub: Subject, obj: Object) {
        if (sub.isInternalUser) {
            return
        }
        if (
            sub.isExternalUser &&
            sub.isBelongToOrganizationIn(obj[ObjectAttribute.ASSIGNED_ORGANIZATION_ID])
        ) {
            return
        }

        if (sub.isSystem) {
            return
        }

        throw AccessDeniedException("CaregivingRound 을 조회하기 위해선 내부 사용자 권한 혹은 할당된 간병 협회 사용자 권한 혹은 시스템 권한이 필요합니다.")
    }

    private fun controlModifyAccess(sub: Subject, obj: Object) {
        if (sub.isInternalUser) {
            return
        }
        if (
            sub.isExternalUser &&
            sub.isBelongToOrganizationIn(obj[ObjectAttribute.ASSIGNED_ORGANIZATION_ID])
        ) {
            return
        }
        throw AccessDeniedException("CaregivingRound 을 수정하기 위해선 내부 사용자 권한 혹은 할당된 간병 협회 사용자 권한이 필요합니다.")
    }
}
