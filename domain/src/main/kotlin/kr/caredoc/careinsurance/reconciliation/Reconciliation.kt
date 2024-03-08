package kr.caredoc.careinsurance.reconciliation

import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import kr.caredoc.careinsurance.AggregateRoot
import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
class Reconciliation(
    id: String,
    @Access(AccessType.FIELD)
    val receptionId: String,
    val caregivingRoundId: String,
    val issuedDate: LocalDate,
    @Enumerated(EnumType.STRING)
    val issuedType: IssuedType,
    val billingAmount: Int,
    val settlementAmount: Int,
    val settlementDepositAmount: Int,
    val settlementWithdrawalAmount: Int,
    val profit: Int,
    val distributedProfit: Int,
    val caregiverPhoneNumberWhenIssued: String,
    val actualCaregivingSecondsWhenIssued: Int,
    reconciledYear: Int? = null,
    reconciledMonth: Int? = null,
) : AggregateRoot(id), Object {
    @Enumerated(EnumType.STRING)
    var closingStatus: ClosingStatus = ClosingStatus.OPEN
        protected set

    var reconciledYear: Int? = reconciledYear
        protected set
    var reconciledMonth: Int? = reconciledMonth
        protected set
    var closedDateTime: LocalDateTime? = null
        protected set

    private fun hasConfirmedReconciledYearMonth(): Boolean {
        return reconciledMonth != null && reconciledYear != null
    }

    fun edit(command: ReconciliationEditingCommand) {
        ReconciliationAccessPolicy.check(command.subject, command, this)

        changeClosingStatus(command.closingStatus, command.subject)
    }

    private fun changeClosingStatus(transitionTo: ClosingStatus, subject: Subject) {
        this.closingStatus.ensureTransitionableTo(transitionTo)

        if (transitionTo == ClosingStatus.CLOSED) {
            close(subject)
        }
    }

    private fun close(subject: Subject) {
        this.closingStatus = ClosingStatus.CLOSED

        if (!hasConfirmedReconciledYearMonth()) {
            val today = Clock.today()

            this.reconciledYear = today.year
            this.reconciledMonth = today.monthValue
        }

        closedDateTime = Clock.now()

        registerEvent(
            ReconciliationClosed(
                this.id,
                this.caregivingRoundId,
                this.receptionId,
                this.issuedType,
                subject,
            )
        )
    }
}
