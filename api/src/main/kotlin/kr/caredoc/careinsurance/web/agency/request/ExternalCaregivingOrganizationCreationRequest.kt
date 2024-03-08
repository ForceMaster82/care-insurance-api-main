package kr.caredoc.careinsurance.web.agency.request

import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationType

data class ExternalCaregivingOrganizationCreationRequest(
    val name: String,
    val externalCaregivingOrganizationType: ExternalCaregivingOrganizationType,
    val address: String,
    val contractName: String,
    val phoneNumber: String,
    val profitAllocationRatio: Float,
    val accountInfo: AccountInfo,
    val businessLicenseFileName: String?,
    val businessLicenseUrl: String?,
) {
    data class AccountInfo(
        val bank: String?,
        val accountNumber: String?,
        val accountHolder: String?,
    )
}
