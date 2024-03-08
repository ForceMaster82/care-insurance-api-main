package kr.caredoc.careinsurance.web.agency.response

import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationType

data class SimpleExternalCaregivingOrganizationResponse(
    val id: String,
    val name: String,
    val externalCaregivingOrganizationType: ExternalCaregivingOrganizationType,
)
