package kr.caredoc.careinsurance.user

import kr.caredoc.careinsurance.security.accesscontrol.Subject
import java.time.LocalDateTime

data class InternalCaregivingManagerGenerated(
    val internalCaregivingManagerId: String,
    val userId: String,
    val grantedDateTime: LocalDateTime,
    val subject: Subject,
)
