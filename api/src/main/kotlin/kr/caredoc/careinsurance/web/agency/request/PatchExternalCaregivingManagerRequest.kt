package kr.caredoc.careinsurance.web.agency.request

data class PatchExternalCaregivingManagerRequest(
    val id: String,
    val suspended: Boolean,
)
