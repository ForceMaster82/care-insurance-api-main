package kr.caredoc.careinsurance.web.user.request

data class PatchInternalCaregivingManagersRequest(
    val id: String,
    val suspended: Boolean,
)
