package kr.caredoc.careinsurance.reception

import kr.caredoc.careinsurance.security.accesscontrol.Subject

data class CaregivingManagerAssignedToReception(
    val receptionId: String,
    val caregivingManagerInfo: CaregivingManagerInfo,
    val subject: Subject,
)
