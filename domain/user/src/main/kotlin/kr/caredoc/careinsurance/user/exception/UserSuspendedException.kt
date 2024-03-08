package kr.caredoc.careinsurance.user.exception

class UserSuspendedException(val userId: String) : RuntimeException("사용 안함 상태의 User($userId)가 활동을 수행했습니다.")
