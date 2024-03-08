package kr.caredoc.careinsurance.user

interface UsersByEmailKeywordQueryHandler {
    fun getUsers(query: UsersByEmailKeywordQuery): List<User>
}
