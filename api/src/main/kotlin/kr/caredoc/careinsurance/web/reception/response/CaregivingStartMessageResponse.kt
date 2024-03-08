package kr.caredoc.careinsurance.web.reception.response

import kr.caredoc.careinsurance.message.SendingStatus
import java.time.LocalDate

data class CaregivingStartMessageResponse(
    val receptionId: String,
    val firstCaregivingRoundId: String,
    val sendingStatus: SendingStatus,
    val sentDate: LocalDate?,
)
