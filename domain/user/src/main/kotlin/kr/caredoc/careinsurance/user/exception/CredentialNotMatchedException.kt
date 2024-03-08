package kr.caredoc.careinsurance.user.exception

class CredentialNotMatchedException(
    val userId: String
) : RuntimeException("User($userId)에 잘못된 크레덴셜로 로그인 시도가 발생했습니다.")
