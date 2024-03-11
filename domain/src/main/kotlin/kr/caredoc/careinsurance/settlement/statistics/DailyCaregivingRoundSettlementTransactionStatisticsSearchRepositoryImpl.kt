package kr.caredoc.careinsurance.settlement.statistics

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import kr.caredoc.careinsurance.getPagedResult
import kr.caredoc.careinsurance.patient.EncryptedPatientInfo
import kr.caredoc.careinsurance.reception.Reception
import kr.caredoc.careinsurance.search.SearchCondition
import kr.caredoc.careinsurance.security.hash.PepperedHasher
import kr.caredoc.careinsurance.toHex
import kr.caredoc.careinsurance.withSort
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

class DailyCaregivingRoundSettlementTransactionStatisticsSearchRepositoryImpl(
    @PersistenceContext
    private val entityManager: EntityManager,
    private val patientNameHasher: PepperedHasher,
) : DailyCaregivingRoundSettlementTransactionStatisticsSearchRepository {
    private val criteriaBuilder = entityManager.criteriaBuilder

    override fun getDataList(
        searchingCriteria: DailyCaregivingRoundSettlementTransactionStatisticsSearchRepository.SearchingCriteria,
        pageable: Pageable,
    ) = entityManager.getPagedResult(
        generateContentQuery(searchingCriteria),
        generateCountQuery(searchingCriteria),
        pageable,
    )

    private fun generateContentQuery(
        searchingCriteria: DailyCaregivingRoundSettlementTransactionStatisticsSearchRepository.SearchingCriteria,
    ): CriteriaQuery<DailyCaregivingRoundSettlementTransactionStatistics> {
        val query = criteriaBuilder.createQuery(DailyCaregivingRoundSettlementTransactionStatistics::class.java)
        val root = query.from(DailyCaregivingRoundSettlementTransactionStatistics::class.java)

        return query.select(root)
            .where(*generatePredicates(root, query, searchingCriteria).toTypedArray())
            .orderBy(
                criteriaBuilder.desc(
                    root.get<LocalDate>(DailyCaregivingRoundSettlementTransactionStatistics::lastEnteredDateTime.name)
                ),
            )
    }

    private fun generateCountQuery(
        searchingCriteria: DailyCaregivingRoundSettlementTransactionStatisticsSearchRepository.SearchingCriteria,
    ): CriteriaQuery<Long> {
        val query = criteriaBuilder.createQuery(Long::class.java)
        val root = query.from(DailyCaregivingRoundSettlementTransactionStatistics::class.java)

        return query.select(criteriaBuilder.count(root))
            .where(*generatePredicates(root, query, searchingCriteria).toTypedArray())
    }

    private fun generatePredicates(
        root: Root<DailyCaregivingRoundSettlementTransactionStatistics>,
        query: CriteriaQuery<*>,
        searchingCriteria: DailyCaregivingRoundSettlementTransactionStatisticsSearchRepository.SearchingCriteria,
    ) = with(searchingCriteria) {
        listOfNotNull(
            generateDatePredicate(root, date),
            generatePatientNamePredicate(root, query, patientName),
        )
    }

    private fun generateDatePredicate(
        root: Root<DailyCaregivingRoundSettlementTransactionStatistics>,
        date: LocalDate?,
    ) = date?.let {
        criteriaBuilder.equal(
            root.get<LocalDate>(DailyCaregivingRoundSettlementTransactionStatistics::date.name),
            date
        )
    }

    private fun generatePatientNamePredicate(
        root: Root<DailyCaregivingRoundSettlementTransactionStatistics>,
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

        return root.get<DailyCaregivingRoundSettlementTransactionStatistics.ReceptionInfo>(DailyCaregivingRoundSettlementTransactionStatistics::receptionInfo.name)
            .get<String>(DailyCaregivingRoundSettlementTransactionStatistics.ReceptionInfo::receptionId.name).`in`(patientNameSubQuery)
    }



}
