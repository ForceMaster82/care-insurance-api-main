package kr.caredoc.careinsurance.caregiving

import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import kr.caredoc.careinsurance.reception.ReceptionProgressingStatus
import kr.caredoc.careinsurance.settlement.SettlementProgressingStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface CaregivingRoundSearchingRepository {
    fun searchCaregivingRounds(
        searchingCriteria: SearchingCriteria,
        pageable: Pageable,
    ): Page<CaregivingRound>

    fun searchCaregivingRounds(
        searchingCriteria: SearchingCriteria
    ): List<CaregivingRound>

    data class SearchingCriteria(
        val caregivingStartDateFrom: LocalDate?,
        val caregivingStartDateUntil: LocalDate?,
        val organizationId: String?,
        val expectedCaregivingStartDate: LocalDate?,
        val receptionProgressingStatuses: Collection<ReceptionProgressingStatus> = setOf(),
        val caregivingProgressingStatuses: Collection<CaregivingProgressingStatus> = setOf(),
        val settlementProgressingStatuses: Collection<SettlementProgressingStatus> = setOf(),
        val billingProgressingStatuses: Collection<BillingProgressingStatus> = setOf(),
        val accidentNumberContains: String?,
        val insuranceNumberContains: String? = null,
        val patientName: String? = null,
        val caregiverName: String? = null,
        val receptionReceivedDateFrom: LocalDate,
        val patientPhoneNumberContains: String? = null,
        val hospitalAndRoom: String? = null,
        val notifyCaregivingProgress: Boolean? = true
    )
}
