package kr.caredoc.careinsurance.agency

interface ExternalCaregivingOrganizationsByIdsQueryHandler {
    fun getExternalCaregivingOrganizations(query: ExternalCaregivingOrganizationsByIdsQuery): List<ExternalCaregivingOrganization>
}
