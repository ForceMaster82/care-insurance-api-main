package kr.caredoc.careinsurance.caregiving

import kr.caredoc.careinsurance.modification.Modification
import java.time.LocalDateTime

class LastCaregivingRoundModified(
    val receptionId: String,
    val lastCaregivingRoundId: String,
    val endDateTime: Modification<LocalDateTime>,
)
