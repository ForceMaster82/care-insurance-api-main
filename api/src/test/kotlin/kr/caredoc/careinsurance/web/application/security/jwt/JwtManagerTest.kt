package kr.caredoc.careinsurance.web.application.security.jwt

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.security.authentication.AuthenticationMethod
import java.time.LocalDateTime
import java.util.Base64

class JwtManagerTest : BehaviorSpec({
    given("jwt issuer and claims") {
        val key = Keys.hmacShaKeyFor(
            Base64.getDecoder().decode("jqQdvzkRZyNfTckx70xltdz66bO9dqoeQV5VEk18lbA=")
        )
        val accessTokenLifeSpan = 5 * 60 * 1000L // 5분
        val refreshTokenLifeSpan = 4 * 24 * 60 * 60 * 1000L // 나흘
        val usedRefreshTokenRepository = relaxedMock<UsedRefreshTokenRepository>()

        val jwtManager = JwtManager(
            key = key,
            accessTokenLifeSpan = accessTokenLifeSpan,
            refreshTokenLifeSpan = refreshTokenLifeSpan,
            usedRefreshTokenRepository = usedRefreshTokenRepository,
            jwtParser = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
        )

        beforeEach {
            every { usedRefreshTokenRepository.existsByJti("01H40CY9TK9XNG43V5GZ4ZYZQJ") } returns false
        }

        afterEach { clearAllMocks() }

        `when`("generating token") {
            val claims = Claims(
                subjectId = "01GNTMSS2D7FE8FK71J4J93MZP",
                credentialRevision = "01H3V8YT9KDJH4SJPASE6MNCWF",
            )

            fun behavior() = jwtManager.issueTokens(claims, AuthenticationMethod.ID_PW_LOGIN)

            then("generated access token should contains expected claims") {
                val issuedTokens = behavior()
                val issuedAccessToken = issuedTokens.accessToken
                val parsedClaims =
                    Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(issuedAccessToken).body

                parsedClaims["sub"].toString() shouldBe "01GNTMSS2D7FE8FK71J4J93MZP"
                parsedClaims["internalCaregivingManagerId"].toString() shouldBe "null"
                parsedClaims["tokenType"].toString() shouldBe "access"
                parsedClaims["credentialRevision"].toString() shouldBe "01H3V8YT9KDJH4SJPASE6MNCWF"
                parsedClaims["authenticationMethod"].toString() shouldBe "ID_PW_LOGIN"
                parsedClaims["iat"] shouldNotBe null
                parsedClaims["exp"] shouldNotBe null
            }

            then("generated refresh token should contains expected claims") {
                val issuedTokens = behavior()
                val issuedAccessToken = issuedTokens.refreshToken
                val parsedClaims =
                    Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(issuedAccessToken).body

                parsedClaims["sub"].toString() shouldBe "01GNTMSS2D7FE8FK71J4J93MZP"
                parsedClaims["tokenType"].toString() shouldBe "refresh"
                parsedClaims["credentialRevision"].toString() shouldBe "01H3V8YT9KDJH4SJPASE6MNCWF"
                parsedClaims["authenticationMethod"].toString() shouldBe "ID_PW_LOGIN"
                parsedClaims["iat"] shouldNotBe null
                parsedClaims["exp"] shouldNotBe null
            }
        }

        `when`("내부 관리자 아이디를 포함하여 토큰을 생성하면") {
            val claims = Claims(
                subjectId = "01GNTMSS2D7FE8FK71J4J93MZP",
                credentialRevision = "01H3V8YT9KDJH4SJPASE6MNCWF",
                internalCaregivingManagerId = "01GVCZ7W7MAYAC6C7JMJHSNEJR",
            )

            fun behavior() = jwtManager.issueTokens(claims, AuthenticationMethod.ID_PW_LOGIN)

            then("생성된 액세스 토큰의 클레임은 내부 관리자 아이디를 포함해야 합니다.") {
                val issuedTokens = behavior()
                val issuedAccessToken = issuedTokens.accessToken
                val parsedClaims =
                    Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(issuedAccessToken).body

                parsedClaims["internalCaregivingManagerId"].toString() shouldBe "01GVCZ7W7MAYAC6C7JMJHSNEJR"
            }

            then("생성된 리프래쉬 토큰의 클레임은 내부 관리자 아이디를 포함해선 안됩니다.") {
                val issuedTokens = behavior()
                val issuedAccessToken = issuedTokens.refreshToken
                val parsedClaims =
                    Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(issuedAccessToken).body

                parsedClaims.keys shouldNotContain "internalCaregivingManagerId"
            }
        }

        `when`("외부 관리자 아이디를 포함하여 토큰을 생성하면") {
            val claims = Claims(
                subjectId = "01GNTMSS2D7FE8FK71J4J93MZP",
                credentialRevision = "01H3V8YT9KDJH4SJPASE6MNCWF",
                externalCaregivingManagerIds = listOf("01GRTXJMJQPT5VW5796PVK6KWC", "01GSVDEYJ7TD9P853GBT6CWZ0J"),
            )

            fun behavior() = jwtManager.issueTokens(claims, AuthenticationMethod.ID_PW_LOGIN)

            then("생성된 액세스 토큰의 클레임은 내부 관리자 아이디를 포함해야 합니다.") {
                val issuedTokens = behavior()
                val issuedAccessToken = issuedTokens.accessToken

                val parsedClaims =
                    Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(issuedAccessToken).body

                parsedClaims["externalCaregivingManagerIds"].toString() shouldBe "[01GRTXJMJQPT5VW5796PVK6KWC, 01GSVDEYJ7TD9P853GBT6CWZ0J]"
            }

            then("생성된 리프래쉬 토큰의 클레임은 내부 관리자 아이디를 포함해선 안됩니다.") {
                val issuedTokens = behavior()
                val issuedAccessToken = issuedTokens.refreshToken
                val parsedClaims =
                    Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(issuedAccessToken).body

                parsedClaims.keys shouldNotContain "externalCaregivingManagerIds"
            }
        }

        `when`("사용하는 리프레쉬 토큰을 포함하여 토큰을 생성하면") {
            val claims = Claims(
                subjectId = "01GNTMSS2D7FE8FK71J4J93MZP",
                credentialRevision = "01H3V8YT9KDJH4SJPASE6MNCWF",
            )

            fun behavior() = jwtManager.issueTokens(
                claims,
                refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIwMUdOVE1TUzJEN0ZFOEZLNzFKNEo5M01aUCIsInRva2VuVHlwZSI6InJlZnJlc2giLCJqdGkiOiIwMUg0MENZOVRLOVhORzQzVjVHWjRaWVpRSiIsImlhdCI6MTY4NzkzNTc2NCwiYXV0aGVudGljYXRpb25NZXRob2QiOiJJRF9QV19MT0dJTiJ9.aYVnAmTuyX6cFLTJm1Vn4zXAxfKXU1byUKfgjUhqRKw",
            )

            then("리프레쉬 토큰의 jti가 사용되었다는 사실을 리포지토리에 기록합니다.") {
                behavior()

                verify {
                    usedRefreshTokenRepository.saveAsUsed(
                        "01H40CY9TK9XNG43V5GZ4ZYZQJ",
                        LocalDateTime.of(2023, 6, 28, 16, 2, 44),
                    )
                }
            }
        }

        `when`("리프레쉬 토큰이 사용되었는지 확인하면") {
            fun behavior() = jwtManager.ensureRefreshTokenNotUsed(
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIwMUdOVE1TUzJEN0ZFOEZLNzFKNEo5M01aUCIsInRva2VuVHlwZSI6InJlZnJlc2giLCJqdGkiOiIwMUg0MENZOVRLOVhORzQzVjVHWjRaWVpRSiIsImlhdCI6MTY4NzkzNTc2NH0.gljIqq8H1O4iIguvWdD4Nbcg6FGLNiOcKgYrK7mN8v0"
            )

            then("아무일도 일어나지 않습니다.") {
                shouldNotThrowAny { behavior() }
            }
        }

        and("리프레쉬 토큰이 이미 사용되었을때") {
            beforeEach {
                every { usedRefreshTokenRepository.existsByJti("01H40CY9TK9XNG43V5GZ4ZYZQJ") } returns true
            }

            afterEach { clearAllMocks() }

            `when`("리프레쉬 토큰이 사용되었는지 확인하면") {
                fun behavior() = jwtManager.ensureRefreshTokenNotUsed(
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIwMUdOVE1TUzJEN0ZFOEZLNzFKNEo5M01aUCIsInRva2VuVHlwZSI6InJlZnJlc2giLCJqdGkiOiIwMUg0MENZOVRLOVhORzQzVjVHWjRaWVpRSiIsImlhdCI6MTY4NzkzNTc2NH0.gljIqq8H1O4iIguvWdD4Nbcg6FGLNiOcKgYrK7mN8v0"
                )

                then("RefreshTokenAlreadyUsed이 발생합니다.") {
                    val thrownException = shouldThrow<RefreshTokenAlreadyUsed> { behavior() }

                    thrownException.jti shouldBe "01H40CY9TK9XNG43V5GZ4ZYZQJ"
                }
            }
        }
    }
})
