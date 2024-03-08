package kr.caredoc.careinsurance.reception.statistics

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface DailyReceptionStatisticsRepository : JpaRepository<DailyReceptionStatistics, String> {
    fun findByReceivedDateBetweenOrderByReceivedDate(from: LocalDate, until: LocalDate): List<DailyReceptionStatistics>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
            SELECT drs
            FROM DailyReceptionStatistics drs
            WHERE drs.receivedDate = :receivedDate
            ORDER BY drs.receivedDate ASC
        """
    )
    fun findByReceivedDateForUpdate(receivedDate: LocalDate): List<DailyReceptionStatistics>
}
