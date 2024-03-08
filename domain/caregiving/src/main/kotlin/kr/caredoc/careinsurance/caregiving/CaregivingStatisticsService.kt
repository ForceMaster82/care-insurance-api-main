package kr.caredoc.careinsurance.caregiving

import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.withSort
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class CaregivingStatisticsService(
    private val caregivingRoundRepository: CaregivingRoundRepository,
) : MonthlyRegionalCaregivingStatisticsByFilterQueryHandler {
    companion object {
        private val DEFAULT_SORT = Sort.by(
            Sort.Order.asc(CaregivingRoundRepository.HOSPITAL_STATE_ORDERING),
            Sort.Order.asc(CaregivingRoundRepository.HOSPITAL_CITY_ORDERING),
        )
    }

    @Transactional
    override fun getMonthlyRegionalCaregivingStatistics(
        query: MonthlyRegionalCaregivingStatisticsByFilterQuery,
        pageRequest: Pageable
    ): Page<MonthlyRegionalCaregivingStatistics> {
        CaregivingStatisticsAccessPolicy.check(query.subject, query, Object.Empty)

        val from = LocalDate.of(query.year, query.month, 1).atStartOfDay()
        val until = from.plusMonths(1)

        return if (query.stateFilter != null && query.cityFilter != null) {
            caregivingRoundRepository.getCaregivingStatisticsByPeriodIntersectAndStateAndCity(
                from,
                until,
                query.stateFilter,
                query.cityFilter,
                pageRequest.withSort(DEFAULT_SORT),
            )
        } else if (query.stateFilter != null) {
            caregivingRoundRepository.getCaregivingStatisticsByPeriodIntersectAndState(
                from,
                until,
                query.stateFilter,
                pageRequest.withSort(DEFAULT_SORT),
            )
        } else {
            caregivingRoundRepository.getCaregivingStatisticsByPeriodIntersect(
                from,
                until,
                pageRequest.withSort(DEFAULT_SORT),
            )
        }
    }

    @Transactional
    override fun getMonthlyRegionalCaregivingStatisticsAsCsv(query: MonthlyRegionalCaregivingStatisticsByFilterQuery): String {
        CaregivingStatisticsAccessPolicy.check(query.subject, query, Object.Empty)

        val from = LocalDate.of(query.year, query.month, 1).atStartOfDay()
        val until = from.plusMonths(1)

        val statistics = if (query.stateFilter != null && query.cityFilter != null) {
            caregivingRoundRepository.getCaregivingStatisticsByPeriodIntersectAndStateAndCity(
                from,
                until,
                query.stateFilter,
                query.cityFilter,
                DEFAULT_SORT,
            )
        } else if (query.stateFilter != null) {
            caregivingRoundRepository.getCaregivingStatisticsByPeriodIntersectAndState(
                from,
                until,
                query.stateFilter,
                DEFAULT_SORT,
            )
        } else {
            caregivingRoundRepository.getCaregivingStatisticsByPeriodIntersect(
                from,
                until,
                DEFAULT_SORT,
            )
        }

        return MonthlyRegionalCaregivingStatisticsCsvTemplate.generate(statistics)
    }
}
