package kr.caredoc.careinsurance

import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.security.accesscontrol.SubjectAttribute
import kr.caredoc.careinsurance.user.UserType

fun generateInternalCaregivingManagerSubject() = object : Subject {
    override fun get(attribute: SubjectAttribute) = when (attribute) {
        SubjectAttribute.USER_TYPE -> setOf(UserType.INTERNAL)
        else -> setOf()
    }
}

fun generateGuestSubject() = object : Subject {}

fun generateExternalCaregivingOrganizationManagerSubject(organizationId: String) = object : Subject {
    override fun get(attribute: SubjectAttribute) = when (attribute) {
        SubjectAttribute.USER_TYPE -> setOf(UserType.EXTERNAL)
        SubjectAttribute.ORGANIZATION_ID -> setOf(organizationId)
        else -> setOf()
    }
}

fun generateUserSubject(userId: String) = object : Subject {
    override fun get(attribute: SubjectAttribute): Set<String> = when (attribute) {
        SubjectAttribute.USER_ID -> setOf(userId)
        else -> setOf()
    }
}

fun generateSystemSubject() = object : Subject {
    override fun get(attribute: SubjectAttribute) = when (attribute) {
        SubjectAttribute.USER_TYPE -> setOf(UserType.SYSTEM)
        else -> setOf()
    }
}
