package kr.caredoc.careinsurance.web.coverage.response

import kr.caredoc.careinsurance.coverage.RenewalType
import java.time.OffsetDateTime

data class SimpleCoverageResponse(
    val id: String,
    val name: String,
    val targetSubscriptionYear: Int,
    val renewalType: RenewalType,
    val lastModifiedDateTime: OffsetDateTime,
)
