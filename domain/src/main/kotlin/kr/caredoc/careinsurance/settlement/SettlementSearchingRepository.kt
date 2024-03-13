package kr.caredoc.careinsurance.settlement

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

interface SettlementSearchingRepository {
    fun searchSettlements(
        searchingCriteria: SearchingCriteria,
        pageable: Pageable,
    ): Page<Settlement>

    fun searchSettlements(
        searchingCriteria: SearchingCriteria,
        sort: Sort,
    ): List<Settlement>

    data class SearchingCriteria(
        val progressingStatus: SettlementProgressingStatus,
        val accidentNumber: String? = null,
        val patientName: String? = null,
        val organizationName: String? = null,
        val expectedSettlementDate: DateRange? = null,
        val lastTransactionDate: DateRange? = null,
        val internalCaregivingOrganizationAssigned: Boolean? = null,
        val caregiverName: String? = null,
    )
}
