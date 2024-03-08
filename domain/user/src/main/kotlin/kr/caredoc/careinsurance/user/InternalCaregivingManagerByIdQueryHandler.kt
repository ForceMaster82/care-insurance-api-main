package kr.caredoc.careinsurance.user

interface InternalCaregivingManagerByIdQueryHandler {
    fun getInternalCaregivingManager(query: InternalCaregivingManagerByIdQuery): InternalCaregivingManager

    fun ensureInternalCaregivingManagerExists(query: InternalCaregivingManagerByIdQuery)
}
