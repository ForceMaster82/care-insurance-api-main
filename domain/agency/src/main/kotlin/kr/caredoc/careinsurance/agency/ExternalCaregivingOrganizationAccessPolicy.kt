package kr.caredoc.careinsurance.agency

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
import kr.caredoc.careinsurance.user.UserType
import kr.caredoc.careinsurance.user.isBelongToOrganizationIn
import kr.caredoc.careinsurance.user.isInternalUser
import kr.caredoc.careinsurance.user.isSystem
import org.springframework.security.access.AccessDeniedException

object ExternalCaregivingOrganizationAccessPolicy : AccessPolicy {
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

        if (actionTypes.contains(ActionType.READ_ALL)) {
            DenyExpiredCredentialPolicy.check(sub, act, obj)
            controlReadAllAccess(sub)
        }

        if (actionTypes.contains(ActionType.MODIFY)) {
            DenyTemporalAuthenticationPolicy.check(sub, act, obj)
            DenyExpiredCredentialPolicy.check(sub, act, obj)
            controlModifyAccess(sub)
        }
    }

    private fun controlCreateAccess(sub: Subject) {
        if (!sub.isInternalUser) {
            throw AccessDeniedException("ExternalCaregivingOrganization 을 생성하기 위해선 내부 사용자 권한이 필요합니다.")
        }
    }

    private fun controlReadOneAccess(sub: Subject, obj: Object) {
        if (sub.isInternalUser || sub.isSystem) {
            return
        }

        if (sub.isBelongToOrganizationIn(obj[ObjectAttribute.ID])) {
            return
        }

        throw AccessDeniedException("ExternalCaregivingOrganization 을 조회하기 위해선 내부 사용자 권한 혹은 시스템 권한 혹은 해당 업체에 소속된 간병 관리자 권한이 필요합니다.")
    }

    private fun controlReadAllAccess(sub: Subject) {
        if (!sub.isInternalUser) {
            throw AccessDeniedException("ExternalCaregivingOrganization 을 조회하기 위해선 내부 사용자 권한이 필요합니다.")
        }
    }

    private fun controlModifyAccess(sub: Subject) {
        if (!sub[SubjectAttribute.USER_TYPE].contains(UserType.INTERNAL)) {
            throw AccessDeniedException("ExternalCaregivingOrganization 을 수정하기 위해선 내부 사용자 권한이 필요합니다.")
        }
    }
}
