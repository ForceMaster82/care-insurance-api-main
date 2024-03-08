package kr.caredoc.careinsurance.caregiving

import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.OrderBy
import kr.caredoc.careinsurance.AggregateRoot
import kr.caredoc.careinsurance.caregiving.exception.CaregivingAdditionalChargeNameDuplicatedException
import kr.caredoc.careinsurance.caregiving.exception.InvalidCaregivingChargeConfirmStatusTransitionException
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.reception.CaregivingManagerInfo
import kr.caredoc.careinsurance.reception.ReceptionModified
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.ObjectAttribute
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.thenThrows
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
class CaregivingCharge private constructor(
    id: String,
    @Embedded
    val caregivingRoundInfo: CaregivingRoundInfo,
    val caregivingChargeDate: LocalDate = LocalDate.now(),
    additionalHoursCharge: Int,
    mealCost: Int,
    transportationFee: Int,
    holidayCharge: Int,
    caregiverInsuranceFee: Int,
    commissionFee: Int,
    vacationCharge: Int,
    patientConditionCharge: Int,
    covid19TestingCost: Int,
    outstandingAmount: Int,
    additionalCharges: List<AdditionalCharge>,
    isCancelAfterArrived: Boolean,
    expectedSettlementDate: LocalDate,
    basicAmount: Int,
    additionalAmount: Int,
    totalAmount: Int,
) : AggregateRoot(id), Object {
    companion object {
        const val caregiverMaxHourConst: Int = 10
        fun calculateBasicAmount(caregivingRoundInfo: CaregivingRoundInfo) =
            (caregivingRoundInfo.calculateCaregivingDays() * caregivingRoundInfo.dailyCaregivingCharge).toInt()

        fun calculateAdditionalAmount(
            additionalHoursCharge: Int,
            mealCost: Int,
            transportationFee: Int,
            holidayCharge: Int,
            caregiverInsuranceFee: Int,
            commissionFee: Int,
            vacationCharge: Int,
            patientConditionCharge: Int,
            covid19TestingCost: Int,
            outstandingAmount: Int,
            additionalCharges: Collection<AdditionalCharge>,
        ): Int {
            val additionalAmount = (
                additionalHoursCharge +
                    mealCost +
                    transportationFee +
                    holidayCharge +
                    caregiverInsuranceFee +
                    commissionFee +
                    vacationCharge +
                    patientConditionCharge +
                    covid19TestingCost +
                    outstandingAmount
                )

            val additionalCharge = additionalCharges.sumOf {
                it.amount
            }
            return additionalAmount + additionalCharge
        }

        fun calculateTotalAmount(basicAmount: Int, additionalAmount: Int) = basicAmount + additionalAmount
    }

    constructor(
        id: String,
        caregivingRoundInfo: CaregivingRoundInfo,
        caregivingChargeDate: LocalDate = LocalDate.now(),
        additionalHoursCharge: Int,
        mealCost: Int,
        transportationFee: Int,
        holidayCharge: Int,
        caregiverInsuranceFee: Int,
        commissionFee: Int,
        vacationCharge: Int,
        patientConditionCharge: Int,
        covid19TestingCost: Int,
        outstandingAmount: Int,
        additionalCharges: List<AdditionalCharge>,
        isCancelAfterArrived: Boolean,
        expectedSettlementDate: LocalDate,
    ) : this(
        id,
        caregivingRoundInfo,
        caregivingChargeDate,
        additionalHoursCharge,
        mealCost,
        transportationFee,
        holidayCharge,
        caregiverInsuranceFee,
        commissionFee,
        vacationCharge,
        patientConditionCharge,
        covid19TestingCost,
        outstandingAmount,
        additionalCharges,
        isCancelAfterArrived,
        expectedSettlementDate,
        basicAmount = calculateBasicAmount(caregivingRoundInfo),
        additionalAmount = calculateAdditionalAmount(
            additionalHoursCharge,
            mealCost,
            transportationFee,
            holidayCharge,
            caregiverInsuranceFee,
            commissionFee,
            vacationCharge,
            patientConditionCharge,
            covid19TestingCost,
            outstandingAmount,
            additionalCharges,
        ),
        totalAmount = calculateTotalAmount(
            calculateBasicAmount(caregivingRoundInfo),
            calculateAdditionalAmount(
                additionalHoursCharge,
                mealCost,
                transportationFee,
                holidayCharge,
                caregiverInsuranceFee,
                commissionFee,
                vacationCharge,
                patientConditionCharge,
                covid19TestingCost,
                outstandingAmount,
                additionalCharges,
            ),
        ),
    ) {
        ensureAdditionalChargeNotDuplicated(additionalCharges)
        registerCaregivingChargeCalculated()
    }

    var basicAmount = basicAmount
        protected set

    var additionalAmount = additionalAmount
        protected set

    var totalAmount = totalAmount
        protected set

    var additionalHoursCharge = additionalHoursCharge
        protected set

    var mealCost = mealCost
        protected set

    var transportationFee = transportationFee
        protected set

    var holidayCharge = holidayCharge
        protected set

    var caregiverInsuranceFee = caregiverInsuranceFee
        protected set

    var commissionFee = commissionFee
        protected set

    var vacationCharge = vacationCharge
        protected set

    var patientConditionCharge = patientConditionCharge
        protected set

    @Column(name = "covid19_testing_cost")
    var covid19TestingCost = covid19TestingCost
        protected set

    var outstandingAmount = outstandingAmount
        protected set

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "caregiving_additional_etc_charge")
    @OrderBy("sequence ASC")
    protected val internalAdditionalCharges = additionalCharges.intoOrderedAdditionalCharges()

    val additionalCharges: List<AdditionalCharge>
        get() = internalAdditionalCharges.map {
            AdditionalCharge(
                name = it.name,
                amount = it.amount,
            )
        }

    var isCancelAfterArrived = isCancelAfterArrived
        protected set

    var expectedSettlementDate = expectedSettlementDate
        protected set

    @Enumerated(EnumType.STRING)
    var caregivingChargeConfirmStatus: CaregivingChargeConfirmStatus = CaregivingChargeConfirmStatus.NOT_STARTED
        protected set

    protected var caregivingManagerInfo: CaregivingManagerInfo? = null

    @Embeddable
    protected data class OrderedAdditionalCharge(
        @Access(AccessType.FIELD)
        val name: String,
        val amount: Int,
        val sequence: Int,
    )

    data class AdditionalCharge(
        val name: String,
        val amount: Int,
    )

    private fun List<AdditionalCharge>.intoOrderedAdditionalCharges() = this.asSequence().withIndex().map {
        OrderedAdditionalCharge(
            name = it.value.name,
            amount = it.value.amount,
            sequence = it.index,
        )
    }.toMutableList()

    @Embeddable
    data class CaregivingRoundInfo(
        var caregivingRoundId: String,
        var caregivingRoundNumber: Int,
        var startDateTime: LocalDateTime,
        var endDateTime: LocalDateTime,
        var dailyCaregivingCharge: Int,
        var receptionId: String,
    ) {
        fun calculateCaregivingDays(): Long {
            if (calculateCaregivingHours() > caregiverMaxHourConst) {
                return Duration.between(startDateTime, endDateTime).toDays() + 1
            }
            return Duration.between(startDateTime, endDateTime).toDays()
        }

        fun calculateCaregivingHours(): Long {
            return Duration.between(startDateTime, endDateTime).toHoursPart().toLong()
        }
    }

    private inner class ModificationTracker {
        private val previous = generateTrackedData()

        fun getModificationEvent(subject: Subject): CaregivingChargeModified? {
            val current = generateTrackedData()
            if (previous == current) {
                return null
            }

            return generateModificationEvent(previous, current, subject)
        }

        private fun generateModificationEvent(
            previous: TrackedData,
            current: TrackedData,
            subject: Subject
        ): CaregivingChargeModified {
            val modification = Modification(previous, current)

            return CaregivingChargeModified(
                receptionId = caregivingRoundInfo.receptionId,
                caregivingRoundId = caregivingRoundInfo.caregivingRoundId,
                caregivingRoundNumber = caregivingRoundInfo.caregivingRoundNumber,
                basicAmount = current.basicAmount,
                additionalAmount = current.additionalAmount,
                totalAmount = current.totalAmount,
                expectedSettlementDate = modification.map { it.expectedSettlementDate },
                additionalHoursCharge = modification.map { it.additionalHoursCharge },
                mealCost = modification.map { it.mealCost },
                transportationFee = modification.map { it.transportationFee },
                holidayCharge = modification.map { it.holidayCharge },
                caregiverInsuranceFee = modification.map { it.caregiverInsuranceFee },
                commissionFee = modification.map { it.commissionFee },
                vacationCharge = modification.map { it.vacationCharge },
                patientConditionCharge = modification.map { it.patientConditionCharge },
                covid19TestingCost = modification.map { it.covid19TestingCost },
                additionalCharges = modification.map { it.additionalCharges },
                outstandingAmount = modification.map { it.outstandingAmount },
                isCancelAfterArrived = modification.map { it.isCancelAfterArrived },
                confirmStatus = current.confirmStatus,
                editingSubject = subject,
            )
        }

        private fun generateTrackedData() = TrackedData(
            basicAmount = basicAmount,
            additionalAmount = additionalAmount,
            totalAmount = totalAmount,
            expectedSettlementDate = expectedSettlementDate,
            confirmStatus = caregivingChargeConfirmStatus,
            additionalHoursCharge = additionalHoursCharge,
            mealCost = mealCost,
            transportationFee = transportationFee,
            holidayCharge = holidayCharge,
            caregiverInsuranceFee = caregiverInsuranceFee,
            commissionFee = commissionFee,
            vacationCharge = vacationCharge,
            patientConditionCharge = patientConditionCharge,
            covid19TestingCost = covid19TestingCost,
            additionalCharges = additionalCharges,
            outstandingAmount = outstandingAmount,
            isCancelAfterArrived = isCancelAfterArrived,
        )
    }

    private data class TrackedData(
        val basicAmount: Int,
        val additionalAmount: Int,
        val totalAmount: Int,
        val expectedSettlementDate: LocalDate,
        val confirmStatus: CaregivingChargeConfirmStatus,
        val additionalHoursCharge: Int,
        val mealCost: Int,
        val transportationFee: Int,
        val holidayCharge: Int,
        val caregiverInsuranceFee: Int,
        val commissionFee: Int,
        val vacationCharge: Int,
        val patientConditionCharge: Int,
        val covid19TestingCost: Int,
        val additionalCharges: List<AdditionalCharge>,
        val outstandingAmount: Int,
        val isCancelAfterArrived: Boolean,
    )

    @Transient
    private var modificationTracker: ModificationTracker? = null

    private fun initModificationTracking() {
        if (this.modificationTracker == null) {
            this.modificationTracker = ModificationTracker()
        }
    }

    private fun updateModifiedEvent(subject: Subject) {
        val modifiedEvent = modificationTracker?.getModificationEvent(subject)
        this.updateEvent(CaregivingChargeModified::class, modifiedEvent)
    }

    private fun trackModification(subject: Subject, block: () -> Unit) {
        initModificationTracking()
        block()
        updateModifiedEvent(subject)
    }

    fun edit(command: CaregivingChargeEditingCommand) = trackModification(command.subject) {
        ensureAdditionalChargeNotDuplicated(command.additionalCharges)
        CaregivingRoundAccessPolicy.check(command.subject, command, this)
        editCaregivingChargeConfirmStatus(command.caregivingChargeConfirmStatus)
        this.additionalHoursCharge = command.additionalHoursCharge
        this.mealCost = command.mealCost
        this.transportationFee = command.transportationFee
        this.holidayCharge = command.holidayCharge
        this.caregiverInsuranceFee = command.caregiverInsuranceFee
        this.commissionFee = command.commissionFee
        this.vacationCharge = command.vacationCharge
        this.patientConditionCharge = command.patientConditionCharge
        this.covid19TestingCost = command.covid19TestingCost
        this.outstandingAmount = command.outstandingAmount
        this.internalAdditionalCharges.clear()
        this.internalAdditionalCharges.addAll(command.additionalCharges.intoOrderedAdditionalCharges())
        this.isCancelAfterArrived = command.isCancelAfterArrived
        this.expectedSettlementDate = command.expectedSettlementDate
        updateBasicAmount()
        updateAdditionalAmount()
        updateTotalAmount()
    }

    private fun updateBasicAmount() {
        this.basicAmount = calculateBasicAmount(caregivingRoundInfo)
    }

    private fun updateAdditionalAmount() {
        this.additionalAmount = calculateAdditionalAmount(
            additionalHoursCharge,
            mealCost,
            transportationFee,
            holidayCharge,
            caregiverInsuranceFee,
            commissionFee,
            vacationCharge,
            patientConditionCharge,
            covid19TestingCost,
            outstandingAmount,
            additionalCharges,
        )
    }

    private fun updateTotalAmount() {
        this.totalAmount = calculateTotalAmount(basicAmount, additionalAmount)
    }

    private fun editCaregivingChargeConfirmStatus(caregivingChargeConfirmStatus: CaregivingChargeConfirmStatus) {
        if (caregivingChargeConfirmStatus == this.caregivingChargeConfirmStatus) {
            return
        }

        when (caregivingChargeConfirmStatus) {
            CaregivingChargeConfirmStatus.CONFIRMED -> this.progressToConfirmed()
            else -> Unit
        }
    }

    private fun progressToConfirmed() {
        if (this.caregivingChargeConfirmStatus != CaregivingChargeConfirmStatus.NOT_STARTED) {
            throw InvalidCaregivingChargeConfirmStatusTransitionException(
                this.caregivingChargeConfirmStatus,
                CaregivingChargeConfirmStatus.CONFIRMED
            )
        }
        this.caregivingChargeConfirmStatus = CaregivingChargeConfirmStatus.CONFIRMED
    }

    private fun ensureAdditionalChargeNotDuplicated(additionalCharges: Collection<AdditionalCharge>) =
        additionalCharges.groupingBy { it.name }.eachCount()
            .let { eachCount ->
                eachCount.any { it.value > 1 }.thenThrows {
                    throw CaregivingAdditionalChargeNameDuplicatedException(
                        eachCount.asSequence()
                            .filter { it.value > 1 }
                            .map { it.key }.toSet()
                    )
                }
            }

    private fun registerCaregivingChargeCalculated() {
        registerEvent(
            CaregivingChargeCalculated(
                receptionId = caregivingRoundInfo.receptionId,
                caregivingRoundId = caregivingRoundInfo.caregivingRoundId,
                roundNumber = caregivingRoundInfo.caregivingRoundNumber,
                dailyCaregivingCharge = caregivingRoundInfo.dailyCaregivingCharge,
                basicAmount = basicAmount,
                additionalAmount = additionalAmount,
                totalAmount = totalAmount,
                expectedSettlementDate = expectedSettlementDate,
                isCancelAfterArrived = isCancelAfterArrived,
            )
        )
    }

    fun handleReceptionModified(event: ReceptionModified) {
        event.caregivingManagerInfo.ifChanged {
            caregivingManagerInfo = current
        }
    }

    override fun get(attribute: ObjectAttribute) = when (attribute) {
        ObjectAttribute.ASSIGNED_ORGANIZATION_ID -> caregivingManagerInfo?.organizationId?.let { setOf(it) } ?: setOf()
        else -> setOf()
    }
}
