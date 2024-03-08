package kr.caredoc.careinsurance.web.autorization

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.security.accesscontrol.SubjectAttribute
import kr.caredoc.careinsurance.security.authentication.AuthenticationMethod
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerByUserIdQueryHandler
import kr.caredoc.careinsurance.user.InternalCaregivingManagerByUserIdQueryHandler
import kr.caredoc.careinsurance.user.InternalCaregivingManagerNotFoundByUserIdException
import kr.caredoc.careinsurance.user.User
import kr.caredoc.careinsurance.user.UserByIdQueryHandler
import kr.caredoc.careinsurance.user.UserType
import kr.caredoc.careinsurance.user.exception.UserNotFoundByIdException
import kr.caredoc.careinsurance.web.autorization.exception.AuthorizationHeaderNotPresentException
import kr.caredoc.careinsurance.web.autorization.exception.ClaimedUserNotExistsException
import kr.caredoc.careinsurance.web.autorization.exception.IllegalTokenException
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.web.context.request.NativeWebRequest
import java.util.Base64

class UserSubjectArgumentResolverTest : BehaviorSpec({
    given("user subject argument resolver") {
        val userByIdQueryHandler = relaxedMock<UserByIdQueryHandler>()
        val internalCaregivingManagerByUserIdQueryHandler = relaxedMock<InternalCaregivingManagerByUserIdQueryHandler>()
        val externalCaregivingManagerByUserIdQueryHandler = relaxedMock<ExternalCaregivingManagerByUserIdQueryHandler>()
        val jwtParser = Jwts.parserBuilder()
            .setSigningKey(
                Keys.hmacShaKeyFor(
                    Base64.getDecoder().decode("jqQdvzkRZyNfTckx70xltdz66bO9dqoeQV5VEk18lbA=")
                )
            )
            .build()
        val userSubjectArgumentResolver = UserSubjectArgumentResolver(
            userByIdQueryHandler = userByIdQueryHandler,
            internalCaregivingManagerByUserIdQueryHandler = internalCaregivingManagerByUserIdQueryHandler,
            externalCaregivingManagerByUserIdQueryHandler = externalCaregivingManagerByUserIdQueryHandler,
            jwtParser = jwtParser,
        )

        and("nullable subject parameter") {
            @Suppress("UNUSED_PARAMETER")
            class Temp {
                fun temp(subject: Subject?) {
                    // do nothing
                }
            }

            val subjectTypeMethodParameter = MethodParameter.forParameter(
                Temp::class.java.getDeclaredMethod("temp", Subject::class.java).parameters[0]
            )

            `when`("checking Subject type parameter is supported") {
                fun behavior() = userSubjectArgumentResolver.supportsParameter(subjectTypeMethodParameter)

                then("should be true") {
                    val result = behavior()
                    result shouldBe true
                }
            }

            `when`("resolving Subject from request without token") {
                val request = mockk<NativeWebRequest>()
                beforeEach {
                    every {
                        request.getHeader(HttpHeaders.AUTHORIZATION)
                    } returns null
                }

                afterEach {
                    clearAllMocks()
                }

                fun behavior() = userSubjectArgumentResolver.resolveArgument(
                    subjectTypeMethodParameter,
                    null,
                    request,
                    null,
                )

                then("returns null") {
                    val result = behavior()

                    result shouldBe null
                }
            }
        }

        and("not nullable subject parameter") {
            @Suppress("UNUSED_PARAMETER")
            class Temp {
                fun temp(subject: Subject) {
                    // do nothing
                }
            }

            val subjectTypeMethodParameter = MethodParameter.forParameter(
                Temp::class.java.getDeclaredMethod("temp", Subject::class.java).parameters[0]
            )

            `when`("checking Subject type parameter is supported") {
                fun behavior() = userSubjectArgumentResolver.supportsParameter(subjectTypeMethodParameter)

                then("should be true") {
                    val result = behavior()
                    result shouldBe true
                }
            }

            `when`("resolving Subject from request without token") {
                val request = mockk<NativeWebRequest>()
                beforeEach {
                    every {
                        request.getHeader(HttpHeaders.AUTHORIZATION)
                    } returns null
                }

                afterEach {
                    clearAllMocks()
                }
                fun behavior() = userSubjectArgumentResolver.resolveArgument(
                    subjectTypeMethodParameter,
                    null,
                    request,
                    null,
                )

                then("throws AuthorizationHeaderNotPresentException") {
                    shouldThrow<AuthorizationHeaderNotPresentException> { behavior() }
                }
            }

            and("jwt token without sub") {
                val token =
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlblR5cGUiOiJhY2Nlc3MiLCJpYXQiOjE1MTYyMzkwMjJ9.kBVHB8wakL9F1KxURE8nywO2RgnazLzQMR2hM1ld2As"

                `when`("resolving Subject from request") {
                    val request = mockk<NativeWebRequest>()
                    beforeEach {
                        every {
                            request.getHeader(HttpHeaders.AUTHORIZATION)
                        } returns "Bearer $token"
                    }

                    afterEach {
                        clearAllMocks()
                    }
                    fun behavior() = userSubjectArgumentResolver.resolveArgument(
                        subjectTypeMethodParameter,
                        null,
                        request,
                        null,
                    )

                    then("throw IllegalTokenException") {
                        shouldThrow<IllegalTokenException> {
                            behavior()
                        }
                    }
                }
            }

            and("jwt token") {
                val token =
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIwMUdEWUIzTTU4VEJCWEcxQTBESjFCODY2ViIsImNyZWRlbnRpYWxSZXZpc2lvbiI6IjAxSDNWOFlUOUtESkg0U0pQQVNFNk1OQ1dGIiwidG9rZW5UeXBlIjoiYWNjZXNzIiwiaWF0IjoxNTE2MjM5MDIyLCJhdXRoZW50aWNhdGlvbk1ldGhvZCI6IklEX1BXX0xPR0lOIn0.1Zi8ENF3Fyhw3TfgAwq5GDMV4yf7ZCJM4aKcWDd6HqI"

                and("user exists") {
                    val user = relaxedMock<User>()
                    beforeEach {
                        every {
                            userByIdQueryHandler.getUser(
                                match {
                                    it.userId == "01GDYB3M58TBBXG1A0DJ1B866V"
                                }
                            )
                        } returns user

                        with(user) {
                            every { id } returns "01GDYB3M58TBBXG1A0DJ1B866V"
                            every { get(SubjectAttribute.USER_ID) } returns setOf("01GDYB3M58TBBXG1A0DJ1B866V")
                        }

                        every {
                            internalCaregivingManagerByUserIdQueryHandler.getInternalCaregivingManager(any())
                        } throws InternalCaregivingManagerNotFoundByUserIdException("01GDYB3M58TBBXG1A0DJ1B866V")
                    }

                    afterEach { clearAllMocks() }

                    `when`("resolving subject from request") {
                        val request = mockk<NativeWebRequest>(relaxed = true)
                        beforeEach {
                            request.let {
                                every {
                                    it.getHeader(HttpHeaders.AUTHORIZATION)
                                } returns "Bearer $token"
                                every {
                                    it.getNativeRequest(HttpServletRequest::class.java)
                                } returns relaxedMock()
                            }
                        }

                        afterEach {
                            clearAllMocks()
                        }
                        fun behavior() = userSubjectArgumentResolver.resolveArgument(
                            subjectTypeMethodParameter,
                            null,
                            request,
                            null,
                        )

                        then("provide subject") {
                            val result = behavior()

                            result!![SubjectAttribute.USER_ID] shouldBe setOf("01GDYB3M58TBBXG1A0DJ1B866V")
                            result[SubjectAttribute.AUTHENTICATION_METHOD] shouldBe setOf(AuthenticationMethod.ID_PW_LOGIN.name)
                        }

                        then("토큰에서 제공된 크레덴셜 리비전이 현재 크레덴셜 리비전과 일치하는지 확인합니다.") {
                            behavior()

                            verify {
                                user.ensureCredentialRevisionMatched("01H3V8YT9KDJH4SJPASE6MNCWF")
                            }
                        }
                    }

                    and("user is registered as internal caregiving manager") {
                        beforeEach {
                            every {
                                internalCaregivingManagerByUserIdQueryHandler.getInternalCaregivingManager(
                                    match {
                                        it.userId == "01GDYB3M58TBBXG1A0DJ1B866V"
                                    }
                                )
                            } returns relaxedMock {
                                every { get(SubjectAttribute.USER_TYPE) } returns setOf(UserType.INTERNAL)
                            }
                        }
                        afterEach { clearAllMocks() }

                        `when`("resolving subject from request") {
                            val request = mockk<NativeWebRequest>(relaxed = true)
                            beforeEach {
                                request.let {
                                    every {
                                        it.getHeader(HttpHeaders.AUTHORIZATION)
                                    } returns "Bearer $token"
                                    every {
                                        it.getNativeRequest(HttpServletRequest::class.java)
                                    } returns relaxedMock()
                                }
                            }

                            afterEach { clearAllMocks() }

                            fun behavior() = userSubjectArgumentResolver.resolveArgument(
                                subjectTypeMethodParameter,
                                null,
                                request,
                                null,
                            )

                            then("provide subject") {
                                val result = behavior()

                                result!![SubjectAttribute.USER_ID] shouldBe setOf("01GDYB3M58TBBXG1A0DJ1B866V")
                                result[SubjectAttribute.USER_TYPE] shouldBe setOf(UserType.INTERNAL)
                            }
                        }
                    }
                    and("user is registered as external caregiving manager") {
                        beforeEach {
                            every {
                                externalCaregivingManagerByUserIdQueryHandler.getExternalCaregivingManager(
                                    match {
                                        it.userId == "01GDYB3M58TBBXG1A0DJ1B866V"
                                    }
                                )
                            } returns relaxedMock {
                                every { get(SubjectAttribute.USER_TYPE) } returns setOf(UserType.EXTERNAL)
                            }
                        }
                        afterEach { clearAllMocks() }

                        `when`("resolving subject from request") {
                            val request = mockk<NativeWebRequest>(relaxed = true)
                            beforeEach {
                                request.let {
                                    every {
                                        it.getHeader(HttpHeaders.AUTHORIZATION)
                                    } returns "Bearer $token"
                                    every {
                                        it.getNativeRequest(HttpServletRequest::class.java)
                                    } returns relaxedMock()
                                }
                            }

                            afterEach { clearAllMocks() }

                            fun behavior() = userSubjectArgumentResolver.resolveArgument(
                                subjectTypeMethodParameter,
                                null,
                                request,
                                null,
                            )

                            then("provide subject") {
                                val result = behavior()

                                result!![SubjectAttribute.USER_ID] shouldBe setOf("01GDYB3M58TBBXG1A0DJ1B866V")
                                result[SubjectAttribute.USER_TYPE] shouldBe setOf(UserType.EXTERNAL)
                            }
                        }
                    }
                }

                and("user not exists") {
                    beforeEach {
                        every {
                            userByIdQueryHandler.getUser(
                                match {
                                    it.userId == "01GDYB3M58TBBXG1A0DJ1B866V"
                                }
                            )
                        } throws UserNotFoundByIdException("01GDYB3M58TBBXG1A0DJ1B866V")
                    }

                    afterEach {
                        clearAllMocks()
                    }

                    `when`("resolving subject from request") {
                        val request = mockk<NativeWebRequest>(relaxed = true)
                        beforeEach {
                            request.let {
                                every {
                                    it.getHeader(HttpHeaders.AUTHORIZATION)
                                } returns "Bearer $token"
                                every {
                                    it.getNativeRequest(HttpServletRequest::class.java)
                                } returns relaxedMock()
                            }
                        }

                        afterEach {
                            clearAllMocks()
                        }
                        fun behavior() = userSubjectArgumentResolver.resolveArgument(
                            subjectTypeMethodParameter,
                            null,
                            request,
                            null,
                        )

                        then("throws ClaimedUserNotExistsException") {
                            val thrownException = shouldThrow<ClaimedUserNotExistsException> { behavior() }

                            thrownException.userId shouldBe "01GDYB3M58TBBXG1A0DJ1B866V"
                        }
                    }
                }
            }

            and("refresh jwt token") {
                val token =
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIwMUdEWUIzTTU4VEJCWEcxQTBESjFCODY2ViIsInRva2VuVHlwZSI6InJlZnJlc2giLCJpYXQiOjE1MTYyMzkwMjJ9.EklT0m4k2y-vJRrhauo4jutfmV0stpHy-a9Ztf-cGcw"

                `when`("resolving Subject from request") {
                    val request = mockk<NativeWebRequest>()
                    beforeEach {
                        every {
                            request.getHeader(HttpHeaders.AUTHORIZATION)
                        } returns "Bearer $token"
                    }

                    afterEach {
                        clearAllMocks()
                    }
                    fun behavior() = userSubjectArgumentResolver.resolveArgument(
                        subjectTypeMethodParameter,
                        null,
                        request,
                        null,
                    )

                    then("throw IllegalTokenException") {
                        shouldThrow<IllegalTokenException> {
                            behavior()
                        }
                    }
                }
            }
        }
    }
})
