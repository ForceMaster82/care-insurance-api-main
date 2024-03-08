package kr.caredoc.careinsurance.user

import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, String> {
    fun findByCredentialEmailAddress(emailAddress: String): User?

    fun findByNameContains(nameKeyword: String): List<User>

    fun findByCredentialEmailAddressContains(emailKeyword: String): List<User>

    fun findByIdIn(ids: Collection<String>): List<User>

    fun existsByCredentialEmailAddress(emailAddress: String): Boolean

    fun findTopByCredentialEmailAddress(emailAddress: String): User?
}
