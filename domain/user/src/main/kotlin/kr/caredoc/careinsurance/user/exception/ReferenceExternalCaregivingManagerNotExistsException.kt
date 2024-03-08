package kr.caredoc.careinsurance.user.exception

class ReferenceExternalCaregivingManagerNotExistsException(val enteredExternalCaregivingManagerId: String) :
    RuntimeException("참조하는데 존재하지 않는 외부 제휴사(협회) 계정 $enteredExternalCaregivingManagerId 입니다.")
