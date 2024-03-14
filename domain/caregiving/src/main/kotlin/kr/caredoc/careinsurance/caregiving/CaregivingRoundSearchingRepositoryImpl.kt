package kr.caredoc.careinsurance.caregiving

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import kr.caredoc.careinsurance.caregiving.state.CaregivingStateData
import kr.caredoc.careinsurance.getPagedResult
import kr.caredoc.careinsurance.patient.EncryptedPatientInfo
import kr.caredoc.careinsurance.reception.AccidentInfo
import kr.caredoc.careinsurance.reception.CaregivingManagerInfo
import kr.caredoc.careinsurance.reception.Reception
import kr.caredoc.careinsurance.reception.ReceptionProgressingStatus
import kr.caredoc.careinsurance.security.hash.PepperedHasher
import kr.caredoc.careinsurance.settlement.SettlementProgressingStatus
import kr.caredoc.careinsurance.toHex
import org.springframework.data.domain.Pageable
import java.time.LocalDate
import java.time.LocalDateTime

class CaregivingRoundSearchingRepositoryImpl(
    @PersistenceContext
    private val entityManager: EntityManager,
    private val patientNameHasher: PepperedHasher,
) : CaregivingRoundSearchingRepository {
    private val criteriaBuilder = entityManager.criteriaBuilder

    override fun searchCaregivingRounds(
        searchingCriteria: CaregivingRoundSearchingRepository.SearchingCriteria,
        pageable: Pageable,
    ) = entityManager.getPagedResult(
        generateContentQuery(searchingCriteria),
        generateCountQuery(searchingCriteria),
        pageable,
    )

    override fun searchCaregivingRounds(
        searchingCriteria: CaregivingRoundSearchingRepository.SearchingCriteria
    ): List<CaregivingRound> {
        return entityManager.createQuery(generateContentQuery(searchingCriteria))
            .resultList
    }

    private fun generateContentQuery(
        searchingCriteria: CaregivingRoundSearchingRepository.SearchingCriteria,
    ): CriteriaQuery<CaregivingRound> {
        val query = criteriaBuilder.createQuery(CaregivingRound::class.java)
        val root = query.from(CaregivingRound::class.java)

        return query.select(root)
            .where(*generatePredicates(root, query, searchingCriteria).toTypedArray())
            .orderBy(
                criteriaBuilder.asc(
                    root.get<CaregivingRound.ReceptionInfo>(CaregivingRound::receptionInfo.name)
                        .get<String>(CaregivingRound.ReceptionInfo::maskedPatientName.name)
                ),
                criteriaBuilder.desc(
                    root.get<CaregivingRound.ReceptionInfo>(CaregivingRound::receptionInfo.name)
                        .get<String>(CaregivingRound.ReceptionInfo::accidentNumber.name)
                ),
                criteriaBuilder.desc(
                    root.get<Any>(CaregivingRound::caregivingRoundNumber.name)
                ),
            )
    }

    private fun generateCountQuery(
        searchingCriteria: CaregivingRoundSearchingRepository.SearchingCriteria,
    ): CriteriaQuery<Long> {
        val query = criteriaBuilder.createQuery(Long::class.java)
        val root = query.from(CaregivingRound::class.java)

        return query.select(criteriaBuilder.count(root))
            .where(*generatePredicates(root, query, searchingCriteria).toTypedArray())
    }

    private fun generatePredicates(
        root: Root<CaregivingRound>,
        query: CriteriaQuery<*>,
        searchingCriteria: CaregivingRoundSearchingRepository.SearchingCriteria,
    ) = with(searchingCriteria) {
        if (!insuranceNumberContains.isNullOrEmpty()
            || !accidentNumberContains.isNullOrEmpty()
            || !patientName.isNullOrEmpty()
            || !caregiverName.isNullOrEmpty()
            || !hospitalAndRoom.isNullOrEmpty()
            || !patientPhoneNumberContains.isNullOrEmpty()) {

            listOfNotNull(
                generateInsuranceNumberPredicate(root, insuranceNumberContains),
                generateAccidentNumberPredicate(root, accidentNumberContains),
                generatePatientNamePredicate(root, query, patientName),
                generateCaregiverNamePredicate(root, caregiverName),
                generateHospitalAndRoomPredicate(query, root, hospitalAndRoom),
                generatePatientPhoneNumberPredicate(query, root, patientPhoneNumberContains),
            )
        } else {
            listOfNotNull(
                generateStartDateTimePredicate(root, caregivingStartDateFrom, caregivingStartDateUntil),
                generateOrganizationIdPredicate(root, organizationId),
                generateExpectedCaregivingStartDateTimePredicate(root, expectedCaregivingStartDate),
                generateReceptionProgressingStatusPredicate(root, receptionProgressingStatuses),
                generateCaregivingProgressingStatusPredicate(root, caregivingProgressingStatuses),
                generateSettlementProgressingStatusPredicate(root, settlementProgressingStatuses),
                generateBillingProgressingStatusPredicate(root, billingProgressingStatuses),
                generateReceptionReceivedDateTimePredicate(query, root, receptionReceivedDateFrom.atTime(0, 0, 0))
            )
        }
    }

    private fun generateHospitalAndRoomPredicate(
        query: CriteriaQuery<*>,
        root: Root<CaregivingRound>,
        hospitalAndRoom: String?,
    ): Predicate? {
        if (hospitalAndRoom.isNullOrBlank()) {
            return null
        }

        val receptionIdSubQuery = query.subquery(String::class.java)
        val receptionRoot = receptionIdSubQuery.from(Reception::class.java)

        receptionIdSubQuery.select(
            receptionRoot.get("id")
        ).where(
            criteriaBuilder.like(
                receptionRoot.get<AccidentInfo>(Reception::accidentInfo.name)
                    .get<AccidentInfo.HospitalAndRoomInfo>(AccidentInfo::hospitalAndRoomInfo.name)
                    .get(AccidentInfo.HospitalAndRoomInfo::hospitalAndRoom.name),
                "%$hospitalAndRoom%",
            )
        )

        return root.get<CaregivingRound.ReceptionInfo>(CaregivingRound::receptionInfo.name)
            .get<String>(CaregivingRound.ReceptionInfo::receptionId.name)
            .`in`(receptionIdSubQuery)
    }

    private fun generatePatientPhoneNumberPredicate(
        query: CriteriaQuery<*>,
        root: Root<CaregivingRound>,
        patientPhoneNumberKeyword: String?,
    ): Predicate? {
        if (patientPhoneNumberKeyword.isNullOrBlank()) {
            return null
        }

        val receptionIdSubQuery = query.subquery(String::class.java)
        val receptionRoot = receptionIdSubQuery.from(Reception::class.java)

        receptionIdSubQuery.select(
            receptionRoot.get("id")
        ).where(
            criteriaBuilder.like(
                receptionRoot.get<EncryptedPatientInfo>(Reception::patientInfo.name)
                    .get<EncryptedPatientInfo.EncryptedContact>(EncryptedPatientInfo::primaryContact.name)
                    .get(EncryptedPatientInfo.EncryptedContact::maskedPhoneNumber.name),
                "%$patientPhoneNumberKeyword%",
            )
        )

        return root.get<CaregivingRound.ReceptionInfo>(CaregivingRound::receptionInfo.name)
            .get<String>(CaregivingRound.ReceptionInfo::receptionId.name)
            .`in`(receptionIdSubQuery)
    }

    private fun generateCaregiverNamePredicate(
        root: Root<CaregivingRound>,
        caregiverName: String?,
    ) = if (!caregiverName.isNullOrBlank()) {
        criteriaBuilder.like(
            // 간변인명이 CaregivingRound > CaregivingStateData > CaregiverInfo > name 이어서 get을 한번더 합니다.
            root.get<CaregivingStateData>(CaregivingRound::caregivingStateData.name)
                .get<CaregiverInfo>(CaregivingStateData::caregiverInfo.name)
                .get(CaregiverInfo::name.name),
            "%$caregiverName%"
        )
    } else {
        null
    }

    private fun generateStartDateTimePredicate(
        root: Root<CaregivingRound>,
        from: LocalDate?,
        until: LocalDate?,
    ) = if (from != null || until != null) {
        criteriaBuilder.and(
            *listOfNotNull(
                from?.let {
                    criteriaBuilder.greaterThanOrEqualTo(
                        root.get<CaregivingStateData>(CaregivingRound::caregivingStateData.name)
                            .get(CaregivingStateData::startDateTime.name),
                        it.atTime(0, 0, 0),
                    )
                },
                until?.let {
                    criteriaBuilder.lessThan(
                        root.get<CaregivingStateData>(CaregivingRound::caregivingStateData.name)
                            .get(CaregivingStateData::startDateTime.name),
                        it.plusDays(1).atTime(0, 0, 0),
                    )
                }
            ).toTypedArray()
        )
    } else {
        null
    }

    private fun generateOrganizationIdPredicate(
        root: Root<CaregivingRound>,
        organizationId: String?
    ) = organizationId?.let {
        criteriaBuilder.equal(
            root.get<CaregivingRound.ReceptionInfo>(CaregivingRound::receptionInfo.name)
                .get<CaregivingManagerInfo>(CaregivingRound.ReceptionInfo::caregivingManagerInfo.name)
                .get<String>(CaregivingManagerInfo::organizationId.name),
            organizationId,
        )
    }

    private fun generateExpectedCaregivingStartDateTimePredicate(
        root: Root<CaregivingRound>,
        expectedCaregivingStartDate: LocalDate?,
    ) = expectedCaregivingStartDate?.let {
        criteriaBuilder.equal(
            root.get<CaregivingRound.ReceptionInfo>(CaregivingRound::receptionInfo.name)
                .get<LocalDate>(CaregivingRound.ReceptionInfo::expectedCaregivingStartDate.name),
            expectedCaregivingStartDate
        )
    }

    private fun generateReceptionProgressingStatusPredicate(
        root: Root<CaregivingRound>,
        receptionProgressingStatuses: Collection<ReceptionProgressingStatus>,
    ) = if (receptionProgressingStatuses.isNotEmpty()) {
        root.get<CaregivingRound.ReceptionInfo>(CaregivingRound::receptionInfo.name)
            .get<ReceptionProgressingStatus>(CaregivingRound.ReceptionInfo::receptionProgressingStatus.name)
            .`in`(
                receptionProgressingStatuses
            )
    } else {
        null
    }

    private fun generateCaregivingProgressingStatusPredicate(
        root: Root<CaregivingRound>,
        caregivingProgressingStatuses: Collection<CaregivingProgressingStatus>,
    ) = if (caregivingProgressingStatuses.isNotEmpty()) {
        root.get<CaregivingStateData>(CaregivingRound::caregivingStateData.name)
            .get<CaregivingProgressingStatus>(CaregivingStateData::progressingStatus.name).`in`(
                caregivingProgressingStatuses
            )
    } else {
        null
    }

    private fun generateSettlementProgressingStatusPredicate(
        root: Root<CaregivingRound>,
        settlementProgressingStatuses: Collection<SettlementProgressingStatus>,
    ) = if (settlementProgressingStatuses.isNotEmpty()) {
        root.get<SettlementProgressingStatus>(CaregivingRound::settlementProgressingStatus.name).`in`(
            settlementProgressingStatuses
        )
    } else {
        null
    }

    private fun generateBillingProgressingStatusPredicate(
        root: Root<CaregivingRound>,
        billingProgressingStatuses: Collection<BillingProgressingStatus>,
    ) = if (billingProgressingStatuses.isNotEmpty()) {
        root.get<BillingProgressingStatus>(CaregivingRound::billingProgressingStatus.name).`in`(
            billingProgressingStatuses
        )
    } else {
        null
    }

    private fun generateInsuranceNumberPredicate(
        root: Root<CaregivingRound>,
        insuranceNumberKeyword: String?,
    ) = if (!insuranceNumberKeyword.isNullOrBlank()) {
        criteriaBuilder.like(
            root.get<CaregivingRound.ReceptionInfo>(CaregivingRound::receptionInfo.name)
                .get(CaregivingRound.ReceptionInfo::insuranceNumber.name),
            "%$insuranceNumberKeyword%",
        )
    } else {
        null
    }

    private fun generatePatientNamePredicate(
        root: Root<CaregivingRound>,
        query: CriteriaQuery<*>,
        patientName: String?,
    ): Predicate? {
        if (patientName.isNullOrBlank()) {
            return null
        }

        val patientNameSubQuery = query.subquery(String::class.java)
        val receptionRoot = patientNameSubQuery.from(Reception::class.java)

        patientNameSubQuery.select(receptionRoot.get("id"))
            .where(
                criteriaBuilder.equal(
                    receptionRoot.get<EncryptedPatientInfo>(Reception::patientInfo.name)
                        .get<EncryptedPatientInfo>(EncryptedPatientInfo::name.name)
                        .get<String>(EncryptedPatientInfo.EncryptedPatientName::hashed.name),
                    patientNameHasher.hash(patientName.toByteArray()).toHex(),
                )
            )

        return root.get<CaregivingRound.ReceptionInfo>(CaregivingRound::receptionInfo.name)
            .get<String>(CaregivingRound.ReceptionInfo::receptionId.name).`in`(patientNameSubQuery)
    }

    private fun generateAccidentNumberPredicate(
        root: Root<CaregivingRound>,
        accidentNumberKeyword: String?,
    ) = if (!accidentNumberKeyword.isNullOrBlank()) {
        criteriaBuilder.like(
            root.get<CaregivingRound.ReceptionInfo>(CaregivingRound::receptionInfo.name)
                .get(AccidentInfo::accidentNumber.name),
            "%$accidentNumberKeyword%",
        )
    } else {
        null
    }

    private fun generateReceptionReceivedDateTimePredicate(
        query: CriteriaQuery<*>,
        root: Root<CaregivingRound>,
        receivedDateTimeFrom: LocalDateTime
    ): Predicate {
        val receptionIdSubQuery = query.subquery(String::class.java)
        val receptionRoot = receptionIdSubQuery.from(Reception::class.java)

        receptionIdSubQuery.select(receptionRoot.get("id"))
            .where(
                criteriaBuilder.greaterThanOrEqualTo(
                    receptionRoot.get(Reception::receivedDateTime.name),
                    receivedDateTimeFrom
                )
            )

        return root.get<CaregivingRound.ReceptionInfo>(CaregivingRound::receptionInfo.name)
            .get<String>(CaregivingRound.ReceptionInfo::receptionId.name)
            .`in`(receptionIdSubQuery)
    }
}
