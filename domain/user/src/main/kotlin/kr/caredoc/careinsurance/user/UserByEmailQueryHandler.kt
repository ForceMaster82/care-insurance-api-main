package kr.caredoc.careinsurance.user

interface UserByEmailQueryHandler {
    fun getUser(query: UserByEmailQuery): User
}
