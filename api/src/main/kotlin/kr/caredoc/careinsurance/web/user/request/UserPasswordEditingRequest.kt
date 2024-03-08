package kr.caredoc.careinsurance.web.user.request

data class UserPasswordEditingRequest(
    val password: String?,
    val currentPassword: String?,
)
