package kr.caredoc.careinsurance.user

interface UserEditingCommandHandler {
    fun editUser(query: UserByIdQuery, command: UserEditingCommand)

    fun editUsers(commandsById: Map<String, UserEditingCommand>)
}
