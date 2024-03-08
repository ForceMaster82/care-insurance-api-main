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
class CaregivingChargeModificationHistory(
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
        ADDITIONAL_HOURS_CHARGE(
            type = DynamicType.NUMBER
        ),
        MEAL_COST(
            type = DynamicType.NUMBER
        ),
        TRANSPORTATION_FEE(
            type = DynamicType.NUMBER
        ),
        HOLIDAY_CHARGE(
            type = DynamicType.NUMBER
        ),
        CAREGIVER_INSURANCE_FEE(
            type = DynamicType.NUMBER
        ),
        COMMISSION_FEE(
            type = DynamicType.NUMBER
        ),
        VACATION_CHARGE(
            type = DynamicType.NUMBER
        ),
        PATIENT_CONDITION_CHARGE(
            type = DynamicType.NUMBER
        ),
        COVID_19_TESTING_COST(
            type = DynamicType.NUMBER
        ),
        ADDITIONAL_CHARGE_1(
            type = DynamicType.STRING
        ),
        ADDITIONAL_CHARGE_2(
            type = DynamicType.STRING
        ),
        ADDITIONAL_CHARGE_3(
            type = DynamicType.STRING
        ),
        OUTSTANDING_AMOUNT(
            type = DynamicType.NUMBER
        ),
        EXPECTED_SETTLEMENT_DATE(
            type = DynamicType.DATE,
        ),
        IS_CANCEL_AFTER_ARRIVED(
            type = DynamicType.BOOLEAN
        );
    }
}
