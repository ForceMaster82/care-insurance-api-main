package kr.caredoc.careinsurance.caregiving

import kr.caredoc.careinsurance.security.accesscontrol.Subject
import java.time.LocalDateTime

data class CaregivingRoundStarted(
    val caregivingRoundNumber: Int,
    val caregivingRoundId: String,
    val receptionId: String,
    val startDateTime: LocalDateTime,
    val subject: Subject,
)
