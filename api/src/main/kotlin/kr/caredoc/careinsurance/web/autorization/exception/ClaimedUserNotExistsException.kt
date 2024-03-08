package kr.caredoc.careinsurance.web.autorization.exception

class ClaimedUserNotExistsException(val userId: String) : RuntimeException("인증 대상 유저($userId)가 존재하지 않습니다.")
