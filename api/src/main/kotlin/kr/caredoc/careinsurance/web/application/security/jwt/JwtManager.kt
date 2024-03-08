package kr.caredoc.careinsurance.web.application.security.jwt

import com.github.guepardoapps.kulid.ULID
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import kr.caredoc.careinsurance.security.authentication.AuthenticationMethod
import kr.caredoc.careinsurance.web.autorization.exception.IllegalTokenException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional
import java.security.Key
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

open class JwtManager(
    protected open val key: Key,
    protected open val accessTokenLifeSpan: Long,
    protected open val refreshTokenLifeSpan: Long,
    protected open val usedRefreshTokenRepository: UsedRefreshTokenRepository,
    protected open val jwtParser: JwtParser,
) {
    fun issueTokens(claims: Claims, authenticationMethod: AuthenticationMethod) = Date().let { issuedAt ->
        TokenSet(
            accessToken = issueAccessToken(claims, issuedAt, authenticationMethod),
            refreshToken = issueRefreshToken(claims, issuedAt, authenticationMethod),
        )
    }

    @Transactional
    open fun issueTokens(claims: Claims, refreshToken: String): TokenSet {
        ensureRefreshTokenNotUsed(refreshToken)
        val refreshTokenClaims = jwtParser.parseClaimsJws(refreshToken).body
        val rootAuthenticationMethod = refreshTokenClaims["authenticationMethod"]?.let {
            AuthenticationMethod.parse(it.toString())
        } ?: throw IllegalTokenException("인증 방법을 확인할 수 없습니다.")
        val jti = refreshTokenClaims["jti"].toString()

        try {
            usedRefreshTokenRepository.saveAsUsed(
                jti,
                LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(refreshTokenClaims["iat"].toString().toLong()),
                    ZoneId.of("Asia/Seoul"),
                )
            )
        } catch (e: DataIntegrityViolationException) {
            throw RefreshTokenAlreadyUsed(jti)
        }

        return issueTokens(claims, rootAuthenticationMethod)
    }

    @Transactional
    open fun ensureRefreshTokenNotUsed(refreshToken: String) {
        val refreshTokenClaims = jwtParser.parseClaimsJws(refreshToken).body
        val jti = refreshTokenClaims["jti"].toString()
        val used = usedRefreshTokenRepository.existsByJti(
            jti,
        )

        if (used) {
            throw RefreshTokenAlreadyUsed(jti)
        }
    }

    private fun issueAccessToken(
        claims: Claims,
        issuedAt: Date,
        authenticationMethod: AuthenticationMethod,
    ) = generateJwt(
        subjectId = claims.subjectId,
        claims = mapOf(
            "tokenType" to "access",
            "internalCaregivingManagerId" to claims.internalCaregivingManagerId,
            "externalCaregivingManagerIds" to claims.externalCaregivingManagerIds,
            "credentialRevision" to claims.credentialRevision,
            "authenticationMethod" to authenticationMethod,
        ).mapNotNull { (k, v) -> v?.let { k to v } }.toMap(),
        lifespan = accessTokenLifeSpan,
        issuedAt = issuedAt,
    )

    private fun issueRefreshToken(
        claims: Claims,
        issuedAt: Date,
        authenticationMethod: AuthenticationMethod,
    ) = generateJwt(
        subjectId = claims.subjectId,
        claims = mapOf(
            "tokenType" to "refresh",
            "credentialRevision" to claims.credentialRevision,
            "authenticationMethod" to authenticationMethod,
        ),
        lifespan = refreshTokenLifeSpan,
        issuedAt = issuedAt,
    )

    private fun generateJwt(
        subjectId: String,
        claims: Map<String, Any>,
        lifespan: Long,
        issuedAt: Date,
    ) = Jwts.builder()
        .setClaims(claims)
        .claim("jti", ULID.random())
        .setIssuedAt(issuedAt)
        .setSubject(subjectId)
        .setExpiration(Date(issuedAt.time + lifespan))
        .signWith(key)
        .compact()
}
