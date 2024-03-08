package kr.caredoc.careinsurance.security.personaldata

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import kr.caredoc.careinsurance.AggregateRoot
import java.time.LocalDateTime

@Entity
class PersonalDataAccessLog(
    id: String,
    val revealedEntityId: String, // 현재는 항상 Reception의 id를 가리킵니다.
    @Enumerated(EnumType.STRING)
    val revealingSubjectType: RevealingSubjectType?,
    val revealingSubjectId: String?,
    val revealingSubjectIp: String?,
    @Enumerated(EnumType.STRING)
    val revealedData: RevealedPersonalData,
    val revealedAt: LocalDateTime,
) : AggregateRoot(id) {
    enum class RevealingSubjectType {
        USER,
        SYSTEM,
    }

    enum class RevealedPersonalData {
        PATIENT_NAME,
        PATIENT_PRIMARY_CONTACT,
        PATIENT_SECONDARY_CONTACT,
    }
}
