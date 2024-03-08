package kr.caredoc.careinsurance.reception.modification

import com.github.guepardoapps.kulid.ULID
import kr.caredoc.careinsurance.caregiving.CaregivingCharge
import kr.caredoc.careinsurance.caregiving.CaregivingChargeModified
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.reception.ReceptionByIdQuery
import kr.caredoc.careinsurance.reception.ReceptionByIdQueryHandler
import kr.caredoc.careinsurance.reception.ReceptionReceived
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.SubjectAttribute
import kr.caredoc.careinsurance.withSort
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CaregivingChargeModificationHistoryService(
    private val caregivingChargeModificationSummaryRepository: CaregivingChargeModificationSummaryRepository,
    private val receptionByIdQueryHandler: ReceptionByIdQueryHandler,
    private val caregivingChargeModificationHistoryRepository: CaregivingChargeModificationHistoryRepository,
) : CaregivingChargeModificationSummaryByReceptionIdQueryHandler,
    CaregivingChargeModificationHistoriesByReceptionIdQueryHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    override fun getCaregivingChargeModificationSummary(query: CaregivingChargeModificationSummaryByReceptionIdQuery): CaregivingChargeModificationSummary {
        receptionByIdQueryHandler.ensureReceptionExists(ReceptionByIdQuery(query.receptionId, query.subject))

        CaregivingChargeModificationHistoryAccessPolicy.check(query.subject, query, Object.Empty)
        return caregivingChargeModificationSummaryRepository.findTopByReceptionId(query.receptionId)
            ?: throw CaregivingChargeModificationSummaryNotFoundByReceptionIdException(query.receptionId)
    }

    @Transactional
    @EventListener(ReceptionReceived::class)
    fun handleReceptionReceived(event: ReceptionReceived) {
        caregivingChargeModificationSummaryRepository.save(
            CaregivingChargeModificationSummary(
                id = ULID.random(),
                receptionId = event.receptionId,
            )
        )
    }

    @Transactional
    @EventListener(CaregivingChargeModified::class)
    fun handleCaregivingChargeModified(event: CaregivingChargeModified) {
        val modifierId = event.editingSubject[SubjectAttribute.USER_ID].firstOrNull()
        if (modifierId == null) {
            logger.warn("Reception(${event.receptionId})의 간병비 산정을 수정한 주체의 사용자 아이디를 특정할 수 없습니다.")
            return
        }

        val modificationHistoryRecords = mutableListOf<CaregivingChargeModificationHistory>()
        fun addModificationHistoryIfChanged(
            property: CaregivingChargeModificationHistory.ModifiedProperty,
            modification: Modification<String?>
        ) {
            if (!modification.hasChanged) {
                return
            }

            modificationHistoryRecords.add(
                CaregivingChargeModificationHistory(
                    id = ULID.random(),
                    receptionId = event.receptionId,
                    caregivingRoundNumber = event.caregivingRoundNumber,
                    modifiedProperty = property,
                    previous = modification.previous,
                    modified = modification.current,
                    modifierId = modifierId,
                    modifiedDateTime = event.calculatedDateTime,
                )
            )
        }

        addModificationHistoryIfChanged(
            CaregivingChargeModificationHistory.ModifiedProperty.ADDITIONAL_HOURS_CHARGE,
            event.additionalHoursCharge.map { it.toString() },
        )
        addModificationHistoryIfChanged(
            CaregivingChargeModificationHistory.ModifiedProperty.MEAL_COST,
            event.mealCost.map { it.toString() },
        )
        addModificationHistoryIfChanged(
            CaregivingChargeModificationHistory.ModifiedProperty.TRANSPORTATION_FEE,
            event.transportationFee.map { it.toString() },
        )
        addModificationHistoryIfChanged(
            CaregivingChargeModificationHistory.ModifiedProperty.HOLIDAY_CHARGE,
            event.holidayCharge.map { it.toString() },
        )
        addModificationHistoryIfChanged(
            CaregivingChargeModificationHistory.ModifiedProperty.CAREGIVER_INSURANCE_FEE,
            event.caregiverInsuranceFee.map { it.toString() },
        )
        addModificationHistoryIfChanged(
            CaregivingChargeModificationHistory.ModifiedProperty.COMMISSION_FEE,
            event.commissionFee.map { it.toString() },
        )
        addModificationHistoryIfChanged(
            CaregivingChargeModificationHistory.ModifiedProperty.VACATION_CHARGE,
            event.vacationCharge.map { it.toString() },
        )
        addModificationHistoryIfChanged(
            CaregivingChargeModificationHistory.ModifiedProperty.PATIENT_CONDITION_CHARGE,
            event.patientConditionCharge.map { it.toString() },
        )
        addModificationHistoryIfChanged(
            CaregivingChargeModificationHistory.ModifiedProperty.COVID_19_TESTING_COST,
            event.covid19TestingCost.map { it.toString() },
        )
        addModificationHistoryIfChanged(
            CaregivingChargeModificationHistory.ModifiedProperty.OUTSTANDING_AMOUNT,
            event.outstandingAmount.map { it.toString() },
        )
        addModificationHistoryIfChanged(
            CaregivingChargeModificationHistory.ModifiedProperty.EXPECTED_SETTLEMENT_DATE,
            event.expectedSettlementDate.map { it.toString() },
        )
        addModificationHistoryIfChanged(
            CaregivingChargeModificationHistory.ModifiedProperty.IS_CANCEL_AFTER_ARRIVED,
            event.isCancelAfterArrived.map { it.toString() },
        )
        addModificationHistoryIfChanged(
            CaregivingChargeModificationHistory.ModifiedProperty.ADDITIONAL_CHARGE_1,
            event.additionalCharges.map { it.getOrNull(0)?.format() },
        )
        addModificationHistoryIfChanged(
            CaregivingChargeModificationHistory.ModifiedProperty.ADDITIONAL_CHARGE_2,
            event.additionalCharges.map { it.getOrNull(1)?.format() },
        )
        addModificationHistoryIfChanged(
            CaregivingChargeModificationHistory.ModifiedProperty.ADDITIONAL_CHARGE_3,
            event.additionalCharges.map { it.getOrNull(2)?.format() },
        )

        if (modificationHistoryRecords.isNotEmpty()) {
            caregivingChargeModificationSummaryRepository.findTopByReceptionId(event.receptionId)
                ?.handleCaregivingChargeModified(event)
        }

        modificationHistoryRecords.forEach { caregivingChargeModificationHistoryRepository.save(it) }
    }

    private fun CaregivingCharge.AdditionalCharge.format() = "(${this.name}) ${"%,d".format(this.amount)}"

    @Transactional(readOnly = true)
    override fun getCaregivingChargeModificationHistories(
        query: CaregivingChargeModificationHistoriesByReceptionIdQuery,
        pageRequest: Pageable
    ): Page<CaregivingChargeModificationHistory> {
        receptionByIdQueryHandler.ensureReceptionExists(ReceptionByIdQuery(query.receptionId, query.subject))

        CaregivingChargeModificationHistoryAccessPolicy.check(query.subject, query, Object.Empty)
        return caregivingChargeModificationHistoryRepository.findByReceptionId(
            query.receptionId,
            pageRequest.withSort(Sort.by(Sort.Order.desc("id"))),
        )
    }
}
