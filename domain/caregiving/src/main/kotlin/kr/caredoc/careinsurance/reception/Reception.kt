package kr.caredoc.careinsurance.reception

import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import kr.caredoc.careinsurance.AggregateRoot
import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.insurance.InsuranceInfo
import kr.caredoc.careinsurance.insurance.InsuranceManagerInfo
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.patient.EncryptedPatientInfo
import kr.caredoc.careinsurance.reception.exception.InvalidReceptionProgressingStatusTransitionException
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.ObjectAttribute
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.security.encryption.Decryptor
import kr.caredoc.careinsurance.security.personaldata.PatientInfoEncryptor
import kr.caredoc.careinsurance.security.personaldata.PersonalDataRevealingAction
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
class Reception(
    id: String,
    insuranceInfo: InsuranceInfo,
    patientInfo: EncryptedPatientInfo,
    accidentInfo: AccidentInfo,
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "branchName", column = Column(name = "insurance_manager_branch_name")),
        AttributeOverride(name = "phoneNumber", column = Column(name = "insurance_manager_phone_number")),
    )
    val insuranceManagerInfo: InsuranceManagerInfo,
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "managingUserId", column = Column(name = "register_manager_id")),
    )
    val registerManagerInfo: RegisterManagerInfo,
    val receivedDateTime: LocalDateTime,
    desiredCaregivingStartDate: LocalDate,
    @Enumerated(EnumType.STRING)
    val urgency: Urgency,
    desiredCaregivingPeriod: Int?,
    additionalRequests: String,
    caregivingManagerInfo: CaregivingManagerInfo? = null,
    notifyCaregivingProgress: Boolean,
) : AggregateRoot(id), Object {
    @Transient
    private var logger = LoggerFactory.getLogger(javaClass)

    @Embedded
    var insuranceInfo = insuranceInfo
        protected set

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "nickname", column = Column(name = "patient_nickname")),
        AttributeOverride(name = "age", column = Column(name = "patient_age")),
        AttributeOverride(name = "sex", column = Column(name = "patient_sex")),
        AttributeOverride(name = "height", column = Column(name = "patient_height")),
        AttributeOverride(name = "weight", column = Column(name = "patient_weight")),
    )
    var patientInfo = patientInfo
        protected set

    @Embedded
    var accidentInfo = accidentInfo
        protected set

    var desiredCaregivingStartDate = desiredCaregivingStartDate
        protected set

    var desiredCaregivingPeriod = desiredCaregivingPeriod
        protected set

    var additionalRequests = additionalRequests
        protected set

    @Embedded
    var caregivingManagerInfo = caregivingManagerInfo
        protected set

    @Enumerated(EnumType.STRING)
    var progressingStatus: ReceptionProgressingStatus = ReceptionProgressingStatus.RECEIVED
        protected set

    @Enumerated(EnumType.STRING)
    var periodType: PeriodType = desiredCaregivingPeriod?.let {
        determinePeriodType(it)
    } ?: PeriodType.NORMAL

    @Embedded
    var applicationFileInfo: ReceptionApplicationFileInfo? = null

    private fun determinePeriodType(desiredCaregivingPeriod: Int): PeriodType {
        return if (desiredCaregivingPeriod > 3) {
            PeriodType.NORMAL
        } else {
            PeriodType.SHORT
        }
    }

    private fun updatePeriodType() {
        periodType = desiredCaregivingPeriod?.let {
            determinePeriodType(it)
        } ?: PeriodType.NORMAL
    }

    var expectedCaregivingLimitDate: LocalDate =
        receivedDateTime.toLocalDate().plusDays(insuranceInfo.caregivingLimitPeriod.toLong())
        protected set

    var reasonForCancellation: String? = null
        protected set

    var canceledDateTime: LocalDateTime? = null
        protected set

    var expectedCaregivingStartDate: LocalDate? = null

    var notifyCaregivingProgress: Boolean = notifyCaregivingProgress
        protected set

    enum class Urgency {
        NORMAL,
        URGENT,
    }

    enum class PeriodType {
        NORMAL,
        SHORT,
    }

    enum class PersonalData {
        PATIENT_NAME,
        PRIMARY_CONTACT,
        SECONDARY_CONTACT,
    }

    private fun assignCaregivingManager(caregivingManagerInfo: CaregivingManagerInfo, subject: Subject) {
        if (this.caregivingManagerInfo == null) {
            this.registerEvent(
                CaregivingManagerAssignedToReception(
                    receptionId = this.id,
                    caregivingManagerInfo = caregivingManagerInfo,
                    subject = subject,
                )
            )
        }
        this.caregivingManagerInfo = caregivingManagerInfo
    }

    fun editProgressingStatus(progressingStatus: ReceptionProgressingStatus, subject: Subject) =
        trackModification(subject = subject) {
            if (progressingStatus == this.progressingStatus) {
                return@trackModification
            }

            when (progressingStatus) {
                ReceptionProgressingStatus.PENDING -> this.progressToPending()
                ReceptionProgressingStatus.MATCHING -> this.progressToMatching()
                ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS -> this.progressToCaregivingInProgress()
                ReceptionProgressingStatus.PENDING_MATCHING -> this.progressToPendingMatching()
                ReceptionProgressingStatus.COMPLETED -> this.progressToCompleted()
                else -> Unit
            }
        }

    private fun progressToMatching() {
        val allowedCurrentStatuses = setOf(ReceptionProgressingStatus.RECEIVED, ReceptionProgressingStatus.PENDING)
        if (!allowedCurrentStatuses.contains(progressingStatus)) {
            throw InvalidReceptionProgressingStatusTransitionException(
                this.progressingStatus,
                ReceptionProgressingStatus.MATCHING,
            )
        }
        this.progressingStatus = ReceptionProgressingStatus.MATCHING
    }

    private fun progressToCaregivingInProgress() {
        if (progressingStatus != ReceptionProgressingStatus.MATCHING) {
            throw InvalidReceptionProgressingStatusTransitionException(
                this.progressingStatus,
                ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS,
            )
        }
        this.progressingStatus = ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
    }

    private fun progressToPending() {
        if (progressingStatus != ReceptionProgressingStatus.RECEIVED) {
            throw InvalidReceptionProgressingStatusTransitionException(
                this.progressingStatus,
                ReceptionProgressingStatus.PENDING,
            )
        }
        this.progressingStatus = ReceptionProgressingStatus.PENDING
    }

    private fun progressToPendingMatching() {
        if (progressingStatus != ReceptionProgressingStatus.MATCHING) {
            throw InvalidReceptionProgressingStatusTransitionException(
                this.progressingStatus,
                ReceptionProgressingStatus.PENDING_MATCHING,
            )
        }
        this.progressingStatus = ReceptionProgressingStatus.PENDING_MATCHING
    }

    private fun progressToCompleted() {
        if (progressingStatus != ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS) {
            throw InvalidReceptionProgressingStatusTransitionException(
                this.progressingStatus,
                ReceptionProgressingStatus.COMPLETED,
            )
        }
        this.progressingStatus = ReceptionProgressingStatus.COMPLETED
    }

    private fun cancel(progressingStatus: ReceptionProgressingStatus, reasonForCancellation: String) {
        val availablePreviousStatusesByTargetStatus = mapOf(
            ReceptionProgressingStatus.CANCELED to setOf(
                ReceptionProgressingStatus.RECEIVED,
                ReceptionProgressingStatus.PENDING,
            ),
            ReceptionProgressingStatus.CANCELED_BY_PERSONAL_CAREGIVER to setOf(
                ReceptionProgressingStatus.RECEIVED,
                ReceptionProgressingStatus.PENDING,
            ),
            ReceptionProgressingStatus.CANCELED_BY_MEDICAL_REQUEST to setOf(
                ReceptionProgressingStatus.RECEIVED,
                ReceptionProgressingStatus.PENDING,
            ),
            ReceptionProgressingStatus.CANCELED_WHILE_MATCHING to setOf(
                ReceptionProgressingStatus.MATCHING,
                ReceptionProgressingStatus.PENDING_MATCHING,
            ),
        )
        if (availablePreviousStatusesByTargetStatus[progressingStatus]?.contains(this.progressingStatus) == false) {
            throw InvalidReceptionProgressingStatusTransitionException(
                this.progressingStatus,
                progressingStatus,
            )
        }
        this.progressingStatus = progressingStatus
        this.reasonForCancellation = reasonForCancellation
        this.canceledDateTime = Clock.now()
    }

    override fun get(attribute: ObjectAttribute) = when (attribute) {
        ObjectAttribute.ASSIGNED_ORGANIZATION_ID -> this.caregivingManagerInfo?.organizationId?.let { setOf(it) }
            ?: setOf()

        ObjectAttribute.INTERNAL_ONLY_MODIFIABLE_PROPERTIES -> this.generateInternalOnlyModifiableProperties()
        else -> setOf()
    }

    private fun generateInternalOnlyModifiableProperties(): Set<String> = sequenceOf(
        "INSURANCE_NUMBER" to this.insuranceInfo.insuranceNumber,
        "SUBSCRIPTION_DATE" to this.insuranceInfo.subscriptionDate,
        "COVERAGE_ID" to this.insuranceInfo.coverageId,
        "CAREGIVING_LIMIT_PERIOD" to this.insuranceInfo.caregivingLimitPeriod,
        "PATIENT_NAME" to this.patientInfo.name.toString(),
        "PATIENT_AGE" to this.patientInfo.age,
        "PATIENT_SEX" to this.patientInfo.sex,
        "PATIENT_PRIMARY_PHONE_NUMBER" to this.patientInfo.primaryContact.partialEncryptedPhoneNumber.toString(),
        "PATIENT_PRIMARY_CONTACT_OWNER" to this.patientInfo.primaryContact.relationshipWithPatient,
        "PATIENT_SECONDARY_PHONE_NUMBER" to this.patientInfo.secondaryContact?.partialEncryptedPhoneNumber?.toString(),
        "PATIENT_SECONDARY_CONTACT_OWNER" to this.patientInfo.secondaryContact?.relationshipWithPatient,
        "ACCIDENT_NUMBER" to this.accidentInfo.accidentNumber,
        "ACCIDENT_DATE_TIME" to this.accidentInfo.accidentDateTime,
        "CLAIM_TYPE" to this.accidentInfo.claimType,
        "CAREGIVING_ORGANIZATION_TYPE" to this.caregivingManagerInfo?.organizationType,
        "CAREGIVING_ORGANIZATION_ID" to this.caregivingManagerInfo?.organizationId,
        "CAREGIVING_MANAGING_USER_ID" to this.caregivingManagerInfo?.managingUserId,
        "EXPECTED_CAREGIVING_LIMIT_DATE" to this.expectedCaregivingLimitDate,
    ).map {
        "${it.first}:${it.second}"
    }.toSet()

    fun inEncryptionContext(
        encryptor: PatientInfoEncryptor,
        decryptor: Decryptor,
        block: ReceptionInEncryptionContext.() -> Unit
    ) {
        block(ReceptionInEncryptionContext(encryptor, decryptor))
    }

    fun updateReceptionApplicationFileInfo(info: ReceptionApplicationFileInfo, subject: Subject) {
        trackModification(ReceptionModified.Cause.DIRECT_EDIT, subject) {
            this.applicationFileInfo = info
        }
    }

    inner class ReceptionInEncryptionContext(
        private val encryptor: PatientInfoEncryptor,
        private val decryptor: Decryptor,
    ) {
        fun edit(command: ReceptionEditingCommand) = trackModification(subject = command.subject) {
            this@Reception.patientInfo.primaryContact.partialEncryptedPhoneNumber.decrypt(decryptor)
            this@Reception.patientInfo.secondaryContact?.partialEncryptedPhoneNumber?.decrypt(decryptor)
            this@Reception.patientInfo.name.decrypt(decryptor)
            ReceptionAccessPolicy.check(
                sub = command.subject,
                act = command,
                obj = this@Reception,
            )

            this@Reception.insuranceInfo = command.insuranceInfo
            this@Reception.patientInfo = encryptor.encrypt(command.patientInfo)
            this@Reception.accidentInfo = command.accidentInfo
            command.caregivingManagerInfo?.let { assignCaregivingManager(it, command.subject) }
            this@Reception.desiredCaregivingStartDate = command.desiredCaregivingStartDate
            this@Reception.desiredCaregivingPeriod = command.desiredCaregivingPeriod
            this@Reception.additionalRequests = command.additionalRequests
            this@Reception.expectedCaregivingLimitDate = command.expectedCaregivingLimitDate
            if (command.progressingStatus.isCancellationStatus) {
                this@Reception.cancel(command.progressingStatus, command.reasonForCancellation ?: "")
            } else {
                this@Reception.editProgressingStatus(command.progressingStatus, command.subject)
            }
            this@Reception.notifyCaregivingProgress = command.notifyCaregivingProgress
            command.expectedCaregivingStartDate?.let {
                this@Reception.expectedCaregivingStartDate = command.expectedCaregivingStartDate
            }

            updatePeriodType()
        }
    }

    private fun trackModification(
        cause: ReceptionModified.Cause = ReceptionModified.Cause.DIRECT_EDIT,
        subject: Subject,
        block: () -> Unit
    ) {
        initModificationTracking()
        block()
        updateModifiedEvent(cause, subject)
    }

    fun <T> inDecryptionContext(
        decryptor: Decryptor,
        subject: Subject,
        block: DecryptionContext.() -> T
    ): T {
        return block(DecryptionContext(decryptor, subject))
    }

    inner class DecryptionContext(
        private val decryptor: Decryptor,
        private val subject: Subject,
    ) {
        fun decryptPatientName(): String {
            ReceptionAccessPolicy.check(subject, PersonalDataRevealingAction, this@Reception)

            if (patientInfo.name.encrypted == "") {
                return ""
            }

            registerPatientPersonalDataRevealedEvent(PersonalData.PATIENT_NAME)

            return patientInfo.name.decrypt(decryptor)
        }

        fun decryptPrimaryContact(): String {
            ReceptionAccessPolicy.check(subject, PersonalDataRevealingAction, this@Reception)

            patientInfo.primaryContact.partialEncryptedPhoneNumber.decrypt(decryptor)

            registerPatientPersonalDataRevealedEvent(PersonalData.PRIMARY_CONTACT)

            return patientInfo.primaryContact.partialEncryptedPhoneNumber.toString()
        }

        fun decryptSecondaryContact(): String? {
            ReceptionAccessPolicy.check(subject, PersonalDataRevealingAction, this@Reception)

            patientInfo.secondaryContact?.partialEncryptedPhoneNumber?.decrypt(decryptor)

            registerPatientPersonalDataRevealedEvent(PersonalData.SECONDARY_CONTACT)

            return patientInfo.secondaryContact?.partialEncryptedPhoneNumber?.toString()
        }

        private fun registerPatientPersonalDataRevealedEvent(
            revealedData: PersonalData,
        ) {
            registerEvent(
                PatientPersonalDataRevealed(
                    receptionId = id,
                    revealedAt = Clock.now(),
                    revealedPersonalData = revealedData,
                    revealingSubject = subject,
                )
            )
        }
    }

    @Transient
    private var modificationTracker: ModificationTracker? = null

    private fun initModificationTracking() {
        if (this.modificationTracker == null) {
            this.modificationTracker = ModificationTracker()
        }
    }

    private fun updateModifiedEvent(cause: ReceptionModified.Cause, subject: Subject) {
        val modifiedEvent = modificationTracker?.getModification(cause, subject)
        this.updateEvent(ReceptionModified::class, modifiedEvent)
    }

    private inner class ModificationTracker {
        private val previous = generateTrackedData(this@Reception)

        fun getModification(cause: ReceptionModified.Cause, subject: Subject): ReceptionModified? {
            val current = generateTrackedData(this@Reception)
            if (previous == current) {
                return null
            }

            return generateModificationData(current, cause, subject)
        }

        fun generateTrackedData(reception: Reception) = TrackedData(
            insuranceInfo = reception.insuranceInfo,
            accidentInfo = reception.accidentInfo,
            patientInfo = reception.patientInfo,
            expectedCaregivingStartDate = reception.expectedCaregivingStartDate,
            notifyCaregivingProgress = reception.notifyCaregivingProgress,
            progressingStatus = reception.progressingStatus,
            caregivingManagerInfo = reception.caregivingManagerInfo,
            periodType = reception.periodType,
            applicationFileInfo = reception.applicationFileInfo,
            desiredCaregivingStartDate = desiredCaregivingStartDate,
            urgency = reception.urgency,
            expectedCaregivingLimitDate = reception.expectedCaregivingLimitDate,
            desiredCaregivingPeriod = reception.desiredCaregivingPeriod,
            additionalRequests = reception.additionalRequests,
        )

        private fun generateModificationData(
            status: TrackedData,
            cause: ReceptionModified.Cause,
            subject: Subject
        ) = ReceptionModified(
            receptionId = id,
            insuranceInfo = Modification(previous.insuranceInfo, status.insuranceInfo),
            accidentInfo = Modification(previous.accidentInfo, status.accidentInfo),
            patientInfo = Modification(previous.patientInfo, status.patientInfo),
            expectedCaregivingStartDate = Modification(
                previous.expectedCaregivingStartDate,
                status.expectedCaregivingStartDate,
            ),
            progressingStatus = Modification(previous.progressingStatus, status.progressingStatus),
            caregivingManagerInfo = Modification(previous.caregivingManagerInfo, status.caregivingManagerInfo),
            periodType = Modification(previous.periodType, status.periodType),
            applicationFileInfo = Modification(previous.applicationFileInfo, status.applicationFileInfo),
            desiredCaregivingStartDate = Modification(
                previous.desiredCaregivingStartDate,
                status.desiredCaregivingStartDate,
            ),
            receivedDateTime = receivedDateTime,
            urgency = status.urgency,
            expectedCaregivingLimitDate = Modification(
                previous.expectedCaregivingLimitDate,
                status.expectedCaregivingLimitDate,
            ),
            notifyCaregivingProgress = Modification(previous.notifyCaregivingProgress, status.notifyCaregivingProgress),
            desiredCaregivingPeriod = Modification(previous.desiredCaregivingPeriod, status.desiredCaregivingPeriod),
            additionalRequests = Modification(previous.additionalRequests, status.additionalRequests),
            cause = cause,
            editingSubject = subject,
        )
    }

    private data class TrackedData(
        val insuranceInfo: InsuranceInfo,
        val accidentInfo: AccidentInfo,
        val patientInfo: EncryptedPatientInfo,
        val expectedCaregivingStartDate: LocalDate?,
        val notifyCaregivingProgress: Boolean,
        val progressingStatus: ReceptionProgressingStatus,
        val caregivingManagerInfo: CaregivingManagerInfo?,
        val periodType: PeriodType,
        val applicationFileInfo: ReceptionApplicationFileInfo?,
        val desiredCaregivingStartDate: LocalDate,
        val urgency: Urgency,
        val expectedCaregivingLimitDate: LocalDate,
        val desiredCaregivingPeriod: Int?,
        val additionalRequests: String,
    )

    fun deletePatientPersonalData(subject: Subject, cause: ReceptionModified.Cause) =
        trackModification(cause, subject) {
            this.patientInfo = EncryptedPatientInfo(
                name = EncryptedPatientInfo.EncryptedPatientName.Empty,
                nickname = this.patientInfo.nickname,
                age = this.patientInfo.age,
                height = this.patientInfo.height,
                weight = this.patientInfo.weight,
                sex = this.patientInfo.sex,
                primaryContact = EncryptedPatientInfo.EncryptedContact.Empty,
                secondaryContact = null,
            )

            logger.info("personal data deleted from Reception($id)")
        }

    init {
        registerEvent(
            ReceptionReceived(
                receptionId = id,
                receivedDateTime = receivedDateTime,
                desiredCaregivingStartDate = desiredCaregivingStartDate,
                urgency = urgency,
                periodType = periodType,
            )
        )
    }

    override fun postLoad() {
        super.postLoad()
        this.logger = LoggerFactory.getLogger(javaClass)
    }
}
