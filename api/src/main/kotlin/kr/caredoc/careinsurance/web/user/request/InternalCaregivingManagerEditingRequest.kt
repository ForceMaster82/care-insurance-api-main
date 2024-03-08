package kr.caredoc.careinsurance.web.user.request

data class InternalCaregivingManagerEditingRequest(
    val email: String,
    val name: String,
    val nickname: String,
    val phoneNumber: String,
    val suspended: Boolean,
    val role: String,
    val remarks: String,
)
