package kr.caredoc.careinsurance.billing

data class CoverageInfo(
    val targetSubscriptionYear: Int,
    val renewalType: RenewalType,
    val annualCoveredCaregivingCharges: List<AnnualCoveredCaregivingCharge>,
) {

    data class AnnualCoveredCaregivingCharge(
        val targetAccidentYear: Int,
        val caregivingCharge: Int,
    )

    enum class RenewalType {
        THREE_YEAR,
        TEN_YEAR,
    }
}
