package kr.caredoc.careinsurance.settlement.statistics

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface DailyCaregivingRoundSettlementTransactionStatisticsByDateQueryHandler {
    fun getDailyCaregivingRoundSettlementTransactionStatistics(
        query: DailyCaregivingRoundSettlementTransactionStatisticsByDateQuery,
        pageRequest: Pageable,
    ): Page<DailyCaregivingRoundSettlementTransactionStatistics>
}
