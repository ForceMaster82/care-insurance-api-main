package kr.caredoc.careinsurance.user

interface ExternalCaregivingManagerByIdsQueryHandler {
    fun getExternalCaregivingManagers(query: ExternalCaregivingManagerByIdsQuery): List<ExternalCaregivingManager>
}
