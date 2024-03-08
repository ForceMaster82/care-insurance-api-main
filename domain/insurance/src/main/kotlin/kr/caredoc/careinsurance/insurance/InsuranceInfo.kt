package kr.caredoc.careinsurance.insurance

import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Embeddable
import java.time.LocalDate

@Embeddable
data class InsuranceInfo(
    @Access(AccessType.FIELD)
    val insuranceNumber: String,
    val subscriptionDate: LocalDate,
    val coverageId: String,
    val caregivingLimitPeriod: Int,
)
