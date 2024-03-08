package kr.caredoc.careinsurance.caregiving

import kr.caredoc.careinsurance.security.accesscontrol.Subject

data class CaregiverAssignedToCaregivingRound(
    val caregivingRoundNumber: Int,
    val receptionId: String,
    val caregiverInfo: CaregiverInfo,
    val subject: Subject,
)
