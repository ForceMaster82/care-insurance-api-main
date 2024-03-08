package kr.caredoc.careinsurance.web.reception.response

import kr.caredoc.careinsurance.reception.caregivingsatisfactionsurvey.ReservationStatus

data class CaregivingSatisfactionSurveyStatusResponse(
    val receptionId: String,
    val lastCaregivingRoundId: String,
    val reservationStatus: ReservationStatus,
)
