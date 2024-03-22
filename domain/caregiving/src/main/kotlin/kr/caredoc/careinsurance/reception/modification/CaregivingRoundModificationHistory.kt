package kr.caredoc.careinsurance.reception.modification

import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import kr.caredoc.careinsurance.AggregateRoot
import kr.caredoc.careinsurance.modification.DynamicType
import java.time.LocalDateTime

@Entity
class CaregivingRoundModificationHistory(
    id: String,
    @Access(AccessType.FIELD)
    val receptionId: String,
    val caregivingRoundNumber: Int,
    @Enumerated(EnumType.STRING)
    val modifiedProperty: ModifiedProperty,
    val previous: String?,
    val modified: String?,
    val modifierId: String,
    val modifiedDateTime: LocalDateTime,
) : AggregateRoot(id) {
    enum class ModifiedProperty(
        val type: DynamicType
    ) {
        CAREGIVER_ORGANIZATION_ID(type = DynamicType.STRING),
        CAREGIVER_NAME(type = DynamicType.STRING),
        CAREGIVER_SEX(type = DynamicType.STRING),
        CAREGIVER_BIRTH_DATE(type = DynamicType.STRING),
        CAREGIVER_PHONE_NUMBER(type = DynamicType.STRING),
        DAILY_CAREGIVING_CHARGE(type = DynamicType.NUMBER),
        COMMISSION_FEE(type = DynamicType.NUMBER),
        CAREGIVER_INSURED(type = DynamicType.BOOLEAN),
        CAREGIVER_ACCOUNT_BANK(type = DynamicType.STRING),
        CAREGIVER_ACCOUNT_HOLDER(type = DynamicType.STRING),
        CAREGIVER_ACCOUNT_NUMBER(type = DynamicType.STRING),
        START_DATE_TIME(type = DynamicType.DATETIME),
        END_DATE_TIME(type = DynamicType.DATETIME),
        REMARKS(type = DynamicType.STRING),
        EXPECTED_SETTLEMENT_DATE(type = DynamicType.DATE),
    }
}
