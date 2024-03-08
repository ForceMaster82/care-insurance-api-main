package kr.caredoc.careinsurance.settlement

enum class SettlementProgressingStatus {
    NOT_STARTED,
    CONFIRMED,
    WAITING,
    COMPLETED;

    companion object {
        private val TRANSITION_AVAILABILITY_MAP = mapOf(
            NOT_STARTED to setOf(),
            CONFIRMED to setOf(WAITING),
            WAITING to setOf(COMPLETED),
        )
    }

    fun isTransitionableTo(transitionTo: SettlementProgressingStatus) =
        TRANSITION_AVAILABILITY_MAP[this]?.contains(transitionTo) == true
}
