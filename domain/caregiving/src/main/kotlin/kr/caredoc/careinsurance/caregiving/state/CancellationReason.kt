package kr.caredoc.careinsurance.caregiving.state

import kr.caredoc.careinsurance.caregiving.ClosingReasonType

enum class CancellationReason {
    CANCELED_WHILE_REMATCHING,
    CANCELED_USING_PERSONAL_CAREGIVER;

    companion object {
        fun contains(closingReasonType: ClosingReasonType) = when (closingReasonType) {
            ClosingReasonType.CANCELED_WHILE_REMATCHING -> true
            ClosingReasonType.CANCELED_USING_PERSONAL_CAREGIVER -> true
            else -> false
        }

        fun fromClosingReasonType(closingReasonType: ClosingReasonType) = when (closingReasonType) {
            ClosingReasonType.CANCELED_WHILE_REMATCHING -> CANCELED_WHILE_REMATCHING
            ClosingReasonType.CANCELED_USING_PERSONAL_CAREGIVER -> CANCELED_USING_PERSONAL_CAREGIVER
            else -> throw IllegalArgumentException()
        }
    }

    fun intoClosingReasonType() = when (this) {
        CANCELED_WHILE_REMATCHING -> ClosingReasonType.CANCELED_WHILE_REMATCHING
        CANCELED_USING_PERSONAL_CAREGIVER -> ClosingReasonType.CANCELED_USING_PERSONAL_CAREGIVER
    }
}
