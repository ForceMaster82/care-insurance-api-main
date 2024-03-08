package kr.caredoc.careinsurance.insurance

import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Embeddable

@Embeddable
data class InsuranceManagerInfo(
    @Access(AccessType.FIELD)
    val branchName: String,
    val receptionistName: String,
    val phoneNumber: String?,
)
