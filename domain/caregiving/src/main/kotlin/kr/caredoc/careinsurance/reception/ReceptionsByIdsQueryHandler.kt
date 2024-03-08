package kr.caredoc.careinsurance.reception

interface ReceptionsByIdsQueryHandler {
    fun getReceptions(query: ReceptionsByIdsQuery): List<Reception>
}
