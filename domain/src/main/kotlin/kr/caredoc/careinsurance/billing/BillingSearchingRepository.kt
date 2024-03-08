package kr.caredoc.careinsurance.billing

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface BillingSearchingRepository {
    fun searchBillings(
        searchingCriteria: SearchingCriteria,
        pageable: Pageable,
    ): Page<Billing>

    data class SearchingCriteria(
        val progressingStatus: Set<BillingProgressingStatus>,
        val accidentNumber: String? = null,
        val patientName: String? = null,
        val usedPeriodFrom: LocalDate?,
        val usedPeriodUntil: LocalDate?,
        val billingDateFrom: LocalDate?,
        val billingDateUntil: LocalDate?,
        val transactionDateFrom: LocalDate?,
        val transactionDateUntil: LocalDate?,
    )
}
