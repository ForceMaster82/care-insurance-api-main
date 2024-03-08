package kr.caredoc.careinsurance.caregiving

enum class CaregivingProgressingStatus {
    NOT_STARTED,
    CAREGIVING_IN_PROGRESS,
    REMATCHING,
    PENDING_REMATCHING,
    CANCELED_WHILE_REMATCHING,
    COMPLETED_RESTARTING,
    COMPLETED,
    COMPLETED_USING_PERSONAL_CAREGIVER,
    RECONCILIATION_COMPLETED;

    companion object {
        val COMPLETED_STATUSES = setOf(
            COMPLETED,
        )
        val CAREGIVING_CHARGE_ACTIONABLE_STATUSES = setOf(
            COMPLETED_USING_PERSONAL_CAREGIVER,
            COMPLETED,
        )
        val RECONCILIATION_COMPLETED_STATUSES = setOf(
            CANCELED_WHILE_REMATCHING,
            RECONCILIATION_COMPLETED,
        )
    }

    val isCaregivingChargeActionableStatus: Boolean
        get() = CAREGIVING_CHARGE_ACTIONABLE_STATUSES.contains(this)

    val isCompletedStatus: Boolean
        get() = COMPLETED_STATUSES.contains(this)

    val isReconciliationCompletedStatus: Boolean
        get() = RECONCILIATION_COMPLETED_STATUSES.contains(this)
}
