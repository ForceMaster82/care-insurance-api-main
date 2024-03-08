package kr.caredoc.careinsurance.settlement.statistics

import com.github.guepardoapps.kulid.ULID
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.settlement.SettlementTransactionRecorded
import kr.caredoc.careinsurance.withSort
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SettlementStatisticsService(
    private val dailySettlementTransactionStatisticsRepository: DailySettlementTransactionStatisticsRepository,
    private val dailyCaregivingRoundSettlementTransactionStatisticsRepository: DailyCaregivingRoundSettlementTransactionStatisticsRepository,
) : DailySettlementTransactionStatisticsByDateQueryHandler,
    DailyCaregivingRoundSettlementTransactionStatisticsByDateQueryHandler {
    @Transactional(readOnly = true)
    override fun getDailySettlementTransactionStatistics(query: DailySettlementTransactionStatisticsByDateQuery): DailySettlementTransactionStatistics? {
        DailySettlementTransactionStatisticsAccessPolicy.check(query.subject, query, Object.Empty)

        val statistics = dailySettlementTransactionStatisticsRepository.findByDate(query.date)

        if (statistics.isEmpty()) {
            return null
        }

        return statistics[0]
    }

    @EventListener(SettlementTransactionRecorded::class)
    @Transactional
    fun handleSettlementTransactionRecorded(event: SettlementTransactionRecorded) {
        updateDailySettlementTransactionStatistics(event)
        updateDailyCaregivingRoundSettlementTransactionStatistics(event)
    }

    private fun updateDailyCaregivingRoundSettlementTransactionStatistics(event: SettlementTransactionRecorded) {
        val statistics =
            dailyCaregivingRoundSettlementTransactionStatisticsRepository.findTopByDateAndCaregivingRoundId(
                event.transactionDate,
                event.caregivingRoundId,
            ) ?: DailyCaregivingRoundSettlementTransactionStatistics(
                id = ULID.random(),
                receptionId = event.receptionId,
                caregivingRoundId = event.caregivingRoundId,
                date = event.transactionDate,
            )

        statistics.handleSettlementTransactionRecorded(event)

        dailyCaregivingRoundSettlementTransactionStatisticsRepository.save(statistics)
    }

    private fun updateDailySettlementTransactionStatistics(event: SettlementTransactionRecorded) {
        val statistics = dailySettlementTransactionStatisticsRepository.findByDate(
            event.transactionDate
        ).ifEmpty {
            listOf(
                DailySettlementTransactionStatistics(
                    id = ULID.random(),
                    date = event.transactionDate,
                )
            )
        }

        statistics.forEach { it.handleSettlementTransactionRecorded(event) }

        dailySettlementTransactionStatisticsRepository.saveAll(statistics)
    }

    @Transactional(readOnly = true)
    override fun getDailyCaregivingRoundSettlementTransactionStatistics(
        query: DailyCaregivingRoundSettlementTransactionStatisticsByDateQuery,
        pageRequest: Pageable,
    ): Page<DailyCaregivingRoundSettlementTransactionStatistics> {
        DailyCaregivingRoundSettlementTransactionStatisticsAccessPolicy.check(query.subject, query, Object.Empty)

        return dailyCaregivingRoundSettlementTransactionStatisticsRepository.findByDate(
            query.date,
            pageRequest.withSort(Sort.by(Sort.Order.desc(DailyCaregivingRoundSettlementTransactionStatistics::lastEnteredDateTime.name)))
        )
    }
}
