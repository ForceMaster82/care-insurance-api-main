package kr.caredoc.careinsurance.reception

import kr.caredoc.careinsurance.security.accesscontrol.Subject
import java.time.LocalDateTime

data class PatientPersonalDataRevealed(
    val receptionId: String,
    val revealedAt: LocalDateTime,
    val revealedPersonalData: Reception.PersonalData,
    val revealingSubject: Subject,
)
