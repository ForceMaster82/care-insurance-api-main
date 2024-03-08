package kr.caredoc.careinsurance.reception

import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Embeddable

@Embeddable
data class RegisterManagerInfo(
    @Access(AccessType.FIELD)
    val managingUserId: String,
)
