package kr.caredoc.careinsurance.caregiving.state

import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import kr.caredoc.careinsurance.caregiving.CaregiverInfo
import kr.caredoc.careinsurance.caregiving.CaregivingProgressingStatus
import kr.caredoc.careinsurance.caregiving.ClosingReasonType
import java.time.LocalDateTime

@Embeddable
data class CaregivingStateData(
    @Enumerated(EnumType.STRING)
    val progressingStatus: CaregivingProgressingStatus,
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "name", column = Column(name = "caregiver_name")),
        AttributeOverride(name = "sex", column = Column(name = "caregiver_sex")),
        AttributeOverride(name = "birthDate", column = Column(name = "caregiver_birth_date")),
        AttributeOverride(name = "phoneNumber", column = Column(name = "caregiver_phone_number")),
        AttributeOverride(name = "insured", column = Column(name = "caregiver_insured")),
    )
    val caregiverInfo: CaregiverInfo? = null,
    val startDateTime: LocalDateTime? = null,
    val endDateTime: LocalDateTime? = null,
    @Enumerated(EnumType.STRING)
    val closingReasonType: ClosingReasonType? = null,
    val detailClosingReason: String? = null,
    val canceledDateTime: LocalDateTime? = null,
)
