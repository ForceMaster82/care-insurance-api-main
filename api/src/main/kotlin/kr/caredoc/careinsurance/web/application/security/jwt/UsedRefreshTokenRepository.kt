package kr.caredoc.careinsurance.web.application.security.jwt

import kr.caredoc.careinsurance.Clock
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class UsedRefreshTokenRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun existsByJti(jti: String): Boolean {
        return try {
            jdbcTemplate.queryForObject(
                """
                    SELECT 1
                    FROM used_refresh_token
                    WHERE jti = ?
                """.trimIndent(),
                Boolean::class.java,
                jti,
            )

            true
        } catch (e: EmptyResultDataAccessException) {
            false
        }
    }

    fun saveAsUsed(jti: String, issuedAt: LocalDateTime) {
        val now = Clock.now()
        jdbcTemplate.update(
            """
                INSERT INTO used_refresh_token(jti, issued_at, used_at)
                VALUES (?, ?, ?)
            """.trimIndent(),
            jti,
            issuedAt,
            now,
        )
    }
}
