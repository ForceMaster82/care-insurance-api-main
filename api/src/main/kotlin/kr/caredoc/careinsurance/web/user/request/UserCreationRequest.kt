package kr.caredoc.careinsurance.web.user.request

data class UserCreationRequest(
    val emailAddress: String,
    val password: String,
)
