package kr.caredoc.careinsurance.agency

interface ExternalCaregivingOrganizationByIdQueryHandler {
    fun getExternalCaregivingOrganization(query: ExternalCaregivingOrganizationByIdQuery): ExternalCaregivingOrganization

    fun ensureExternalCaregivingOrganizationExists(query: ExternalCaregivingOrganizationByIdQuery)
}
