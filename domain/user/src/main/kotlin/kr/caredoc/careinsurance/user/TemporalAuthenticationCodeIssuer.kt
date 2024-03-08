package kr.caredoc.careinsurance.user

interface TemporalAuthenticationCodeIssuer {
    fun issueTemporalAuthenticationCode(query: UserByIdQuery)
}
