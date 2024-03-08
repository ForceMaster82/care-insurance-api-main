package kr.caredoc.careinsurance.web.reception.response

import kr.caredoc.careinsurance.reception.ReceptionProgressingStatus
import java.time.LocalDate

data class DailyReceptionStatisticsResponse(
    val receivedDate: LocalDate,
    val receptionCount: Int,
    val canceledReceptionCount: Int,
    val canceledReceptionCountsByReason: Map<ReceptionProgressingStatus, Int>,
    val requestedBillingCount: Int,
    val requestedBillingAmount: Int,
    val depositCount: Int,
    val depositAmount: Int,
    val withdrawalCount: Int,
    val withdrawalAmount: Int,
    val sameDayAssignmentReceptionCount: Int,
    val startedSameDayAssignmentReceptionCount: Int,
    val shortTermReceptionCount: Int,
    val startedShortTermReceptionCount: Int,
)
