package kr.caredoc.careinsurance.user

interface UserByIdQueryHandler {
    fun getUser(query: UserByIdQuery): User
}
