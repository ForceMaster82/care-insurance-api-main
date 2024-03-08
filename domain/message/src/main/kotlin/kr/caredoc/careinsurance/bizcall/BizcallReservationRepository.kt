package kr.caredoc.careinsurance.bizcall

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface BizcallReservationRepository : JpaRepository<BizcallReservation, String> {
    fun findBySendingDateTime(sendingDateTime: LocalDateTime): BizcallReservation?
}
