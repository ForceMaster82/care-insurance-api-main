package kr.caredoc.careinsurance.reception

import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.time.LocalDateTime

@Embeddable
data class AccidentInfo(
    @Access(AccessType.FIELD)
    val accidentNumber: String,
    val accidentDateTime: LocalDateTime,
    @Enumerated(EnumType.STRING)
    val claimType: ClaimType,
    val patientDescription: String,
    val admissionDateTime: LocalDateTime,
    @AttributeOverrides(
        AttributeOverride(name = "state", column = Column(name = "hospital_state")),
        AttributeOverride(name = "city", column = Column(name = "hospital_city")),
    )
    val hospitalAndRoomInfo: HospitalAndRoomInfo,
) {
    @Embeddable
    data class HospitalAndRoomInfo(
        @Access(AccessType.FIELD)
        val state: String?,
        val city: String?,
        val hospitalAndRoom: String,
    )
}
