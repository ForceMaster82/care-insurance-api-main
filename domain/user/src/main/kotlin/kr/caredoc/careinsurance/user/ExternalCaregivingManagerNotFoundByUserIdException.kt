package kr.caredoc.careinsurance.user

class ExternalCaregivingManagerNotFoundByUserIdException(userId: String) : RuntimeException("외부 제휴사(협회) 계정을 사용자 아이디($userId)로 특정할 수 없습니다.")
