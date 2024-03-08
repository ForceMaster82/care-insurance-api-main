package kr.caredoc.careinsurance.billing.revision

import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import kr.caredoc.careinsurance.BaseEntity
import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import java.time.LocalDateTime

@Entity
class BillingRevision(
    id: String,
    @Access(AccessType.FIELD)
    val billingId: String,
    @Enumerated(EnumType.STRING)
    val billingProgressingStatus: BillingProgressingStatus,
    val billingAmount: Int,
    val totalDepositAmount: Int,
    val totalWithdrawalAmount: Int,
    val issuedDateTime: LocalDateTime,
) : BaseEntity(id)
