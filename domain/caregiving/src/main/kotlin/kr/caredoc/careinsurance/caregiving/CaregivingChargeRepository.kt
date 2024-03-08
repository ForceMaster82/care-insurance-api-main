package kr.caredoc.careinsurance.caregiving

import org.springframework.data.jpa.repository.JpaRepository

interface CaregivingChargeRepository : JpaRepository<CaregivingCharge, String> {
    fun findByCaregivingRoundInfoCaregivingRoundId(caregivingRoundId: String): CaregivingCharge?
    fun findByCaregivingRoundInfoCaregivingRoundIdIn(caregivingRoundIds: Collection<String>): List<CaregivingCharge>
}
