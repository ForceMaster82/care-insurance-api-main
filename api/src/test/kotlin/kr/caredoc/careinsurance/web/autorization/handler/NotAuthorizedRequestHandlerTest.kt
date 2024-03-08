package kr.caredoc.careinsurance.web.autorization.handler

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.user.exception.UserSuspendedException
import kr.caredoc.careinsurance.web.autorization.exception.AuthorizationHeaderNotPresentException
import kr.caredoc.careinsurance.web.autorization.exception.IllegalTokenException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.security.access.AccessDeniedException

class NotAuthorizedRequestHandlerTest : BehaviorSpec({
    given("exception handler") {
        val handler = NotAuthorizedRequestHandler()

        `when`("handle UserSuspendedException") {
            fun behavior() = handler.handleUserSuspendedException(
                UserSuspendedException("01GNXBZAQ0J9J8DD8K461RDF26")
            )

            then("response status should be 403 Forbidden") {
                val actualResponse = behavior()

                actualResponse.statusCode shouldBe HttpStatusCode.valueOf(
                    HttpStatus.FORBIDDEN.value()
                )
            }

            then("response payload should contains error message and type") {
                val actualResponse = behavior()

                with(actualResponse.body!!) {
                    this.message shouldBe "사용이 중지된 사용자입니다."
                    this.errorType shouldBe "USER_SUSPENDED"
                }
            }

            then("response payload should contains suspended user id in data") {
                val actualResponse = behavior()

                actualResponse.body!!.data.userId shouldBe "01GNXBZAQ0J9J8DD8K461RDF26"
            }
        }

        `when`("handle AuthorizationHeaderNotPresentException") {
            fun behavior() = handler.handleAuthorizationHeaderNotPresentException(
                AuthorizationHeaderNotPresentException()
            )

            then("response status should be 401 Unauthorized") {
                val actualResponse = behavior()

                actualResponse.statusCode shouldBe HttpStatusCode.valueOf(
                    HttpStatus.UNAUTHORIZED.value()
                )
            }

            then("response payload should contains error message and type") {
                val actualResponse = behavior()

                with(actualResponse.body!!) {
                    this.message shouldBe "크레덴셜이 제공되지 않았습니다."
                    this.errorType shouldBe "CREDENTIAL_NOT_SUPPLIED"
                }
            }
        }

        `when`("handle IllegalTokenException") {
            fun behavior() = handler.handleIllegalTokenException(
                IllegalTokenException("mock")
            )

            then("response status should be 401 Unauthorized") {
                val actualResponse = behavior()

                actualResponse.statusCode shouldBe HttpStatusCode.valueOf(
                    HttpStatus.UNAUTHORIZED.value()
                )
            }

            then("response payload should contains error message and type") {
                val actualResponse = behavior()

                with(actualResponse.body!!) {
                    this.message shouldBe "토큰 형식이 잘못되었습니다."
                    this.errorType shouldBe "ILLEGAL_TOKEN_SUPPLIED"
                }
            }
        }

        `when`("handle ExpiredJwtException") {
            fun behavior() = handler.handleExpiredJwtException(relaxedMock())

            then("response status should be 401 Unauthorized") {
                val actualResponse = behavior()

                actualResponse.statusCode shouldBe HttpStatusCode.valueOf(
                    HttpStatus.UNAUTHORIZED.value()
                )
            }

            then("response payload should contains error message and type") {
                val actualResponse = behavior()

                with(actualResponse.body!!) {
                    this.message shouldBe "토큰이 만료되었습니다."
                    this.errorType shouldBe "TOKEN_EXPIRED"
                }
            }
        }

        `when`("handle AccessDeniedException") {
            fun behavior() = handler.handleAccessDeniedHandler(
                AccessDeniedException("mock")
            )

            then("response status should be 403 Forbidden") {
                val actualResponse = behavior()

                actualResponse.statusCode shouldBe HttpStatusCode.valueOf(
                    HttpStatus.FORBIDDEN.value()
                )
            }

            then("response payload should contains error message and type") {
                val actualResponse = behavior()

                with(actualResponse.body!!) {
                    this.message shouldBe "권한이 불충분합니다."
                    this.errorType shouldBe "NOT_AUTHORIZED"
                }
            }
        }
    }
})
