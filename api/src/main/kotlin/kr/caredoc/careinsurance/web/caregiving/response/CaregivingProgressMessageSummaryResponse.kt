package kr.caredoc.careinsurance.web.caregiving.response

import kr.caredoc.careinsurance.message.SendingStatus
import java.time.LocalDate

data class CaregivingProgressMessageSummaryResponse(
    val receptionId: String,
    val caregivingRoundId: String,
    val sendingStatus: SendingStatus,
    val sentDate: LocalDate?,
)
