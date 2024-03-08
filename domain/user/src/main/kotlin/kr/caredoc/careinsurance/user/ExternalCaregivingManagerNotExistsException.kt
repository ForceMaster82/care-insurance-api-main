package kr.caredoc.careinsurance.user

class ExternalCaregivingManagerNotExistsException(val externalCaregivingManagerId: String) :
    RuntimeException("external caregiving manager ${externalCaregivingManagerId}가 존재하지 않습니다.")
