package kr.caredoc.careinsurance.web.settlement

import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.settlement.statistics.DailySettlementTransactionStatistics
import kr.caredoc.careinsurance.settlement.statistics.DailySettlementTransactionStatisticsByDateQuery
import kr.caredoc.careinsurance.settlement.statistics.DailySettlementTransactionStatisticsByDateQueryHandler
import kr.caredoc.careinsurance.web.settlement.response.DailySettlementTransactionStatisticsResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/daily-settlement-transaction-statistics")
class DailySettlementTransactionStatisticsController(
    private val dailySettlementTransactionStatisticsByDateQueryHandler: DailySettlementTransactionStatisticsByDateQueryHandler,
) {
    @GetMapping
    fun getDailySettlementTransactionStatistics(
        @RequestParam("date") date: LocalDate,
        subject: Subject,
    ): ResponseEntity<List<DailySettlementTransactionStatisticsResponse>> {
        val statistics = dailySettlementTransactionStatisticsByDateQueryHandler.getDailySettlementTransactionStatistics(
            DailySettlementTransactionStatisticsByDateQuery(
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

    private fun DailySettlementTransactionStatistics.intoResponse() = DailySettlementTransactionStatisticsResponse(
        date = date,
        totalDepositAmount = totalDepositAmount,
        totalWithdrawalAmount = totalWithdrawalAmount,
    )
}
