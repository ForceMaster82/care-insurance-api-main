package kr.caredoc.careinsurance.caregiving

import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import kr.caredoc.careinsurance.account.AccountInfo
import kr.caredoc.careinsurance.patient.Sex
import java.time.LocalDate

@Embeddable
data class CaregiverInfo(
    @Access(AccessType.FIELD)
    val caregiverOrganizationId: String? = null,
    val name: String,
    @Enumerated(EnumType.STRING)
    val sex: Sex,
    val birthDate: String? = null,
    val phoneNumber: String,
    val dailyCaregivingCharge: Int = 0,
    val commissionFee: Int = 0,
    val insured: Boolean = false,
    val accountInfo: AccountInfo,
)
