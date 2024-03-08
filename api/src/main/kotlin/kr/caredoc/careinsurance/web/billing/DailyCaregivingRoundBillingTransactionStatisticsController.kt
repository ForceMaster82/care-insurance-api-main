package kr.caredoc.careinsurance.web.billing

import kr.caredoc.careinsurance.billing.statistics.DailyCaregivingRoundBillingTransactionStatistics
import kr.caredoc.careinsurance.billing.statistics.DailyCaregivingRoundBillingTransactionStatisticsByDateQuery
import kr.caredoc.careinsurance.billing.statistics.DailyCaregivingRoundBillingTransactionStatisticsByDateQueryHandler
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.web.billing.response.DailyCaregivingRoundBillingTransactionStatisticsResponse
import kr.caredoc.careinsurance.web.paging.PagedResponse
import kr.caredoc.careinsurance.web.paging.PagingRequest
import kr.caredoc.careinsurance.web.paging.intoPageable
import kr.caredoc.careinsurance.web.paging.intoPagedResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/daily-caregiving-round-billing-transaction-statistics")
class DailyCaregivingRoundBillingTransactionStatisticsController(
    private val dailyCaregivingRoundBillingTransactionStatisticsByDateQueryHandler: DailyCaregivingRoundBillingTransactionStatisticsByDateQueryHandler,
) {

    @GetMapping
    fun getDailyCaregivingRoundBillingTransactionStatics(
        pagingRequest: PagingRequest,
        @RequestParam("date", required = true) date: LocalDate,
        subject: Subject,
    ): ResponseEntity<PagedResponse<DailyCaregivingRoundBillingTransactionStatisticsResponse>> {
        val dailyCaregivingRoundBillingTransactionStatistics =
            dailyCaregivingRoundBillingTransactionStatisticsByDateQueryHandler.getDailyCaregivingRoundBillingTransactionStatistics(
                DailyCaregivingRoundBillingTransactionStatisticsByDateQuery(
                    date = date,
                    subject = subject,
                ),
                pagingRequest.intoPageable(),
            )
        return ResponseEntity.ok(
            dailyCaregivingRoundBillingTransactionStatistics.map { it.intoResponse() }.intoPagedResponse(),
        )
    }

    private fun DailyCaregivingRoundBillingTransactionStatistics.intoResponse() = DailyCaregivingRoundBillingTransactionStatisticsResponse(
        receptionId = receptionId,
        caregivingRoundId = caregivingRoundId,
        date = date,
        totalDepositAmount = totalDepositAmount,
        totalWithdrawalAmount = totalWithdrawalAmount,
    )
}
