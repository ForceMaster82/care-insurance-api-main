package kr.caredoc.careinsurance.web.authentication.response

data class JwtResponse(
    val accessToken: String,
    val refreshToken: String,
)
