package kr.caredoc.careinsurance.coverage

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface CoverageRepository : JpaRepository<Coverage, String> {
    fun findByNameContaining(nameKeyword: String, pageable: Pageable): Page<Coverage>
    fun existsByName(name: String): Boolean
    fun existsByRenewalTypeAndTargetSubscriptionYear(renewalType: RenewalType, targetSubscriptionYear: Int): Boolean
}
