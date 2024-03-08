package kr.caredoc.careinsurance.web.coverage.request

import kr.caredoc.careinsurance.coverage.RenewalType

data class CoverageCreationRequest(
    val name: String,
    val targetSubscriptionYear: Int,
    val renewalType: RenewalType,
    val annualCoveredCaregivingCharges: List<AnnualCoveredCaregivingCharge>,
) {
    data class AnnualCoveredCaregivingCharge(
        val targetAccidentYear: Int,
        val caregivingCharge: Int,
    )
}
