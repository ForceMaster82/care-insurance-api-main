package kr.caredoc.careinsurance.web.settlement

import kr.caredoc.careinsurance.caregiving.CaregivingRoundsByFilterQuery
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.settlement.statistics.DailyCaregivingRoundSettlementTransactionStatistics
import kr.caredoc.careinsurance.settlement.statistics.DailyCaregivingRoundSettlementTransactionStatisticsByDateQuery
import kr.caredoc.careinsurance.settlement.statistics.DailyCaregivingRoundSettlementTransactionStatisticsByDateQueryHandler
import kr.caredoc.careinsurance.web.paging.PagedResponse
import kr.caredoc.careinsurance.web.paging.PagingRequest
import kr.caredoc.careinsurance.web.paging.intoPageable
import kr.caredoc.careinsurance.web.paging.intoPagedResponse
import kr.caredoc.careinsurance.web.search.QueryParser
import kr.caredoc.careinsurance.web.settlement.response.DailyCaregivingRoundSettlementTransactionStatisticsResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/daily-caregiving-round-settlement-transaction-statistics")
class DailyCaregivingRoundSettlementTransactionStatisticsController(
    private val dailyCaregivingRoundSettlementTransactionStatisticsByDateQueryHandler: DailyCaregivingRoundSettlementTransactionStatisticsByDateQueryHandler,
) {
    private val queryParser = QueryParser(
        mapOf(
            "patientName" to DailyCaregivingRoundSettlementTransactionStatisticsByDateQuery.SearchingProperty.PATIENT_NAME,
        )
    )

    @GetMapping
    fun getDailyCaregivingSettlementTransactionStatistics(
        @RequestParam("date") date: LocalDate,
        pagingRequest: PagingRequest,
        @RequestParam("query", required = false) query: String?,
        subject: Subject,
    ): ResponseEntity<PagedResponse<DailyCaregivingRoundSettlementTransactionStatisticsResponse>> {
        val statistics = dailyCaregivingRoundSettlementTransactionStatisticsByDateQueryHandler
            .getDailyCaregivingRoundSettlementTransactionStatistics(
                query = DailyCaregivingRoundSettlementTransactionStatisticsByDateQuery(
                    date = date,
                    subject = subject,
                    searchCondition = query?.let { queryParser.parse(it) },
                ),
                pageRequest = pagingRequest.intoPageable(),
            )

        return ResponseEntity.ok(
            statistics.map {
                it.intoResponse()
            }.intoPagedResponse()
        )
    }

    private fun DailyCaregivingRoundSettlementTransactionStatistics.intoResponse() =
        DailyCaregivingRoundSettlementTransactionStatisticsResponse(
            receptionId = receptionId,
            caregivingRoundId = caregivingRoundId,
            date = date,
            totalDepositAmount = totalDepositAmount,
            totalWithdrawalAmount = totalWithdrawalAmount,
        )
}
