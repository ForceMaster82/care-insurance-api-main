package kr.caredoc.careinsurance.web.agency.response

import java.time.OffsetDateTime

data class DetailExternalCaregivingManagerResponse(
    val id: String,
    val email: String,
    val name: String,
    val phoneNumber: String,
    val remarks: String?,
    val lastLoginDateTime: OffsetDateTime,
    val suspended: Boolean,
    val externalCaregivingOrganizationId: String,
)
