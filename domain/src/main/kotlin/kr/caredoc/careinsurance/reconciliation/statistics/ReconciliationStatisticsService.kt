package kr.caredoc.careinsurance.reconciliation.statistics

import kr.caredoc.careinsurance.reconciliation.ClosingStatus
import kr.caredoc.careinsurance.reconciliation.ReconciliationRepository
import kr.caredoc.careinsurance.security.accesscontrol.Object
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReconciliationStatisticsService(
    private val reconciliationRepository: ReconciliationRepository,
) : MonthlyReconciliationStatisticsByYearAndMonthQueryHandler {
    @Transactional
    override fun getMonthlyReconciliationStatistics(query: MonthlyReconciliationStatisticsByYearAndMonthQuery): MonthlyReconciliationStatistics? {
        ReconciliationStatisticsAccessPolicy.check(query.subject, query, Object.Empty)

        return try {
            getReconciliationStatistics(query.year, query.month)
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }

    private fun getReconciliationStatistics(
        year: Int,
        month: Int,
    ): MonthlyReconciliationStatistics {
        val distinctReceptionCount =
            reconciliationRepository.countDistinctReceptionIdByReconciledYearAndReconciledMonthAndClosingStatus(
                year,
                month,
                ClosingStatus.CLOSED,
            )

        val distinctPhoneNumberCount =
            reconciliationRepository.countDistinctReceptionCaregiverPhoneNumberCountByReconciledYearAndReconciledMonthAndClosingStatus(
                year,
                month,
                ClosingStatus.CLOSED,
            )

        val latestCaregivingPeriodSummation =
            reconciliationRepository.sumLatestReconciliationCaregivingSecondsByReconciledYearAndReconciledMonthAndClosingStatus(
                year,
                month,
                ClosingStatus.CLOSED,
            ) / 86400

        val financialSummations =
            reconciliationRepository.sumReconciliationFinancialPropertiesByReconciledYearAndReconciledMonthAndClosingStatus(
                year,
                month,
                ClosingStatus.CLOSED,
            )

        return MonthlyReconciliationStatistics(
            year = year,
            month = month,
            receptionCount = distinctReceptionCount,
            caregiverCount = distinctPhoneNumberCount,
            totalCaregivingPeriod = latestCaregivingPeriodSummation.toInt(),
            totalBillingAmount = financialSummations.totalBillingAmount.toInt(),
            totalSettlementAmount = financialSummations.totalSettlementAmount.toInt(),
            totalSales = financialSummations.totalSales.toInt(),
            totalDistributedProfit = financialSummations.totalDistributedProfit.toInt(),
        )
    }
}
