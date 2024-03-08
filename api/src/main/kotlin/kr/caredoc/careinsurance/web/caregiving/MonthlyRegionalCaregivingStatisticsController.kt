package kr.caredoc.careinsurance.web.caregiving

import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.caregiving.MonthlyRegionalCaregivingStatistics
import kr.caredoc.careinsurance.caregiving.MonthlyRegionalCaregivingStatisticsByFilterQuery
import kr.caredoc.careinsurance.caregiving.MonthlyRegionalCaregivingStatisticsByFilterQueryHandler
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.web.caregiving.response.MonthlyRegionalCaregivingStatisticsResponse
import kr.caredoc.careinsurance.web.paging.PagedResponse
import kr.caredoc.careinsurance.web.paging.PagingRequest
import kr.caredoc.careinsurance.web.paging.intoPageable
import kr.caredoc.careinsurance.web.paging.intoPagedResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/v1/monthly-regional-caregiving-statistics")
class MonthlyRegionalCaregivingStatisticsController(
    private val monthlyRegionalCaregivingStatisticsByFilterQueryHandler: MonthlyRegionalCaregivingStatisticsByFilterQueryHandler,
) {
    @GetMapping(headers = ["Accept!=text/csv"])
    fun getMonthlyRegionalCaregivingStatistics(
        @RequestParam("year") year: Int,
        @RequestParam("month") month: Int,
        @RequestParam("state") state: String?,
        @RequestParam("city") city: String?,
        pagingRequest: PagingRequest,
        subject: Subject,
    ): ResponseEntity<PagedResponse<MonthlyRegionalCaregivingStatisticsResponse>> {
        return ResponseEntity.ok(
            monthlyRegionalCaregivingStatisticsByFilterQueryHandler.getMonthlyRegionalCaregivingStatistics(
                MonthlyRegionalCaregivingStatisticsByFilterQuery(
                    year = year,
                    month = month,
                    stateFilter = state,
                    cityFilter = city,
                    subject = subject,
                ),
                pagingRequest.intoPageable(),
            ).map {
                it.intoResponse(year, month)
            }.intoPagedResponse()
        )
    }

    @GetMapping(headers = ["Accept=text/csv"])
    fun getMonthlyRegionalCaregivingStatistics(
        @RequestParam("year") year: Int,
        @RequestParam("month") month: Int,
        @RequestParam("state") state: String?,
        @RequestParam("city") city: String?,
        subject: Subject,
    ): ResponseEntity<ByteArray> {
        val regionalCaregivingStatistics =
            monthlyRegionalCaregivingStatisticsByFilterQueryHandler.getMonthlyRegionalCaregivingStatisticsAsCsv(
                MonthlyRegionalCaregivingStatisticsByFilterQuery(
                    year = year,
                    month = month,
                    stateFilter = state,
                    cityFilter = city,
                    subject = subject,
                ),
            )

        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                generateMonthlyRegionalCaregivingStatisticsCsvContentDispositionHeader(Clock.today()),
            )
            .header(
                HttpHeaders.CONTENT_ENCODING,
                "UTF-8"
            )
            .header(
                HttpHeaders.CONTENT_TYPE,
                "text/csv; charset=UTF-8"
            )
            .body(
                byteArrayOf(239.toByte(), 187.toByte(), 191.toByte()) +
                    regionalCaregivingStatistics.toByteArray(Charsets.UTF_8)
            )
    }

    private fun generateMonthlyRegionalCaregivingStatisticsCsvContentDispositionHeader(date: LocalDate): String {
        return "attachment; filename=\"${generateMonthlyRegionalCaregivingStatisticsCsvFileName(date)}\""
    }

    private fun generateMonthlyRegionalCaregivingStatisticsCsvFileName(date: LocalDate): String {
        val dateString = date.format(DateTimeFormatter.BASIC_ISO_DATE)
        return URLEncoder.encode("지역별간병현황_$dateString.csv", "UTF-8")
    }

    private fun MonthlyRegionalCaregivingStatistics.intoResponse(year: Int, month: Int) =
        MonthlyRegionalCaregivingStatisticsResponse(
            year = year,
            month = month,
            state = state,
            city = city,
            receptionCount = receptionCount,
        )
}
