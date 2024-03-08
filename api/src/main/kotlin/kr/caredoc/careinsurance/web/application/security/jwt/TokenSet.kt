package kr.caredoc.careinsurance.web.application.security.jwt

data class TokenSet(
    val accessToken: String,
    val refreshToken: String,
)
