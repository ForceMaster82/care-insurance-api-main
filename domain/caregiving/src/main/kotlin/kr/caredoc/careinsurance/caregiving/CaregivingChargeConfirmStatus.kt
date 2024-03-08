package kr.caredoc.careinsurance.caregiving

enum class CaregivingChargeConfirmStatus {
    NOT_STARTED,
    CONFIRMED;

    companion object {
        val EDITABLE_STATUS = setOf(
            NOT_STARTED,
        )
    }

    val isEditableStatus: Boolean
        get() = EDITABLE_STATUS.contains(this)
}
