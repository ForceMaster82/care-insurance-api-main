package kr.caredoc.careinsurance.reception.caregivingsatisfactionsurvey

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface CaregivingSatisfactionSurveyStatusRepository : JpaRepository<CaregivingSatisfactionSurveyStatus, String> {
    @Query(
        """
            SELECT csss
            FROM CaregivingSatisfactionSurveyStatus csss
            JOIN Reception r on r.id = csss.receptionId
            WHERE r.notifyCaregivingProgress = true
                AND csss.expectedSendingDate = :expectedSendingDate
        """
    )
    fun findByExpectedSendingDate(
        expectedSendingDate: LocalDate,
        pageable: Pageable,
    ): Page<CaregivingSatisfactionSurveyStatus>

    @Query(
        """
            SELECT csss
            FROM CaregivingSatisfactionSurveyStatus csss
            JOIN Reception r on r.id = csss.receptionId
            WHERE r.notifyCaregivingProgress = true
                AND csss.expectedSendingDate = :expectedSendingDate
                AND r.patientInfo.name.hashed = :hashedPatientName
        """
    )
    fun findByExpectedSendingDateAndHashedPatientName(
        expectedSendingDate: LocalDate,
        hashedPatientName: String,
        pageable: Pageable,
    ): Page<CaregivingSatisfactionSurveyStatus>

    @Query(
        """
            SELECT csss
            FROM CaregivingSatisfactionSurveyStatus csss
            JOIN Reception r on r.id = csss.receptionId
            WHERE r.notifyCaregivingProgress = true
                AND csss.expectedSendingDate = :expectedSendingDate
                AND r.accidentInfo.accidentNumber LIKE %:accidentNumberKeyword%
        """
    )
    fun findByExpectedSendingDateAndAccidentNumberContaining(
        expectedSendingDate: LocalDate,
        accidentNumberKeyword: String,
        pageable: Pageable,
    ): Page<CaregivingSatisfactionSurveyStatus>

    @Query(
        """
            SELECT csss
            FROM CaregivingSatisfactionSurveyStatus csss
            JOIN Reception r on r.id = csss.receptionId
            WHERE r.notifyCaregivingProgress = true
                AND csss.expectedSendingDate = :expectedSendingDate
                AND csss.caregivingRoundId in (select cr.id from CaregivingRound cr where cr.caregivingStateData.caregiverInfo.name LIKE %:caregiverName%)
        """
    )
    fun findByExpectedCaregiverNameLike(
        expectedSendingDate: LocalDate,
        caregiverName: String,
        pageable: Pageable,
    ): Page<CaregivingSatisfactionSurveyStatus>

    fun existsByReceptionId(receptionId: String): Boolean

    fun findByReceptionId(receptionId: String): List<CaregivingSatisfactionSurveyStatus>

    fun findByReceptionIdIn(receptionIds: Collection<String>): List<CaregivingSatisfactionSurveyStatus>
}
