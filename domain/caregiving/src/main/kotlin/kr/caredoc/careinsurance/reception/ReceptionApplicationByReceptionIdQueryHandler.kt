package kr.caredoc.careinsurance.reception

interface ReceptionApplicationByReceptionIdQueryHandler {
    fun getReceptionApplication(query: ReceptionApplicationByReceptionIdQuery): ReceptionApplicationFileInfoResult
    fun deleteReceptionApplication(query: ReceptionApplicationByReceptionIdQuery)
}
