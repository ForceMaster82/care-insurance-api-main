package kr.caredoc.careinsurance.agency

class ExternalCaregivingOrganizationNotFoundByIdException(val externalCaregivingOrganizationId: String) :
    RuntimeException("ExternalCaregivingOrganization($externalCaregivingOrganizationId)이 존재하지 않습니다.")
