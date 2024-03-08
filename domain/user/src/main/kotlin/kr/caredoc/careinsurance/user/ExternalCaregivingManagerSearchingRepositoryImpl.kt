package kr.caredoc.careinsurance.user

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import kr.caredoc.careinsurance.getPagedResult
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

class ExternalCaregivingManagerSearchingRepositoryImpl(
    @PersistenceContext
    private val entityManager: EntityManager
) : ExternalCaregivingManagerSearchingRepository {
    private val criteriaBuilder = entityManager.criteriaBuilder

    override fun searchExternalCaregivingManagers(
        searchingCriteria: ExternalCaregivingManagerSearchingRepository.SearchingCriteria,
        pageable: Pageable,
    ): Page<ExternalCaregivingManager> {
        if (searchingCriteria.isEmpty()) {
            throw SearchCriteriaEmptyException()
        }

        return entityManager.getPagedResult(
            generateContentQuery(searchingCriteria),
            generateCountQuery(searchingCriteria),
            pageable,
        )
    }

    private fun generateContentQuery(searchingCriteria: ExternalCaregivingManagerSearchingRepository.SearchingCriteria): CriteriaQuery<ExternalCaregivingManager> {
        val query = criteriaBuilder.createQuery(ExternalCaregivingManager::class.java)
        val root = query.from(ExternalCaregivingManager::class.java)

        return query.select(root)
            .where(*generatePredicates(root, searchingCriteria).toTypedArray())
            .orderBy(criteriaBuilder.desc(root.get<Any>("id")))
    }

    private fun generateCountQuery(searchingCriteria: ExternalCaregivingManagerSearchingRepository.SearchingCriteria): CriteriaQuery<Long> {
        val query = criteriaBuilder.createQuery(Long::class.java)
        val root = query.from(ExternalCaregivingManager::class.java)

        return query.select(criteriaBuilder.count(root))
            .where(*generatePredicates(root, searchingCriteria).toTypedArray())
    }

    private fun generatePredicates(
        root: Root<ExternalCaregivingManager>,
        searchingCriteria: ExternalCaregivingManagerSearchingRepository.SearchingCriteria,
    ): List<Predicate> {
        return listOfNotNull(
            generateEmailPredicate(root, searchingCriteria.email),
            generateNamePredicate(root, searchingCriteria.name),
            generateExternalCaregivingOrganizationIdPredicate(root, searchingCriteria.externalCaregivingOrganizationId),
        )
    }

    private fun generateEmailPredicate(root: Root<ExternalCaregivingManager>, email: String?): Predicate? {
        return if (!email.isNullOrBlank()) {
            criteriaBuilder.like(
                root.get(ExternalCaregivingManager::email.name),
                "%$email%"
            )
        } else {
            null
        }
    }

    private fun generateNamePredicate(root: Root<ExternalCaregivingManager>, name: String?): Predicate? {
        return if (!name.isNullOrBlank()) {
            criteriaBuilder.like(
                root.get(ExternalCaregivingManager::name.name),
                "%$name%"
            )
        } else {
            null
        }
    }

    private fun generateExternalCaregivingOrganizationIdPredicate(
        root: Root<ExternalCaregivingManager>,
        externalCaregivingOrganizationId: String?,
    ): Predicate? {
        return if (!externalCaregivingOrganizationId.isNullOrBlank()) {
            criteriaBuilder.equal(
                root.get<String>(ExternalCaregivingManager::externalCaregivingOrganizationId.name),
                externalCaregivingOrganizationId,
            )
        } else {
            null
        }
    }
}
