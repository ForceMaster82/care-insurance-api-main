package kr.caredoc.careinsurance.user

import kr.caredoc.careinsurance.security.accesscontrol.Subject
import java.time.LocalDateTime

data class ExternalCaregivingManagerGenerated(
    val externalCaregivingManagerId: String,
    val userId: String,
    val grantedDateTime: LocalDateTime,
    val subject: Subject,
)
