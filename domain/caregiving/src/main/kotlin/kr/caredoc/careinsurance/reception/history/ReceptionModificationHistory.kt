package kr.caredoc.careinsurance.reception.history

import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import kr.caredoc.careinsurance.AggregateRoot
import kr.caredoc.careinsurance.modification.DynamicType
import java.time.LocalDateTime

@Entity
class ReceptionModificationHistory(
    id: String,
    @Access(AccessType.FIELD)
    val receptionId: String,
    @Enumerated(EnumType.STRING)
    val modifiedProperty: ModificationProperty,
    val previous: String?,
    val modified: String?,
    val modifierId: String,
    val modifiedDateTime: LocalDateTime,
) : AggregateRoot(id) {
    enum class ModificationProperty(
        val type: DynamicType
    ) {
        INSURANCE_NUMBER(
            type = DynamicType.STRING
        ),
        SUBSCRIPTION_DATE(
            type = DynamicType.DATE
        ),
        COVERAGE_ID(
            type = DynamicType.STRING
        ),
        CAREGIVING_LIMIT_PERIOD(
            type = DynamicType.NUMBER
        ),
        PATIENT_NAME(
            type = DynamicType.STRING
        ),
        PATIENT_AGE(
            type = DynamicType.NUMBER
        ),
        PATIENT_SEX(
            type = DynamicType.STRING
        ),
        PATIENT_PRIMARY_PHONE_NUMBER(
            type = DynamicType.STRING
        ),
        PATIENT_PRIMARY_RELATIONSHIP(
            type = DynamicType.STRING
        ),
        PATIENT_SECONDARY_PHONE_NUMBER(
            type = DynamicType.STRING
        ),
        PATIENT_SECONDARY_RELATIONSHIP(
            type = DynamicType.STRING
        ),
        ACCIDENT_NUMBER(
            type = DynamicType.STRING
        ),
        ACCIDENT_DATE_TIME(
            type = DynamicType.DATETIME
        ),
        CLAIM_TYPE(
            type = DynamicType.STRING
        ),
        CAREGIVING_ORGANIZATION_TYPE(
            type = DynamicType.STRING
        ),
        CAREGIVING_ORGANIZATION_ID(
            type = DynamicType.STRING
        ),
        CAREGIVING_MANAGING_USER_ID(
            type = DynamicType.STRING
        ),
        EXPECTED_CAREGIVING_LIMIT_DATE(
            type = DynamicType.DATE
        ),
        PATIENT_NICKNAME(
            type = DynamicType.STRING
        ),
        PATIENT_HEIGHT(
            type = DynamicType.NUMBER
        ),
        PATIENT_WEIGHT(
            type = DynamicType.NUMBER
        ),
        ADMISSION_DATE_TIME(
            type = DynamicType.DATETIME
        ),
        HOSPITAL_AND_ROOM(
            type = DynamicType.STRING
        ),
        HOSPITAL_CITY(
            type = DynamicType.STRING
        ),
        HOSPITAL_STATE(
            type = DynamicType.STRING
        ),
        DESIRED_CAREGIVING_START_DATE(
            type = DynamicType.DATE
        ),
        DESIRED_CAREGIVING_PERIOD(
            type = DynamicType.NUMBER
        ),
        PATIENT_DESCRIPTION(
            type = DynamicType.STRING
        ),
        ADDITIONAL_REQUESTS(
            type = DynamicType.STRING
        ),
        EXPECTED_CAREGIVING_START_DATE(
            type = DynamicType.DATE
        ),
        NOTIFY_CAREGIVING_PROGRESS(
            type = DynamicType.BOOLEAN
        ),
        RECEPTION_APPLICATION_FILE_NAME(
            type = DynamicType.STRING
        )
    }
}
