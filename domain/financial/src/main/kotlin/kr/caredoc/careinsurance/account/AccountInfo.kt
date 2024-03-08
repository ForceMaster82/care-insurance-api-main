package kr.caredoc.careinsurance.account

import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Embeddable

@Embeddable
data class AccountInfo(
    @Access(AccessType.FIELD)
    val bank: String? = null,
    val accountNumber: String? = null,
    val accountHolder: String? = null,
)
