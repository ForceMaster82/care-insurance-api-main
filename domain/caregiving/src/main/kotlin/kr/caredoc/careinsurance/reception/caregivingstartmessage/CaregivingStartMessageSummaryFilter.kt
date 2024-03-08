package kr.caredoc.careinsurance.reception.caregivingstartmessage

import kr.caredoc.careinsurance.message.SendingStatus
import java.time.LocalDate

data class CaregivingStartMessageSummaryFilter(
    val date: LocalDate,
    val sendingStatus: SendingStatus?,
)
