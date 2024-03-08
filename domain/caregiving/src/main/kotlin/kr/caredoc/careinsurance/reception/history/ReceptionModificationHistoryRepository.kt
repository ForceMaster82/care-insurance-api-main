package kr.caredoc.careinsurance.reception.history

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ReceptionModificationHistoryRepository : JpaRepository<ReceptionModificationHistory, String> {

    fun findByReceptionId(receptionId: String, pageable: Pageable): Page<ReceptionModificationHistory>
}
