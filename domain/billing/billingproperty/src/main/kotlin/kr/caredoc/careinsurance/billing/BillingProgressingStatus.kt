package kr.caredoc.careinsurance.billing

enum class BillingProgressingStatus {
    NOT_STARTED,
    WAITING_FOR_BILLING,
    WAITING_DEPOSIT,
    OVER_DEPOSIT,
    UNDER_DEPOSIT,
    COMPLETED_DEPOSIT;

    companion object {
        private val PROGRESSING_STATUS_AVAILABILITY_MAP = mapOf(
            NOT_STARTED to setOf(),
            WAITING_FOR_BILLING to setOf(WAITING_DEPOSIT),
            WAITING_DEPOSIT to setOf(OVER_DEPOSIT, UNDER_DEPOSIT, COMPLETED_DEPOSIT),
            OVER_DEPOSIT to setOf(UNDER_DEPOSIT, COMPLETED_DEPOSIT),
            UNDER_DEPOSIT to setOf(OVER_DEPOSIT, COMPLETED_DEPOSIT),
            COMPLETED_DEPOSIT to setOf(UNDER_DEPOSIT, OVER_DEPOSIT),
        )
    }

    fun isTransitionableTo(transitionTo: BillingProgressingStatus) =
        PROGRESSING_STATUS_AVAILABILITY_MAP[this]?.contains(transitionTo) == true
}
