package kr.caredoc.careinsurance.user.exception

class UserNotFoundByIdException(val userId: String) : RuntimeException("User($userId)는 존재하지 않습니다.")
