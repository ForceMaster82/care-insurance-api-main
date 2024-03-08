package kr.caredoc.careinsurance.caregiving.state

import kr.caredoc.careinsurance.caregiving.ClosingReasonType

enum class FinishingReason {
    FINISHED,
    FINISHED_CONTINUE,
    FINISHED_RESTARTING,
    FINISHED_CHANGING_CAREGIVER,
    FINISHED_CHANGING_HOSPITAL,
    FINISHED_CHANGING_CAREGIVER_AND_HOSPITAL,
    FINISHED_USING_PERSONAL_CAREGIVER;

    companion object {
        fun contains(closingReasonType: ClosingReasonType) = when (closingReasonType) {
            ClosingReasonType.FINISHED -> true
            ClosingReasonType.FINISHED_CONTINUE -> true
            ClosingReasonType.FINISHED_RESTARTING -> true
            ClosingReasonType.FINISHED_CHANGING_CAREGIVER -> true
            ClosingReasonType.FINISHED_CHANGING_HOSPITAL -> true
            ClosingReasonType.FINISHED_CHANGING_CAREGIVER_AND_HOSPITAL -> true
            ClosingReasonType.FINISHED_USING_PERSONAL_CAREGIVER -> true
            else -> false
        }

        fun fromClosingReasonType(closingReasonType: ClosingReasonType) = when (closingReasonType) {
            ClosingReasonType.FINISHED -> FINISHED
            ClosingReasonType.FINISHED_CONTINUE -> FINISHED_CONTINUE
            ClosingReasonType.FINISHED_RESTARTING -> FINISHED_RESTARTING
            ClosingReasonType.FINISHED_CHANGING_CAREGIVER -> FINISHED_CHANGING_CAREGIVER
            ClosingReasonType.FINISHED_CHANGING_HOSPITAL -> FINISHED_CHANGING_HOSPITAL
            ClosingReasonType.FINISHED_CHANGING_CAREGIVER_AND_HOSPITAL -> FINISHED_CHANGING_CAREGIVER_AND_HOSPITAL
            ClosingReasonType.FINISHED_USING_PERSONAL_CAREGIVER -> FINISHED_USING_PERSONAL_CAREGIVER
            else -> throw IllegalArgumentException()
        }
    }

    fun intoClosingReasonType() = when (this) {
        FINISHED -> ClosingReasonType.FINISHED
        FINISHED_CONTINUE -> ClosingReasonType.FINISHED_CONTINUE
        FINISHED_RESTARTING -> ClosingReasonType.FINISHED_RESTARTING
        FINISHED_CHANGING_CAREGIVER -> ClosingReasonType.FINISHED_CHANGING_CAREGIVER
        FINISHED_CHANGING_HOSPITAL -> ClosingReasonType.FINISHED_CHANGING_HOSPITAL
        FINISHED_CHANGING_CAREGIVER_AND_HOSPITAL -> ClosingReasonType.FINISHED_CHANGING_CAREGIVER_AND_HOSPITAL
        FINISHED_USING_PERSONAL_CAREGIVER -> ClosingReasonType.FINISHED_USING_PERSONAL_CAREGIVER
    }
}
