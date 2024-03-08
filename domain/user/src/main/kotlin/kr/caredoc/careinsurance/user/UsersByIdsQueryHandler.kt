package kr.caredoc.careinsurance.user

interface UsersByIdsQueryHandler {
    fun getUsers(query: UsersByIdsQuery): List<User>
}
