package kr.caredoc.careinsurance.settlement

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.Subquery
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganization
import kr.caredoc.careinsurance.caregiving.CaregiverInfo
import kr.caredoc.careinsurance.caregiving.CaregivingRound
import kr.caredoc.careinsurance.caregiving.state.CaregivingStateData
import kr.caredoc.careinsurance.getPagedResult
import kr.caredoc.careinsurance.patient.EncryptedPatientInfo
import kr.caredoc.careinsurance.reception.Reception
import kr.caredoc.careinsurance.security.hash.PepperedHasher
import kr.caredoc.careinsurance.toHex
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import kotlin.streams.asSequence

class SettlementSearchingRepositoryImpl(
    @PersistenceContext
    private val entityManager: EntityManager,
    private val patientNameHasher: PepperedHasher,
) : SettlementSearchingRepository {
    private val criteriaBuilder = entityManager.criteriaBuilder
    override fun searchSettlements(
        searchingCriteria: SettlementSearchingRepository.SearchingCriteria,
        pageable: Pageable,
    ) = entityManager.getPagedResult(
        generateContentQuery(searchingCriteria, pageable.sort),
        generateCountQuery(searchingCriteria),
        pageable,
    )

    private fun generateContentQuery(
        searchingCriteria: SettlementSearchingRepository.SearchingCriteria,
        sort: Sort,
    ): CriteriaQuery<Settlement> {
        val query = criteriaBuilder.createQuery(Settlement::class.java)
        val root = query.from(Settlement::class.java)

        return query.select(root).where(*generatePredicates(root, query, searchingCriteria).toTypedArray())
            .orderBy(*generateOrders(root, sort).toTypedArray())
    }

    private fun generateCountQuery(
        searchingCriteria: SettlementSearchingRepository.SearchingCriteria,
    ): CriteriaQuery<Long> {
        val query = criteriaBuilder.createQuery(Long::class.java)
        val root = query.from(Settlement::class.java)

        return query.select(criteriaBuilder.count(root))
            .where(*generatePredicates(root, query, searchingCriteria).toTypedArray())
    }

    private fun generatePredicates(
        root: Root<Settlement>,
        query: CriteriaQuery<*>,
        searchingCriteria: SettlementSearchingRepository.SearchingCriteria,
    ): List<Predicate> {
        return listOfNotNull(
            generateSettlementProgressingStatusPredicate(root, searchingCriteria.progressingStatus),
            generatePatientNamePredicate(root, query, searchingCriteria.patientName),
            generateCaregiverOrganizationPredicate(
                root,
                query,
                searchingCriteria.internalCaregivingOrganizationAssigned,
                searchingCriteria.organizationName
            ),
            generateAccidentNumberPredicate(root, searchingCriteria.accidentNumber),
            generateExpectedSettlementDatePredicate(
                root,
                searchingCriteria.expectedSettlementDate
            ),
            generateLastTransactionDatePredicate(
                root,
                searchingCriteria.lastTransactionDate
            ),
        )
    }

    private fun generateSettlementProgressingStatusPredicate(
        root: Root<Settlement>,
        settlementProgressingStatus: SettlementProgressingStatus,
    ): Predicate {
        return criteriaBuilder.equal(
            root.get<SettlementProgressingStatus>(Settlement::progressingStatus.name),
            settlementProgressingStatus
        )
    }

    private fun generatePatientNamePredicate(
        root: Root<Settlement>,
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

        return root.get<String>(Settlement::receptionId.name).`in`(patientNameSubQuery)
    }

    private fun generateAccidentNumberPredicate(root: Root<Settlement>, accidentNumber: String?): Predicate? {
        return if (!accidentNumber.isNullOrBlank()) {
            criteriaBuilder.like(
                root.get(Settlement::accidentNumber.name),
                "%$accidentNumber%",
            )
        } else {
            null
        }
    }

    private fun generateExpectedSettlementDatePredicate(
        root: Root<Settlement>,
        expectedSettlementDate: DateRange?,
    ): Predicate? {
        if (expectedSettlementDate == null) {
            return null
        }

        return criteriaBuilder.and(
            criteriaBuilder.greaterThanOrEqualTo(
                root.get(Settlement::expectedSettlementDate.name),
                expectedSettlementDate.from,
            ),
            criteriaBuilder.lessThanOrEqualTo(
                root.get(Settlement::expectedSettlementDate.name),
                expectedSettlementDate.until,
            ),
        )
    }

    private fun generateLastTransactionDatePredicate(
        root: Root<Settlement>,
        lastTransactionDate: DateRange?,
    ): Predicate? {
        if (lastTransactionDate == null) {
            return null
        }

        return criteriaBuilder.and(
            criteriaBuilder.greaterThanOrEqualTo(
                root.get(Settlement::lastTransactionDatetime.name),
                lastTransactionDate.from.atTime(0, 0, 0),
            ),
            criteriaBuilder.lessThan(
                root.get(Settlement::lastTransactionDatetime.name),
                lastTransactionDate.until.plusDays(1).atTime(0, 0, 0),
            ),
        )
    }

    private fun generateInternalCaregivingOrganizationAssignedSubQuery(
        query: CriteriaQuery<*>,
    ): Subquery<String> {
        val caregivingRoundIdsSubQuery = query.subquery(String::class.java)
        val caregivingRoundRoot = caregivingRoundIdsSubQuery.from(CaregivingRound::class.java)

        caregivingRoundIdsSubQuery.select(caregivingRoundRoot.get("id"))
            .where(
                caregivingRoundRoot.get<CaregivingStateData>(CaregivingRound::caregivingStateData.name)
                    .get<CaregiverInfo>(CaregivingStateData::caregiverInfo.name)
                    .get<String>(CaregiverInfo::caregiverOrganizationId.name).isNull,
            )

        return caregivingRoundIdsSubQuery
    }

    private fun generateCaregiverOrganizationPredicate(
        root: Root<Settlement>,
        query: CriteriaQuery<*>,
        internalCaregivingOrganizationAssigned: Boolean?,
        organizationName: String?,
    ) = if (!organizationName.isNullOrBlank() && internalCaregivingOrganizationAssigned == true) {
        criteriaBuilder.or(
            root.get<String>(Settlement::caregivingRoundId.name).`in`(
                generateExternalCaregivingOrganizationIdsSubQuery(query, organizationName)
            ),
            root.get<String>(Settlement::caregivingRoundId.name).`in`(
                generateInternalCaregivingOrganizationAssignedSubQuery(query)
            ),
        )
    } else if (!organizationName.isNullOrBlank() && internalCaregivingOrganizationAssigned == false) {
        root.get<String>(Settlement::caregivingRoundId.name).`in`(
            generateExternalCaregivingOrganizationIdsSubQuery(query, organizationName)
        )
    } else {
        null
    }

    private fun generateExternalCaregivingOrganizationIdsSubQuery(
        query: CriteriaQuery<*>,
        organizationName: String,
    ): Subquery<String> {
        val caregivingRoundIdsSubQuery = query.subquery(String::class.java)
        val caregivingRoundRoot = caregivingRoundIdsSubQuery.from(CaregivingRound::class.java)
        val externalCaregivingOrganizationRoot = caregivingRoundIdsSubQuery.from(ExternalCaregivingOrganization::class.java)

        caregivingRoundIdsSubQuery.select(caregivingRoundRoot.get("id"))
            .where(
                criteriaBuilder.equal(
                    caregivingRoundRoot.get<CaregivingStateData>(CaregivingRound::caregivingStateData.name)
                        .get<CaregiverInfo>(CaregivingStateData::caregiverInfo.name)
                        .get<String>(CaregiverInfo::caregiverOrganizationId.name),
                    externalCaregivingOrganizationRoot.get<String>("id"),
                ),
                criteriaBuilder.like(externalCaregivingOrganizationRoot.get(ExternalCaregivingOrganization::name.name), "%$organizationName%"),
            )

        return caregivingRoundIdsSubQuery
    }

    override fun searchSettlements(
        searchingCriteria: SettlementSearchingRepository.SearchingCriteria,
        sort: Sort,
    ): List<Settlement> {
        return entityManager.createQuery(generateContentQuery(searchingCriteria, sort))
            .resultList
    }

    private fun generateOrders(root: Root<Settlement>, sort: Sort) = sort.get().asSequence().map {
        if (it.isDescending) {
            criteriaBuilder.desc(root.get<Any>(it.property))
        } else {
            criteriaBuilder.asc(root.get<Any>(it.property))
        }
    }.toList()
}
