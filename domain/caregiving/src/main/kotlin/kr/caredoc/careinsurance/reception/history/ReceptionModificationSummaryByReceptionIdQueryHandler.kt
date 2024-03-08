package kr.caredoc.careinsurance.reception.history

interface ReceptionModificationSummaryByReceptionIdQueryHandler {
    fun getReceptionModificationSummary(query: ReceptionModificationSummaryByReceptionIdQuery): ReceptionModificationSummary
}
