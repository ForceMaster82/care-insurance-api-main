package kr.caredoc.careinsurance.user

class InternalCaregivingManagerNotFoundByUserIdException(userId: String) :
    RuntimeException("내부 간병 관리자를 사용자 아이디($userId)로 특정할 수 없습니다.")
