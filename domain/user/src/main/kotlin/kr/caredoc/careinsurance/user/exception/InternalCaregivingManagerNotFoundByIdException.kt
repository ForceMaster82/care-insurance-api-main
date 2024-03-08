package kr.caredoc.careinsurance.user.exception

class InternalCaregivingManagerNotFoundByIdException(val internalCaregivingManagerId: String) :
    RuntimeException("InternalCaregivingManager($internalCaregivingManagerId)는 존재하지 않습니다.")
