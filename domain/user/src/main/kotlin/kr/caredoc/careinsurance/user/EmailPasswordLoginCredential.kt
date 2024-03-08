package kr.caredoc.careinsurance.user

data class EmailPasswordLoginCredential(
    val emailAddress: String,
    val password: String,
)
