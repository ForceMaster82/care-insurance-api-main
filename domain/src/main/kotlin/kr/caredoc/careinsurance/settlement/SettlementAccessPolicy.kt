package kr.caredoc.careinsurance.settlement

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

object SettlementAccessPolicy : AccessPolicy {
    override fun check(sub: Subject, act: Action, obj: Object) {
        DenyTemporalAuthenticationPolicy.check(sub, act, obj)
        DenyExpiredCredentialPolicy.check(sub, act, obj)

        val actionTypes = act[ActionAttribute.ACTION_TYPE]

        if (actionTypes.contains(ActionType.READ_ONE)) {
            handleReadOneAccess(sub, obj)
        }
        if (actionTypes.contains(ActionType.MODIFY)) {
            denyIfNotInternalUser(sub)
        }
    }

    private fun handleReadOneAccess(sub: Subject, obj: Object) {
        if (sub.isInternalUser) {
            return
        }
        if (sub.isSystem) {
            return
        }

        if (sub.isExternalUser && sub.isBelongToOrganizationIn(obj[ObjectAttribute.ASSIGNED_ORGANIZATION_ID])) {
            return
        }

        throw AccessDeniedException("정산을 조회하기 위해서는 내부 사용자 권한, 혹은 할당된 협회 사용자의 권한이 필요합니다.")
    }

    private fun denyIfNotInternalUser(sub: Subject) {
        if (sub.isInternalUser) {
            return
        }
        if (sub.isSystem) {
            return
        }

        throw AccessDeniedException("정산을 조회하기 위해서는 내부 사용자 권한이 필요합니다.")
    }
}
