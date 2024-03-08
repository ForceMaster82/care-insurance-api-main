package kr.caredoc.careinsurance.user.exception

class AlreadyExistsUserEmailAddressException(val emailAddress: String) : RuntimeException("이미 사용 중인 $emailAddress 입니다.")
