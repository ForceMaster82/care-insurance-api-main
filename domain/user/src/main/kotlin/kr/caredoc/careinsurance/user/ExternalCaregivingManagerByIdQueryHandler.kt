package kr.caredoc.careinsurance.user

interface ExternalCaregivingManagerByIdQueryHandler {
    fun getExternalCaregivingManager(query: ExternalCaregivingManagerByIdQuery): ExternalCaregivingManager
}
