package kr.caredoc.careinsurance.reconciliation

import kr.caredoc.careinsurance.reconciliation.statistics.ReconciliationFinancialSummations
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface ReconciliationRepository : JpaRepository<Reconciliation, String> {
    @Query(
        """
            SELECT r
            FROM Reconciliation r
            JOIN Reception re ON re.id = r.receptionId
            JOIN CaregivingRound cr on cr.id = r.caregivingRoundId
            WHERE
                r.issuedDate BETWEEN :from AND :until
                AND r.closingStatus = :closingStatus
            ORDER BY re.patientInfo.name.masked ASC, re.accidentInfo.accidentNumber DESC, cr.caregivingRoundNumber DESC
        """
    )
    fun findByIssuedDateBetweenAndClosingStatus(
        from: LocalDate,
        until: LocalDate,
        closingStatus: ClosingStatus,
        pageable: Pageable,
    ): Page<Reconciliation>

    @Query(
        """
            SELECT r
            FROM Reconciliation r
            JOIN Reception re ON re.id = r.receptionId
            JOIN CaregivingRound cr on cr.id = r.caregivingRoundId
            WHERE
                r.issuedDate BETWEEN :from AND :until
                AND r.closingStatus = :closingStatus
            ORDER BY re.patientInfo.name.masked ASC, re.accidentInfo.accidentNumber DESC, cr.caregivingRoundNumber DESC
        """
    )
    fun findByIssuedDateBetweenAndClosingStatus(
        from: LocalDate,
        until: LocalDate,
        closingStatus: ClosingStatus,
    ): List<Reconciliation>

    @Query(
        """
            SELECT r
            FROM Reconciliation r
            JOIN Reception re ON re.id = r.receptionId
            JOIN CaregivingRound cr on cr.id = r.caregivingRoundId
            WHERE
                r.issuedDate BETWEEN :from AND :until
                AND r.closingStatus = :closingStatus
                AND re.patientInfo.name.hashed = :hashedPatientName
            ORDER BY re.patientInfo.name.masked ASC, re.accidentInfo.accidentNumber DESC, cr.caregivingRoundNumber DESC
        """
    )
    fun findByIssuedDateBetweenAndClosingStatusAndHashedPatientName(
        from: LocalDate,
        until: LocalDate,
        closingStatus: ClosingStatus,
        hashedPatientName: String,
        pageable: Pageable,
    ): Page<Reconciliation>

    @Query(
        """
            SELECT r
            FROM Reconciliation r
            JOIN Reception re ON re.id = r.receptionId
            JOIN CaregivingRound cr on cr.id = r.caregivingRoundId
            WHERE
                r.issuedDate BETWEEN :from AND :until
                AND r.closingStatus = :closingStatus
                AND re.patientInfo.name.hashed = :hashedPatientName
            ORDER BY re.patientInfo.name.masked ASC, re.accidentInfo.accidentNumber DESC, cr.caregivingRoundNumber DESC
        """
    )
    fun findByIssuedDateBetweenAndClosingStatusAndHashedPatientName(
        from: LocalDate,
        until: LocalDate,
        closingStatus: ClosingStatus,
        hashedPatientName: String,
    ): List<Reconciliation>

    @Query(
        """
            SELECT r
            FROM Reconciliation r
            JOIN Reception re ON re.id = r.receptionId
            JOIN CaregivingRound cr on cr.id = r.caregivingRoundId
            WHERE
                r.issuedDate BETWEEN :from AND :until
                AND r.closingStatus = :closingStatus
                AND re.accidentInfo.accidentNumber LIKE %:accidentNumber%
            ORDER BY re.patientInfo.name.masked ASC, re.accidentInfo.accidentNumber DESC, cr.caregivingRoundNumber DESC
        """
    )
    fun findByIssuedDateBetweenAndClosingStatusAndAccidentNumberLike(
        from: LocalDate,
        until: LocalDate,
        closingStatus: ClosingStatus,
        accidentNumber: String,
        pageable: Pageable,
    ): Page<Reconciliation>

    @Query(
        """
            SELECT r
            FROM Reconciliation r
            JOIN Reception re ON re.id = r.receptionId
            JOIN CaregivingRound cr on cr.id = r.caregivingRoundId
            WHERE
                r.issuedDate BETWEEN :from AND :until
                AND r.closingStatus = :closingStatus
                AND re.accidentInfo.accidentNumber LIKE %:accidentNumber%
            ORDER BY re.patientInfo.name.masked ASC, re.accidentInfo.accidentNumber DESC, cr.caregivingRoundNumber DESC
        """
    )
    fun findByIssuedDateBetweenAndClosingStatusAndAccidentNumberLike(
        from: LocalDate,
        until: LocalDate,
        closingStatus: ClosingStatus,
        accidentNumber: String,
    ): List<Reconciliation>

    fun findByReconciledYearAndReconciledMonthAndClosingStatusOrderByIdDesc(
        reconciledYear: Int,
        reconciledMonth: Int,
        closingStatus: ClosingStatus,
        pageable: Pageable,
    ): Page<Reconciliation>

    fun findByReconciledYearAndReconciledMonthAndClosingStatusOrderByIdDesc(
        reconciledYear: Int,
        reconciledMonth: Int,
        closingStatus: ClosingStatus,
    ): List<Reconciliation>

    fun findByIdIn(ids: Collection<String>): List<Reconciliation>

    @Query(
        """
            SELECT COUNT(DISTINCT r.receptionId)
            FROM Reconciliation r
            WHERE
                r.reconciledYear = :year
                AND r.reconciledMonth = :month
                AND r.closingStatus = :closingStatus
        """
    )
    fun countDistinctReceptionIdByReconciledYearAndReconciledMonthAndClosingStatus(
        year: Int,
        month: Int,
        closingStatus: ClosingStatus,
    ): Int

    @Query(
        """
            SELECT DISTINCT COUNT(r)
            FROM Reconciliation r
            WHERE r.id IN (
                SELECT MAX(r2.id)
                FROM Reconciliation r2
                WHERE
                    r2.reconciledYear = :year
                    AND r2.reconciledMonth = :month
                    AND r2.closingStatus = :closingStatus
                GROUP BY r2.receptionId, r2.caregiverPhoneNumberWhenIssued
            )
        """
    )
    fun countDistinctReceptionCaregiverPhoneNumberCountByReconciledYearAndReconciledMonthAndClosingStatus(
        year: Int,
        month: Int,
        closingStatus: ClosingStatus,
    ): Int

    @Query(
        """
            SELECT SUM(r.actualCaregivingSecondsWhenIssued)
            FROM Reconciliation r
            WHERE
                r.reconciledYear = :year
                AND r.reconciledMonth = :month
                AND r.closingStatus = :closingStatus
                AND r.closedDateTime = (
                    SELECT MAX(r2.closedDateTime)
                    FROM Reconciliation r2
                    WHERE r.caregivingRoundId = r2.caregivingRoundId
                )
        """
    )
    fun sumLatestReconciliationCaregivingSecondsByReconciledYearAndReconciledMonthAndClosingStatus(
        year: Int,
        month: Int,
        closingStatus: ClosingStatus,
    ): Long

    @Query(
        """
            SELECT
                new kr.caredoc.careinsurance.reconciliation.statistics.ReconciliationFinancialSummations(
                    SUM(r.billingAmount),
                    SUM(r.settlementAmount),
                    SUM(r.profit),
                    SUM(r.distributedProfit)
                )
            FROM Reconciliation r
            WHERE
                r.reconciledYear = :year
                AND r.reconciledMonth = :month
                AND r.closingStatus = :closingStatus
        """
    )
    fun sumReconciliationFinancialPropertiesByReconciledYearAndReconciledMonthAndClosingStatus(
        year: Int,
        month: Int,
        closingStatus: ClosingStatus,
    ): ReconciliationFinancialSummations
}
