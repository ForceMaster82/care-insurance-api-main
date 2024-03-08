package kr.caredoc.careinsurance.security.accesscontrol

fun Action.isScopedOrganizationWithIn(organizationIds: Set<String>): Boolean {
    return this[ActionAttribute.SCOPED_ORGANIZATION_ID].subtract(organizationIds).isEmpty()
}
