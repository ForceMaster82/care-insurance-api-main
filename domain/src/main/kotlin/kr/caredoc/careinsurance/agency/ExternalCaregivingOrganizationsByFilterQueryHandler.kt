package kr.caredoc.careinsurance.agency

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ExternalCaregivingOrganizationsByFilterQueryHandler {
    fun getExternalCaregivingOrganizations(
        query: ExternalCaregivingOrganizationsByFilterQuery,
        pageRequest: Pageable
    ): Page<ExternalCaregivingOrganization>
}
