package kr.caredoc.careinsurance.user

interface UserCreationCommandHandler {
    fun createUser(command: UserCreationCommand): UserCreationResult
}
