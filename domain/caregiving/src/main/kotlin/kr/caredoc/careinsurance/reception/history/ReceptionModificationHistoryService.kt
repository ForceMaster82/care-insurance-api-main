package kr.caredoc.careinsurance.reception.history

import com.github.guepardoapps.kulid.ULID
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.reception.ReceptionByIdQuery
import kr.caredoc.careinsurance.reception.ReceptionByIdQueryHandler
import kr.caredoc.careinsurance.reception.ReceptionModified
import kr.caredoc.careinsurance.reception.ReceptionReceived
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.SubjectAttribute
import kr.caredoc.careinsurance.security.personaldata.IncludingPersonalData
import kr.caredoc.careinsurance.withSort
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReceptionModificationHistoryService(
    private val receptionModificationHistoryRepository: ReceptionModificationHistoryRepository,
    private val receptionByIdQueryHandler: ReceptionByIdQueryHandler,
    private val receptionModificationSummaryRepository: ReceptionModificationSummaryRepository,
) : ReceptionModificationHistoriesByReceptionIdQueryHandler,
    ReceptionModificationSummaryByReceptionIdQueryHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    @EventListener(ReceptionModified::class)
    fun handleReceptionModified(@IncludingPersonalData event: ReceptionModified) {
        if (event.cause != ReceptionModified.Cause.DIRECT_EDIT) {
            return
        }

        val modifierId = event.editingSubject[SubjectAttribute.USER_ID].firstOrNull()
        if (modifierId == null) {
            logger.warn("사용자가 로그인하였지만 사용자의 아이디를 추적할 수 없습니다. 시스템에서 생성된것으로 간주하고 감사 로그를 작성하지 않습니다.")
            return
        }

        val modificationHistoryRecords = mutableListOf<ReceptionModificationHistory>()
        fun addModificationHistoryIfChanged(
            property: ReceptionModificationHistory.ModificationProperty,
            modification: Modification<String?>
        ) {
            if (!modification.hasChanged) {
                return
            }

            modificationHistoryRecords.add(
                ReceptionModificationHistory(
                    id = ULID.random(),
                    receptionId = event.receptionId,
                    modifiedProperty = property,
                    previous = modification.previous,
                    modified = modification.current,
                    modifierId = modifierId,
                    modifiedDateTime = event.modifiedDateTime,
                )
            )
        }

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.INSURANCE_NUMBER,
            event.insuranceInfo.map { it.insuranceNumber },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.COVERAGE_ID,
            event.insuranceInfo.map { it.coverageId },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.SUBSCRIPTION_DATE,
            event.insuranceInfo.map { it.subscriptionDate.toString() },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.CAREGIVING_LIMIT_PERIOD,
            event.insuranceInfo.map { it.caregivingLimitPeriod.toString() },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.ACCIDENT_NUMBER,
            event.accidentInfo.map { it.accidentNumber },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.PATIENT_DESCRIPTION,
            event.accidentInfo.map { it.patientDescription },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.ACCIDENT_DATE_TIME,
            event.accidentInfo.map { it.accidentDateTime.toString() },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.CLAIM_TYPE,
            event.accidentInfo.map { it.claimType.name },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.ADMISSION_DATE_TIME,
            event.accidentInfo.map { it.admissionDateTime.toString() },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.HOSPITAL_AND_ROOM,
            event.accidentInfo.map { it.hospitalAndRoomInfo.hospitalAndRoom },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.HOSPITAL_CITY,
            event.accidentInfo.map { it.hospitalAndRoomInfo.city },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.HOSPITAL_STATE,
            event.accidentInfo.map { it.hospitalAndRoomInfo.state },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.PATIENT_NAME,
            event.patientInfo.map { it.name.masked },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.PATIENT_NICKNAME,
            event.patientInfo.map { it.nickname },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.PATIENT_AGE,
            event.patientInfo.map { it.age.toString() },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.PATIENT_SEX,
            event.patientInfo.map { it.sex.name },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.PATIENT_HEIGHT,
            event.patientInfo.map { it.height?.toString() },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.PATIENT_WEIGHT,
            event.patientInfo.map { it.weight?.toString() },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.PATIENT_PRIMARY_PHONE_NUMBER,
            event.patientInfo.map { it.primaryContact.maskedPhoneNumber },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.PATIENT_PRIMARY_RELATIONSHIP,
            event.patientInfo.map { it.primaryContact.relationshipWithPatient },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.PATIENT_SECONDARY_PHONE_NUMBER,
            event.patientInfo.map { it.secondaryContact?.maskedPhoneNumber },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.PATIENT_SECONDARY_RELATIONSHIP,
            event.patientInfo.map { it.secondaryContact?.relationshipWithPatient },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.EXPECTED_CAREGIVING_START_DATE,
            event.expectedCaregivingStartDate.map { it?.toString() },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.CAREGIVING_ORGANIZATION_ID,
            event.caregivingManagerInfo.map { it?.organizationId },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.CAREGIVING_MANAGING_USER_ID,
            event.caregivingManagerInfo.map { it?.managingUserId },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.CAREGIVING_ORGANIZATION_TYPE,
            event.caregivingManagerInfo.map { it?.organizationType?.name },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.DESIRED_CAREGIVING_START_DATE,
            event.desiredCaregivingStartDate.map { it.toString() },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.EXPECTED_CAREGIVING_LIMIT_DATE,
            event.expectedCaregivingLimitDate.map { it.toString() },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.DESIRED_CAREGIVING_PERIOD,
            event.desiredCaregivingPeriod.map { it?.toString() },
        )

        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.ADDITIONAL_REQUESTS,
            event.additionalRequests.map { it },
        )
        addModificationHistoryIfChanged(
            ReceptionModificationHistory.ModificationProperty.NOTIFY_CAREGIVING_PROGRESS,
            event.notifyCaregivingProgress.map { it.toString() },
        )
        if (event.applicationFileInfo.previous != null) {
            addModificationHistoryIfChanged(
                ReceptionModificationHistory.ModificationProperty.RECEPTION_APPLICATION_FILE_NAME,
                event.applicationFileInfo.map { it?.receptionApplicationFileName ?: "" },
            )
        }

        if (modificationHistoryRecords.isNotEmpty()) {
            receptionModificationSummaryRepository.findTopByReceptionId(event.receptionId)
                ?.handleReceptionModified(event)
        }

        modificationHistoryRecords.forEach { receptionModificationHistoryRepository.save(it) }
    }

    @Transactional(readOnly = true)
    override fun getReceptionModificationHistories(
        query: ReceptionModificationHistoriesByReceptionIdQuery,
        pageRequest: Pageable
    ): Page<ReceptionModificationHistory> {
        receptionByIdQueryHandler.ensureReceptionExists(ReceptionByIdQuery(query.receptionId, query.subject))

        ReceptionModificationHistoryAccessPolicy.check(query.subject, query, Object.Empty)
        return receptionModificationHistoryRepository.findByReceptionId(
            query.receptionId,
            pageRequest.withSort(Sort.by(Sort.Order.desc("id")))
        )
    }

    @Transactional(readOnly = true)
    override fun getReceptionModificationSummary(
        query: ReceptionModificationSummaryByReceptionIdQuery
    ): ReceptionModificationSummary {
        receptionByIdQueryHandler.ensureReceptionExists(ReceptionByIdQuery(query.receptionId, query.subject))

        ReceptionModificationHistoryAccessPolicy.check(query.subject, query, Object.Empty)
        return receptionModificationSummaryRepository.findTopByReceptionId(query.receptionId)
            ?: throw ReceptionModificationSummaryNotFoundByReceptionIdException(query.receptionId)
    }

    @Transactional
    @EventListener(ReceptionReceived::class)
    fun handleReceptionReceived(event: ReceptionReceived) {
        receptionModificationSummaryRepository.save(
            ReceptionModificationSummary(
                id = ULID.random(),
                receptionId = event.receptionId,
            )
        )
    }
}
