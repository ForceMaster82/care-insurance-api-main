package kr.caredoc.careinsurance.user

interface UserPasswordResetCommandHandler {
    fun resetPassword(query: UserByIdQuery, command: UserPasswordResetCommand)
}
