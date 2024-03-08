package kr.caredoc.careinsurance.user

import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.ObjectAttribute
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.security.accesscontrol.SubjectAttribute

val Subject.isInternalUser: Boolean
    get() = this[SubjectAttribute.USER_TYPE].contains(UserType.INTERNAL)

val Subject.isExternalUser: Boolean
    get() = this[SubjectAttribute.USER_TYPE].contains(UserType.EXTERNAL)

val Subject.isSystem: Boolean
    get() = this[SubjectAttribute.USER_TYPE].contains(UserType.SYSTEM)

fun Subject.isBelongToOrganizationIn(organizationIds: Collection<String>) =
    this[SubjectAttribute.ORGANIZATION_ID].intersect((organizationIds).toSet()).isNotEmpty()

fun Subject.isOwning(obj: Object) = obj[ObjectAttribute.OWNER_ID].intersect(this[SubjectAttribute.USER_ID]).isNotEmpty()
