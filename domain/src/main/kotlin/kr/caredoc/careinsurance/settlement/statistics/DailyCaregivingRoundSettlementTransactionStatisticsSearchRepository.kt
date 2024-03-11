package kr.caredoc.careinsurance.settlement.statistics

import kr.caredoc.careinsurance.search.SearchCondition
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface DailyCaregivingRoundSettlementTransactionStatisticsSearchRepository {
    fun getDataList(
        searchingCriteria: SearchingCriteria,
        pageable: Pageable,
    ): Page<DailyCaregivingRoundSettlementTransactionStatistics>

    data class SearchingCriteria(
        val date: LocalDate?,
        val patientName: String? = null,
    )


}
