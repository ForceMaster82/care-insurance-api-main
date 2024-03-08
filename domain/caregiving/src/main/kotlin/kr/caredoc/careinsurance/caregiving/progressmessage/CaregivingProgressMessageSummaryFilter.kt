package kr.caredoc.careinsurance.caregiving.progressmessage

import kr.caredoc.careinsurance.message.SendingStatus
import java.time.LocalDate

data class CaregivingProgressMessageSummaryFilter(
    val date: LocalDate,
    val sendingStatus: SendingStatus?,
)
