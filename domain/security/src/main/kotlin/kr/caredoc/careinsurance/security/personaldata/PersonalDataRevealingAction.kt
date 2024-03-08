package kr.caredoc.careinsurance.security.personaldata

import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType

object PersonalDataRevealingAction : Action {
    override fun get(attribute: ActionAttribute): Set<String> {
        return if (attribute == ActionAttribute.ACTION_TYPE) {
            setOf(ActionType.REVEAL_PERSONAL_DATA)
        } else {
            setOf()
        }
    }
}
