package kr.caredoc.careinsurance.billing.statistics

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface DailyBillingTransactionStatisticsRepository : JpaRepository<DailyBillingTransactionStatistics, String> {
    fun findByDate(date: LocalDate): List<DailyBillingTransactionStatistics>
}
