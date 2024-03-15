package kr.caredoc.careinsurance.caregiving

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface CaregivingRoundRepository : JpaRepository<CaregivingRound, String>, CaregivingRoundSearchingRepository {
    companion object {
        const val HOSPITAL_STATE_ORDERING = "r.accidentInfo.hospitalAndRoomInfo.state"
        const val HOSPITAL_CITY_ORDERING = "r.accidentInfo.hospitalAndRoomInfo.city"
    }

    fun existsByReceptionInfoReceptionId(receptionId: String): Boolean

    fun findByReceptionInfoReceptionIdOrderByCaregivingRoundNumberDesc(
        receptionId: String
    ): List<CaregivingRound>

    fun findByIdIn(id: Collection<String>): List<CaregivingRound>

    @Query(
        """
            SELECT new kr.caredoc.careinsurance.caregiving.MonthlyRegionalCaregivingStatistics(
                r.accidentInfo.hospitalAndRoomInfo.state,
                '',
                COUNT(DISTINCT r.id)
            )
            FROM CaregivingRound cr
            JOIN Reception r on r.id = cr.receptionInfo.receptionId
            WHERE 
                cr.caregivingStateData.startDateTime < :until
                AND cr.caregivingStateData.startDateTime >= :from
                AND r.accidentInfo.hospitalAndRoomInfo.state != null 
            GROUP BY
                r.accidentInfo.hospitalAndRoomInfo.state
        """
    )
    fun getCaregivingStatisticsByPeriodIntersect(
        from: LocalDateTime,
        until: LocalDateTime,
        pageable: Pageable,
    ): Page<MonthlyRegionalCaregivingStatistics>

    @Query(
        """
            SELECT new kr.caredoc.careinsurance.caregiving.MonthlyRegionalCaregivingStatistics(
                r.accidentInfo.hospitalAndRoomInfo.state,
                r.accidentInfo.hospitalAndRoomInfo.city,
                COUNT(DISTINCT r.id)
            )
            FROM CaregivingRound cr
            JOIN Reception r on r.id = cr.receptionInfo.receptionId
            WHERE 
                cr.caregivingStateData.startDateTime < :until
                AND cr.caregivingStateData.startDateTime >= :from
                AND r.accidentInfo.hospitalAndRoomInfo.state = :state 
            GROUP BY
                r.accidentInfo.hospitalAndRoomInfo.state,
                r.accidentInfo.hospitalAndRoomInfo.city
        """
    )
    fun getCaregivingStatisticsByPeriodIntersectAndState(
        from: LocalDateTime,
        until: LocalDateTime,
        state: String,
        pageable: Pageable,
    ): Page<MonthlyRegionalCaregivingStatistics>

    @Query(
        """
            SELECT new kr.caredoc.careinsurance.caregiving.MonthlyRegionalCaregivingStatistics(
                r.accidentInfo.hospitalAndRoomInfo.state,
                r.accidentInfo.hospitalAndRoomInfo.city,
                COUNT(DISTINCT r.id)
            )
            FROM CaregivingRound cr
            JOIN Reception r on r.id = cr.receptionInfo.receptionId
            WHERE 
                cr.caregivingStateData.startDateTime < :until
                AND cr.caregivingStateData.startDateTime >= :from
                AND r.accidentInfo.hospitalAndRoomInfo.state = :state 
                AND r.accidentInfo.hospitalAndRoomInfo.city = :city
            GROUP BY
                r.accidentInfo.hospitalAndRoomInfo.state,
                r.accidentInfo.hospitalAndRoomInfo.city
        """
    )
    fun getCaregivingStatisticsByPeriodIntersectAndStateAndCity(
        from: LocalDateTime,
        until: LocalDateTime,
        state: String,
        city: String,
        pageable: Pageable,
    ): Page<MonthlyRegionalCaregivingStatistics>

    @Query(
        """
            SELECT new kr.caredoc.careinsurance.caregiving.MonthlyRegionalCaregivingStatistics(
                r.accidentInfo.hospitalAndRoomInfo.state,
                '',
                COUNT(DISTINCT r.id)
            )
            FROM CaregivingRound cr
            JOIN Reception r on r.id = cr.receptionInfo.receptionId
            WHERE 
                cr.caregivingStateData.startDateTime < :until
                AND cr.caregivingStateData.startDateTime >= :from
                AND r.accidentInfo.hospitalAndRoomInfo.state != null 
            GROUP BY
                r.accidentInfo.hospitalAndRoomInfo.state
        """
    )
    fun getCaregivingStatisticsByPeriodIntersect(
        from: LocalDateTime,
        until: LocalDateTime,
        sort: Sort,
    ): List<MonthlyRegionalCaregivingStatistics>

    @Query(
        """
            SELECT new kr.caredoc.careinsurance.caregiving.MonthlyRegionalCaregivingStatistics(
                r.accidentInfo.hospitalAndRoomInfo.state,
                r.accidentInfo.hospitalAndRoomInfo.city,
                COUNT(DISTINCT r.id)
            )
            FROM CaregivingRound cr
            JOIN Reception r on r.id = cr.receptionInfo.receptionId
            WHERE 
                cr.caregivingStateData.startDateTime < :until
                AND cr.caregivingStateData.startDateTime >= :from
                AND r.accidentInfo.hospitalAndRoomInfo.state = :state 
            GROUP BY
                r.accidentInfo.hospitalAndRoomInfo.state,
                r.accidentInfo.hospitalAndRoomInfo.city
        """
    )
    fun getCaregivingStatisticsByPeriodIntersectAndState(
        from: LocalDateTime,
        until: LocalDateTime,
        state: String,
        sort: Sort,
    ): List<MonthlyRegionalCaregivingStatistics>

    @Query(
        """
            SELECT new kr.caredoc.careinsurance.caregiving.MonthlyRegionalCaregivingStatistics(
                r.accidentInfo.hospitalAndRoomInfo.state,
                r.accidentInfo.hospitalAndRoomInfo.city,
                COUNT(DISTINCT r.id)
            )
            FROM CaregivingRound cr
            JOIN Reception r on r.id = cr.receptionInfo.receptionId
            WHERE 
                cr.caregivingStateData.startDateTime < :until
                AND cr.caregivingStateData.startDateTime >= :from
                AND r.accidentInfo.hospitalAndRoomInfo.state = :state 
                AND r.accidentInfo.hospitalAndRoomInfo.city = :city
            GROUP BY
                r.accidentInfo.hospitalAndRoomInfo.state,
                r.accidentInfo.hospitalAndRoomInfo.city
        """
    )
    fun getCaregivingStatisticsByPeriodIntersectAndStateAndCity(
        from: LocalDateTime,
        until: LocalDateTime,
        state: String,
        city: String,
        sort: Sort,
    ): List<MonthlyRegionalCaregivingStatistics>
}
