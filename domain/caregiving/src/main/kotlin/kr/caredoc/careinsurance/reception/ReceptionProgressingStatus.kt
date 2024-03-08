package kr.caredoc.careinsurance.reception

enum class ReceptionProgressingStatus {
    RECEIVED,
    CANCELED,
    CANCELED_BY_PERSONAL_CAREGIVER,
    CANCELED_BY_MEDICAL_REQUEST,
    PENDING,
    MATCHING,
    PENDING_MATCHING,
    CANCELED_WHILE_MATCHING,
    CAREGIVING_IN_PROGRESS,
    COMPLETED;

    companion object {
        val CANCELLATION_STATUSES = setOf(
            CANCELED,
            CANCELED_BY_PERSONAL_CAREGIVER,
            CANCELED_BY_MEDICAL_REQUEST,
            CANCELED_WHILE_MATCHING,
        )
    }

    val isCancellationStatus: Boolean
        get() = CANCELLATION_STATUSES.contains(this)
}
