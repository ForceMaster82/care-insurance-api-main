package kr.caredoc.careinsurance.user

import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Entity
import kr.caredoc.careinsurance.BaseEntity
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.ObjectAttribute
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.security.accesscontrol.SubjectAttribute

@Entity
class ExternalCaregivingManager(
    id: String,
    email: String,
    name: String,
    phoneNumber: String,
    remarks: String? = null,
    externalCaregivingOrganizationId: String,
    @Access(AccessType.FIELD)
    val userId: String,
) : BaseEntity(id), Object, Subject {
    var email: String = email
        protected set
    var name: String = name
        protected set
    var phoneNumber: String = phoneNumber
        protected set
    var remarks: String? = remarks
        protected set
    var externalCaregivingOrganizationId: String = externalCaregivingOrganizationId
        protected set

    override fun get(attribute: SubjectAttribute): Set<String> = when (attribute) {
        SubjectAttribute.USER_TYPE -> setOf(UserType.EXTERNAL)
        SubjectAttribute.ORGANIZATION_ID -> setOf(externalCaregivingOrganizationId)
        else -> setOf()
    }

    override fun get(attribute: ObjectAttribute): Set<String> = when (attribute) {
        ObjectAttribute.OWNER_ID -> setOf(userId)
        ObjectAttribute.BELONGING_ORGANIZATION_ID -> setOf(externalCaregivingOrganizationId)
        else -> setOf()
    }

    fun edit(command: ExternalCaregivingManagerEditCommand) {
        ExternalCaregivingManagerAccessPolicy.check(command.subject, command, this)
        this.email = command.email.compareWith(this.email).valueToOverwrite
        this.name = command.name.compareWith(this.name).valueToOverwrite
        this.phoneNumber = command.phoneNumber.compareWith(this.phoneNumber).valueToOverwrite
        this.remarks = command.remarks.compareWith(this.remarks).valueToOverwrite
        this.externalCaregivingOrganizationId =
            command.externalCaregivingOrganizationId.compareWith(this.externalCaregivingOrganizationId).valueToOverwrite
    }
}
