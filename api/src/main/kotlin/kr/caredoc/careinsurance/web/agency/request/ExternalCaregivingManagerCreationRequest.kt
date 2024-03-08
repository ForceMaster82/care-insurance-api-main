package kr.caredoc.careinsurance.web.agency.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class ExternalCaregivingManagerCreationRequest(
    val externalCaregivingOrganizationId: String,
    @NotBlank
    @Email
    val email: String,
    @NotBlank
    val name: String,
    @NotBlank
    val phoneNumber: String,
    val remarks: String? = null,
)
