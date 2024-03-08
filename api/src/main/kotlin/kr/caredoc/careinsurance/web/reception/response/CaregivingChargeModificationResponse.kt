package kr.caredoc.careinsurance.web.reception.response

import java.time.OffsetDateTime

data class CaregivingChargeModificationResponse(
    val lastModifiedDateTime: OffsetDateTime?,
    val lastModifierId: String?,
    val modificationCount: Int,
)
