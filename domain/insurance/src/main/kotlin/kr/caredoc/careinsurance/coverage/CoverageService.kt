package kr.caredoc.careinsurance.coverage

import com.github.guepardoapps.kulid.ULID
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.ReadOneAccess
import kr.caredoc.careinsurance.security.accesscontrol.checkAll
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CoverageService(
    private val coverageRepository: CoverageRepository,
) : AllCoveragesQueryHandler,
    CoverageCreationCommandHandler,
    CoverageByIdQueryHandler,
    CoverageEditingCommandHandler,
    CoveragesBySearchConditionQueryHandler {
    @Transactional(readOnly = true)
    override fun getCoverages(query: AllCoveragesQuery, pageRequest: Pageable): Page<Coverage> {
        CoverageAccessPolicy.check(query.subject, query, Object.Empty)

        val pagedCoverages = coverageRepository.findAll(pageRequest)
        for (coverage in pagedCoverages.content) {
            CoverageAccessPolicy.check(
                query.subject,
                CoverageByIdQuery(coverageId = coverage.id, query.subject),
                coverage
            )
        }

        return pagedCoverages
    }

    @Transactional
    override fun createCoverage(command: CoverageCreationCommand): CoverageCreationResult {
        CoverageAccessPolicy.check(command.subject, command, Object.Empty)

        ensureRenewalTypeIsLegalForCreation(command.renewalType)
        ensureNameNotDuplicated(command.name)
        ensureRenewalTypeAndSubscriptionYearNotDuplicated(command.renewalType, command.targetSubscriptionYear)

        val newCoverage = command.intoEntity()
        coverageRepository.save(newCoverage)

        return CoverageCreationResult(newCoverage.id)
    }

    private fun ensureNameNotDuplicated(name: String) {
        if (coverageRepository.existsByName(name)) {
            throw CoverageNameDuplicatedException(name)
        }
    }

    private fun ensureRenewalTypeAndSubscriptionYearNotDuplicated(renewalType: RenewalType, subscriptionYear: Int) {
        if (coverageRepository.existsByRenewalTypeAndTargetSubscriptionYear(renewalType, subscriptionYear)) {
            throw SubscriptionYearDuplicatedException(renewalType, subscriptionYear)
        }
    }

    private fun ensureRenewalTypeIsLegalForCreation(renewalType: RenewalType) {
        if (renewalType == RenewalType.THREE_YEAR) {
            throw IllegalRenewalTypeEnteredException(renewalType)
        }
    }

    private fun CoverageCreationCommand.intoEntity() = Coverage(
        id = ULID.random(),
        name = this.name,
        targetSubscriptionYear = this.targetSubscriptionYear,
        renewalType = renewalType,
        annualCoveredCaregivingCharges = this.annualCoveredCaregivingCharges,
    )

    @Transactional(readOnly = true)
    override fun <T> getCoverage(query: CoverageByIdQuery, mapper: (Coverage) -> T) = mapper(getCoverage(query))

    override fun ensureCoverageExists(query: CoverageByIdQuery) {
        val coverage = coverageRepository.findByIdOrNull(query.coverageId)
            ?: throw CoverageNotFoundByIdException(query.coverageId)

        CoverageAccessPolicy.check(query.subject, query, coverage)
    }

    private fun getCoverage(query: CoverageByIdQuery): Coverage {
        val coverage = coverageRepository.findByIdOrNull(query.coverageId)
            ?: throw CoverageNotFoundByIdException(query.coverageId)

        CoverageAccessPolicy.check(query.subject, query, coverage)

        return coverage
    }

    @Transactional
    override fun editCoverage(command: CoverageEditingCommand) {
        val coverage = getCoverage(CoverageByIdQuery(command.coverageId, command.subject))

        CoverageAccessPolicy.check(command.subject, command, coverage)

        if (coverage.name != command.name) {
            ensureNameNotDuplicated(command.name)
        }

        coverage.editMetaData(command)
    }

    @Transactional
    override fun getCoverages(query: CoveragesBySearchConditionQuery, pageRequest: Pageable): Page<Coverage> {
        CoverageAccessPolicy.check(query.subject, query, Object.Empty)

        return coverageRepository.findByNameContaining(query.searchCondition.keyword, pageRequest).also {
            CoverageAccessPolicy.checkAll(query.subject, ReadOneAccess, it.content)
        }
    }
}
