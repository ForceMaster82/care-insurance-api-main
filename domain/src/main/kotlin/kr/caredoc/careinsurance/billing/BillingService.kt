package kr.caredoc.careinsurance.billing

import com.github.guepardoapps.kulid.ULID
import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.caregiving.CaregivingChargeCalculated
import kr.caredoc.careinsurance.caregiving.CaregivingChargeModified
import kr.caredoc.careinsurance.caregiving.CaregivingRound
import kr.caredoc.careinsurance.caregiving.CaregivingRoundByIdQuery
import kr.caredoc.careinsurance.caregiving.CaregivingRoundByIdQueryHandler
import kr.caredoc.careinsurance.caregiving.CaregivingRoundModified
import kr.caredoc.careinsurance.caregiving.certificate.CertificateByCaregivingRoundIdQuery
import kr.caredoc.careinsurance.caregiving.certificate.CertificateByCaregivingRoundIdQueryHandler
import kr.caredoc.careinsurance.coverage.CoverageByIdQuery
import kr.caredoc.careinsurance.coverage.CoverageByIdQueryHandler
import kr.caredoc.careinsurance.coverage.RenewalType
import kr.caredoc.careinsurance.reception.Reception
import kr.caredoc.careinsurance.reception.ReceptionByIdQuery
import kr.caredoc.careinsurance.reception.ReceptionByIdQueryHandler
import kr.caredoc.careinsurance.reception.ReceptionModified
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.ReadOneAccess
import kr.caredoc.careinsurance.security.accesscontrol.SystemUser
import kr.caredoc.careinsurance.security.accesscontrol.checkAll
import kr.caredoc.careinsurance.security.personaldata.IncludingPersonalData
import kr.caredoc.careinsurance.withSort
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BillingService(
    private val billingRepository: BillingRepository,
    private val caregivingRoundByIdQueryHandler: CaregivingRoundByIdQueryHandler,
    private val receptionByIdQueryHandler: ReceptionByIdQueryHandler,
    private val coverageByIdQueryHandler: CoverageByIdQueryHandler,
    private val certificateByCaregivingRoundIdQueryHandler: CertificateByCaregivingRoundIdQueryHandler,
) : BillingByIdQueryHandler,
    DownloadCertificateCommandHandler,
    BillingTransactionRecordingCommandHandler,
    BillingByCaregivingRoundIdQueryHandler,
    BillingByFilterQueryHandler,
    BillingByReceptionIdQueryHandler {
    @EventListener(CaregivingChargeCalculated::class)
    @Transactional
    fun handleCaregivingChargeCalculated(event: CaregivingChargeCalculated) {
        val caregivingRound = caregivingRoundByIdQueryHandler.getCaregivingRound(
            CaregivingRoundByIdQuery(
                caregivingRoundId = event.caregivingRoundId,
                subject = event.subject,
            )
        )

        val reception = receptionByIdQueryHandler.getReception(
            ReceptionByIdQuery(
                receptionId = event.receptionId,
                subject = event.subject,
            )
        )

        val coverageInfo = coverageByIdQueryHandler.getCoverage(
            CoverageByIdQuery(
                coverageId = reception.insuranceInfo.coverageId,
                subject = event.subject,
            )
        ) {
            CoverageInfo(
                targetSubscriptionYear = it.targetSubscriptionYear,
                renewalType = if (it.renewalType == RenewalType.TEN_YEAR) {
                    CoverageInfo.RenewalType.TEN_YEAR
                } else {
                    CoverageInfo.RenewalType.THREE_YEAR
                },
                annualCoveredCaregivingCharges = it.annualCoveredCaregivingCharges.map {
                    CoverageInfo.AnnualCoveredCaregivingCharge(
                        targetAccidentYear = it.targetAccidentYear,
                        caregivingCharge = it.caregivingCharge,
                    )
                }
            )
        }

        billingRepository.save(
            event.generateBilling(
                caregivingRound = caregivingRound,
                reception = reception,
                coverageInfo = coverageInfo,
            )
        )
    }

    private fun CaregivingChargeCalculated.generateBilling(
        caregivingRound: CaregivingRound,
        reception: Reception,
        coverageInfo: CoverageInfo
    ) = Billing(
        id = ULID.random(),
        receptionInfo = Billing.ReceptionInfo(
            receptionId = this.receptionId,
            accidentNumber = caregivingRound.receptionInfo.accidentNumber,
            subscriptionDate = reception.insuranceInfo.subscriptionDate
        ),
        caregivingRoundInfo = Billing.CaregivingRoundInfo(
            caregivingRoundId = caregivingRound.id,
            roundNumber = this.roundNumber,
            startDateTime = caregivingRound.startDateTime ?: Clock.now(),
            endDateTime = caregivingRound.endDateTime ?: Clock.now(),
        ),
        billingProgressingStatus = BillingProgressingStatus.WAITING_FOR_BILLING,
        coverageInfo = coverageInfo,
        isCancelAfterArrived = isCancelAfterArrived,
        caregivingManagerInfo = reception.caregivingManagerInfo,
    )

    @Transactional(readOnly = true)
    override fun getBilling(query: BillingByIdQuery): Billing {
        val billing =
            billingRepository.findByIdOrNull(query.billingId) ?: throw BillingNotExistsException(query.billingId)

        BillingAccessPolicy.check(query.subject, query, billing)

        return billing
    }

    @Transactional
    override fun downloadCertification(command: DownloadCertificateCommand): ByteArray {
        val billing = getBilling(
            BillingByIdQuery(
                billingId = command.billingId,
                subject = command.subject,
            )
        )
        BillingAccessPolicy.check(command.subject, command, billing)

        if (billing.billingProgressingStatus == BillingProgressingStatus.WAITING_FOR_BILLING) {
            billing.waitDeposit()
            billingRepository.save(billing)
        }

        return certificateByCaregivingRoundIdQueryHandler.getCertificate(
            CertificateByCaregivingRoundIdQuery(
                caregivingRoundId = billing.caregivingRoundInfo.caregivingRoundId,
                subject = command.subject,
            )
        )
    }

    @Transactional
    override fun recordTransaction(
        query: BillingByIdQuery,
        command: BillingTransactionRecordingCommand
    ) {
        val billing = getBilling(query)

        billing.recordTransaction(command)
        billingRepository.save(billing)
    }

    @Transactional(readOnly = true)
    override fun getBilling(query: BillingByCaregivingRoundIdQuery) = getBillingOrNull(query)
        ?: throw BillingNotFoundByCaregivingRoundIdException(query.caregivingRoundId)

    private fun getBillingOrNull(query: BillingByCaregivingRoundIdQuery) =
        billingRepository.findTopByCaregivingRoundInfoCaregivingRoundId(query.caregivingRoundId)?.also {
            BillingAccessPolicy.check(query.subject, query, it)
        }

    @Transactional(readOnly = true)
    override fun getBillings(query: BillingByFilterQuery, pageRequest: Pageable): Page<Billing> {
        BillingAccessPolicy.check(query.subject, query, Object.Empty)

        val billings = billingRepository.searchBillings(
            BillingSearchingRepository.SearchingCriteria(
                progressingStatus = query.progressingStatus,
                accidentNumber = query.getKeyword(
                    propertyToExtractingKeyword = BillingByFilterQuery.SearchingProperty.ACCIDENT_NUMBER
                ),
                patientName = query.getKeyword(
                    propertyToExtractingKeyword = BillingByFilterQuery.SearchingProperty.PATIENT_NAME
                ),
                usedPeriodFrom = query.usedPeriodFrom,
                usedPeriodUntil = query.usedPeriodUntil,
                billingDateFrom = query.billingDateFrom,
                billingDateUntil = query.billingDateUntil,
                transactionDateFrom = query.transactionDateFrom,
                transactionDateUntil = query.transactionDateUntil,
                caregiverName = query.getKeyword(
                    propertyToExtractingKeyword = BillingByFilterQuery.SearchingProperty.CAREGIVER_NAME
                ),
        ),
            pageable = pageRequest.withSort(query.sorting),
        )

        billings.forEach {
            BillingAccessPolicy.check(query.subject, BillingByIdQuery(it.id, query.subject), it)
        }

        return billings
    }

    private fun BillingByFilterQuery.getKeyword(
        propertyToExtractingKeyword: BillingByFilterQuery.SearchingProperty
    ) = if (searchQuery?.searchingProperty == propertyToExtractingKeyword) {
        searchQuery.keyword
    } else {
        null
    }

    @EventListener(CaregivingRoundModified::class)
    @Transactional
    fun handleCaregivingRoundModified(@IncludingPersonalData event: CaregivingRoundModified) {
        val subject = SystemUser
        val billing = getBillingOrNull(
            BillingByCaregivingRoundIdQuery(
                caregivingRoundId = event.caregivingRoundId,
                subject = subject,
            )
        ) ?: return

        if (!billing.willBeAffectedBy(event)) {
            return
        }

        val reception = receptionByIdQueryHandler.getReception(
            ReceptionByIdQuery(
                receptionId = event.receptionId,
                subject = subject,
            )
        )

        val coverageInfo = coverageByIdQueryHandler.getCoverage(
            CoverageByIdQuery(
                coverageId = reception.insuranceInfo.coverageId,
                subject = subject,
            )
        ) {
            CoverageInfo(
                targetSubscriptionYear = it.targetSubscriptionYear,
                renewalType = if (it.renewalType == RenewalType.TEN_YEAR) {
                    CoverageInfo.RenewalType.TEN_YEAR
                } else {
                    CoverageInfo.RenewalType.THREE_YEAR
                },
                annualCoveredCaregivingCharges = it.annualCoveredCaregivingCharges.map {
                    CoverageInfo.AnnualCoveredCaregivingCharge(
                        targetAccidentYear = it.targetAccidentYear,
                        caregivingCharge = it.caregivingCharge,
                    )
                }
            )
        }

        billing.handleCaregivingRoundModified(event, coverageInfo)
        billingRepository.save(billing)
    }

    @EventListener(CaregivingChargeModified::class)
    @Transactional
    fun handleCaregivingChargeModified(event: CaregivingChargeModified) {
        val subject = SystemUser
        val billing = getBillingOrNull(
            BillingByCaregivingRoundIdQuery(
                caregivingRoundId = event.caregivingRoundId,
                subject = subject,
            )
        ) ?: return

        val reception = receptionByIdQueryHandler.getReception(
            ReceptionByIdQuery(
                receptionId = event.receptionId,
                subject = subject,
            )
        )

        val coverageInfo = coverageByIdQueryHandler.getCoverage(
            CoverageByIdQuery(
                coverageId = reception.insuranceInfo.coverageId,
                subject = subject,
            )
        ) {
            CoverageInfo(
                targetSubscriptionYear = it.targetSubscriptionYear,
                renewalType = if (it.renewalType == RenewalType.TEN_YEAR) {
                    CoverageInfo.RenewalType.TEN_YEAR
                } else {
                    CoverageInfo.RenewalType.THREE_YEAR
                },
                annualCoveredCaregivingCharges = it.annualCoveredCaregivingCharges.map {
                    CoverageInfo.AnnualCoveredCaregivingCharge(
                        targetAccidentYear = it.targetAccidentYear,
                        caregivingCharge = it.caregivingCharge,
                    )
                }
            )
        }

        billing.handleCaregivingChargeModified(event, coverageInfo)
        billingRepository.save(billing)
    }

    @Transactional(readOnly = true)
    override fun getBillingReception(query: BillingByReceptionIdQuery): List<Billing> {
        receptionByIdQueryHandler.ensureReceptionExists(
            ReceptionByIdQuery(
                receptionId = query.receptionId,
                subject = query.subject
            )
        )

        val billings = billingRepository.findByReceptionInfoReceptionIdAndBillingDateIsNotNull(query.receptionId)

        BillingAccessPolicy.checkAll(query.subject, ReadOneAccess, billings)

        return billings
    }

    private fun Pageable.withSort(sorting: BillingByFilterQuery.Sorting?) = this.withSort(orderBy(sorting))

    private fun orderBy(sorting: BillingByFilterQuery.Sorting?) = when (sorting) {
        BillingByFilterQuery.Sorting.ID_DESC -> Sort.by(Sort.Order.desc("id"))
        BillingByFilterQuery.Sorting.BILLING_DATE_ASC -> Sort.by(Sort.Order.asc(Billing::billingDate.name))
        BillingByFilterQuery.Sorting.TRANSACTION_DATE_DESC -> Sort.by(Sort.Order.desc(Billing::lastTransactionDate.name))
        null -> Sort.by(Sort.Order.desc("id"))
    }

    @Transactional
    @EventListener(ReceptionModified::class)
    fun handleReceptionModified(event: ReceptionModified) {
        if (!Billing.isAffectsToBilling(event)) {
            return
        }

        billingRepository.findByReceptionInfoReceptionId(event.receptionId).forEach {
            it.handleReceptionModified(event)
        }
    }
}
