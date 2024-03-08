package kr.caredoc.careinsurance.user

interface InternalCaregivingManagerByUserIdQueryHandler {
    fun getInternalCaregivingManager(query: InternalCaregivingManagerByUserIdQuery): InternalCaregivingManager

    fun existInternalCaregivingManager(query: InternalCaregivingManagerByUserIdQuery): Boolean
}
