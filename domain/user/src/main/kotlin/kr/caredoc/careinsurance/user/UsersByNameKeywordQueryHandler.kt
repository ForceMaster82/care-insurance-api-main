package kr.caredoc.careinsurance.user

interface UsersByNameKeywordQueryHandler {
    fun getUsers(query: UsersByNameKeywordQuery): List<User>
}
