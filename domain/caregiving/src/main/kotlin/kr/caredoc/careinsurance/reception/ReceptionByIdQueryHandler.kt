package kr.caredoc.careinsurance.reception

interface ReceptionByIdQueryHandler {
    fun getReception(query: ReceptionByIdQuery): Reception
    fun <T> getReception(query: ReceptionByIdQuery, mapper: (Reception) -> T): T
    fun ensureReceptionExists(query: ReceptionByIdQuery)
}
