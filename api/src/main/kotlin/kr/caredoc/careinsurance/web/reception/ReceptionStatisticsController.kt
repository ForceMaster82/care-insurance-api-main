package kr.caredoc.careinsurance.web.reception

import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.reception.ReceptionProgressingStatus
import kr.caredoc.careinsurance.reception.statistics.DailyReceptionStatistics
import kr.caredoc.careinsurance.reception.statistics.DailyReceptionStatisticsByDateRangeQuery
import kr.caredoc.careinsurance.reception.statistics.DailyReceptionStatisticsByDateRangeQueryHandler
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.web.reception.response.DailyReceptionStatisticsResponse
import kr.caredoc.careinsurance.web.request.DatePeriodSpecifyingRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/v1/daily-reception-statistics")
class ReceptionStatisticsController(
    private val dailyReceptionStatisticsByDateRangeQueryHandler: DailyReceptionStatisticsByDateRangeQueryHandler,
) {
    @GetMapping(headers = ["Accept!=text/csv"])
    fun getDailyReceptionStatistics(
        datePeriodSpecifyingRequest: DatePeriodSpecifyingRequest,
        subject: Subject,
    ): ResponseEntity<List<DailyReceptionStatisticsResponse>> {
        val dailyReceptionStatistics = dailyReceptionStatisticsByDateRangeQueryHandler.getDailyReceptionStatistics(
            DailyReceptionStatisticsByDateRangeQuery(
                from = datePeriodSpecifyingRequest.from,
                until = datePeriodSpecifyingRequest.until,
                subject = subject,
            )
        )
        return ResponseEntity.ok(
            dailyReceptionStatistics.map {
                it.intoResponse()
            }
        )
    }

    @GetMapping(headers = ["Accept=text/csv"])
    fun getDailyReceptionStatisticsAsCsv(
        datePeriodSpecifyingRequest: DatePeriodSpecifyingRequest,
        subject: Subject,
    ): ResponseEntity<ByteArray> {
        val dailyReceptionStatisticsSheet =
            dailyReceptionStatisticsByDateRangeQueryHandler.getDailyReceptionStatisticsAsCsv(
                DailyReceptionStatisticsByDateRangeQuery(
                    from = datePeriodSpecifyingRequest.from,
                    until = datePeriodSpecifyingRequest.until,
                    subject = subject,
                )
            )

        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                generateDailyReceptionStatisticsCsvContentDispositionHeader(Clock.today()),
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
                    dailyReceptionStatisticsSheet.toByteArray(Charsets.UTF_8)
            )
    }

    fun generateDailyReceptionStatisticsCsvContentDispositionHeader(date: LocalDate): String {
        return "attachment; filename=\"${generateDailyReceptionStatisticsCsvFileName(date)}\""
    }

    fun generateDailyReceptionStatisticsCsvFileName(date: LocalDate): String {
        val dateString = date.format(DateTimeFormatter.BASIC_ISO_DATE)
        return URLEncoder.encode("케어닥_일일업무보고_$dateString.csv", "UTF-8")
    }

    private fun DailyReceptionStatistics.intoResponse(): DailyReceptionStatisticsResponse {
        return DailyReceptionStatisticsResponse(
            receivedDate = receivedDate,
            receptionCount = receptionCount,
            canceledReceptionCount = canceledReceptionCount,
            canceledReceptionCountsByReason = mapOf(
                ReceptionProgressingStatus.CANCELED_BY_PERSONAL_CAREGIVER to canceledByPersonalCaregiverReceptionCount,
                ReceptionProgressingStatus.CANCELED_BY_MEDICAL_REQUEST to canceledByMedicalRequestReceptionCount,
            ),
            requestedBillingCount = requestedBillingCount,
            requestedBillingAmount = requestedBillingAmount,
            depositCount = depositCount,
            depositAmount = depositAmount,
            withdrawalCount = withdrawalCount,
            withdrawalAmount = withdrawalAmount,
            sameDayAssignmentReceptionCount = sameDayAssignmentReceptionCount,
            startedSameDayAssignmentReceptionCount = startedSameDayAssignmentReceptionCount,
            shortTermReceptionCount = shortTermReceptionCount,
            startedShortTermReceptionCount = startedShortTermReceptionCount,
        )
    }
}
