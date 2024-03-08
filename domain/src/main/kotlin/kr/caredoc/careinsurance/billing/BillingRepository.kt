package kr.caredoc.careinsurance.billing

import org.springframework.data.jpa.repository.JpaRepository

interface BillingRepository : JpaRepository<Billing, String>, BillingSearchingRepository {
    fun findTopByCaregivingRoundInfoCaregivingRoundId(caregivingRoundId: String): Billing?

    fun findByReceptionInfoReceptionIdAndBillingDateIsNotNull(receptionId: String): List<Billing>

    fun findByReceptionInfoReceptionId(receptionId: String): List<Billing>
}
