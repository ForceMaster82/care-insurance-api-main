package kr.caredoc.careinsurance.web.user.response

import kr.caredoc.careinsurance.reception.OrganizationType
import java.time.OffsetDateTime

data class DetailUserResponse(
    val id: String,
    val name: String,
    val organizations: List<UserOrganization>,
    val lastLoginDateTime: OffsetDateTime,
) {
    data class UserOrganization(
        val organizationType: OrganizationType,
        val id: String?,
    )
}
