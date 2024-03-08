package kr.caredoc.careinsurance.caregiving

import com.github.guepardoapps.kulid.ULID
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Transient
import kr.caredoc.careinsurance.AggregateRoot
import kr.caredoc.careinsurance.billing.BillingGenerated
import kr.caredoc.careinsurance.billing.BillingModified
import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import kr.caredoc.careinsurance.caregiving.exception.CaregivingRoundStartDateTimeNoDataException
import kr.caredoc.careinsurance.caregiving.state.CanceledState
import kr.caredoc.careinsurance.caregiving.state.CancellationReason
import kr.caredoc.careinsurance.caregiving.state.CaregivingRoundInfo
import kr.caredoc.careinsurance.caregiving.state.CaregivingState
import kr.caredoc.careinsurance.caregiving.state.CaregivingStateData
import kr.caredoc.careinsurance.caregiving.state.CompleteState
import kr.caredoc.careinsurance.caregiving.state.FinishingReason
import kr.caredoc.careinsurance.caregiving.state.InProgressState
import kr.caredoc.careinsurance.caregiving.state.InitialState
import kr.caredoc.careinsurance.caregiving.state.PendingState
import kr.caredoc.careinsurance.caregiving.state.ReconciliationCompletedState
import kr.caredoc.careinsurance.caregiving.state.RematchingState
import kr.caredoc.careinsurance.caregiving.state.StoppedState
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.reception.CaregivingManagerInfo
import kr.caredoc.careinsurance.reception.ReceptionModified
import kr.caredoc.careinsurance.reception.ReceptionProgressingStatus
import kr.caredoc.careinsurance.reconciliation.ReconciliationClosed
import kr.caredoc.careinsurance.security.accesscontrol.ModifyingAccess
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.ObjectAttribute
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.settlement.SettlementGenerated
import kr.caredoc.careinsurance.settlement.SettlementModified
import kr.caredoc.careinsurance.settlement.SettlementProgressingStatus
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
class CaregivingRound protected constructor(
    id: String,
    val caregivingRoundNumber: Int,
    receptionInfo: ReceptionInfo,
    caregivingStateData: CaregivingStateData,
) : AggregateRoot(id), Object {
    companion object {
        val INITIAL_STATE_DATA = CaregivingStateData(progressingStatus = CaregivingProgressingStatus.NOT_STARTED)

        private fun initState(
            caregivingRoundInfo: CaregivingRoundInfo,
            caregivingStateData: CaregivingStateData
        ): CaregivingState {
            return when (caregivingStateData.progressingStatus) {
                CaregivingProgressingStatus.NOT_STARTED -> InitialState(caregivingRoundInfo, caregivingStateData.caregiverInfo)
                CaregivingProgressingStatus.REMATCHING -> RematchingState(
                    caregivingRoundInfo,
                    caregivingStateData.caregiverInfo ?: throw IllegalArgumentException()
                )

                CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS -> InProgressState(
                    caregivingRoundInfo,
                    caregivingStateData.caregiverInfo ?: throw IllegalArgumentException(),
                    caregivingStateData.startDateTime ?: throw IllegalArgumentException()
                )

                CaregivingProgressingStatus.PENDING_REMATCHING -> PendingState(
                    caregivingRoundInfo,
                    caregivingStateData.caregiverInfo ?: throw IllegalArgumentException(),
                )

                CaregivingProgressingStatus.CANCELED_WHILE_REMATCHING -> CanceledState(
                    caregivingRoundInfo,
                    caregivingStateData.caregiverInfo ?: throw IllegalArgumentException(),
                    CancellationReason.fromClosingReasonType(
                        caregivingStateData.closingReasonType ?: throw IllegalArgumentException()
                    ),
                    caregivingStateData.detailClosingReason ?: throw IllegalArgumentException()
                )

                CaregivingProgressingStatus.COMPLETED_RESTARTING -> StoppedState(
                    caregivingRoundInfo,
                    caregivingStateData.caregiverInfo ?: throw IllegalArgumentException(),
                    caregivingStateData.startDateTime ?: throw IllegalArgumentException(),
                    caregivingStateData.endDateTime ?: throw IllegalArgumentException(),
                )

                CaregivingProgressingStatus.COMPLETED,
                CaregivingProgressingStatus.COMPLETED_USING_PERSONAL_CAREGIVER -> CompleteState(
                    caregivingRoundInfo,
                    caregivingStateData.caregiverInfo ?: throw IllegalArgumentException(),
                    caregivingStateData.startDateTime ?: throw IllegalArgumentException(),
                    caregivingStateData.endDateTime ?: throw IllegalArgumentException(),
                    FinishingReason.fromClosingReasonType(
                        caregivingStateData.closingReasonType ?: throw IllegalArgumentException(),
                    )
                )

                CaregivingProgressingStatus.RECONCILIATION_COMPLETED -> ReconciliationCompletedState(
                    caregivingRoundInfo,
                    caregivingStateData.caregiverInfo ?: throw IllegalArgumentException(),
                    caregivingStateData.startDateTime ?: throw IllegalArgumentException(),
                    caregivingStateData.endDateTime ?: throw IllegalArgumentException(),
                    FinishingReason.fromClosingReasonType(
                        caregivingStateData.closingReasonType ?: throw IllegalArgumentException(),
                    )
                )
            }
        }
    }

    constructor(
        id: String,
        caregivingRoundNumber: Int,
        receptionInfo: ReceptionInfo,
    ) : this(
        id = id,
        caregivingRoundNumber = caregivingRoundNumber,
        receptionInfo = receptionInfo,
        caregivingStateData = INITIAL_STATE_DATA,
    )

    fun willBeAffectedBy(event: BillingModified): Boolean {
        return event.progressingStatus.hasChanged && event.progressingStatus.current != billingProgressingStatus
    }

    fun willBeAffectedBy(event: SettlementModified): Boolean {
        return event.progressingStatus.hasChanged && event.progressingStatus.current != settlementProgressingStatus
    }

    @Embedded
    var receptionInfo = receptionInfo
        protected set

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "progressingStatus", column = Column(name = "caregiving_progressing_status")),
        AttributeOverride(name = "detailClosingReason", column = Column(name = "cancel_reason_detail")),
        AttributeOverride(name = "canceledDateTime", column = Column(name = "cancel_date_time")),
    )
    var caregivingStateData = caregivingStateData
        protected set(value) {
            ensureCaregivingPeriodConstraints(value.startDateTime, value.endDateTime)
            field = value
        }

    @Transient
    protected var caregivingState = initState(
        CaregivingRoundInfo(
            id,
            caregivingRoundNumber
        ),
        caregivingStateData
    )
        protected set(value) {
            field = value
            caregivingStateData = value.stateData
        }

    val caregivingProgressingStatus: CaregivingProgressingStatus
        get() = this.caregivingStateData.progressingStatus

    @Enumerated(EnumType.STRING)
    var settlementProgressingStatus: SettlementProgressingStatus = SettlementProgressingStatus.NOT_STARTED
        protected set

    @Enumerated(EnumType.STRING)
    var billingProgressingStatus: BillingProgressingStatus = BillingProgressingStatus.NOT_STARTED
        protected set

    val startDateTime: LocalDateTime?
        get() = this.caregivingStateData.startDateTime

    val caregiverInfo: CaregiverInfo?
        get() = this.caregivingStateData.caregiverInfo

    val cancelDateTime: LocalDateTime?
        get() = this.caregivingStateData.canceledDateTime

    val caregivingRoundClosingReasonDetail: String?
        get() = this.caregivingStateData.detailClosingReason

    val caregivingRoundClosingReasonType: ClosingReasonType?
        get() = this.caregivingStateData.closingReasonType

    val endDateTime: LocalDateTime?
        get() = this.caregivingStateData.endDateTime

    var remarks: String = ""
        protected set

    @Embeddable
    data class ReceptionInfo(
        val receptionId: String,
        val insuranceNumber: String,
        val accidentNumber: String,
        val maskedPatientName: String,
        val expectedCaregivingStartDate: LocalDate?,
        @Enumerated(EnumType.STRING)
        val receptionProgressingStatus: ReceptionProgressingStatus,
        @Embedded
        val caregivingManagerInfo: CaregivingManagerInfo,
    )

    override fun get(attribute: ObjectAttribute) = when (attribute) {
        ObjectAttribute.ASSIGNED_ORGANIZATION_ID -> this.receptionInfo.caregivingManagerInfo.organizationId?.let {
            setOf(
                it
            )
        }
            ?: setOf()

        else -> setOf()
    }

    fun assignCaregiver(
        caregiverInfo: CaregiverInfo,
        subject: Subject
    ) = trackModification(subject = subject) {
        CaregivingRoundAccessPolicy.check(subject, ModifyingAccess, this)
        if (this.caregiverInfo == caregiverInfo) {
            return@trackModification
        }

        val nextState = caregivingState.assignCaregiver(caregiverInfo)
        caregivingState = nextState

        this.registerEvent(
            CaregiverAssignedToCaregivingRound(
                caregivingRoundNumber = this.caregivingRoundNumber,
                receptionId = this.receptionInfo.receptionId,
                caregiverInfo = caregiverInfo,
                subject = subject
            )
        )
    }

    fun startCaregiving(
        caregivingStartDateTime: LocalDateTime,
        subject: Subject
    ) = trackModification(subject = subject) {
        CaregivingRoundAccessPolicy.check(subject, ModifyingAccess, this)

        val nextState = caregivingState.start(caregivingStartDateTime)
        caregivingState = nextState

        this.registerEvent(
            CaregivingRoundStarted(
                caregivingRoundNumber = this.caregivingRoundNumber,
                caregivingRoundId = this.id,
                receptionId = this.receptionInfo.receptionId,
                startDateTime = caregivingStartDateTime,
                subject = subject
            )
        )
    }

    fun editCaregivingStartDateTime(
        startDateTime: LocalDateTime,
        subject: Subject
    ) = trackModification(subject = subject) {
        CaregivingRoundAccessPolicy.check(subject, ModifyingAccess, this)

        if (this.startDateTime == startDateTime) {
            return@trackModification
        }

        val nextState = caregivingState.editStartDateTime(startDateTime)
        caregivingState = nextState
    }

    private fun ensureCaregivingPeriodConstraints(startDateTime: LocalDateTime?, endDateTime: LocalDateTime?) {
        if (startDateTime == null) {
            return
        }

        if (endDateTime == null) {
            return
        }

        if (startDateTime.isAfter(endDateTime)) {
            throw IllegalCaregivingPeriodEnteredException(
                targetCaregivingRoundId = this.id,
                enteredStartDateTime = startDateTime,
            )
        }
    }

    fun pend(subject: Subject) {
        CaregivingRoundAccessPolicy.check(subject, ModifyingAccess, this)

        val nextState = caregivingState.pend()
        caregivingState = nextState
    }

    fun cancel(
        reason: CancellationReason,
        detailReason: String,
        subject: Subject
    ) = trackModification(subject = subject) {
        CaregivingRoundAccessPolicy.check(subject, ModifyingAccess, this)

        val nextState = caregivingState.cancel(reason, detailReason)
        caregivingState = nextState
    }

    fun finish(
        endDateTime: LocalDateTime,
        finishingReason: FinishingReason,
        subject: Subject
    ) = trackModification(subject = subject) {
        CaregivingRoundAccessPolicy.check(subject, ModifyingAccess, this)
        val nextState = caregivingState.complete(
            endDateTime,
            finishingReason
        )
        caregivingState = nextState

        if (finishingReason == FinishingReason.FINISHED) {
            updateEvent(
                LastCaregivingRoundFinished::class,
                LastCaregivingRoundFinished(
                    receptionId = receptionInfo.receptionId,
                    lastCaregivingRoundId = id,
                    endDateTime = endDateTime,
                )
            )
        }

        when (finishingReason) {
            FinishingReason.FINISHED_CONTINUE -> generateNextRoundInProgress(endDateTime).also {
                it.notifyCaregivingStarted(it, subject)
            }

            FinishingReason.FINISHED_RESTARTING,
            FinishingReason.FINISHED_CHANGING_HOSPITAL,
            FinishingReason.FINISHED_CHANGING_CAREGIVER,
            FinishingReason.FINISHED_CHANGING_CAREGIVER_AND_HOSPITAL -> generateNextRoundWith(INITIAL_STATE_DATA)

            else -> null
        }.let {
            FinishingResult(it)
        }
    }

    class FinishingResult(val nextRound: CaregivingRound?)

    fun stop(stopDateTime: LocalDateTime, subject: Subject) = trackModification(subject = subject) {
        CaregivingRoundAccessPolicy.check(subject, ModifyingAccess, this)
        val nextState = this.caregivingState.stop(stopDateTime)
        caregivingState = nextState
    }

    fun editCaregivingEndDateTime(
        endDateTime: LocalDateTime,
        subject: Subject
    ) = trackModification(subject = subject) {
        CaregivingRoundAccessPolicy.check(subject, ModifyingAccess, this)

        if (this.endDateTime == endDateTime) {
            return@trackModification
        }

        val nextState = caregivingState.editEndDateTime(endDateTime)
        caregivingState = nextState
    }

    fun updateRemarks(remarks: String, subject: Subject) = trackModification(subject = subject) {
        CaregivingRoundAccessPolicy.check(subject, ModifyingAccess, this)

        if (this.remarks == remarks) {
            return@trackModification
        }

        this.remarks = remarks
    }

    private fun generateNextRoundWith(stateData: CaregivingStateData) = CaregivingRound(
        id = ULID.random(),
        caregivingRoundNumber = this.caregivingRoundNumber + 1,
        receptionInfo = this.receptionInfo,
        caregivingStateData = stateData
    )

    private fun generateNextRoundInProgress(endDateTime: LocalDateTime) = generateNextRoundWith(
        CaregivingStateData(
            CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS,
            caregiverInfo = this.caregiverInfo,
            startDateTime = endDateTime,
        )
    )

    private fun notifyCaregivingStarted(new: CaregivingRound, subject: Subject) {
        if (new.startDateTime == null) {
            throw CaregivingRoundStartDateTimeNoDataException(new.id)
        }
        new.startDateTime?.let {
            this.registerEvent(
                CaregivingRoundStarted(
                    caregivingRoundNumber = new.caregivingRoundNumber,
                    caregivingRoundId = new.id,
                    receptionId = new.receptionInfo.receptionId,
                    startDateTime = it,
                    subject = subject
                )
            )
        }
    }

    fun handleReceptionModified(event: ReceptionModified) {
        this.receptionInfo = ReceptionInfo(
            receptionId = this.receptionInfo.receptionId,
            insuranceNumber = event.insuranceInfo.current.insuranceNumber,
            accidentNumber = event.accidentInfo.current.accidentNumber,
            maskedPatientName = event.patientInfo.current.name.masked,
            receptionProgressingStatus = event.progressingStatus.current,
            expectedCaregivingStartDate = event.expectedCaregivingStartDate.current,
            caregivingManagerInfo = event.caregivingManagerInfo.current ?: this.receptionInfo.caregivingManagerInfo,
        )
    }

    private inner class ModificationTracker {
        private val previous = generateTrackData(this@CaregivingRound)

        private fun generateTrackData(status: CaregivingRound) = TrackedData(
            billingProgressingStatus = status.billingProgressingStatus,
            settlementProgressingStatus = status.settlementProgressingStatus,
            caregivingProgressingStatus = status.caregivingProgressingStatus,
            caregiverInfo = caregiverInfo,
            startDateTime = status.startDateTime,
            endDateTime = status.endDateTime,
            remarks = remarks,
            isLastCaregivingRound = endDateTime != null && caregivingProgressingStatus.isCompletedStatus && caregivingRoundClosingReasonType == ClosingReasonType.FINISHED
        )

        private fun generateModifiedEvent(
            current: TrackedData,
            cause: CaregivingRoundModified.Cause,
            subject: Subject
        ) = CaregivingRoundModified(
            caregivingRoundId = id,
            caregivingRoundNumber = caregivingRoundNumber,
            receptionId = receptionInfo.receptionId,
            billingProgressingStatus = Modification(
                previous.billingProgressingStatus,
                current.billingProgressingStatus
            ),
            settlementProgressingStatus = Modification(
                previous.settlementProgressingStatus,
                current.settlementProgressingStatus
            ),
            caregivingProgressingStatus = Modification(
                previous.caregivingProgressingStatus,
                current.caregivingProgressingStatus,
            ),
            caregiverInfo = Modification(
                previous.caregiverInfo,
                current.caregiverInfo,
            ),
            startDateTime = Modification(
                previous.startDateTime,
                current.startDateTime,
            ),
            endDateTime = Modification(
                previous.endDateTime,
                current.endDateTime,
            ),
            remarks = Modification(
                previous.remarks,
                current.remarks,
            ),
            cause = cause,
            editingSubject = subject,
        )

        private fun updateCaregivingRoundModifiedEvent(
            current: TrackedData,
            cause: CaregivingRoundModified.Cause,
            subject: Subject
        ) {
            updateEvent(CaregivingRoundModified::class, generateModifiedEvent(current, cause, subject))
        }

        private fun updateLastCaregivingRoundModifiedEvent(current: TrackedData) {
            val event = LastCaregivingRoundModified(
                receptionId = receptionInfo.receptionId,
                lastCaregivingRoundId = id,
                endDateTime = Modification(
                    previous.endDateTime ?: return,
                    current.endDateTime ?: return,
                ),
            )

            updateEvent(LastCaregivingRoundModified::class, event)
        }

        fun updateModifiedEvent(cause: CaregivingRoundModified.Cause, subject: Subject) {
            val current = generateTrackData(this@CaregivingRound)
            if (previous == current) {
                return
            }

            updateCaregivingRoundModifiedEvent(current, cause, subject)
            if (current.isLastCaregivingRound && previous.isLastCaregivingRound) {
                updateLastCaregivingRoundModifiedEvent(current)
            }
        }
    }

    @Transient
    private var modificationTracker: ModificationTracker? = null

    private fun initModificationTracking() {
        if (this.modificationTracker == null) {
            this.modificationTracker = ModificationTracker()
        }
    }

    private fun <T> trackModification(
        cause: CaregivingRoundModified.Cause = CaregivingRoundModified.Cause.DIRECT_EDIT,
        subject: Subject,
        block: () -> T
    ): T {
        initModificationTracking()
        val result = block()
        modificationTracker?.updateModifiedEvent(cause, subject)

        return result
    }

    private data class TrackedData(
        val billingProgressingStatus: BillingProgressingStatus,
        val settlementProgressingStatus: SettlementProgressingStatus,
        val caregivingProgressingStatus: CaregivingProgressingStatus,
        val caregiverInfo: CaregiverInfo?,
        val startDateTime: LocalDateTime?,
        val endDateTime: LocalDateTime?,
        val remarks: String,
        val isLastCaregivingRound: Boolean,
    )

    override fun clearEvents() {
        super.clearEvents()
        this.modificationTracker = ModificationTracker()
    }

    fun handleBillingModified(event: BillingModified) = trackModification(subject = event.subject) {
        event.progressingStatus.ifChanged { billingProgressingStatus = current }
    }

    fun handleSettlementModified(event: SettlementModified) = trackModification(subject = event.subject) {
        event.progressingStatus.ifChanged { settlementProgressingStatus = current }
    }

    fun handleSettlementGenerated(event: SettlementGenerated) {
        settlementProgressingStatus = event.progressingStatus
    }

    fun handleBillingGenerated(event: BillingGenerated) {
        billingProgressingStatus = event.progressingStatus
    }

    fun handleReconciliationClosed(event: ReconciliationClosed) = trackModification(subject = event.subject) {
        CaregivingRoundAccessPolicy.check(event.subject, ModifyingAccess, this)

        val nextState = caregivingState.completeReconciliation()
        caregivingState = nextState
    }

    override fun postLoad() {
        super.postLoad()
        this.caregivingState = initState(
            CaregivingRoundInfo(
                id,
                caregivingRoundNumber
            ),
            caregivingStateData
        )
    }
}
