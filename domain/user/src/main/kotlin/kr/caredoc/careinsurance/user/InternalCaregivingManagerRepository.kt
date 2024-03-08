package kr.caredoc.careinsurance.user

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface InternalCaregivingManagerRepository : JpaRepository<InternalCaregivingManager, String> {
    fun findByUserId(userId: String): InternalCaregivingManager?

    fun findByNameContains(name: String, pageable: Pageable): Page<InternalCaregivingManager>

    fun findByUserIdIn(userIds: Collection<String>, pageable: Pageable): Page<InternalCaregivingManager>

    fun existsByUserId(userId: String): Boolean
}
