package kr.caredoc.careinsurance.web.reconciliation

import kr.caredoc.careinsurance.reconciliation.statistics.MonthlyReconciliationStatistics
import kr.caredoc.careinsurance.reconciliation.statistics.MonthlyReconciliationStatisticsByYearAndMonthQuery
import kr.caredoc.careinsurance.reconciliation.statistics.MonthlyReconciliationStatisticsByYearAndMonthQueryHandler
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.web.reconciliation.response.MonthlyReconciliationStatisticsResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/monthly-reconciliation-statistics")
class ReconciliationStatisticsController(
    private val monthlyReconciliationStatisticsByYearAndMonthQueryHandler: MonthlyReconciliationStatisticsByYearAndMonthQueryHandler
) {

    @GetMapping
    fun getMonthlyReconciliationStatistics(
        @RequestParam("year") year: Int,
        @RequestParam("month") month: Int,
        subject: Subject,
    ): ResponseEntity<List<MonthlyReconciliationStatisticsResponse>> {
        val monthlyReconciliationStatistics = monthlyReconciliationStatisticsByYearAndMonthQueryHandler
            .getMonthlyReconciliationStatistics(
                MonthlyReconciliationStatisticsByYearAndMonthQuery(
                    year = year,
                    month = month,
                    subject = subject,
                )
            )

        return ResponseEntity.ok(
            monthlyReconciliationStatistics?.intoResponse()?.let { listOf(it) } ?: listOf()
        )
    }

    fun MonthlyReconciliationStatistics.intoResponse() = MonthlyReconciliationStatisticsResponse(
        year = year,
        month = month,
        receptionCount = receptionCount,
        caregiverCount = caregiverCount,
        totalCaregivingPeriod = totalCaregivingPeriod,
        totalBillingAmount = totalBillingAmount,
        totalSettlementAmount = totalSettlementAmount,
        totalProfit = totalSales,
        totalDistributedProfit = totalDistributedProfit,
    )
}
