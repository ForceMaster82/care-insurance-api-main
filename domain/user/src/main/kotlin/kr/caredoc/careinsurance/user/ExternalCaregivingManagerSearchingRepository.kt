package kr.caredoc.careinsurance.user

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ExternalCaregivingManagerSearchingRepository {
    fun searchExternalCaregivingManagers(
        searchingCriteria: SearchingCriteria,
        pageable: Pageable,
    ): Page<ExternalCaregivingManager>

    data class SearchingCriteria(
        val name: String? = null,
        val email: String? = null,
        val externalCaregivingOrganizationId: String? = null,
    ) {
        fun isEmpty() = this.name.isNullOrBlank() && this.email.isNullOrBlank() && this.externalCaregivingOrganizationId.isNullOrBlank()
    }
}
