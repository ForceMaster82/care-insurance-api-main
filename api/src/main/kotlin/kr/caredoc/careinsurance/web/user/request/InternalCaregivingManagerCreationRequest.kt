package kr.caredoc.careinsurance.web.user.request

data class InternalCaregivingManagerCreationRequest(
    val email: String,
    val name: String,
    val nickname: String,
    val phoneNumber: String,
    val role: String,
    val remarks: String?,
)
