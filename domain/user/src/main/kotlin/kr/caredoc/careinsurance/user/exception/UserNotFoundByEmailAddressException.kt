package kr.caredoc.careinsurance.user.exception

class UserNotFoundByEmailAddressException(
    val enteredEmailAddress: String
) : RuntimeException("이메일 주소($enteredEmailAddress)를 보유한 사용자가 존재하지 않습니다.")
