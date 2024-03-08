package kr.caredoc.careinsurance.billing.statistics

import com.github.guepardoapps.kulid.ULID
import kr.caredoc.careinsurance.billing.BillingTransactionRecorded
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.withSort
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BillingStatisticsService(
    private val dailyBillingTransactionStatisticsRepository: DailyBillingTransactionStatisticsRepository,
    private val dailyCaregivingRoundBillingTransactionStatisticsRepository: DailyCaregivingRoundBillingTransactionStatisticsRepository,
) : DailyBillingTransactionStatisticsByDateQueryHandler,
    DailyCaregivingRoundBillingTransactionStatisticsByDateQueryHandler {
    @Transactional(readOnly = true)
    override fun getDailyBillingTransactionStatistics(query: DailyBillingTransactionStatisticsByDateQuery): DailyBillingTransactionStatistics? {
        DailyBillingTransactionStatisticsAccessPolicy.check(query.subject, query, Object.Empty)

        val statistics = dailyBillingTransactionStatisticsRepository.findByDate(query.date)

        if (statistics.isEmpty()) {
            return null
        }

        return statistics[0]
    }

    @EventListener(BillingTransactionRecorded::class)
    @Transactional
    fun handleBillingTransactionRecorded(event: BillingTransactionRecorded) {
        updateDailyBillingTransactionStatistics(event)
        updateDailyCaregivingRoundBillingTransactionStatistics(event)
    }

    @Transactional
    override fun getDailyCaregivingRoundBillingTransactionStatistics(query: DailyCaregivingRoundBillingTransactionStatisticsByDateQuery, pageRequest: Pageable): Page<DailyCaregivingRoundBillingTransactionStatistics> {
        DailyCaregivingRoundBillingTransactionStatisticsAccessPolicy.check(query.subject, query, Object.Empty)

        return dailyCaregivingRoundBillingTransactionStatisticsRepository.findByDate(
            query.date,
            pageRequest.withSort(Sort.by(Sort.Order.desc(DailyCaregivingRoundBillingTransactionStatistics::lastEnteredDateTime.name)))
        )
    }

    private fun updateDailyCaregivingRoundBillingTransactionStatistics(event: BillingTransactionRecorded) {
        val statistics =
            dailyCaregivingRoundBillingTransactionStatisticsRepository.findTopByDateAndCaregivingRoundId(
                event.transactionDate,
                event.caregivingRoundId,
            ) ?: DailyCaregivingRoundBillingTransactionStatistics(
                id = ULID.random(),
                receptionId = event.receptionId,
                caregivingRoundId = event.caregivingRoundId,
                date = event.transactionDate,
            )

        statistics.handleBillingTransactionRecorded(event)

        dailyCaregivingRoundBillingTransactionStatisticsRepository.save(statistics)
    }

    private fun updateDailyBillingTransactionStatistics(event: BillingTransactionRecorded) {
        val statistics = dailyBillingTransactionStatisticsRepository.findByDate(
            event.transactionDate
        ).ifEmpty {
            listOf(
                DailyBillingTransactionStatistics(
                    id = ULID.random(),
                    date = event.transactionDate,
                )
            )
        }

        statistics.forEach { it.handleBillingTransactionRecorded(event) }

        dailyBillingTransactionStatisticsRepository.saveAll(statistics)
    }
}
