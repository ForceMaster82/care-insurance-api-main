package kr.caredoc.careinsurance.web.reception.response

import java.time.OffsetDateTime

data class CaregivingRoundModificationResponse(
    val lastModifiedDateTime: OffsetDateTime?,
    val lastModifierId: String?,
    val modificationCount: Int,
)
