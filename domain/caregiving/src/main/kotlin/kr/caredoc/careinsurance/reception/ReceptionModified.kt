package kr.caredoc.careinsurance.reception

import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.insurance.InsuranceInfo
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.patient.EncryptedPatientInfo
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import java.time.LocalDate
import java.time.LocalDateTime

class ReceptionModified(
    val receptionId: String,
    val insuranceInfo: Modification<InsuranceInfo>,
    val accidentInfo: Modification<AccidentInfo>,
    val patientInfo: Modification<EncryptedPatientInfo>,
    val expectedCaregivingStartDate: Modification<LocalDate?>,
    val notifyCaregivingProgress: Modification<Boolean>,
    val progressingStatus: Modification<ReceptionProgressingStatus>,
    val caregivingManagerInfo: Modification<CaregivingManagerInfo?>,
    val periodType: Modification<Reception.PeriodType>,
    val applicationFileInfo: Modification<ReceptionApplicationFileInfo?>,
    val desiredCaregivingStartDate: Modification<LocalDate>,
    val receivedDateTime: LocalDateTime,
    val urgency: Reception.Urgency,
    val expectedCaregivingLimitDate: Modification<LocalDate>,
    val desiredCaregivingPeriod: Modification<Int?>,
    val additionalRequests: Modification<String>,
    val cause: Cause,
    val editingSubject: Subject,
) {
    enum class Cause {
        DIRECT_EDIT,
        SYSTEM,
        ETC,
    }

    val modifiedDateTime: LocalDateTime = Clock.now()
}
