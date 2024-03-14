package kr.caredoc.careinsurance.reception.statistics

import com.github.guepardoapps.kulid.ULID
import kr.caredoc.careinsurance.billing.BillingModified
import kr.caredoc.careinsurance.billing.BillingTransactionRecorded
import kr.caredoc.careinsurance.reception.ReceptionByIdQuery
import kr.caredoc.careinsurance.reception.ReceptionByIdQueryHandler
import kr.caredoc.careinsurance.reception.ReceptionModified
import kr.caredoc.careinsurance.reception.ReceptionReceived
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.personaldata.IncludingPersonalData
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class ReceptionStatisticsService(
    private val dailyReceptionStatisticsRepository: DailyReceptionStatisticsRepository,
    private val receptionByIdQueryHandler: ReceptionByIdQueryHandler,
) : DailyReceptionStatisticsByDateRangeQueryHandler {
    @Transactional(readOnly = true)
    override fun getDailyReceptionStatistics(query: DailyReceptionStatisticsByDateRangeQuery): List<DailyReceptionStatistics> {
        DailyReceptionStatisticsAccessPolicy.check(query.subject, query, Object.Empty)

        val resList: List<DailyReceptionStatistics> = dailyReceptionStatisticsRepository.findByReceivedDateBetweenOrderByReceivedDate(query.from, query.until)
        var dayList: MutableList<DailyReceptionStatistics> = arrayListOf()

        LocalDate.parse(query.from.toString())
                .datesUntil(LocalDate.parse(query.until.toString()).plusDays(1))
                .forEach{
                    var drs = DailyReceptionStatistics("1", it)
                    dayList.add(drs)

        }
        resList.forEach{
            var idx : Int = getIndex(it.receivedDate, dayList)
            dayList.set(idx, it)
        }

        return dayList
    }

    fun getIndex(receivedDate:LocalDate, dayList:MutableList<DailyReceptionStatistics>): Int {
        var i : Int = 0
        dayList.forEach {
            if(it.receivedDate == receivedDate){
                return i
            }
            i++
        }
        return 0
    }

    @Transactional(readOnly = true)
    override fun getDailyReceptionStatisticsAsCsv(query: DailyReceptionStatisticsByDateRangeQuery): String {
        DailyReceptionStatisticsAccessPolicy.check(query.subject, query, Object.Empty)
        val dailyReceptionStatistics = getDailyReceptionStatistics(query)

        return DailyReceptionStatisticsCsvTemplate.generate(dailyReceptionStatistics)
    }

    private fun getDailyReceptionStatisticsForUpdate(receivedDate: LocalDate): List<DailyReceptionStatistics> {
        return dailyReceptionStatisticsRepository.findByReceivedDateForUpdate(receivedDate).ifEmpty {
            listOf(
                DailyReceptionStatistics(
                    id = ULID.random(),
                    receivedDate = receivedDate,
                )
            )
        }
    }

    @EventListener(ReceptionReceived::class)
    @Transactional
    fun handleReceptionReceived(event: ReceptionReceived) {
        val dailyReceptionStatistics = getDailyReceptionStatisticsForUpdate(event.receivedDateTime.toLocalDate())

        dailyReceptionStatistics.forEach {
            it.handleReceptionReceived(event)
        }

        dailyReceptionStatisticsRepository.saveAll(dailyReceptionStatistics)
    }

    @EventListener(ReceptionModified::class)
    @Transactional
    fun handleReceptionModified(@IncludingPersonalData event: ReceptionModified) {
        val dailyReceptionStatistics = getDailyReceptionStatisticsForUpdate(event.receivedDateTime.toLocalDate())

        dailyReceptionStatistics.forEach {
            it.handleReceptionModified(event)
        }

        dailyReceptionStatisticsRepository.saveAll(dailyReceptionStatistics)
    }

    @EventListener(BillingModified::class)
    @Transactional
    fun handleBillingModified(event: BillingModified) {
        val reception = receptionByIdQueryHandler.getReception(
            ReceptionByIdQuery(
                receptionId = event.receptionId,
                subject = event.subject
            )
        )

        val dailyReceptionStatistics = getDailyReceptionStatisticsForUpdate(reception.receivedDateTime.toLocalDate())

        dailyReceptionStatistics.forEach {
            it.handleBillingModified(event)
        }

        dailyReceptionStatisticsRepository.saveAll(dailyReceptionStatistics)
    }

    @EventListener(BillingTransactionRecorded::class)
    @Transactional
    fun handleBillingTransactionRecorded(event: BillingTransactionRecorded) {
        val reception = receptionByIdQueryHandler.getReception(
            ReceptionByIdQuery(
                receptionId = event.receptionId,
                subject = event.subject
            )
        )

        val dailyReceptionStatistics = getDailyReceptionStatisticsForUpdate(reception.receivedDateTime.toLocalDate())

        dailyReceptionStatistics.forEach {
            it.handleBillingTransactionRecorded(event)
        }

        dailyReceptionStatisticsRepository.saveAll(dailyReceptionStatistics)
    }
}
