package kr.caredoc.careinsurance.user

import org.springframework.data.jpa.repository.JpaRepository

interface ExternalCaregivingManagerRepository :
    JpaRepository<ExternalCaregivingManager, String>,
    ExternalCaregivingManagerSearchingRepository {
    fun findByEmail(email: String): ExternalCaregivingManager?
    fun findByUserId(userId: String): ExternalCaregivingManager?
    fun findByIdIn(ids: Collection<String>): List<ExternalCaregivingManager>
    fun existsByUserId(userId: String): Boolean
}
