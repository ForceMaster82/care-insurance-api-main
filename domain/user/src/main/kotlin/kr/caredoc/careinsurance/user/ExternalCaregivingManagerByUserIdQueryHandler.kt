package kr.caredoc.careinsurance.user

interface ExternalCaregivingManagerByUserIdQueryHandler {
    fun getExternalCaregivingManager(query: ExternalCaregivingManagerByUserIdQuery): ExternalCaregivingManager

    fun existExternalCaregivingManager(query: ExternalCaregivingManagerByUserIdQuery): Boolean
}
