package kr.caredoc.careinsurance.security.personaldata

import com.github.guepardoapps.kulid.ULID
import kr.caredoc.careinsurance.reception.PatientPersonalDataRevealed
import kr.caredoc.careinsurance.reception.Reception
import kr.caredoc.careinsurance.security.accesscontrol.SubjectAttribute
import kr.caredoc.careinsurance.user.UserType
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronizationManager

@Service
class PersonalDataAccessLogService(
    private val personalDataAccessLogRepository: PersonalDataAccessLogRepository,
) {
    @Transactional
    @EventListener(PatientPersonalDataRevealed::class)
    fun logPatientPersonalDataAccess(event: PatientPersonalDataRevealed) {
        if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            throw IllegalStateException("couldn't log personal data access log under readonly transaction")
        }

        personalDataAccessLogRepository.save(
            PersonalDataAccessLog(
                id = ULID.random(),
                revealedEntityId = event.receptionId,
                revealingSubjectType = if (event.revealingSubject[SubjectAttribute.USER_TYPE].contains(UserType.SYSTEM)) {
                    PersonalDataAccessLog.RevealingSubjectType.SYSTEM
                } else {
                    PersonalDataAccessLog.RevealingSubjectType.USER
                },
                revealingSubjectId = event.revealingSubject[SubjectAttribute.USER_ID].firstOrNull(),
                revealingSubjectIp = event.revealingSubject[SubjectAttribute.CLIENT_IP].firstOrNull(),
                revealedData = when (event.revealedPersonalData) {
                    Reception.PersonalData.PATIENT_NAME -> PersonalDataAccessLog.RevealedPersonalData.PATIENT_NAME
                    Reception.PersonalData.PRIMARY_CONTACT -> PersonalDataAccessLog.RevealedPersonalData.PATIENT_PRIMARY_CONTACT
                    Reception.PersonalData.SECONDARY_CONTACT -> PersonalDataAccessLog.RevealedPersonalData.PATIENT_SECONDARY_CONTACT
                },
                revealedAt = event.revealedAt,
            )
        )
    }
}
