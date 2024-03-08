package kr.caredoc.careinsurance.reception

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

object ReceptionAccessPolicy : AccessPolicy {
    private val INTERNAL_ONLY_PROCEEDINGS = sequenceOf(
        ReceptionProgressingStatus.CANCELED,
        ReceptionProgressingStatus.CANCELED_BY_MEDICAL_REQUEST,
        ReceptionProgressingStatus.CANCELED_BY_PERSONAL_CAREGIVER,
    ).map { it.toString() }
        .toSet()

    override fun check(sub: Subject, act: Action, obj: Object) {
        DenyTemporalAuthenticationPolicy.check(sub, act, obj)
        DenyExpiredCredentialPolicy.check(sub, act, obj)
        val actionTypes = act[ActionAttribute.ACTION_TYPE]

        if (actionTypes.contains(ActionType.CREATE)) {
            controlCreateAccess(sub)
        }
        if (actionTypes.contains(ActionType.READ_ALL)) {
            controlReadAllAccess(sub)
        }
        if (actionTypes.contains(ActionType.READ_ONE)) {
            controlReadOneAccess(sub, obj)
        }
        if (actionTypes.contains(ActionType.MODIFY)) {
            controlModifyOneAccess(sub, act, obj)
        }
        if (actionTypes.contains(ActionType.REVEAL_PERSONAL_DATA)) {
            controlRevealPersonalDataAccess(sub, obj)
        }
    }

    private fun controlCreateAccess(sub: Subject) {
        if (!sub.isInternalUser) {
            throw AccessDeniedException("Reception 을 생성하기 위해선 내부 사용자 권한이 필요합니다.")
        }
    }

    private fun controlReadAllAccess(sub: Subject) {
        if (!sub.isInternalUser) {
            throw AccessDeniedException("Reception 을 조회하기 위해선 내부 사용자 권한이 필요합니다.")
        }
    }

    private fun controlReadOneAccess(sub: Subject, obj: Object) {
        if (hasAuthorityToRead(sub, obj)) {
            return
        }

        throw AccessDeniedException("Reception 을 조회하기 위해선 내부 사용자 권한이 필요합니다.")
    }

    private fun hasAuthorityToRead(sub: Subject, obj: Object): Boolean {
        if (sub.isInternalUser || sub.isSystem) {
            return true
        }

        return sub.isExternalUser && sub.isBelongToOrganizationIn(obj[ObjectAttribute.ASSIGNED_ORGANIZATION_ID])
    }

    private fun controlModifyOneAccess(sub: Subject, act: Action, obj: Object) {
        if (sub.isInternalUser) {
            return
        }

        if (
            sub.isExternalUser &&
            sub.isBelongToOrganizationIn(obj[ObjectAttribute.ASSIGNED_ORGANIZATION_ID]) &&
            act.notHavingInternalOnlyProceeding &&
            notContainsInternalOnlyDiff(act, obj)
        ) {
            return
        }

        throw AccessDeniedException("Reception 을 수정하기 위해선 내부 사용자 권한 혹은 할당된 간병 협회 사용자 권한이 필요합니다.")
    }

    private val Action.notHavingInternalOnlyProceeding: Boolean
        get() {
            return INTERNAL_ONLY_PROCEEDINGS.intersect(this[ActionAttribute.PROCEED_INTO]).isEmpty()
        }

    private fun notContainsInternalOnlyDiff(act: Action, obj: Object): Boolean =
        act[ActionAttribute.INTERNAL_ONLY_MODIFICATIONS]
            .subtract(obj[ObjectAttribute.INTERNAL_ONLY_MODIFIABLE_PROPERTIES])
            .isEmpty()

    private fun controlRevealPersonalDataAccess(sub: Subject, obj: Object) {
        if (hasAuthorityToRead(sub, obj)) {
            return
        }

        throw AccessDeniedException("접수에 포함된 개인정보를 조회하기 위해선 내부 사용자 혹은 해당 접수를 관리하는 권한이 필요합니다.")
    }
}
