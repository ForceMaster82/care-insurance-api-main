package kr.caredoc.careinsurance.security.accesscontrol

object ReadOneAccess : Action {
    override fun get(attribute: ActionAttribute) = when (attribute) {
        ActionAttribute.ACTION_TYPE -> setOf(ActionType.READ_ONE)
        else -> setOf()
    }
}
