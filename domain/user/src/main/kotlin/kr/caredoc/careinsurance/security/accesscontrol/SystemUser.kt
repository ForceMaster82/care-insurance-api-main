package kr.caredoc.careinsurance.security.accesscontrol

import kr.caredoc.careinsurance.user.UserType

object SystemUser : Subject {
    override fun get(attribute: SubjectAttribute) = when (attribute) {
        SubjectAttribute.USER_TYPE -> setOf(UserType.SYSTEM)
        else -> setOf()
    }
}
