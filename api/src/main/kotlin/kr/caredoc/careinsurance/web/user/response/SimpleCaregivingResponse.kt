package kr.caredoc.careinsurance.web.user.response

import java.time.OffsetDateTime

data class SimpleCaregivingResponse(
    val id: String,
    val userId: String,
    val email: String,
    val name: String,
    val nickname: String,
    val phoneNumber: String,
    val lastLoginDateTime: OffsetDateTime,
    val suspended: Boolean,
)
