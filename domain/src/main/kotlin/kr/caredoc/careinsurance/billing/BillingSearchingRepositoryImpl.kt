package kr.caredoc.careinsurance.billing

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import kr.caredoc.careinsurance.caregiving.CaregiverInfo
import kr.caredoc.careinsurance.caregiving.CaregivingRound
import kr.caredoc.careinsurance.caregiving.state.CaregivingStateData
import kr.caredoc.careinsurance.getPagedResult
import kr.caredoc.careinsurance.patient.EncryptedPatientInfo
import kr.caredoc.careinsurance.reception.Reception
import kr.caredoc.careinsurance.security.hash.PepperedHasher
import kr.caredoc.careinsurance.settlement.Settlement
import kr.caredoc.careinsurance.toHex
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import java.time.LocalDate
import kotlin.streams.asSequence

class BillingSearchingRepositoryImpl(
    @PersistenceContext private val entityManager: EntityManager,
    private val patientNameHasher: PepperedHasher,
) : BillingSearchingRepository {
    private val criteriaBuilder = entityManager.criteriaBuilder

    override fun searchBillings(
        searchingCriteria: BillingSearchingRepository.SearchingCriteria,
        pageable: Pageable
    ) = entityManager.getPagedResult(
        generateContentQuery(searchingCriteria, pageable.sort),
        generateCountQuery(searchingCriteria),
        pageable,
    )

    private fun generateContentQuery(
        searchingCriteria: BillingSearchingRepository.SearchingCriteria,
        sort: Sort,
    ): CriteriaQuery<Billing> {
        val query = criteriaBuilder.createQuery(Billing::class.java)
        val root = query.from(Billing::class.java)

        return query.select(root).where(*generatePredicates(root, query, searchingCriteria).toTypedArray())
            .orderBy(*generateOrders(root, sort).toTypedArray())
    }

    private fun generateCountQuery(searchingCriteria: BillingSearchingRepository.SearchingCriteria): CriteriaQuery<Long> {
        val query = criteriaBuilder.createQuery(Long::class.java)
        val root = query.from(Billing::class.java)

        return query.select(criteriaBuilder.count(root))
            .where(*generatePredicates(root, query, searchingCriteria).toTypedArray())
    }

    private fun generatePredicates(
        root: Root<Billing>,
        query: CriteriaQuery<*>,
        searchingCriteria: BillingSearchingRepository.SearchingCriteria
    ): List<Predicate> {
        return listOfNotNull(
            generateBillingProgressingStatusPredicate(root, searchingCriteria.progressingStatus),
            generatePatientNamePredicate(root, query, searchingCriteria.patientName),
            generateAccidentNumberPredicate(root, searchingCriteria.accidentNumber),
            generateCaregivingPeriodPredicate(
                root,
                searchingCriteria.usedPeriodFrom,
                searchingCriteria.usedPeriodUntil
            ),
            generateBillingDatePredicate(
                root,
                searchingCriteria.billingDateFrom,
                searchingCriteria.billingDateUntil
            ),
            generateTransactionDatePredicate(
                root,
                searchingCriteria.transactionDateFrom,
                searchingCriteria.transactionDateUntil
            ),
            generateCaregiverNamePredicate(query, root, searchingCriteria.caregiverName),
        )
    }

    private fun generateCaregiverNamePredicate(
        query: CriteriaQuery<*>,
        root: Root<Billing>,
        caregiverName: String?,
    ): Predicate? {
        if (caregiverName.isNullOrBlank()) {
            return null
        }

        val caregivingRoundIdSubQuery = query.subquery(String::class.java)
        val caregivingRoundRoot = caregivingRoundIdSubQuery.from(CaregivingRound::class.java)

        caregivingRoundIdSubQuery.select(
            caregivingRoundRoot.get("id")
        ).where(
            criteriaBuilder.like(
                caregivingRoundRoot.get<CaregivingStateData>(CaregivingRound::caregivingStateData.name)
                    .get<CaregiverInfo>(CaregivingStateData::caregiverInfo.name)
                    .get(CaregiverInfo::name.name),
                "%$caregiverName%"
            )
        )

        return root.get<Billing>(Billing::caregivingRoundId.name)     // caregivingRoundId
            .`in`(caregivingRoundIdSubQuery)
    }

    private fun generatePatientNamePredicate(
        root: Root<Billing>,
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

        return root.get<Billing.ReceptionInfo>(Billing::receptionInfo.name)
            .get<String>(Billing.ReceptionInfo::receptionId.name).`in`(patientNameSubQuery)
    }

    private fun generateAccidentNumberPredicate(root: Root<Billing>, accidentNumber: String?): Predicate? {
        return if (!accidentNumber.isNullOrBlank()) {
            criteriaBuilder.like(
                root.get<Billing.ReceptionInfo>(Billing::receptionInfo.name)
                    .get(Billing.ReceptionInfo::accidentNumber.name),
                "%$accidentNumber%",
            )
        } else {
            null
        }
    }

    private fun generateCaregivingPeriodPredicate(
        root: Root<Billing>,
        from: LocalDate?,
        until: LocalDate?,
    ): Predicate? {
        if (from == null || until == null) {
            return null
        }

        return criteriaBuilder.and(
            criteriaBuilder.greaterThanOrEqualTo(
                root.get<Billing.CaregivingRoundInfo>(Billing::caregivingRoundInfo.name)
                    .get(Billing.CaregivingRoundInfo::startDateTime.name),
                from.atTime(0, 0, 0),
            ),
            criteriaBuilder.lessThan(
                root.get<Billing.CaregivingRoundInfo>(Billing::caregivingRoundInfo.name)
                    .get(Billing.CaregivingRoundInfo::endDateTime.name),
                until.plusDays(1).atTime(0, 0, 0),
            ),
        )
    }

    private fun generateBillingDatePredicate(
        root: Root<Billing>,
        from: LocalDate?,
        until: LocalDate?,
    ): Predicate? {
        if (from == null || until == null) {
            return null
        }

        return criteriaBuilder.and(
            criteriaBuilder.greaterThanOrEqualTo(root.get(Billing::billingDate.name), from),
            criteriaBuilder.lessThanOrEqualTo(root.get(Billing::billingDate.name), until),
        )
    }

    private fun generateTransactionDatePredicate(
        root: Root<Billing>,
        from: LocalDate?,
        until: LocalDate?,
    ): Predicate? {
        if (from == null || until == null) {
            return null
        }

        return criteriaBuilder.and(
            criteriaBuilder.greaterThanOrEqualTo(root.get(Billing::lastTransactionDate.name), from),
            criteriaBuilder.lessThanOrEqualTo(root.get(Billing::lastTransactionDate.name), until),
        )
    }

    private fun generateBillingProgressingStatusPredicate(
        root: Root<Billing>,
        billingProgressingStatus: Collection<BillingProgressingStatus>,
    ): Predicate? {
        return if (billingProgressingStatus.isNotEmpty()) {
            root.get<BillingProgressingStatus>(Billing::billingProgressingStatus.name).`in`(billingProgressingStatus)
        } else {
            null
        }
    }

    private fun generateOrders(root: Root<Billing>, sort: Sort) = sort.get().asSequence().map {
        if (it.isDescending) {
            criteriaBuilder.desc(root.get<Any>(it.property))
        } else {
            criteriaBuilder.asc(root.get<Any>(it.property))
        }
    }.toList()
}
