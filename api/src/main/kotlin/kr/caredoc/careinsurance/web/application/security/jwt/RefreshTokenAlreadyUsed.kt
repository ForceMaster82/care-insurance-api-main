package kr.caredoc.careinsurance.web.application.security.jwt

class RefreshTokenAlreadyUsed(val jti: String) : RuntimeException("Refresh token(jti: $jti)는 이미 사용이 완료되어 사용할 수 없습니다.")
