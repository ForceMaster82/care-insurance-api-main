package kr.caredoc.careinsurance.reception.caregivingsatisfactionsurvey

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

object CaregivingSatisfactionSurveyStatusAccessPolicy : AccessPolicy {
    override fun check(sub: Subject, act: Action, obj: Object) {
        DenyTemporalAuthenticationPolicy.check(sub, act, obj)
        DenyExpiredCredentialPolicy.check(sub, act, obj)

        val actionTypes = act[ActionAttribute.ACTION_TYPE]

        if (actionTypes.contains(ActionType.READ_ALL)) {
            denyIfNotInternalUser(sub)
        } else if (actionTypes.contains(ActionType.CREATE)) {
            denyIfNotInternalUser(sub)
        } else if (actionTypes.contains(ActionType.MODIFY)) {
            denyIfNotInternalUser(sub)
        } else {
            throw AccessDeniedException("허용되지 않은 행위입니다.")
        }
    }

    private fun denyIfNotInternalUser(sub: Subject) {
        if (!sub.isInternalUser) {
            throw AccessDeniedException("CaregivingSatisfactionSurveyStatus에 접근하기 위해선 내부 사용자 권한이 필요합니다.")
        }
    }
}
