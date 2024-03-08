package kr.caredoc.careinsurance.agency

interface BusinessLicenseSavingCommandHandler {
    fun saveBusinessLicenseFile(command: BusinessLicenseSavingCommand): BusinessLicenseSavingResult
}
