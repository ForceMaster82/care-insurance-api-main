package kr.caredoc.careinsurance.web.authentication

import com.ninjasquad.springmockk.MockkBean
import io.jsonwebtoken.JwtParser
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.security.authentication.AuthenticationMethod
import kr.caredoc.careinsurance.security.password.IllegalPasswordException
import kr.caredoc.careinsurance.user.EmailAuthenticationCodeLoginCredential
import kr.caredoc.careinsurance.user.EmailAuthenticationCodeLoginHandler
import kr.caredoc.careinsurance.user.EmailPasswordLoginCredential
import kr.caredoc.careinsurance.user.EmailPasswordLoginHandler
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerByUserIdQueryHandler
import kr.caredoc.careinsurance.user.InternalCaregivingManagerByUserIdQueryHandler
import kr.caredoc.careinsurance.user.User
import kr.caredoc.careinsurance.user.UserByIdQueryHandler
import kr.caredoc.careinsurance.user.exception.CredentialNotMatchedException
import kr.caredoc.careinsurance.user.exception.UserNotFoundByEmailAddressException
import kr.caredoc.careinsurance.user.exception.UserSuspendedException
import kr.caredoc.careinsurance.web.application.security.jwt.JwtManager
import kr.caredoc.careinsurance.web.application.security.jwt.RefreshTokenAlreadyUsed
import kr.caredoc.careinsurance.web.application.security.jwt.TokenSet
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@CareInsuranceWebMvcTest(AuthenticationController::class)
class AuthenticationControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean(relaxed = true)
    private val emailPasswordLoginHandler: EmailPasswordLoginHandler,
    @MockkBean(relaxed = true)
    private val emailAuthenticationCodeLoginHandler: EmailAuthenticationCodeLoginHandler,
    @MockkBean(relaxed = true)
    private val jwtManager: JwtManager,
    @MockkBean(relaxed = true)
    private val jwtParser: JwtParser,
    @MockkBean(relaxed = true)
    private val userByIdQueryHandler: UserByIdQueryHandler,
    @MockkBean(relaxed = true)
    private val internalCaregivingManagerByUserIdQueryHandler: InternalCaregivingManagerByUserIdQueryHandler,
    @MockkBean(relaxed = true)
    private val externalCaregivingManagerByUserIdQueryHandler: ExternalCaregivingManagerByUserIdQueryHandler,
) : ShouldSpec({
    context("when requesting authentication using user credential") {
        val request = post("/api/v1/authentications")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                        "email": "sm_lim2@caredoc.kr",
                        "password": "1Q2w3e4r!!"
                    }
                """.trimIndent()
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                emailPasswordLoginHandler.handleLogin(
                    EmailPasswordLoginCredential(
                        emailAddress = "sm_lim2@caredoc.kr",
                        password = "1Q2w3e4r!!",
                    )
                )
            } returns relaxedMock {
                every { id } returns "01GNS86P7W2NWXZDRJ86Q8KHBF"
            }

            every {
                jwtManager.issueTokens(
                    match {
                        it.subjectId == "01GNS86P7W2NWXZDRJ86Q8KHBF" &&
                            it.internalCaregivingManagerId == "01GVCZ7W7MAYAC6C7JMJHSNEJR" &&
                            it.externalCaregivingManagerIds.contains("01GT3P0201DP6JAH0P1NYBBBCJ")
                    },
                    AuthenticationMethod.ID_PW_LOGIN,
                )
            } returns TokenSet(
                accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
                refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
            )

            every {
                internalCaregivingManagerByUserIdQueryHandler.getInternalCaregivingManager(
                    match {
                        it.userId == "01GNS86P7W2NWXZDRJ86Q8KHBF"
                    }
                )
            } returns relaxedMock {
                every { id } returns "01GVCZ7W7MAYAC6C7JMJHSNEJR"
            }

            every {
                externalCaregivingManagerByUserIdQueryHandler.getExternalCaregivingManager(
                    match {
                        it.userId == "01GNS86P7W2NWXZDRJ86Q8KHBF"
                    }
                )
            } returns relaxedMock {
                every { id } returns "01GT3P0201DP6JAH0P1NYBBBCJ"
            }
        }

        afterEach { clearAllMocks() }

        should("response status should be 200 Ok") {
            expectResponse(status().isOk)
        }

        should("response payload should contains tokens") {
            expectResponse(
                content().json(
                    """
                        {
                          "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
                          "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
                        }
                    """.trimIndent()
                )
            )
        }

        should("JwtIssuer를 통해 JWT를 생성합니다.") {
            mockMvc.perform(request)

            verify {
                jwtManager.issueTokens(
                    withArg {
                        it.subjectId shouldBe "01GNS86P7W2NWXZDRJ86Q8KHBF"
                        it.internalCaregivingManagerId shouldBe "01GVCZ7W7MAYAC6C7JMJHSNEJR"
                        it.externalCaregivingManagerIds shouldContainExactlyInAnyOrder setOf("01GT3P0201DP6JAH0P1NYBBBCJ")
                    },
                    AuthenticationMethod.ID_PW_LOGIN,
                )
            }
        }

        context("when user not exists having entered email address") {
            beforeEach {
                every {
                    emailPasswordLoginHandler.handleLogin(
                        EmailPasswordLoginCredential(
                            emailAddress = "sm_lim2@caredoc.kr",
                            password = "1Q2w3e4r!!",
                        )
                    )
                } throws UserNotFoundByEmailAddressException(
                    enteredEmailAddress = "sm_lim2@caredoc.kr"
                )
            }

            afterEach { clearAllMocks() }

            should("response status should be 401 Unauthorized") {
                expectResponse(status().isUnauthorized)
            }

            should("response should contains error data") {
                expectResponse(
                    content().json(
                        """
                            {
                              "message": "입력한 이메일은 등록되지 않은 이메일입니다.",
                              "errorType": "NOT_REGISTERED_EMAIL_ADDRESS",
                              "data": {
                                "enteredEmailAddress": "sm_lim2@caredoc.kr"
                              }
                            }
                        """.trimIndent()
                    )
                )
            }
        }

        context("when entered credential not matched with existing user") {
            beforeEach {
                every {
                    emailPasswordLoginHandler.handleLogin(
                        EmailPasswordLoginCredential(
                            emailAddress = "sm_lim2@caredoc.kr",
                            password = "1Q2w3e4r!!",
                        )
                    )
                } throws CredentialNotMatchedException("01GNS86P7W2NWXZDRJ86Q8KHBF")
            }

            afterEach { clearAllMocks() }

            should("response status should be 401 Unauthorized") {
                expectResponse(status().isUnauthorized)
            }

            should("response should contains error data") {
                expectResponse(
                    content().json(
                        """
                            {
                              "message": "잘못된 로그인 정보입니다.",
                              "errorType": "WRONG_CREDENTIAL"
                            }
                        """.trimIndent()
                    )
                )
            }
        }

        context("when user suspended") {
            beforeEach {
                every {
                    emailPasswordLoginHandler.handleLogin(
                        EmailPasswordLoginCredential(
                            emailAddress = "sm_lim2@caredoc.kr",
                            password = "1Q2w3e4r!!",
                        )
                    )
                } throws UserSuspendedException("01GNS86P7W2NWXZDRJ86Q8KHBF")
            }

            afterEach { clearAllMocks() }

            should("response status should be 403 Forbidden") {
                expectResponse(status().isForbidden)
            }

            should("response should contains error data") {
                expectResponse(
                    content().json(
                        """
                            {
                              "message": "사용이 중지된 사용자입니다.",
                              "errorType": "USER_SUSPENDED",
                              "data": {
                                "userId": "01GNS86P7W2NWXZDRJ86Q8KHBF"
                              }
                            }
                        """.trimIndent()
                    )
                )
            }
        }
    }

    context("when requesting authentication without email address") {
        val requests = listOf(
            """
                {
                    "email": null,
                    "password": "1Q2w3e4r!!"
                }
            """.trimIndent(),
            """
                {
                    "password": "1Q2w3e4r!!"
                }
            """.trimIndent(),
        ).map { payload ->
            post("/api/v1/authentications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        }

        val expectResponse = ResponseMatcher(mockMvc, requests)

        should("response status should be 400 Bad Request") {
            expectResponse(status().isBadRequest)
        }

        should("response payload should contains error data") {
            expectResponse(
                content().json(
                    """
                        {
                          "message": "유효한 자격증명이 제공되지 않았습니다.",
                          "errorType": "CREDENTIAL_NOT_SUPPLIED"
                        }
                    """.trimIndent()
                )
            )
        }
    }

    context("when requesting authentication without password") {
        val requests = listOf(
            """
                {
                    "email": "sm_lim2@caredoc.kr"
                }
            """.trimIndent(),
            """
                {
                    "email": "sm_lim2@caredoc.kr",
                    "password": null
                }
            """.trimIndent(),
        ).map { payload ->
            post("/api/v1/authentications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        }

        val expectResponse = ResponseMatcher(mockMvc, requests)

        should("response status should be 400 Bad Request") {
            expectResponse(status().isBadRequest)
        }

        should("response payload should contains error data") {
            expectResponse(
                content().json(
                    """
                        {
                          "message": "유효한 자격증명이 제공되지 않았습니다.",
                          "errorType": "CREDENTIAL_NOT_SUPPLIED"
                        }
                    """.trimIndent()
                )
            )
        }
    }

    context("when requesting authentication with illegal email address") {
        val request = post("/api/v1/authentications")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                        "email": "sm_lim2caredoc.kr",
                        "password": "1Q2w3e4r!!"
                    }
                """.trimIndent()
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        should("response status should be 400 Bad Request") {
            expectResponse(status().isBadRequest)
        }

        should("response payload should contains error data") {
            expectResponse(
                content().json(
                    """
                        {
                          "message": "아이디는 영문 대/소문자, 숫자, 특수문자를 조합하여 입력해야 합니다.",
                          "errorType": "EMAIL_VALIDATION_POLICY_VIOLATION"
                        }
                    """.trimIndent()
                )
            )
        }
    }

    context("when requesting authentication with illegal password") {
        val requests = listOf(
            """
                     {
                         "email": "sm_lim2@caredoc.kr",
                         "password": "1q2w3e4r!!"
                     }
            """.trimIndent(),
            """
                     {
                         "email": "sm_lim2@caredoc.kr",
                         "password": "1Q2w3e4r"
                     }
            """.trimIndent(),
            """
                     {
                         "email": "sm_lim2@caredoc.kr",
                         "password": "1Q2!!"
                     }
            """.trimIndent(),
            """
                    {
                        "email": "sm_lim2@caredoc.kr",
                        "password": ""
                    }
            """.trimIndent(),
        ).map { payload ->
            post("/api/v1/authentications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        }

        beforeEach {
            every { emailPasswordLoginHandler.handleLogin(any()) } throws IllegalPasswordException()
        }

        afterEach { clearAllMocks() }

        val expectResponse = ResponseMatcher(mockMvc, requests)

        should("response status should be 400 Bad Request") {
            expectResponse(status().isBadRequest)
        }

        should("response payload should contains error data") {
            expectResponse(
                content().json(
                    """
                        {
                          "message": "비밀번호는 영문 대/소문자, 숫자, 특수문자(!@#$%^&*-_)를 조합하여 8~20자 이내로 입력해야 합니다.",
                          "errorType": "PASSWORD_VALIDATION_POLICY_VIOLATION"
                        }
                    """.trimIndent()
                )
            )
        }
    }

    context("이메일과 임시 인증번호로 로그인을 시도하면") {
        val request = post("/api/v1/authentications")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                        "email": "sm_lim2@caredoc.kr",
                        "authenticationCode": "856731"
                    }
                """.trimIndent()
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                emailAuthenticationCodeLoginHandler.handleLogin(
                    EmailAuthenticationCodeLoginCredential(
                        emailAddress = "sm_lim2@caredoc.kr",
                        authenticationCode = "856731",
                    )
                )
            } returns relaxedMock {
                every { id } returns "01GNS86P7W2NWXZDRJ86Q8KHBF"
            }

            every {
                jwtManager.issueTokens(
                    match {
                        it.subjectId == "01GNS86P7W2NWXZDRJ86Q8KHBF"
                    },
                    AuthenticationMethod.TEMPORAL_CODE,
                )
            } returns TokenSet(
                accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
                refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
            )
        }

        afterEach { clearAllMocks() }

        should("상태 코드 200 Ok 로 응답한다.") {
            expectResponse(status().isOk)
        }

        should("응답 페이로드는 토큰을 포함해야 한다.") {
            expectResponse(
                content().json(
                    """
                        {
                          "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
                          "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
                        }
                    """.trimIndent()
                )
            )
        }

        should("이메일/인증코드 로그인 핸들러로 로그인 시도를 처리한다.") {
            mockMvc.perform(request)

            verify {
                emailAuthenticationCodeLoginHandler.handleLogin(
                    withArg {
                        it.emailAddress shouldBe "sm_lim2@caredoc.kr"
                        it.authenticationCode shouldBe "856731"
                    }
                )
            }
        }
    }

    context("리프래쉬 토큰으로 로그인을 시도하면") {
        val request = post("/api/v1/authentications")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
                    }
                """.trimIndent()
            )

        val user = relaxedMock<User>()
        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                jwtParser.parseClaimsJws("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
            } returns relaxedMock {
                every { body } returns relaxedMock claims@{
                    every { this@claims["tokenType"] } returns "refresh"
                    every { this@claims["sub"] } returns "01GNS86P7W2NWXZDRJ86Q8KHBF"
                    every { this@claims["credentialRevision"] } returns "01H3V8YT9KDJH4SJPASE6MNCWF"
                    every { this@claims.subject } returns "01GNS86P7W2NWXZDRJ86Q8KHBF"
                }
            }

            every {
                userByIdQueryHandler.getUser(match { it.userId == "01GNS86P7W2NWXZDRJ86Q8KHBF" })
            } returns user

            with(user) {
                every { id } returns "01GNS86P7W2NWXZDRJ86Q8KHBF"
            }

            every {
                jwtManager.issueTokens(
                    match {
                        it.subjectId == "01GNS86P7W2NWXZDRJ86Q8KHBF"
                    },
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
                )
            } returns TokenSet(
                accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
                refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
            )
        }

        afterEach { clearAllMocks() }

        should("상태 코드 200 Ok 로 응답한다.") {
            expectResponse(status().isOk)
        }

        should("응답 페이로드는 토큰을 포함해야 한다.") {
            expectResponse(
                content().json(
                    """
                        {
                          "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
                          "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
                        }
                    """.trimIndent()
                )
            )
        }

        should("Jwt Parser로 공급받은 토큰이 유효한지 확인한다.") {
            mockMvc.perform(request)

            verify {
                jwtParser.parseClaimsJws(
                    withArg {
                        it shouldBe "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
                    }
                )
            }
        }

        should("토큰에 입력된 사용자 아이디로 유저를 조회한다.") {
            mockMvc.perform(request)

            verify {
                userByIdQueryHandler.getUser(
                    withArg {
                        it.userId shouldBe "01GNS86P7W2NWXZDRJ86Q8KHBF"
                    }
                )
            }
        }

        should("입력받은 크레덴셜 리비전이 사용자의 현재 크레덴셜 리비전과 일치하는지 확인한다.") {
            mockMvc.perform(request)

            verify {
                user.ensureCredentialRevisionMatched(
                    withArg {
                        it shouldBe "01H3V8YT9KDJH4SJPASE6MNCWF"
                    }
                )
            }
        }

        should("리프레쉬 토큰이 이미 사용되었는지 확인합니다.") {
            jwtManager.ensureRefreshTokenNotUsed("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
        }
    }

    context("이메일과 비밀번호, 임시 인증번호로 로그인을 시도하면") {
        val request = post("/api/v1/authentications")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                        "email": "sm_lim2@caredoc.kr",
                        "password": "1Q2w3e4r!!",
                        "authenticationCode": "856731"
                    }
                """.trimIndent()
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        should("상태 코드 400 Bad Request 로 응답한다.") {
            expectResponse(status().isBadRequest)
        }

        should("응답 페이로드는 에러메시지와 에러타입을 포함한다.") {
            expectResponse(
                content().json(
                    """
                        {
                          "message": "유효한 자격증명이 제공되지 않았습니다.",
                          "errorType": "CREDENTIAL_NOT_SUPPLIED"
                        }
                    """.trimIndent()
                )
            )
        }
    }

    context("이메일과 비밀번호, 리프래시 토큰으로 로그인을 시도하면") {
        val request = post("/api/v1/authentications")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                        "email": "sm_lim2@caredoc.kr",
                        "password": "1Q2w3e4r!!",
                        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
                    }
                """.trimIndent()
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        should("상태 코드 400 Bad Request 로 응답한다.") {
            expectResponse(status().isBadRequest)
        }

        should("응답 페이로드는 에러메시지와 에러타입을 포함한다.") {
            expectResponse(
                content().json(
                    """
                        {
                          "message": "유효한 자격증명이 제공되지 않았습니다.",
                          "errorType": "CREDENTIAL_NOT_SUPPLIED"
                        }
                    """.trimIndent()
                )
            )
        }
    }

    context("리프래시 토큰으로 로그인을 시도했으나 이미 사용한 리프레쉬 토큰이라면") {
        val request = post("/api/v1/authentications")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
                    }
                """.trimIndent()
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                jwtManager.ensureRefreshTokenNotUsed("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
            } throws RefreshTokenAlreadyUsed("01H40CY9TK9XNG43V5GZ4ZYZQJ")
        }

        afterEach { clearAllMocks() }

        should("401 Unauthorized로 응답합니다.") {
            expectResponse(status().isUnauthorized)
        }

        should("에러 메시지와 타입을 페이로드에 포함하여 응답합니다.") {
            expectResponse(
                content().json(
                    """
                        {
                          "message": "이미 사용된 RefreshToken입니다.",
                          "errorType": "REFRESH_TOKEN_ALREADY_USED"
                        }
                    """.trimIndent()
                )
            )
        }
    }
})
