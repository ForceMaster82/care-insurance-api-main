package kr.caredoc.careinsurance.security.accesscontrol

object ModifyingAccess : Action {
    override fun get(attribute: ActionAttribute) = when (attribute) {
        ActionAttribute.ACTION_TYPE -> setOf(ActionType.MODIFY)
        else -> setOf()
    }
}
