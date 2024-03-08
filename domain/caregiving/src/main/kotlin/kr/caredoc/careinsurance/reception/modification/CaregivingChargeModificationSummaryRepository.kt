package kr.caredoc.careinsurance.reception.modification

import org.springframework.data.jpa.repository.JpaRepository

interface CaregivingChargeModificationSummaryRepository : JpaRepository<CaregivingChargeModificationSummary, String> {
    fun findTopByReceptionId(receptionId: String): CaregivingChargeModificationSummary?
}
