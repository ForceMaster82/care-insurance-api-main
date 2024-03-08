package kr.caredoc.careinsurance.billing

import kr.caredoc.careinsurance.security.accesscontrol.AccessPolicy
import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.DenyExpiredCredentialPolicy
import kr.caredoc.careinsurance.security.accesscontrol.DenyTemporalAuthenticationPolicy
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.ObjectAttribute
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.user.isBelongToOrganizationIn
import kr.caredoc.careinsurance.user.isExternalUser
import kr.caredoc.careinsurance.user.isInternalUser
import kr.caredoc.careinsurance.user.isSystem
import org.springframework.security.access.AccessDeniedException

object BillingAccessPolicy : AccessPolicy {
    override fun check(sub: Subject, act: Action, obj: Object) {
        DenyTemporalAuthenticationPolicy.check(sub, act, obj)
        DenyExpiredCredentialPolicy.check(sub, act, obj)
        val actionTypes = act[ActionAttribute.ACTION_TYPE]
        if (actionTypes.contains(ActionType.CREATE)) {
            controlCreateAccess(sub)
        } else if (actionTypes.contains(ActionType.READ_ONE)) {
            controlReadOneAccess(sub, obj)
        } else if (actionTypes.contains(ActionType.MODIFY)) {
            controlModifyAccess(sub)
        } else if (actionTypes.contains(ActionType.READ_ALL)) {
            controlReadAllAccess(sub)
        } else {
            throw AccessDeniedException("허용되지 않은 행위입니다.")
        }
    }

    private fun controlCreateAccess(sub: Subject) {
        if (!sub.isSystem) {
            throw AccessDeniedException("Billing 을 생성하기 위해서 시스템 권한이 필요합니다.")
        }
    }

    private fun controlReadOneAccess(sub: Subject, obj: Object) {
        if (sub.isInternalUser || sub.isSystem) {
            return
        }

        if (sub.isExternalUser && sub.isBelongToOrganizationIn(obj[ObjectAttribute.ASSIGNED_ORGANIZATION_ID])) {
            return
        }

        throw AccessDeniedException("Billing 을 조회하기 위해서 내부 사용자 권한이나 시스템 권한이나 할당된 협회의 관리자 권한이 필요합니다.")
    }

    private fun controlModifyAccess(sub: Subject) {
        if (sub.isInternalUser || sub.isSystem) {
            return
        }

        throw AccessDeniedException("Billing 을 수정하기 위해서 내부 사용자 권한이나 시스템 권한이 필요합니다.")
    }

    private fun controlReadAllAccess(sub: Subject) {
        if (sub.isInternalUser) {
            return
        }

        throw AccessDeniedException("Billing 목록을 조회하기 위해서 내부 사용자 권한이 필요합니다.")
    }
}
