package kr.caredoc.careinsurance.reconciliation

enum class ClosingStatus {
    OPEN,
    CLOSED;

    companion object {
        private val TRANSITION_AVAILABILITY_MAP = mapOf(
            OPEN to setOf(CLOSED),
            CLOSED to setOf(),
        )
    }

    fun isTransitionableTo(transitionTo: ClosingStatus) =
        TRANSITION_AVAILABILITY_MAP[this]?.contains(transitionTo) == true

    fun ensureTransitionableTo(transitionTo: ClosingStatus) {
        if (!this.isTransitionableTo(transitionTo)) {
            throw InvalidReconciliationClosingStatusTransitionException(
                this,
                transitionTo,
            )
        }
    }
}
