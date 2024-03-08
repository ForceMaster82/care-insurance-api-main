package kr.caredoc.careinsurance.user

data class EmailAuthenticationCodeLoginCredential(
    val emailAddress: String,
    val authenticationCode: String,
)
