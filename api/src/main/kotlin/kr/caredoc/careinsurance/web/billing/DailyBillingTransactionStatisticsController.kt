package kr.caredoc.careinsurance.web.billing

import kr.caredoc.careinsurance.billing.statistics.DailyBillingTransactionStatistics
import kr.caredoc.careinsurance.billing.statistics.DailyBillingTransactionStatisticsByDateQuery
import kr.caredoc.careinsurance.billing.statistics.DailyBillingTransactionStatisticsByDateQueryHandler
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.web.billing.response.DailyBillingTransactionStatisticsResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/daily-billing-transaction-statistics")
class DailyBillingTransactionStatisticsController(
    private val dailyBillingTransactionStatisticsByDateQueryHandler: DailyBillingTransactionStatisticsByDateQueryHandler,
) {
    @GetMapping
    fun getDailyBillingTransactionStatistics(
        @RequestParam("date") date: LocalDate,
        subject: Subject,
    ): ResponseEntity<List<DailyBillingTransactionStatisticsResponse>> {
        val statistics =
            dailyBillingTransactionStatisticsByDateQueryHandler.getDailyBillingTransactionStatistics(
                DailyBillingTransactionStatisticsByDateQuery(
                    date = date,
                    subject = subject,
                )
            )

        return ResponseEntity.ok(
            statistics?.let {
                listOf(it.intoResponse())
            } ?: listOf()
        )
    }

    private fun DailyBillingTransactionStatistics.intoResponse() = DailyBillingTransactionStatisticsResponse(
        date = date,
        totalDepositAmount = totalDepositAmount,
        totalWithdrawalAmount = totalWithdrawalAmount,
    )
}
