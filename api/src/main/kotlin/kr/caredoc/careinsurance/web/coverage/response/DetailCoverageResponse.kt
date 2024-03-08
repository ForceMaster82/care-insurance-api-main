package kr.caredoc.careinsurance.web.coverage.response

import kr.caredoc.careinsurance.coverage.RenewalType
import java.time.OffsetDateTime

data class DetailCoverageResponse(
    val id: String,
    val name: String,
    val targetSubscriptionYear: Int,
    val renewalType: RenewalType,
    val annualCoveredCaregivingCharges: List<AnnualCoveredCaregivingCharge>,
    val lastModifiedDateTime: OffsetDateTime,
) {
    data class AnnualCoveredCaregivingCharge(
        val targetAccidentYear: Int,
        val caregivingCharge: Int,
    )
}
