package kr.caredoc.careinsurance.web.user.response

import java.time.OffsetDateTime

data class DetailCaregivingResponse(
    val id: String,
    val userId: String,
    val email: String,
    val name: String,
    val nickname: String,
    val phoneNumber: String,
    val lastLoginDateTime: OffsetDateTime,
    val suspended: Boolean,
    val role: String,
    val remarks: String,
)
