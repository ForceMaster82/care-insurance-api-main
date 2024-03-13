package kr.caredoc.careinsurance.reception

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.Subquery
import kr.caredoc.careinsurance.caregiving.CaregiverInfo
import kr.caredoc.careinsurance.caregiving.CaregivingRound
import kr.caredoc.careinsurance.caregiving.state.CaregivingStateData
import kr.caredoc.careinsurance.getPagedResult
import kr.caredoc.careinsurance.insurance.InsuranceInfo
import kr.caredoc.careinsurance.patient.EncryptedPatientInfo
import kr.caredoc.careinsurance.security.hash.PepperedHasher
import kr.caredoc.careinsurance.toHex
import kr.caredoc.careinsurance.user.ExternalCaregivingManager
import kr.caredoc.careinsurance.user.InternalCaregivingManager
import kr.caredoc.careinsurance.user.User
import org.springframework.data.domain.Pageable
import java.time.LocalDate

class ReceptionSearchingRepositoryImpl(
    @PersistenceContext
    private val entityManager: EntityManager,
    private val patientNameHasher: PepperedHasher,
) : ReceptionSearchingRepository {
    private val criteriaBuilder = entityManager.criteriaBuilder

    override fun searchReceptions(
        searchingCriteria: ReceptionSearchingRepository.SearchingCriteria,
        pageable: Pageable
    ) = entityManager.getPagedResult(
        generateContentQuery(searchingCriteria),
        generateCountQuery(searchingCriteria),
        pageable
    )

    private fun generateContentQuery(
        searchingCriteria: ReceptionSearchingRepository.SearchingCriteria,
    ): CriteriaQuery<Reception> {
        val query = criteriaBuilder.createQuery(Reception::class.java)
        val root = query.from(Reception::class.java)

        return query.select(root)
            .where(*generatePredicates(query, root, searchingCriteria).toTypedArray())
            .orderBy(criteriaBuilder.desc(root.get<Any>("id")))
    }

    private fun generateCountQuery(
        searchingCriteria: ReceptionSearchingRepository.SearchingCriteria,
    ): CriteriaQuery<Long> {
        val query = criteriaBuilder.createQuery(Long::class.java)
        val root = query.from(Reception::class.java)

        return query.select(criteriaBuilder.count(root))
            .where(*generatePredicates(query, root, searchingCriteria).toTypedArray())
    }

    private fun generatePredicates(
        query: CriteriaQuery<*>,
        root: Root<Reception>,
        searchingCriteria: ReceptionSearchingRepository.SearchingCriteria,
    ) = with(searchingCriteria) {
        listOfNotNull(
            generateReceivedDateTimePredicate(root, from, until),
            periodType?.let {
                criteriaBuilder.equal(
                    root.get<Reception.PeriodType>(Reception::periodType.name),
                    periodType,
                )
            },
            generateUrgencyPredicate(root, urgency),
            generateCaregivingManagerAssigned(root, caregivingManagerAssigned),
            if (caregivingManagerAssigned != false) {
                generateOrganizationTypePredicate(root, organizationType)
            } else {
                null
            },
            generateProgressingStatusPredicate(root, progressingStatuses),
            generateInsuranceNumberPredicate(root, insuranceNumberContains),
            generatePatientNamePredicate(root, patientNameContains),
            generatePatientPhoneNumberPredicate(root, patientPhoneNumberContains),
            generateAccidentNumberPredicate(root, accidentNumberContains),
            generateManagerNamePredicate(query, root, managerNameContains),
            generateCaregiverNamePredicate(query, root, caregiverName),
        )
    }

    private fun generateCaregiverNamePredicate(
      query: CriteriaQuery<*>,
      root: Root<Reception>,
      caregiverName: String?,
    ): Predicate? {
        if (caregiverName.isNullOrBlank()) {
            return null
        }

        val receptionIdSubQuery = query.subquery(String::class.java)
        val caregivingRoundRoot = receptionIdSubQuery.from(CaregivingRound::class.java)

        receptionIdSubQuery.select(
            caregivingRoundRoot.get("receptionId")
        ).where(
            criteriaBuilder.like(
                caregivingRoundRoot.get<CaregivingStateData>(CaregivingRound::caregivingStateData.name)
                    .get<CaregiverInfo>(CaregivingStateData::caregiverInfo.name)
                    .get(CaregiverInfo::name.name),
                "%$caregiverName%"
            )
        )

        return root.get<Reception>("id")
            .`in`(receptionIdSubQuery)
    }

    private fun generateReceivedDateTimePredicate(
        root: Root<Reception>,
        from: LocalDate,
        until: LocalDate,
    ) =
        criteriaBuilder.and(
            criteriaBuilder.greaterThanOrEqualTo(
                root.get(Reception::receivedDateTime.name),
                from.atTime(0, 0, 0),
            ),
            criteriaBuilder.lessThan(
                root.get(Reception::receivedDateTime.name),
                until.plusDays(1).atTime(0, 0, 0),
            ),
        )

    private fun generateUrgencyPredicate(
        root: Root<Reception>,
        urgency: Reception.Urgency?,
    ) = urgency?.let {
        criteriaBuilder.equal(
            root.get<Reception.Urgency>(Reception::urgency.name),
            urgency,
        )
    }

    private fun generateCaregivingManagerAssigned(
        root: Root<Reception>,
        caregivingManagerAssigned: Boolean?,
    ) = caregivingManagerAssigned?.let {
        if (caregivingManagerAssigned) {
            root.get<CaregivingManagerInfo>(Reception::caregivingManagerInfo.name)
                .get<OrganizationType>(CaregivingManagerInfo::organizationType.name).isNotNull
        } else {
            root.get<CaregivingManagerInfo>(Reception::caregivingManagerInfo.name)
                .get<OrganizationType>(CaregivingManagerInfo::organizationType.name).isNull
        }
    }

    private fun generateOrganizationTypePredicate(
        root: Root<Reception>,
        organizationType: OrganizationType?,
    ) = organizationType?.let {
        criteriaBuilder.equal(
            root.get<CaregivingManagerInfo>(Reception::caregivingManagerInfo.name)
                .get<OrganizationType>(CaregivingManagerInfo::organizationType.name),
            organizationType,
        )
    }

    private fun generateProgressingStatusPredicate(
        root: Root<Reception>,
        progressingStatuses: Collection<ReceptionProgressingStatus>,
    ) = if (progressingStatuses.isNotEmpty()) {
        root.get<ReceptionProgressingStatus>(Reception::progressingStatus.name).`in`(
            progressingStatuses
        )
    } else {
        null
    }

    private fun generateInsuranceNumberPredicate(
        root: Root<Reception>,
        insuranceNumberKeyword: String?,
    ) = if (!insuranceNumberKeyword.isNullOrBlank()) {
        criteriaBuilder.like(
            root.get<InsuranceInfo>(Reception::insuranceInfo.name)
                .get(InsuranceInfo::insuranceNumber.name),
            "%$insuranceNumberKeyword%",
        )
    } else {
        null
    }

    private fun generatePatientNamePredicate(
        root: Root<Reception>,
        patientName: String?,
    ) = if (!patientName.isNullOrBlank()) {
        criteriaBuilder.equal(
            root.get<EncryptedPatientInfo>(Reception::patientInfo.name)
                .get<EncryptedPatientInfo.EncryptedPatientName>(EncryptedPatientInfo::name.name)
                .get<String>(EncryptedPatientInfo.EncryptedPatientName::hashed.name),
            patientNameHasher.hash(patientName.toByteArray()).toHex(),
        )
    } else {
        null
    }

    private fun generatePatientPhoneNumberPredicate(
        root: Root<Reception>,
        patientPhoneNumberKeyword: String?,
    ) = if (!patientPhoneNumberKeyword.isNullOrBlank()) {
        criteriaBuilder.like(
            root.get<EncryptedPatientInfo>(Reception::patientInfo.name)
                .get<EncryptedPatientInfo.EncryptedContact>(EncryptedPatientInfo::primaryContact.name)
                .get(EncryptedPatientInfo.EncryptedContact::maskedPhoneNumber.name),
            "%$patientPhoneNumberKeyword%",
        )
    } else {
        null
    }

    private fun generateAccidentNumberPredicate(
        root: Root<Reception>,
        accidentNumberKeyword: String?,
    ) = if (!accidentNumberKeyword.isNullOrBlank()) {
        criteriaBuilder.like(
            root.get<AccidentInfo>(Reception::accidentInfo.name)
                .get(AccidentInfo::accidentNumber.name),
            "%$accidentNumberKeyword%",
        )
    } else {
        null
    }

    private fun generateManagerNamePredicate(
        query: CriteriaQuery<*>,
        root: Root<Reception>,
        userNameKeyword: String?,
    ) = if (!userNameKeyword.isNullOrBlank()) {
        criteriaBuilder.or(
            root.get<CaregivingManagerInfo>(Reception::caregivingManagerInfo.name)
                .get<String>(CaregivingManagerInfo::managingUserId.name).`in`(
                    generateInternalCaregivingManagerIdsSubQuery(query, userNameKeyword)
                ),
            root.get<CaregivingManagerInfo>(Reception::caregivingManagerInfo.name)
                .get<String>(CaregivingManagerInfo::managingUserId.name).`in`(
                    generateExternalCaregivingManagerIdsSubQuery(query, userNameKeyword)
                ),
        )
    } else {
        null
    }

    private fun generateInternalCaregivingManagerIdsSubQuery(
        query: CriteriaQuery<*>,
        nameKeyword: String,
    ): Subquery<String> {
        val internalCaregivingManagerIdsSubQuery = query.subquery(String::class.java)
        val internalCaregivingManagerRoot =
            internalCaregivingManagerIdsSubQuery.from(InternalCaregivingManager::class.java)
        val userRoot = internalCaregivingManagerIdsSubQuery.from(User::class.java)

        internalCaregivingManagerIdsSubQuery.select(internalCaregivingManagerRoot.get("id"))
            .where(
                criteriaBuilder.equal(
                    internalCaregivingManagerRoot.get<String>(InternalCaregivingManager::userId.name),
                    userRoot.get<String>("id"),
                ),
                criteriaBuilder.like(userRoot.get(User::name.name), "%$nameKeyword%")
            )

        return internalCaregivingManagerIdsSubQuery
    }

    private fun generateExternalCaregivingManagerIdsSubQuery(
        query: CriteriaQuery<*>,
        nameKeyword: String,
    ): Subquery<String> {
        val externalCaregivingManagerIdsSubQuery = query.subquery(String::class.java)
        val externalCaregivingManagerRoot =
            externalCaregivingManagerIdsSubQuery.from(ExternalCaregivingManager::class.java)
        val userRoot = externalCaregivingManagerIdsSubQuery.from(User::class.java)

        externalCaregivingManagerIdsSubQuery.select(externalCaregivingManagerRoot.get("id"))
            .where(
                criteriaBuilder.equal(
                    externalCaregivingManagerRoot.get<String>(ExternalCaregivingManager::userId.name),
                    userRoot.get<String>("id"),
                ),
                criteriaBuilder.like(userRoot.get(User::name.name), "%$nameKeyword%"),
            )

        return externalCaregivingManagerIdsSubQuery
    }

    private fun generateManagingUserIdPredicate(
        root: Root<Reception>,
        managingUserIds: Collection<String>,
    ) = if (managingUserIds.isNotEmpty()) {
        root.get<CaregivingManagerInfo>(Reception::caregivingManagerInfo.name)
            .get<String>(CaregivingManagerInfo::managingUserId.name).`in`(managingUserIds)
    } else {
        null
    }
}
