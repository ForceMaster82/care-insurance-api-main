package kr.caredoc.careinsurance.web.application.security.jwt

import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.support.BeanDefinitionValidationException
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.Base64

@Configuration
class JwtConfig(
    @Value("\${jwt.secret}")
    private val jwtSecret: String,
    @Value("\${jwt.access-token-life-span}")
    private val accessTokenLifeSpan: Long,
    @Value("\${jwt.refresh-token-life-span}")
    private val refreshTokenLifeSpan: Long,
) {
    @Bean
    fun jwtManager(
        usedRefreshTokenRepository: UsedRefreshTokenRepository,
        jwtParser: JwtParser,
    ) = JwtManager(
        decodeJwtSecretKey(),
        accessTokenLifeSpan,
        refreshTokenLifeSpan,
        usedRefreshTokenRepository,
        jwtParser,
    )

    @Bean
    fun jwtParser() = Jwts.parserBuilder()
        .setSigningKey(decodeJwtSecretKey())
        .build() ?: throw BeanDefinitionValidationException("Jwt 파서를 초기화할 수 없습니다.")

    private fun decodeJwtSecretKey() = Keys.hmacShaKeyFor(
        Base64.getDecoder().decode(jwtSecret)
    )
}
