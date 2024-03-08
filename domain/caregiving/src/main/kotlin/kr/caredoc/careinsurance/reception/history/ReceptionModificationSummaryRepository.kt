package kr.caredoc.careinsurance.reception.history

import org.springframework.data.jpa.repository.JpaRepository

interface ReceptionModificationSummaryRepository : JpaRepository<ReceptionModificationSummary, String> {

    fun findTopByReceptionId(receptionId: String): ReceptionModificationSummary?
}
