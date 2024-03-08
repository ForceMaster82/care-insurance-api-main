package kr.caredoc.careinsurance.settlement.revision

import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import kr.caredoc.careinsurance.BaseEntity
import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.settlement.SettlementProgressingStatus
import java.time.LocalDateTime

@Entity
class SettlementRevision(
    id: String,
    @Access(AccessType.FIELD)
    val settlementId: String,
    @Enumerated(EnumType.STRING)
    val progressingStatus: SettlementProgressingStatus,
    val totalAmount: Int,
    val totalDepositAmount: Int,
    val totalWithdrawalAmount: Int,
) : BaseEntity(id) {
    val issuedDateTime: LocalDateTime = Clock.now()
}
