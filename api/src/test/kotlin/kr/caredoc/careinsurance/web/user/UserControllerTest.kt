package kr.caredoc.careinsurance.web.user

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationByIdQueryHandler
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationType
import kr.caredoc.careinsurance.patch.Patches
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerByUserIdQueryHandler
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerNotFoundByUserIdException
import kr.caredoc.careinsurance.user.InternalCaregivingManagerByUserIdQueryHandler
import kr.caredoc.careinsurance.user.InternalCaregivingManagerNotFoundByUserIdException
import kr.caredoc.careinsurance.user.TemporalAuthenticationCodeIssuer
import kr.caredoc.careinsurance.user.UserByEmailQueryHandler
import kr.caredoc.careinsurance.user.UserByIdQueryHandler
import kr.caredoc.careinsurance.user.UserEditingCommandHandler
import kr.caredoc.careinsurance.user.UserPasswordResetCommandHandler
import kr.caredoc.careinsurance.user.exception.CredentialNotMatchedException
import kr.caredoc.careinsurance.user.exception.UserNotFoundByEmailAddressException
import kr.caredoc.careinsurance.user.exception.UserNotFoundByIdException
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@CareInsuranceWebMvcTest(UserController::class)
class UserControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean
    private val userByIdQueryHandler: UserByIdQueryHandler,
    @MockkBean
    private val internalCaregivingManagerByUserIdQueryHandler: InternalCaregivingManagerByUserIdQueryHandler,
    @MockkBean
    private val externalCaregivingManagerByUserIdQueryHandler: ExternalCaregivingManagerByUserIdQueryHandler,
    @MockkBean
    private val externalCaregivingOrganizationByIdQueryHandler: ExternalCaregivingOrganizationByIdQueryHandler,
    @MockkBean(relaxed = true)
    private val userEditingCommandHandler: UserEditingCommandHandler,
    @MockkBean(relaxed = true)
    private val userPasswordResetCommandHandler: UserPasswordResetCommandHandler,
    @MockkBean(relaxed = true)
    private val temporalAuthenticationCodeIssuer: TemporalAuthenticationCodeIssuer,
    @MockkBean
    private val userByEmailQueryHandler: UserByEmailQueryHandler,
) : ShouldSpec({
    context("when getting single user") {
        val request = get("/api/v1/users/01GPD5EE21TGK5A5VCYWQ9Z73W")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                userByIdQueryHandler.getUser(match { it.userId == "01GPD5EE21TGK5A5VCYWQ9Z73W" })
            } returns relaxedMock {
                every { id } returns "01GPD5EE21TGK5A5VCYWQ9Z73W"
                every { name } returns "임석민"
                every { lastLoginDateTime } returns LocalDateTime.of(2023, 1, 27, 16, 18, 51)
            }
        }

        afterEach { clearAllMocks() }

        context("조회된 사용자가 내부 사용자라면") {
            beforeEach {
                every {
                    internalCaregivingManagerByUserIdQueryHandler.getInternalCaregivingManager(
                        match {
                            it.userId == "01GPD5EE21TGK5A5VCYWQ9Z73W"
                        }
                    )
                } returns relaxedMock()

                every {
                    externalCaregivingManagerByUserIdQueryHandler.getExternalCaregivingManager(
                        match {
                            it.userId == "01GPD5EE21TGK5A5VCYWQ9Z73W"
                        }
                    )
                } throws ExternalCaregivingManagerNotFoundByUserIdException("01GPD5EE21TGK5A5VCYWQ9Z73W")
            }

            afterEach { clearAllMocks() }

            should("200 Ok로 응답합니다.") {
                expectResponse(status().isOk)
            }

            should("내부 사용자임이 명시된 사용자 정보를 반환합니다.") {
                expectResponse(
                    content().json(
                        """
                            {
                              "id": "01GPD5EE21TGK5A5VCYWQ9Z73W",
                              "name": "임석민",
                              "organizations": [
                                {
                                  "organizationType": "INTERNAL",
                                  "id": null
                                }
                              ],
                              "lastLoginDateTime": "2023-01-27T07:18:51Z"
                            }
                        """.trimIndent()
                    )
                )
            }
        }

        context("조회된 사용자가 외부 간병 업체의 사용자라면") {
            beforeEach {
                every {
                    internalCaregivingManagerByUserIdQueryHandler.getInternalCaregivingManager(
                        match {
                            it.userId == "01GPD5EE21TGK5A5VCYWQ9Z73W"
                        }
                    )
                } throws InternalCaregivingManagerNotFoundByUserIdException("01GPD5EE21TGK5A5VCYWQ9Z73W")

                every {
                    externalCaregivingManagerByUserIdQueryHandler.getExternalCaregivingManager(
                        match {
                            it.userId == "01GPD5EE21TGK5A5VCYWQ9Z73W"
                        }
                    )
                } returns relaxedMock {
                    every { externalCaregivingOrganizationId } returns "01GQ4PE5J0SHCS5BQTJBBKTX8Q"
                }

                every {
                    externalCaregivingOrganizationByIdQueryHandler.getExternalCaregivingOrganization(
                        match {
                            it.id == "01GQ4PE5J0SHCS5BQTJBBKTX8Q"
                        }
                    )
                } returns relaxedMock {
                    every { id } returns "01GQ4PE5J0SHCS5BQTJBBKTX8Q"
                    every { externalCaregivingOrganizationType } returns ExternalCaregivingOrganizationType.ORGANIZATION
                }
            }

            afterEach { clearAllMocks() }

            should("200 Ok로 응답합니다.") {
                expectResponse(status().isOk)
            }

            should("간병 업체 정보를 포함한 사용자 정보를 반환합니다.") {
                expectResponse(
                    content().json(
                        """
                            {
                              "id": "01GPD5EE21TGK5A5VCYWQ9Z73W",
                              "name": "임석민",
                              "organizations": [
                                {
                                  "organizationType": "ORGANIZATION",
                                  "id": "01GQ4PE5J0SHCS5BQTJBBKTX8Q"
                                }
                              ],
                              "lastLoginDateTime": "2023-01-27T07:18:51Z"
                            }
                        """.trimIndent()
                    )
                )
            }
        }

        context("but user not exists") {
            beforeEach {
                every {
                    userByIdQueryHandler.getUser(match { it.userId == "01GPD5EE21TGK5A5VCYWQ9Z73W" })
                } throws UserNotFoundByIdException("01GPD5EE21TGK5A5VCYWQ9Z73W")
            }

            afterEach { clearAllMocks() }

            should("response status should be 404 Not Found") {
                expectResponse(status().isNotFound)
            }

            should("response payload should contains error message and error type") {
                expectResponse(
                    content().json(
                        """
                            {
                              "message": "조회하고자 하는 사용자가 존재하지 않습니다.",
                              "errorType": "USER_NOT_EXISTS"
                            }
                        """.trimIndent()
                    )
                )
            }

            should("response should contains entered user id as data") {
                expectResponse(
                    content().json(
                        """
                            {
                              "data": {
                                "enteredUserId": "01GPD5EE21TGK5A5VCYWQ9Z73W"
                              }
                            }
                        """.trimIndent()
                    )
                )
            }
        }
    }

    context("비밀번호 수정을 요청하면") {
        val request = put("/api/v1/users/01GPD5EE21TGK5A5VCYWQ9Z73W/password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                      "password": "4Q3w2e1r!!",
                      "currentPassword": "1Q2w3e4r!!"
                    }
                """.trimIndent()
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        should("상태 코드 204 No Content 로 응답한다.") {
            expectResponse(status().isNoContent)
        }

        should("응답 페이로드는 비어있어야 한다.") {
            expectResponse(content().string(""))
        }

        should("요청 페이로드의 데이터를 바탕으로 도메인 영역에 처리를 위임한다.") {
            mockMvc.perform(request)

            verify {
                userEditingCommandHandler.editUser(
                    withArg {
                        it.userId shouldBe "01GPD5EE21TGK5A5VCYWQ9Z73W"
                    },
                    withArg {
                        it.email shouldBe Patches.ofEmpty()
                        it.name shouldBe Patches.ofEmpty()
                        it.suspended shouldBe Patches.ofEmpty()
                        it.passwordModification?.currentPassword shouldBe "1Q2w3e4r!!"
                        it.passwordModification?.newPassword shouldBe "4Q3w2e1r!!"
                    },
                )
            }
        }

        context("하지만 사용자의 현재 비밀번호와 입력한 현재 비밀번호가 일치하지 않는다면") {
            beforeEach {
                every {
                    userEditingCommandHandler.editUser(
                        match {
                            it.userId == "01GPD5EE21TGK5A5VCYWQ9Z73W"
                        },
                        match {
                            it.passwordModification?.currentPassword == "1Q2w3e4r!!"
                        }
                    )
                } throws CredentialNotMatchedException("01GPD5EE21TGK5A5VCYWQ9Z73W")
            }

            should("상태코드 401 Unauthorized로 응답한다.") {
                expectResponse(status().isUnauthorized)
            }

            should("에러 데이터를 페이로드에 포함하여 응답한다.") {
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
    }

    context("비밀번호 초기화를 요청하면") {
        val request = put("/api/v1/users/01GPD5EE21TGK5A5VCYWQ9Z73W/password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                      "password": null
                    }
                """.trimIndent()
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        should("상태 코드 204 No Content 로 응답한다.") {
            expectResponse(status().isNoContent)
        }

        should("응답 페이로드는 비어있어야 한다.") {
            expectResponse(content().string(""))
        }

        should("요청 페이로드의 데이터를 바탕으로 도메인 영역에 패스워드 초기화 처리를 위임한다.") {
            mockMvc.perform(request)

            verify {
                userPasswordResetCommandHandler.resetPassword(
                    withArg {
                        it.userId shouldBe "01GPD5EE21TGK5A5VCYWQ9Z73W"
                    },
                    any(),
                )
            }
        }
    }

    context("사용자의 임시 인증번호 발급을 요청하면") {
        val request = put("/api/v1/users/01GPD5EE21TGK5A5VCYWQ9Z73W/authentication-code")

        val expectResponse = ResponseMatcher(mockMvc, request)

        should("상태 코드 204 No Content 로 응답한다.") {
            expectResponse(status().isNoContent)
        }

        should("응답 페이로드는 비어있어야 한다.") {
            expectResponse(content().string(""))
        }

        should("도메인 영역에 임시 인증번호 발급 절차를 위임한다.") {
            mockMvc.perform(request)

            verify {
                temporalAuthenticationCodeIssuer.issueTemporalAuthenticationCode(
                    withArg {
                        it.userId shouldBe "01GPD5EE21TGK5A5VCYWQ9Z73W"
                    }
                )
            }
        }
    }

    context("이메일 주소로 사용자를 조회하면") {
        val request = get("/api/v1/users")
            .queryParam("email", "boris@caredoc.kr")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every { userByEmailQueryHandler.getUser(match { it.email == "boris@caredoc.kr" }) } returns relaxedMock {
                every { id } returns "01GQRKAZD81YPFHBZZS4DCC805"
            }
        }

        afterEach { clearAllMocks() }

        should("상태 코드 200 Ok 로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("사용자 아이디 목록을 페이로드에 포함하여 응답합니다.") {
            expectResponse(
                content().json(
                    """
                        [
                          {
                            "id": "01GQRKAZD81YPFHBZZS4DCC805"
                          }
                        ]
                    """.trimIndent()
                )
            )
        }

        should("도메인 영역에 사용자 조회를 위임합니다.") {
            mockMvc.perform(request)

            verify {
                userByEmailQueryHandler.getUser(withArg { it.email shouldBe "boris@caredoc.kr" })
            }
        }

        context("하지만 해당 이메일을 사용중인 사용자가 없다면") {
            beforeEach {
                every {
                    userByEmailQueryHandler.getUser(match { it.email == "boris@caredoc.kr" })
                } throws UserNotFoundByEmailAddressException(
                    enteredEmailAddress = "boris@caredoc.kr"
                )
            }

            afterEach { clearAllMocks() }

            should("상태 코드 200 Ok 로 응답합니다.") {
                expectResponse(status().isOk)
            }

            should("빈 목록을 페이로드로 삼아 응답합니다.") {
                expectResponse(content().json("[]".trimIndent()))
            }
        }
    }
})
