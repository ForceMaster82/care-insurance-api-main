package kr.caredoc.careinsurance.reception

import com.github.guepardoapps.kulid.ULID
import kr.caredoc.careinsurance.caregiving.AllCaregivingRoundReconciliationCompleted
import kr.caredoc.careinsurance.caregiving.CaregiverAssignedToCaregivingRound
import kr.caredoc.careinsurance.caregiving.CaregivingRoundStarted
import kr.caredoc.careinsurance.coverage.CoverageByIdQuery
import kr.caredoc.careinsurance.coverage.CoverageByIdQueryHandler
import kr.caredoc.careinsurance.coverage.CoverageNotFoundByIdException
import kr.caredoc.careinsurance.file.FileByUrlQuery
import kr.caredoc.careinsurance.file.FileByUrlQueryHandler
import kr.caredoc.careinsurance.file.FileSavingCommand
import kr.caredoc.careinsurance.file.FileSavingCommandHandler
import kr.caredoc.careinsurance.reception.exception.InvalidReceptionProgressingStatusTransitionException
import kr.caredoc.careinsurance.reception.exception.ManagingUserNotFoundException
import kr.caredoc.careinsurance.reception.exception.ReceptionApplicationNotFoundException
import kr.caredoc.careinsurance.reception.exception.ReceptionNotFoundByIdException
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.ReadOneAccess
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.security.accesscontrol.checkAll
import kr.caredoc.careinsurance.security.encryption.Decryptor
import kr.caredoc.careinsurance.security.personaldata.IncludingPersonalData
import kr.caredoc.careinsurance.security.personaldata.PatientInfoEncryptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReceptionService(
    private val receptionRepository: ReceptionRepository,
    private val coverageByIdQueryHandler: CoverageByIdQueryHandler,
    private val saveFileSavingCommandHandler: FileSavingCommandHandler,
    private val fileByUrlQueryHandler: FileByUrlQueryHandler,
    private val encryptor: PatientInfoEncryptor,
    private val decryptor: Decryptor,
    @Value("\${cloud.aws.s3.bucket.careinsurance-reception-application}")
    private val receptionApplicationBucket: String
) : ReceptionCreationCommandHandler,
    ReceptionsByFilterQueryHandler,
    ReceptionByIdQueryHandler,
    ReceptionEditingCommandHandler,
    ReceptionsByIdsQueryHandler,
    ReceptionApplicationCreationCommandHandler,
    ReceptionApplicationByReceptionIdQueryHandler {
    @Transactional
    override fun createReception(@IncludingPersonalData command: ReceptionCreationCommand): ReceptionCreationResult {
        ReceptionAccessPolicy.check(command.subject, command, Object.Empty)
        ensureReferenceCoverageExists(command.insuranceInfo.coverageId, command.subject)

        val entity = command.intoEntity()
        receptionRepository.save(entity)

        return entity.intoCreationResult()
    }

    private fun Reception.intoCreationResult() = ReceptionCreationResult(id)

    private fun ReceptionCreationCommand.intoEntity() = Reception(
        id = ULID.random(),
        insuranceInfo = this.insuranceInfo,
        patientInfo = encryptor.encrypt(this.patientInfo),
        accidentInfo = this.accidentInfo,
        insuranceManagerInfo = this.insuranceManagerInfo,
        receivedDateTime = this.receivedDateTime,
        desiredCaregivingStartDate = this.desiredCaregivingStartDate,
        urgency = this.urgency,
        desiredCaregivingPeriod = this.desiredCaregivingPeriod,
        registerManagerInfo = this.registerManagerInfo,
        notifyCaregivingProgress = this.notifyCaregivingProgress,
        additionalRequests = this.additionalRequests,
    )

    private fun ensureReferenceCoverageExists(coverageId: String, subject: Subject) = try {
        coverageByIdQueryHandler.ensureCoverageExists(
            CoverageByIdQuery(
                coverageId = coverageId,
                subject = subject,
            )
        )
    } catch (e: CoverageNotFoundByIdException) {
        throw ReferenceCoverageNotExistsException(e.coverageId, e)
    }

    @Transactional(readOnly = true)
    override fun getReceptions(
        @IncludingPersonalData query: ReceptionsByFilterQuery,
        pageRequest: Pageable,
    ): Page<Reception> {
        val receptions = try {
            receptionRepository.searchReceptions(query.intoSearchCriteria(), pageRequest)
        } catch (e: ManagingUserNotFoundException) {
            return PageImpl(listOf(), pageRequest, 0)
        }
        receptions.forEach {
            ReceptionAccessPolicy.check(query.subject, ReceptionByIdQuery(it.id, query.subject), it)
        }

        return receptions
    }

    private fun ReceptionsByFilterQuery.intoSearchCriteria() = ReceptionSearchingRepository.SearchingCriteria(
        from = this.from,
        until = this.until,
        urgency = this.urgency,
        periodType = this.periodType,
        caregivingManagerAssigned = this.caregivingManagerAssigned,
        organizationType = this.organizationType,
        progressingStatuses = this.progressingStatuses,
        insuranceNumberContains = this.getKeyword(
            propertyToExtractionKeyword = ReceptionsByFilterQuery.SearchingProperty.INSURANCE_NUMBER
        ),
        patientNameContains = this.getKeyword(
            propertyToExtractionKeyword = ReceptionsByFilterQuery.SearchingProperty.PATIENT_NAME
        ),
        patientPhoneNumberContains = this.getKeyword(
            propertyToExtractionKeyword = ReceptionsByFilterQuery.SearchingProperty.PATIENT_PHONE_NUMBER
        ),
        accidentNumberContains = this.getKeyword(
            propertyToExtractionKeyword = ReceptionsByFilterQuery.SearchingProperty.ACCIDENT_NUMBER
        ),
        managerNameContains = this.getKeyword(
            propertyToExtractionKeyword = ReceptionsByFilterQuery.SearchingProperty.CAREGIVING_MANAGER_NAME
        ),
    )

    private fun ReceptionsByFilterQuery.getKeyword(
        propertyToExtractionKeyword: ReceptionsByFilterQuery.SearchingProperty
    ) = if (searchCondition?.searchingProperty == propertyToExtractionKeyword) {
        searchCondition.keyword
    } else {
        null
    }

    @Transactional(readOnly = true)
    override fun getReception(query: ReceptionByIdQuery): Reception {
        val reception = receptionRepository.findByIdOrNull(query.receptionId)
            ?: throw ReceptionNotFoundByIdException(query.receptionId)

        ReceptionAccessPolicy.check(query.subject, query, reception)

        return reception
    }

    @Transactional
    override fun <T> getReception(query: ReceptionByIdQuery, mapper: Reception.() -> T) = mapper(getReception(query))

    @Transactional
    override fun ensureReceptionExists(query: ReceptionByIdQuery) {
        getReception(query)
    }

    @Transactional
    override fun editReception(query: ReceptionByIdQuery, @IncludingPersonalData command: ReceptionEditingCommand) {
        val reception = getReception(query)

        reception.inEncryptionContext(encryptor, decryptor) {
            edit(command)
        }

        receptionRepository.save(reception)
    }

    @Transactional
    @EventListener(CaregiverAssignedToCaregivingRound::class)
    fun handleCaregiverAssignedToCaregivingRound(@IncludingPersonalData e: CaregiverAssignedToCaregivingRound) {
        if (e.caregivingRoundNumber != 1) {
            return
        }

        receptionRepository.findByIdOrNull(e.receptionId)
            ?.run {
                try {
                    editProgressingStatus(ReceptionProgressingStatus.MATCHING, e.subject)
                    receptionRepository.save(this)
                } catch (e: InvalidReceptionProgressingStatusTransitionException) {
                    // do nothing
                }
            }
    }

    @Transactional
    @EventListener(CaregivingRoundStarted::class)
    fun handleCaregivingRoundStarted(e: CaregivingRoundStarted) {
        if (e.caregivingRoundNumber != 1) {
            return
        }

        receptionRepository.findByIdOrNull(e.receptionId)
            ?.run {
                editProgressingStatus(ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS, e.subject)
                receptionRepository.save(this)
            }
    }

    @Transactional
    override fun getReceptions(query: ReceptionsByIdsQuery): List<Reception> {
        val receptions = receptionRepository.findByIdIn(query.receptionIds)

        ReceptionAccessPolicy.checkAll(query.subject, ReadOneAccess, receptions)
        ensureAllReceptionFound(receptions, query.receptionIds)

        return receptions
    }

    private fun ensureAllReceptionFound(foundReceptions: Collection<Reception>, targetIds: Collection<String>) {
        val notFoundReceptionIds = targetIds.toSet() subtract foundReceptions.map { it.id }.toSet()
        notFoundReceptionIds.firstOrNull()?.let {
            throw ReceptionNotFoundByIdException(it)
        }
    }

    @Transactional
    @EventListener(AllCaregivingRoundReconciliationCompleted::class)
    fun handleAllCaregivingRoundReconciliationCompleted(e: AllCaregivingRoundReconciliationCompleted) {
        receptionRepository.findByIdOrNull(e.receptionId)
            ?.run {
                editProgressingStatus(ReceptionProgressingStatus.COMPLETED, e.subject)
                receptionRepository.save(this)
            }
    }

    @Transactional
    override fun createReceptionApplication(receptionApplicationCreationCommand: ReceptionApplicationCreationCommand) {
        val reception = getReception(
            ReceptionByIdQuery(
                receptionId = receptionApplicationCreationCommand.receptionId,
                subject = receptionApplicationCreationCommand.subject
            )
        )

        val legacyFileInfo = reception.applicationFileInfo

        val result = saveFileSavingCommandHandler.saveFile(
            FileSavingCommand(
                bucketName = receptionApplicationBucket,
                path = ULID.random(),
                fileStream = receptionApplicationCreationCommand.file,
                contentLength = receptionApplicationCreationCommand.contentLength,
                mime = receptionApplicationCreationCommand.mime,
            )
        )

        reception.updateReceptionApplicationFileInfo(
            ReceptionApplicationFileInfo(
                receptionApplicationFileName = receptionApplicationCreationCommand.fileName,
                receptionApplicationFileUrl = result.savedFileUrl,
            ),
            receptionApplicationCreationCommand.subject,
        )

        receptionRepository.save(reception)

        if (legacyFileInfo != null) {
            fileByUrlQueryHandler.deleteFile(FileByUrlQuery(legacyFileInfo.receptionApplicationFileUrl))
        }
    }

    @Transactional
    override fun getReceptionApplication(query: ReceptionApplicationByReceptionIdQuery): ReceptionApplicationFileInfoResult {
        val reception = getReception(
            ReceptionByIdQuery(
                receptionId = query.receptionId,
                subject = query.subject,
            )
        )

        val applicationFileInfo = reception.applicationFileInfo
        return if (applicationFileInfo != null) {
            ReceptionApplicationFileInfoResult(
                receptionApplicationFileName = applicationFileInfo.receptionApplicationFileName,
                receptionApplicationFileUrl = applicationFileInfo.receptionApplicationFileUrl,
            )
        } else {
            throw ReceptionApplicationNotFoundException(query.receptionId)
        }
    }

    override fun deleteReceptionApplication(query: ReceptionApplicationByReceptionIdQuery) {
        val reception = getReception(
            ReceptionByIdQuery(
                receptionId = query.receptionId,
                subject = query.subject,
            )
        )

        val applicationFileInfo = reception.applicationFileInfo
        if (applicationFileInfo != null) {
            fileByUrlQueryHandler.deleteFile(FileByUrlQuery(applicationFileInfo.receptionApplicationFileUrl))
        } else {
            throw ReceptionApplicationNotFoundException(query.receptionId)
        }
    }
}
