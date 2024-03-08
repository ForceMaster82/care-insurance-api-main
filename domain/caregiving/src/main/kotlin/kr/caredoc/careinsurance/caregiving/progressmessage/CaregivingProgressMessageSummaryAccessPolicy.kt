package kr.caredoc.careinsurance.caregiving.progressmessage

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

object CaregivingProgressMessageSummaryAccessPolicy : AccessPolicy {
    override fun check(sub: Subject, act: Action, obj: Object) {
        DenyTemporalAuthenticationPolicy.check(sub, act, obj)
        DenyExpiredCredentialPolicy.check(sub, act, obj)

        val actionTypes = act[ActionAttribute.ACTION_TYPE]

        if (actionTypes.contains(ActionType.READ_ALL)) {
            controlReadAllAccess(sub)
        }
    }

    private fun controlReadAllAccess(sub: Subject) {
        if (!sub.isInternalUser) {
            throw AccessDeniedException("간병 진행 메시지 요약에 접근하기 위해서는 내부 사용자 권한이 필요합니다.")
        }
    }
}
