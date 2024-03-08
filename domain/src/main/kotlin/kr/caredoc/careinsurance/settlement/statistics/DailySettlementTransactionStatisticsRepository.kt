package kr.caredoc.careinsurance.settlement.statistics

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface DailySettlementTransactionStatisticsRepository : JpaRepository<DailySettlementTransactionStatistics, String> {
    fun findByDate(date: LocalDate): List<DailySettlementTransactionStatistics>
}
