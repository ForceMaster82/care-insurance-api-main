package kr.caredoc.careinsurance.caregiving

import java.util.StringJoiner

object MonthlyRegionalCaregivingStatisticsCsvTemplate {
    private const val HEADER =
        "시/도,시/군/구,돌봄환자 수"

    fun generate(data: Collection<MonthlyRegionalCaregivingStatistics>): String {
        val joiner = StringJoiner("\n")
        joiner.add(HEADER)

        data.forEach { joiner.add(generateDataRecord(it)) }

        return joiner.toString()
    }

    private fun generateDataRecord(data: MonthlyRegionalCaregivingStatistics): String {
        val joiner = StringJoiner(",")

        joiner.add(data.state)
        joiner.add(data.city)
        joiner.add(data.receptionCount.toString())

        return joiner.toString()
    }
}
