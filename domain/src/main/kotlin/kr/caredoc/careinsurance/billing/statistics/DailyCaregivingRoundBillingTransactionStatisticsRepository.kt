package kr.caredoc.careinsurance.billing.statistics

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface DailyCaregivingRoundBillingTransactionStatisticsRepository : JpaRepository<DailyCaregivingRoundBillingTransactionStatistics, String> {
    fun findByDate(date: LocalDate, pageable: Pageable): Page<DailyCaregivingRoundBillingTransactionStatistics>

    fun findTopByDateAndCaregivingRoundId(
        date: LocalDate,
        caregivingRoundId: String,
    ): DailyCaregivingRoundBillingTransactionStatistics?
}
