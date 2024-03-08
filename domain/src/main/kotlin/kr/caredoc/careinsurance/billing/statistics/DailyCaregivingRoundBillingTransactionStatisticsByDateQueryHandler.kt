package kr.caredoc.careinsurance.billing.statistics

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface DailyCaregivingRoundBillingTransactionStatisticsByDateQueryHandler {
    fun getDailyCaregivingRoundBillingTransactionStatistics(
        query: DailyCaregivingRoundBillingTransactionStatisticsByDateQuery,
        pageRequest: Pageable,
    ): Page<DailyCaregivingRoundBillingTransactionStatistics>
}
