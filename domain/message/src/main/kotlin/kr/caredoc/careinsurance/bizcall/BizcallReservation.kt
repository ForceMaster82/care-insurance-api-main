package kr.caredoc.careinsurance.bizcall

import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Entity
import kr.caredoc.careinsurance.AggregateRoot
import java.time.LocalDateTime

@Entity
class BizcallReservation(
    id: String,
    @Access(AccessType.FIELD)
    val sendingDateTime: LocalDateTime,
    val bizcallId: String,
) : AggregateRoot(id)
