package kr.caredoc.careinsurance.reconciliation.statistics

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

object ReconciliationStatisticsAccessPolicy : AccessPolicy {
    override fun check(sub: Subject, act: Action, obj: Object) {
        DenyTemporalAuthenticationPolicy.check(sub, act, obj)
        DenyExpiredCredentialPolicy.check(sub, act, obj)

        val actionType = act[ActionAttribute.ACTION_TYPE]

        if (actionType.contains(ActionType.READ_ALL)) {
            denyIfNotInternalUser(sub)
        } else {

            throw AccessDeniedException("허용되지 않은 행위입니다.")
        }
    }

    private fun denyIfNotInternalUser(sub: Subject) {
        if (sub.isInternalUser) {
            return
        }

        throw AccessDeniedException("월별 대사 통계를 조회하기 위해서는 내부 사용자 권한이 필요합니다.")
    }
}
