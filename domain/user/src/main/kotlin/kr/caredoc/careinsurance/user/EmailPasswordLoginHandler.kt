package kr.caredoc.careinsurance.user

interface EmailPasswordLoginHandler {
    fun handleLogin(credential: EmailPasswordLoginCredential): User
}
