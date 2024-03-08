package kr.caredoc.careinsurance.web.authentication.request

import jakarta.validation.constraints.Pattern

data class AuthenticationRequest(
    @field:Pattern(
        regexp = """^[\w-.]+@([\w-]+\.)+[\w-]{2,4}$""",
        message = "아이디는 영문 대/소문자, 숫자, 특수문자를 조합하여 입력해야 합니다.",
    )
    val email: String?,
    val password: String?,
    val authenticationCode: String?,
    val refreshToken: String?,
)
