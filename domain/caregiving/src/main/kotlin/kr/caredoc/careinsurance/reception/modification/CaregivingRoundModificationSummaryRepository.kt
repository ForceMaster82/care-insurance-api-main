package kr.caredoc.careinsurance.reception.modification

import org.springframework.data.jpa.repository.JpaRepository

interface CaregivingRoundModificationSummaryRepository : JpaRepository<CaregivingRoundModificationSummary, String> {
    fun findTopByReceptionId(receptionId: String): CaregivingRoundModificationSummary?
}
