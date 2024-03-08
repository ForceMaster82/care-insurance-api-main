package kr.caredoc.careinsurance.web.agency.response

import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationType

data class DetailExternalCaregivingOrganizationResponse(
    val id: String,
    val name: String,
    val externalCaregivingOrganizationType: ExternalCaregivingOrganizationType,
    val address: String,
    val contractName: String,
    val phoneNumber: String,
    val profitAllocationRatio: Float,
    val businessLicenseFileName: String?,
    val businessLicenseFileUrl: String?,
    val accountInfo: AccountInfo,
) {
    data class AccountInfo(
        val bank: String?,
        val accountNumber: String?,
        val accountHolder: String?,
    )
}
