package kr.caredoc.careinsurance.settlement.statistics

import kr.caredoc.careinsurance.search.SearchCondition
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface DailyCaregivingRoundSettlementTransactionStatisticsRepository :
    JpaRepository<DailyCaregivingRoundSettlementTransactionStatistics, String>, DailyCaregivingRoundSettlementTransactionStatisticsSearchRepository {
    fun findByDate(
        date: LocalDate,
        pageable: Pageable
    ): Page<DailyCaregivingRoundSettlementTransactionStatistics>

    fun findTopByDateAndCaregivingRoundId(
        date: LocalDate,
        caregivingRoundId: String,
    ): DailyCaregivingRoundSettlementTransactionStatistics?

}
