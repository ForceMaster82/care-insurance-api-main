package kr.caredoc.careinsurance.caregiving.progressmessage

import kr.caredoc.careinsurance.message.SendingStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface CaregivingProgressMessageSummaryRepository : JpaRepository<CaregivingProgressMessageSummary, String> {

    companion object {
        const val RECEPTION_RECEIVED_DATE_TIME_ORDERING = "receivedDateTime"
    }

    fun existsByCaregivingRoundId(caregivingRoundId: String): Boolean

    @Query(
        """
            SELECT summary
            FROM Reception reception
            JOIN CaregivingProgressMessageSummary summary on reception.id = summary.receptionId
            WHERE 
                reception.notifyCaregivingProgress = true
                AND summary.expectedSendingDate = :expectedSendingDate
                AND summary.caregivingProgressingStatus = "CAREGIVING_IN_PROGRESS"
        """
    )
    fun getCaregivingProgressMessageSummaryByDate(
        expectedSendingDate: LocalDate,
        pageable: Pageable,
    ): Page<CaregivingProgressMessageSummary>

    @Query(
        """
            SELECT summary
            FROM Reception reception
            JOIN CaregivingProgressMessageSummary summary on reception.id = summary.receptionId
            WHERE 
                reception.notifyCaregivingProgress = true
                AND summary.expectedSendingDate = :expectedSendingDate
                AND summary.sendingStatus = :sendingStatus
                AND summary.caregivingProgressingStatus = "CAREGIVING_IN_PROGRESS"
        """
    )
    fun getCaregivingProgressMessageSummaryByDateAndSendingStatus(
        expectedSendingDate: LocalDate,
        sendingStatus: SendingStatus,
        pageable: Pageable,
    ): Page<CaregivingProgressMessageSummary>

    @Query(
        """
            SELECT summary
            FROM Reception reception
            JOIN CaregivingProgressMessageSummary summary on reception.id = summary.receptionId
            WHERE 
                reception.notifyCaregivingProgress = true
                AND reception.accidentInfo.accidentNumber LIKE %:accidentNumberKeyword%
                AND summary.expectedSendingDate = :expectedSendingDate
                AND summary.caregivingProgressingStatus = "CAREGIVING_IN_PROGRESS"
        """
    )
    fun getCaregivingProgressMessageSummaryByDateAndAccidentNumber(
        expectedSendingDate: LocalDate,
        accidentNumberKeyword: String,
        pageable: Pageable,
    ): Page<CaregivingProgressMessageSummary>

    @Query(
        """
            SELECT summary
            FROM Reception reception
            JOIN CaregivingProgressMessageSummary summary on reception.id = summary.receptionId
            WHERE 
                reception.notifyCaregivingProgress = true
                AND reception.id in (select cr.receptionId from CaregivingRound cr where cr.caregivingStateData.caregiverInfo.name LIKE %:caregiverName%)
                AND summary.expectedSendingDate = :expectedSendingDate
                AND summary.caregivingProgressingStatus = "CAREGIVING_IN_PROGRESS"
        """
    )
    fun getCaregivingProgressCaregiverNameLike(
        expectedSendingDate: LocalDate,
        caregiverName: String,
        pageable: Pageable,
    ): Page<CaregivingProgressMessageSummary>

    @Query(
        """
            SELECT summary
            FROM Reception reception
            JOIN CaregivingProgressMessageSummary summary on reception.id = summary.receptionId
            WHERE 
                reception.notifyCaregivingProgress = true
                AND reception.accidentInfo.accidentNumber LIKE %:accidentNumberKeyword%
                AND summary.expectedSendingDate = :expectedSendingDate
                AND summary.sendingStatus = :sendingStatus
                AND summary.caregivingProgressingStatus = "CAREGIVING_IN_PROGRESS"
        """
    )
    fun getCaregivingProgressMessageSummaryByDateAndSendingStatusAndAccidentNumber(
        expectedSendingDate: LocalDate,
        accidentNumberKeyword: String,
        sendingStatus: SendingStatus,
        pageable: Pageable,
    ): Page<CaregivingProgressMessageSummary>

    @Query(
        """
            SELECT summary
            FROM Reception reception
            JOIN CaregivingProgressMessageSummary summary on reception.id = summary.receptionId
            WHERE 
                reception.notifyCaregivingProgress = true
                AND summary.caregivingRoundId in (select cr.id from CaregivingRound cr where cr.caregivingStateData.caregiverInfo.name LIKE %:caregiverName%)
                AND summary.expectedSendingDate = :expectedSendingDate
                AND summary.sendingStatus = :sendingStatus
                AND summary.caregivingProgressingStatus = "CAREGIVING_IN_PROGRESS"
        """
    )
    fun getCaregivingProgressSendingStatusCaregiverNameLike(
        expectedSendingDate: LocalDate,
        caregiverName: String,
        sendingStatus: SendingStatus,
        pageable: Pageable,
    ): Page<CaregivingProgressMessageSummary>

    @Query(
        """
            SELECT summary
            FROM Reception reception
            JOIN CaregivingProgressMessageSummary summary on reception.id = summary.receptionId
            WHERE 
                reception.notifyCaregivingProgress = true
                AND reception.patientInfo.name.hashed = :hashedPatientName
                AND summary.expectedSendingDate = :expectedSendingDate
                AND summary.caregivingProgressingStatus = "CAREGIVING_IN_PROGRESS"
        """
    )
    fun getCaregivingProgressMessageSummaryByDateAndHashedPatientName(
        expectedSendingDate: LocalDate,
        hashedPatientName: String,
        pageable: Pageable,
    ): Page<CaregivingProgressMessageSummary>

    @Query(
        """
            SELECT summary
            FROM Reception reception
            JOIN CaregivingProgressMessageSummary summary on reception.id = summary.receptionId
            WHERE 
                reception.notifyCaregivingProgress = true
                AND reception.patientInfo.name.hashed = :hashedPatientName
                AND summary.expectedSendingDate = :expectedSendingDate
                AND summary.sendingStatus = :sendingStatus
        """
    )
    fun getCaregivingProgressMessageSummaryByDateAndSendingStatusAndHashedPatientName(
        expectedSendingDate: LocalDate,
        hashedPatientName: String,
        sendingStatus: SendingStatus,
        pageable: Pageable,
    ): Page<CaregivingProgressMessageSummary>

    fun findByCaregivingRoundId(caregivingRoundId: String): List<CaregivingProgressMessageSummary>
}
