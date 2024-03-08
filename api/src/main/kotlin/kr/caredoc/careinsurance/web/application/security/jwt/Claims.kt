package kr.caredoc.careinsurance.web.application.security.jwt

data class Claims(
    val subjectId: String,
    val credentialRevision: String,
    val internalCaregivingManagerId: String? = null,
    val externalCaregivingManagerIds: List<String> = listOf(),
)
