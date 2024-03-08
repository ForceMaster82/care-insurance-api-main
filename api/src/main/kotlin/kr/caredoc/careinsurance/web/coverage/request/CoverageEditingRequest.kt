package kr.caredoc.careinsurance.web.coverage.request

data class CoverageEditingRequest(
    val name: String,
    val targetSubscriptionYear: Int,
    val annualCoveredCaregivingCharges: List<AnnualCoveredCaregivingCharge>,
) {
    data class AnnualCoveredCaregivingCharge(
        val targetAccidentYear: Int,
        val caregivingCharge: Int,
    )
}
