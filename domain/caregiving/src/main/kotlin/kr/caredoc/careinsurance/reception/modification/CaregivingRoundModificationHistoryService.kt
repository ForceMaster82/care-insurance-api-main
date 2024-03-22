package kr.caredoc.careinsurance.reception.modification

import com.github.guepardoapps.kulid.ULID
import kr.caredoc.careinsurance.caregiving.CaregiverInfo
import kr.caredoc.careinsurance.caregiving.CaregivingRoundModified
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.reception.ReceptionByIdQuery
import kr.caredoc.careinsurance.reception.ReceptionByIdQueryHandler
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
class CaregivingRoundModificationHistoryService(
    private val caregivingRoundModificationSummaryRepository: CaregivingRoundModificationSummaryRepository,
    private val receptionByIdQueryHandler: ReceptionByIdQueryHandler,
    private val caregivingRoundModificationHistoryRepository: CaregivingRoundModificationHistoryRepository,
) : CaregivingRoundModificationSummaryByReceptionIdQueryHandler,
    CaregivingRoundModificationHistoriesByReceptionIdQueryHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    override fun getCaregivingRoundModificationSummary(query: CaregivingRoundModificationSummaryByReceptionIdQuery): CaregivingRoundModificationSummary {
        receptionByIdQueryHandler.ensureReceptionExists(ReceptionByIdQuery(query.receptionId, query.subject))

        CaregivingRoundModificationHistoryAccessPolicy.check(query.subject, query, Object.Empty)
        return caregivingRoundModificationSummaryRepository.findTopByReceptionId(query.receptionId)
            ?: throw CaregivingRoundModificationSummaryNotFoundByReceptionIdException(query.receptionId)
    }

    @Transactional
    @EventListener(CaregivingRoundModified::class)
    fun handleCaregivingRoundModified(@IncludingPersonalData event: CaregivingRoundModified) {
        if (event.cause != CaregivingRoundModified.Cause.DIRECT_EDIT) {
            return
        }

        val modifierId = event.editingSubject[SubjectAttribute.USER_ID].firstOrNull()
        if (modifierId == null) {
            logger.warn("Reception(${event.receptionId})의 간병 회차를 수정한 주체의 사용자 아이디를 특정할 수 없습니다.")
            return
        }

        val modificationHistoryRecords = mutableListOf<CaregivingRoundModificationHistory>()
        fun addModificationHistoryIfChanged(
            property: CaregivingRoundModificationHistory.ModifiedProperty,
            modification: Modification<String?>
        ) {
            if (!modification.hasChanged) {
                return
            }

            modificationHistoryRecords.add(
                CaregivingRoundModificationHistory(
                    id = ULID.random(),
                    receptionId = event.receptionId,
                    caregivingRoundNumber = event.caregivingRoundNumber,
                    modifiedProperty = property,
                    previous = modification.previous,
                    modified = modification.current,
                    modifierId = modifierId,
                    modifiedDateTime = event.modifiedDateTime,
                )
            )
        }

        if (!event.caregiverInfo.isInitialAssigning()) {
            addModificationHistoryIfChanged(
                CaregivingRoundModificationHistory.ModifiedProperty.CAREGIVER_ORGANIZATION_ID,
                event.caregiverInfo.map { it?.caregiverOrganizationId },
            )
            addModificationHistoryIfChanged(
                CaregivingRoundModificationHistory.ModifiedProperty.CAREGIVER_NAME,
                event.caregiverInfo.map { it?.name },
            )
            addModificationHistoryIfChanged(
                CaregivingRoundModificationHistory.ModifiedProperty.CAREGIVER_SEX,
                event.caregiverInfo.map { it?.sex?.name },
            )
            addModificationHistoryIfChanged(
                CaregivingRoundModificationHistory.ModifiedProperty.CAREGIVER_BIRTH_DATE,
                event.caregiverInfo.map { it?.birthDate?.toString() },
            )
            addModificationHistoryIfChanged(
                CaregivingRoundModificationHistory.ModifiedProperty.CAREGIVER_PHONE_NUMBER,
                event.caregiverInfo.map { it?.phoneNumber },
            )
            addModificationHistoryIfChanged(
                CaregivingRoundModificationHistory.ModifiedProperty.DAILY_CAREGIVING_CHARGE,
                event.caregiverInfo.map { it?.dailyCaregivingCharge?.toString() },
            )
            addModificationHistoryIfChanged(
                CaregivingRoundModificationHistory.ModifiedProperty.COMMISSION_FEE,
                event.caregiverInfo.map { it?.commissionFee?.toString() },
            )
            addModificationHistoryIfChanged(
                CaregivingRoundModificationHistory.ModifiedProperty.CAREGIVER_INSURED,
                event.caregiverInfo.map { it?.insured?.toString() },
            )
            addModificationHistoryIfChanged(
                CaregivingRoundModificationHistory.ModifiedProperty.CAREGIVER_ACCOUNT_BANK,
                event.caregiverInfo.map { it?.accountInfo?.bank },
            )
            addModificationHistoryIfChanged(
                CaregivingRoundModificationHistory.ModifiedProperty.CAREGIVER_ACCOUNT_HOLDER,
                event.caregiverInfo.map { it?.accountInfo?.accountHolder },
            )
            addModificationHistoryIfChanged(
                CaregivingRoundModificationHistory.ModifiedProperty.CAREGIVER_ACCOUNT_NUMBER,
                event.caregiverInfo.map { it?.accountInfo?.accountNumber },
            )
        }

        addModificationHistoryIfChanged(
            CaregivingRoundModificationHistory.ModifiedProperty.START_DATE_TIME,
            event.startDateTime.map { it?.toString() },
        )
        addModificationHistoryIfChanged(
            CaregivingRoundModificationHistory.ModifiedProperty.END_DATE_TIME,
            event.endDateTime.map { it?.toString() },
        )
        addModificationHistoryIfChanged(
            CaregivingRoundModificationHistory.ModifiedProperty.REMARKS,
            event.remarks.map { it },
        )
        addModificationHistoryIfChanged(
                CaregivingRoundModificationHistory.ModifiedProperty.EXPECTED_SETTLEMENT_DATE,
                event.expectedSettlementDate.map { it?.toString() },
        )

        if (modificationHistoryRecords.isNotEmpty()) {
            caregivingRoundModificationSummaryRepository.findTopByReceptionId(event.receptionId)
                ?.handleCaregivingRoundModified(event)
        }

        modificationHistoryRecords.forEach { caregivingRoundModificationHistoryRepository.save(it) }
    }

    private fun Modification<CaregiverInfo?>.isInitialAssigning(): Boolean {
        return this.previous == null && this.current != null
    }

    @Transactional
    @EventListener(ReceptionReceived::class)
    fun handleReceptionReceived(event: ReceptionReceived) {
        caregivingRoundModificationSummaryRepository.save(
            CaregivingRoundModificationSummary(
                id = ULID.random(),
                receptionId = event.receptionId,
            )
        )
    }

    @Transactional(readOnly = true)
    override fun getCaregivingRoundModificationHistories(
        query: CaregivingRoundModificationHistoriesByReceptionIdQuery,
        pageRequest: Pageable
    ): Page<CaregivingRoundModificationHistory> {
        receptionByIdQueryHandler.ensureReceptionExists(ReceptionByIdQuery(query.receptionId, query.subject))

        CaregivingRoundModificationHistoryAccessPolicy.check(query.subject, query, Object.Empty)
        return caregivingRoundModificationHistoryRepository.findByReceptionId(
            query.receptionId,
            pageRequest.withSort(Sort.by(Sort.Order.desc("id"))),
        )
    }
}
