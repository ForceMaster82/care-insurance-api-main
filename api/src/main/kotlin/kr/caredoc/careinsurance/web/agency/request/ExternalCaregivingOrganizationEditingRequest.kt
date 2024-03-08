package kr.caredoc.careinsurance.web.agency.request

import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationType

data class ExternalCaregivingOrganizationEditingRequest(
    val name: String,
    val externalCaregivingOrganizationType: ExternalCaregivingOrganizationType,
    val address: String,
    val contractName: String,
    val phoneNumber: String,
    val profitAllocationRatio: Float,
    val accountInfo: AccountInfo,
) {
    data class AccountInfo(
        val bank: String?,
        val accountNumber: String?,
        val accountHolder: String?,
    )
}
