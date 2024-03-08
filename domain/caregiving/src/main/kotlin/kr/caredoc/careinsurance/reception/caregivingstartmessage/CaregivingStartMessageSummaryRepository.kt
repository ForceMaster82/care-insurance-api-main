package kr.caredoc.careinsurance.reception.caregivingstartmessage

import kr.caredoc.careinsurance.message.SendingStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface CaregivingStartMessageSummaryRepository : JpaRepository<CaregivingStartMessageSummary, String> {
    companion object {
        const val RECEPTION_RECEIVED_DATE_TIME_ORDERING = "r.receivedDateTime"
    }

    @Query(
        """
            SELECT csms
            FROM CaregivingStartMessageSummary csms
            JOIN Reception r on r.id = csms.receptionId
            JOIN CaregivingRound cr on cr.id = csms.firstCaregivingRoundId
            WHERE csms.expectedSendingDate = :expectedSendingDate
                AND r.notifyCaregivingProgress = true
                AND cr.caregivingStateData.progressingStatus = "CAREGIVING_IN_PROGRESS"
        """
    )
    fun findByExpectedSendingDate(
        expectedSendingDate: LocalDate,
        pageable: Pageable,
    ): Page<CaregivingStartMessageSummary>

    @Query(
        """
            SELECT csms
            FROM CaregivingStartMessageSummary csms
            JOIN Reception r on r.id = csms.receptionId
            JOIN CaregivingRound cr on cr.id = csms.firstCaregivingRoundId
            WHERE csms.expectedSendingDate = :expectedSendingDate
                AND csms.sendingStatus = :sendingStatus
                AND r.notifyCaregivingProgress = true
                AND cr.caregivingStateData.progressingStatus = "CAREGIVING_IN_PROGRESS"
        """
    )
    fun findByExpectedSendingDateAndSendingStatus(
        expectedSendingDate: LocalDate,
        sendingStatus: SendingStatus,
        pageable: Pageable,
    ): Page<CaregivingStartMessageSummary>

    @Query(
        """
            SELECT csms
            FROM CaregivingStartMessageSummary csms
            JOIN Reception r on r.id = csms.receptionId
            JOIN CaregivingRound cr on cr.id = csms.firstCaregivingRoundId
            WHERE csms.expectedSendingDate = :expectedSendingDate
                AND r.accidentInfo.accidentNumber LIKE %:accidentNumberKeyword%
                AND r.notifyCaregivingProgress = true
                AND cr.caregivingStateData.progressingStatus = "CAREGIVING_IN_PROGRESS"
        """
    )
    fun searchByExpectedSendingDateAndAccidentNumberKeyword(
        expectedSendingDate: LocalDate,
        accidentNumberKeyword: String,
        pageable: Pageable,
    ): Page<CaregivingStartMessageSummary>

    @Query(
        """
            SELECT csms
            FROM CaregivingStartMessageSummary csms
            JOIN Reception r on r.id = csms.receptionId
            JOIN CaregivingRound cr on cr.id = csms.firstCaregivingRoundId
            WHERE csms.expectedSendingDate = :expectedSendingDate
                AND r.patientInfo.name.hashed = :hashedPatientName
                AND r.notifyCaregivingProgress = true
                AND cr.caregivingStateData.progressingStatus = "CAREGIVING_IN_PROGRESS"
        """
    )
    fun searchByExpectedSendingDateAndHashedPatientName(
        expectedSendingDate: LocalDate,
        hashedPatientName: String,
        pageable: Pageable,
    ): Page<CaregivingStartMessageSummary>

    @Query(
        """
            SELECT csms
            FROM CaregivingStartMessageSummary csms
            JOIN Reception r on r.id = csms.receptionId
            JOIN CaregivingRound cr on cr.id = csms.firstCaregivingRoundId
            WHERE csms.expectedSendingDate = :expectedSendingDate
                AND csms.sendingStatus = :sendingStatus
                AND r.accidentInfo.accidentNumber LIKE %:accidentNumberKeyword%
                AND r.notifyCaregivingProgress = true
                AND cr.caregivingStateData.progressingStatus = "CAREGIVING_IN_PROGRESS"
        """
    )
    fun searchByExpectedSendingDateAndSendingStatusAndAccidentNumberKeyword(
        expectedSendingDate: LocalDate,
        sendingStatus: SendingStatus,
        accidentNumberKeyword: String,
        pageable: Pageable,
    ): Page<CaregivingStartMessageSummary>

    @Query(
        """
            SELECT csms
            FROM CaregivingStartMessageSummary csms
            JOIN Reception r on r.id = csms.receptionId
            JOIN CaregivingRound cr on cr.id = csms.firstCaregivingRoundId
            WHERE csms.expectedSendingDate = :expectedSendingDate
                AND csms.sendingStatus = :sendingStatus
                AND r.patientInfo.name.hashed = :hashedPatientName
                AND r.notifyCaregivingProgress = true
                AND cr.caregivingStateData.progressingStatus = "CAREGIVING_IN_PROGRESS"
        """
    )
    fun searchByExpectedSendingDateAndSendingStatusAndHashedPatientName(
        expectedSendingDate: LocalDate,
        sendingStatus: SendingStatus,
        hashedPatientName: String,
        pageable: Pageable,
    ): Page<CaregivingStartMessageSummary>

    fun findByReceptionId(receptionId: String): List<CaregivingStartMessageSummary>
}
