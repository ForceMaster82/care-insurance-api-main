package kr.caredoc.careinsurance.reception.modification

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface CaregivingChargeModificationHistoryRepository : JpaRepository<CaregivingChargeModificationHistory, String> {
    fun findByReceptionId(receptionId: String, pageable: Pageable): Page<CaregivingChargeModificationHistory>
}
