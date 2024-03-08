package kr.caredoc.careinsurance.reception

import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
data class CaregivingManagerInfo(
    @Enumerated(EnumType.STRING)
    val organizationType: OrganizationType,
    val organizationId: String?,
    val managingUserId: String,
)
