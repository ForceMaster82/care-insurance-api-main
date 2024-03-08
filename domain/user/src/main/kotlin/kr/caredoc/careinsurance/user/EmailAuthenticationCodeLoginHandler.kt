package kr.caredoc.careinsurance.user

interface EmailAuthenticationCodeLoginHandler {
    fun handleLogin(credential: EmailAuthenticationCodeLoginCredential): User
}
