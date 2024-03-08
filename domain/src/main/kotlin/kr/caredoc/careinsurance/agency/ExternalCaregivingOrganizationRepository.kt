package kr.caredoc.careinsurance.agency

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ExternalCaregivingOrganizationRepository : JpaRepository<ExternalCaregivingOrganization, String> {
    fun findByNameContains(name: String, pageable: Pageable): Page<ExternalCaregivingOrganization>

    fun findByExternalCaregivingOrganizationType(
        externalCaregivingOrganizationType: ExternalCaregivingOrganizationType,
        pageable: Pageable,
    ): Page<ExternalCaregivingOrganization>

    fun findByExternalCaregivingOrganizationTypeAndNameContains(
        externalCaregivingOrganizationType: ExternalCaregivingOrganizationType,
        name: String,
        pageable: Pageable,
    ): Page<ExternalCaregivingOrganization>

    fun findByIdIn(ids: Collection<String>): List<ExternalCaregivingOrganization>
}
