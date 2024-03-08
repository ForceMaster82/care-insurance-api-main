package kr.caredoc.careinsurance.reception

import org.springframework.data.jpa.repository.JpaRepository

interface ReceptionRepository : JpaRepository<Reception, String>, ReceptionSearchingRepository {
    fun findByIdIn(ids: Collection<String>): List<Reception>
}
