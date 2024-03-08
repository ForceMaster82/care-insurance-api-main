package kr.caredoc.careinsurance.web.agency.response

import java.time.OffsetDateTime

data class ExternalCaregivingManagerResponse(
    val id: String,
    val externalCaregivingOrganizationId: String,
    val email: String,
    val name: String,
    val lastLoginDateTime: OffsetDateTime,
    val suspended: Boolean,
)
