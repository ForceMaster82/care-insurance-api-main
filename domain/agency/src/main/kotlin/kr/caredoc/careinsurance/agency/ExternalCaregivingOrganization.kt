package kr.caredoc.careinsurance.agency

import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import kr.caredoc.careinsurance.BaseEntity
import kr.caredoc.careinsurance.account.AccountInfo
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.ObjectAttribute

@Entity
class ExternalCaregivingOrganization(
    id: String,
    name: String,
    externalCaregivingOrganizationType: ExternalCaregivingOrganizationType,
    address: String,
    contractName: String,
    phoneNumber: String,
    profitAllocationRatio: Float,
    accountInfo: AccountInfo,
) : BaseEntity(id), Object {
    var businessLicenseFileName: String? = null
        protected set

    @Column(name = "business_license_url")
    var businessLicenseFileUrl: String? = null
        protected set
    var name = name
        protected set

    @Enumerated(EnumType.STRING)
    var externalCaregivingOrganizationType = externalCaregivingOrganizationType
        protected set
    var address = address
        protected set
    var contractName = contractName
        protected set
    var phoneNumber = phoneNumber
        protected set
    var profitAllocationRatio = profitAllocationRatio
        protected set

    @Embedded
    var accountInfo: AccountInfo? = accountInfo
        protected set

    fun updateBusinessLicenseInfo(data: SavedBusinessLicenseFileData) {
        this.businessLicenseFileName = data.businessLicenseFileName
        this.businessLicenseFileUrl = data.businessLicenseFileUrl
    }

    fun editMetaData(command: ExternalCaregivingOrganizationEditingCommand) {
        ExternalCaregivingOrganizationAccessPolicy.check(command.subject, command, this)

        this.name = command.name
        this.externalCaregivingOrganizationType = command.externalCaregivingOrganizationType
        this.address = command.address
        this.contractName = command.contractName
        this.phoneNumber = command.phoneNumber
        this.profitAllocationRatio = command.profitAllocationRatio
        this.accountInfo = command.accountInfo
    }

    override fun get(attribute: ObjectAttribute): Set<String> = when (attribute) {
        ObjectAttribute.ID -> setOf(id)
        else -> setOf()
    }
}
