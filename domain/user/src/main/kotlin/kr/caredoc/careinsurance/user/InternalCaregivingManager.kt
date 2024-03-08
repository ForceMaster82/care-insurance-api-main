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
class InternalCaregivingManager(
    id: String,
    @Access(AccessType.FIELD)
    val userId: String,
    name: String,
    nickname: String,
    phoneNumber: String,
    role: String,
    remarks: String? = null
) : BaseEntity(id), Object, Subject {
    override fun get(attribute: SubjectAttribute): Set<String> = when (attribute) {
        SubjectAttribute.USER_TYPE -> setOf(UserType.INTERNAL)
        else -> setOf()
    }

    override fun get(attribute: ObjectAttribute): Set<String> = when (attribute) {
        ObjectAttribute.OWNER_ID -> setOf(userId)
        else -> setOf()
    }

    var name = name
        protected set
    var nickname = nickname
        protected set
    var phoneNumber = phoneNumber
        protected set
    var role = role
        protected set
    var remarks = remarks
        protected set

    fun edit(command: InternalCaregivingManagerEditingCommand) {
        InternalCaregivingManagerAccessPolicy.check(command.subject, command, this)

        this.name = command.name.compareWith(this.name).valueToOverwrite
        this.nickname = command.nickname.compareWith(this.nickname).valueToOverwrite
        this.phoneNumber = command.phoneNumber.compareWith(this.phoneNumber).valueToOverwrite
        this.role = command.role.compareWith(this.role).valueToOverwrite
        this.remarks = command.remarks.compareWith(this.remarks).valueToOverwrite
    }
}
