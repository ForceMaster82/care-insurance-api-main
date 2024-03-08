package kr.caredoc.careinsurance.reception.statistics

import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.util.StringJoiner

object DailyReceptionStatisticsCsvTemplate {
    private const val HEADER =
        "구분,요일,전체 배정 건,간병인 파견 건,개인구인 건,석션 등 의료행위,취소 건,지급 요청 건,지급요청금액,지급받은 건,지급받은 금액,환수 건,환수금액,민원 건,당일배정요청 건,배정 건,단기 요청 건,배정 건"

    fun generate(data: Collection<DailyReceptionStatistics>): String {
        val joiner = StringJoiner("\n")
        joiner.add(HEADER)

        data.forEach { joiner.add(generateDataRecord(it)) }

        return joiner.toString()
    }

    private fun generateDataRecord(data: DailyReceptionStatistics): String {
        val joiner = StringJoiner(",")

        joiner.add(data.receivedDate.format(DateTimeFormatter.ISO_DATE))
        joiner.add(data.receivedDate.dayOfWeek.toKoreanString())
        joiner.add(data.receptionCount.toString())
        joiner.add((data.receptionCount - data.canceledReceptionCount).toString())
        joiner.add(data.canceledByPersonalCaregiverReceptionCount.toString())
        joiner.add(data.canceledByMedicalRequestReceptionCount.toString())
        joiner.add(data.canceledReceptionCount.toString())
        joiner.add(data.requestedBillingCount.toString())
        joiner.add(data.requestedBillingAmount.toString())
        joiner.add(data.depositCount.toString())
        joiner.add(data.depositAmount.toString())
        joiner.add(data.withdrawalCount.toString())
        joiner.add(data.withdrawalAmount.toString())
        joiner.add("0")
        joiner.add(data.sameDayAssignmentReceptionCount.toString())
        joiner.add(data.startedSameDayAssignmentReceptionCount.toString())
        joiner.add(data.shortTermReceptionCount.toString())
        joiner.add(data.startedShortTermReceptionCount.toString())

        return joiner.toString()
    }

    private fun DayOfWeek.toKoreanString() = when (this) {
        DayOfWeek.MONDAY -> "월"
        DayOfWeek.TUESDAY -> "화"
        DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"
        DayOfWeek.FRIDAY -> "금"
        DayOfWeek.SATURDAY -> "토"
        DayOfWeek.SUNDAY -> "일"
    }
}
