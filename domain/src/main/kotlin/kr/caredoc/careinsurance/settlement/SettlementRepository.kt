package kr.caredoc.careinsurance.settlement

import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository

interface SettlementRepository : JpaRepository<Settlement, String>, SettlementSearchingRepository {
    fun findByReceptionId(
        receptionId: String,
        sort: Sort,
    ): List<Settlement>

    fun findByReceptionId(receptionId: String): List<Settlement>

    fun findByIdIn(ids: Collection<String>): List<Settlement>

    fun findByCaregivingRoundId(caregivingRoundId: String): List<Settlement>

    fun findTopByCaregivingRoundId(caregivingRoundId: String): Settlement?
}
