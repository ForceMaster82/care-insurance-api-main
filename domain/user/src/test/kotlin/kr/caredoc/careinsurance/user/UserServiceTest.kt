package kr.caredoc.careinsurance.user

import com.github.guepardoapps.kulid.ULID
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kr.caredoc.careinsurance.email.Email
import kr.caredoc.careinsurance.email.EmailSender
import kr.caredoc.careinsurance.generateExternalCaregivingOrganizationManagerSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.patch.Patches
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.security.password.PasswordPolicy
import kr.caredoc.careinsurance.user.exception.AlreadyExistsUserEmailAddressException
import kr.caredoc.careinsurance.user.exception.UserNotFoundByEmailAddressException
import kr.caredoc.careinsurance.user.exception.UserNotFoundByIdException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.AccessDeniedException

class UserServiceTest : BehaviorSpec({
    given("user service") {
        val userRepository = relaxedMock<UserRepository>()
        val userPasswordGuidanceTemplate = relaxedMock<NewUserPasswordGuidanceEmailTemplate>()
        val temporalPasswordGuidanceEmailTemplate = relaxedMock<TemporalPasswordGuidanceEmailTemplate>()
        val temporalAuthenticationCodeGuidanceEmailTemplate =
            relaxedMock<TemporalAuthenticationCodeGuidanceEmailTemplate>()
        val emailSender = relaxedMock<EmailSender>()
        val userService = UserService(
            userRepository = userRepository,
            userPasswordGuidanceTemplate = userPasswordGuidanceTemplate,
            temporalPasswordGuidanceEmailTemplate = temporalPasswordGuidanceEmailTemplate,
            temporalAuthenticationCodeGuidanceEmailTemplate = temporalAuthenticationCodeGuidanceEmailTemplate,
            emailSender = emailSender,
        )

        beforeEach {
            val userSlot = slot<User>()
            every {
                userRepository.save(capture(userSlot))
            } answers {
                userSlot.captured
            }
        }

        afterEach { clearAllMocks() }

        `when`("attempt to login using email/password credential") {
            val credential = EmailPasswordLoginCredential(
                emailAddress = "sm_lim2@caredoc.kr",
                password = "1Q2w3e4r!!",
            )

            val mockUser = relaxedMock<User>()

            beforeEach {
                every {
                    userRepository.findByCredentialEmailAddress("sm_lim2@caredoc.kr")
                } returns mockUser
            }

            afterEach { clearAllMocks() }

            fun behavior() = userService.handleLogin(credential)

            then("query user by email address") {
                behavior()
                verify {
                    userRepository.findByCredentialEmailAddress("sm_lim2@caredoc.kr")
                }
            }

            then("login as user") {
                behavior()
                verify {
                    mockUser.login("1Q2w3e4r!!")
                }
            }

            then("ensure user activated") {
                behavior()
                verify {
                    mockUser.ensureUserActivated()
                }
            }

            then("returns user") {
                val actualResult = behavior()
                actualResult shouldBe mockUser
            }

            and("but user not exists") {
                beforeEach {
                    every {
                        userRepository.findByCredentialEmailAddress("sm_lim2@caredoc.kr")
                    } returns null
                }

                afterEach { clearAllMocks() }

                then("throws UserNotFoundByEmailAddressException") {
                    val actualException = shouldThrow<UserNotFoundByEmailAddressException> {
                        userService.handleLogin(credential)
                    }

                    actualException.enteredEmailAddress shouldBe "sm_lim2@caredoc.kr"
                }
            }
        }

        `when`("searching users by name keyword") {
            val existingUsers = listOf<User>(
                relaxedMock(),
                relaxedMock(),
            )
            beforeEach {
                every {
                    userRepository.findByNameContains("보리")
                } returns existingUsers
            }

            afterEach { clearAllMocks() }

            val query = UsersByNameKeywordQuery(
                nameKeyword = "보리",
                subject = generateInternalCaregivingManagerSubject(),
            )

            fun behavior() = userService.getUsers(query)

            then("query users using keyword") {
                behavior()

                verify {
                    userRepository.findByNameContains("보리")
                }
            }

            then("returns queried users") {
                val actualResult = behavior()

                actualResult shouldContainExactlyInAnyOrder existingUsers
            }
        }

        `when`("getting user by id") {
            val query = UserByIdQuery(userId = "01GP0C6MA2ECPYD3EVWXRFZ471")
            val mockedUser = relaxedMock<User>()

            beforeEach {
                every {
                    userRepository.findByIdOrNull("01GP0C6MA2ECPYD3EVWXRFZ471")
                } returns mockedUser
            }

            afterEach { clearAllMocks() }

            fun behavior() = userService.getUser(query)

            then("query user from repository") {
                behavior()
                verify {
                    userRepository.findByIdOrNull("01GP0C6MA2ECPYD3EVWXRFZ471")
                }
            }

            then("returns queried user") {
                val actualResult = behavior()

                actualResult shouldBe mockedUser
            }

            and("but user not exists") {
                beforeEach {
                    every {
                        userRepository.findByIdOrNull("01GP0C6MA2ECPYD3EVWXRFZ471")
                    } returns null
                }

                afterEach { clearAllMocks() }

                then("throws UserNotFoundByIdException") {
                    val thrownException = shouldThrow<UserNotFoundByIdException> { behavior() }

                    thrownException.userId shouldBe "01GP0C6MA2ECPYD3EVWXRFZ471"
                }
            }
        }

        `when`("creating user") {
            val createdUserId = "01GPJS2YF08TQFQY1Y71S5D6J2"
            val command = UserCreationCommand(
                name = "레나",
                emailAddressForLogin = "my_bang@caredoc.kr",
            )

            val passwordGuidanceEmail = relaxedMock<Email>()

            fun behavior() = userService.createUser(command)

            beforeEach {
                mockkObject(ULID)
                every { ULID.random() } returns "01GPJS2YF08TQFQY1Y71S5D6J2"

                every {
                    userPasswordGuidanceTemplate.generate(any())
                } returns passwordGuidanceEmail
            }

            afterEach { clearAllMocks() }

            `when`("사용 가능한 이메일로 유저를 생성하면") {
                beforeEach {
                    every {
                        userRepository.existsByCredentialEmailAddress(match { it == "my_bang@caredoc.kr" })
                    } returns false

                    mockkObject(PasswordPolicy)

                    with(PasswordPolicy) {
                        every { generateRandomPassword() } returns "zq2@ia#N3nN#@1"
                        justRun { ensurePasswordLegal("zq2@ia#N3nN#@1") }
                    }
                }

                afterEach {
                    clearAllMocks()
                    unmockkObject(PasswordPolicy)
                }

                then("사용 가능한 이메일인지 확인합니다.") {
                    behavior()

                    verify {
                        userRepository.existsByCredentialEmailAddress(
                            withArg {
                                it shouldBe "my_bang@caredoc.kr"
                            }
                        )
                    }
                }

                then("persist user entity") {
                    behavior()

                    verify {
                        userRepository.save(
                            withArg {
                                it.id shouldBe createdUserId
                                it.name shouldBe "레나"
                                it.emailAddress shouldBe "my_bang@caredoc.kr"
                            }
                        )
                    }
                }

                then("returns creation result") {
                    val actualResult = behavior()
                    actualResult.createdUserId shouldBe "01GPJS2YF08TQFQY1Y71S5D6J2"
                }

                then("비밀번호 안내 이메일을 생성합니다.") {
                    behavior()

                    verify {
                        userPasswordGuidanceTemplate.generate(
                            withArg {
                                it.userEmail shouldBe "my_bang@caredoc.kr"
                                it.userName shouldBe "레나"
                                it.rawPassword shouldBe "zq2@ia#N3nN#@1"
                            }
                        )
                    }
                }

                then("템플릿으로 생성된 이메일을 보냅니다.") {
                    behavior()

                    verify {
                        emailSender.send(passwordGuidanceEmail)
                    }
                }

                then("비밀번호 정책으로부터 무작위 비밀번호를 생성한다.") {
                    behavior()

                    verify {
                        PasswordPolicy.generateRandomPassword()
                    }
                }
            }

            and("사용 불가능한 이메일로 유저를 생성하면") {
                beforeEach {
                    every {
                        userRepository.existsByCredentialEmailAddress(match { it == "my_bang@caredoc.kr" })
                    } returns true
                }
                afterEach { clearAllMocks() }

                then("AlreadyExistsUserEmailAddressException 발생합니다.") {
                    shouldThrow<AlreadyExistsUserEmailAddressException> { behavior() }
                }
            }
        }

        `when`("searching users by email address") {
            val mockUser = relaxedMock<User>()

            beforeEach {
                every {
                    userRepository.findByCredentialEmailAddressContains(match { "boris@caredoc.kr".contains(it) })
                } returns listOf(mockUser)
            }

            afterEach { clearAllMocks() }

            val query = UsersByEmailKeywordQuery(
                emailKeyword = "boris",
                subject = generateInternalCaregivingManagerSubject()
            )

            fun behavior() = userService.getUsers(query)

            then("query users by email keyword") {
                behavior()

                verify {
                    userRepository.findByCredentialEmailAddressContains("boris")
                }
            }

            then("returns queried users") {
                val actualResult = behavior()

                actualResult shouldContainExactlyInAnyOrder setOf(mockUser)
            }
        }

        `when`("searching users by email address without internal user attribute") {
            val query = UsersByEmailKeywordQuery(
                emailKeyword = "boris",
                subject = generateExternalCaregivingOrganizationManagerSubject("01GPWXVJB2WPDNXDT5NE3B964N")
            )

            fun behavior() = userService.getUsers(query)

            then("throws AccessDeniedException") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }

        `when`("getting users by ids") {
            val mockUsers = listOf<User>(
                relaxedMock(),
                relaxedMock(),
            )

            beforeEach {
                every {
                    userRepository.findByIdIn(
                        match {
                            it.containsAll(
                                setOf(
                                    "01GRKBNXSSWRM08DDHGCH84YMA",
                                    "01GRKBP1AFWHARZWEB94079Q5K",
                                )
                            )
                        }
                    )
                } returns mockUsers
            }

            afterEach { clearAllMocks() }

            val query = UsersByIdsQuery(
                userIds = setOf(
                    "01GRKBNXSSWRM08DDHGCH84YMA",
                    "01GRKBP1AFWHARZWEB94079Q5K",
                ),
                subject = generateInternalCaregivingManagerSubject()
            )

            fun behavior() = userService.getUsers(query)

            then("query users using user ids") {
                behavior()

                verify {
                    userRepository.findByIdIn(
                        setOf(
                            "01GRKBNXSSWRM08DDHGCH84YMA",
                            "01GRKBP1AFWHARZWEB94079Q5K",
                        )
                    )
                }
            }

            then("returns queried users") {
                val actualResult = behavior()

                actualResult shouldContainExactlyInAnyOrder mockUsers
            }
        }

        `when`("getting users by ids without internal user attribute") {
            val mockUsers = listOf<User>(
                relaxedMock(),
                relaxedMock(),
            )

            beforeEach {
                every {
                    userRepository.findByIdIn(
                        match {
                            it.containsAll(
                                setOf(
                                    "01GRKBNXSSWRM08DDHGCH84YMA",
                                    "01GRKBP1AFWHARZWEB94079Q5K",
                                )
                            )
                        }
                    )
                } returns mockUsers
            }

            afterEach { clearAllMocks() }

            val query = UsersByIdsQuery(
                userIds = setOf(
                    "01GRKBNXSSWRM08DDHGCH84YMA",
                    "01GRKBP1AFWHARZWEB94079Q5K",
                ),
                subject = generateExternalCaregivingOrganizationManagerSubject("01GPWXVJB2WPDNXDT5NE3B964N")
            )

            fun behavior() = userService.getUsers(query)

            then("throws AccessDeniedException") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }

        `when`("사용자 비밀번호 초기화를 요청하면") {
            val subject = generateInternalCaregivingManagerSubject()

            val user = relaxedMock<User>()
            val guidanceEmail = relaxedMock<Email>()

            beforeEach {
                every { userRepository.findByIdOrNull("01GRKBNXSSWRM08DDHGCH84YMA") } returns user
                with(user) {
                    every { emailAddress } returns "boris@caredoc.kr"
                    every { name } returns "보리스"
                    every { resetPassword(any()) } returns PasswordResetResult("4Q3w2e1r!!")
                }
                every { temporalPasswordGuidanceEmailTemplate.generate(any()) } returns guidanceEmail
            }

            afterEach { clearAllMocks() }

            fun behavior() = userService.resetPassword(
                query = UserByIdQuery("01GRKBNXSSWRM08DDHGCH84YMA"),
                command = UserPasswordResetCommand(subject)
            )

            then("사용자를 조회한다.") {
                behavior()

                verify {
                    userRepository.findByIdOrNull("01GRKBNXSSWRM08DDHGCH84YMA")
                }
            }

            then("사용자의 비밀번호를 초기화한다.") {
                behavior()

                verify {
                    user.resetPassword(
                        withArg {
                            it.subject shouldBe subject
                        }
                    )
                }
            }

            then("사용자의 비밀번호가 초기화되고 임시 비밀번호가 설정되었음을 알리는 이메일을 생성한다.") {
                behavior()

                verify {
                    temporalPasswordGuidanceEmailTemplate.generate(
                        withArg {
                            it.userEmail shouldBe "boris@caredoc.kr"
                            it.userName shouldBe "보리스"
                            it.temporalRawPassword shouldBe "4Q3w2e1r!!"
                        }
                    )
                }
            }

            then("임시 비밀번호 설정 알림을 이메일로 발송합니다.") {
                behavior()

                verify {
                    emailSender.send(guidanceEmail)
                }
            }
        }

        `when`("존재하지 않는 사용자의 비밀번호 초기화를 요청하면") {
            val subject = relaxedMock<Subject>()

            beforeEach {
                every { userRepository.findByIdOrNull("01GRKBNXSSWRM08DDHGCH84YMA") } returns null
            }

            afterEach { clearAllMocks() }

            fun behavior() = userService.resetPassword(
                query = UserByIdQuery("01GRKBNXSSWRM08DDHGCH84YMA"),
                command = UserPasswordResetCommand(subject)
            )

            then("UserNotFoundByIdException이 발생한다.") {
                val thrownException = shouldThrow<UserNotFoundByIdException> { behavior() }

                thrownException.userId shouldBe "01GRKBNXSSWRM08DDHGCH84YMA"
            }

            then("이메일은 발송되지 않는다.") {
                shouldThrowAny { behavior() }

                verify(exactly = 0) {
                    emailSender.send(any())
                }
            }
        }

        `when`("사용자의 임시 인증번호 발급을 요청하면") {
            val user = relaxedMock<User>()
            val guidanceEmail = relaxedMock<Email>()

            beforeEach {
                every { userRepository.findByIdOrNull("01GRKBNXSSWRM08DDHGCH84YMA") } returns user
                with(user) {
                    every { emailAddress } returns "boris@caredoc.kr"
                    every { name } returns "보리스"
                    every { issueTemporalAuthenticationCode() } returns AuthenticationCodeIssuingResult("836716")
                }
                every { temporalAuthenticationCodeGuidanceEmailTemplate.generate(any()) } returns guidanceEmail
            }

            afterEach { clearAllMocks() }

            fun behavior() = userService.issueTemporalAuthenticationCode(
                query = UserByIdQuery("01GRKBNXSSWRM08DDHGCH84YMA"),
            )

            then("대상으로 지목한 사용자를 조회한다.") {
                behavior()

                verify {
                    userRepository.findByIdOrNull("01GRKBNXSSWRM08DDHGCH84YMA")
                }
            }

            then("사용자의 임시 인증번호를 발급한다.") {
                behavior()

                verify {
                    user.issueTemporalAuthenticationCode()
                }
            }

            then("임시 사용자 인증번호 이메일 탬플릿으로부터 이메일을 생성한다.") {
                behavior()

                verify {
                    temporalAuthenticationCodeGuidanceEmailTemplate.generate(
                        withArg {
                            it.userEmail shouldBe "boris@caredoc.kr"
                            it.userName shouldBe "보리스"
                            it.authenticationCode shouldBe "836716"
                        }
                    )
                }
            }

            then("탬플릿으로부터 생성된 이메일을 발송한다.") {
                behavior()

                verify {
                    emailSender.send(guidanceEmail)
                }
            }
        }

        `when`("존재하지 않는 사용자의 임시 인증 코드 발급을 요청하면") {
            beforeEach {
                every { userRepository.findByIdOrNull("01GRKBNXSSWRM08DDHGCH84YMA") } returns null
            }

            afterEach { clearAllMocks() }

            fun behavior() = userService.issueTemporalAuthenticationCode(
                query = UserByIdQuery("01GRKBNXSSWRM08DDHGCH84YMA"),
            )

            then("UserNotFoundByIdException이 발생한다.") {
                val thrownException = shouldThrow<UserNotFoundByIdException> { behavior() }

                thrownException.userId shouldBe "01GRKBNXSSWRM08DDHGCH84YMA"
            }

            then("이메일은 발송되지 않는다.") {
                shouldThrowAny { behavior() }

                verify(exactly = 0) {
                    emailSender.send(any())
                }
            }
        }

        `when`("임시 인증번호를 활용한 로그인을 시도하면") {
            val credential = EmailAuthenticationCodeLoginCredential(
                emailAddress = "sm_lim2@caredoc.kr",
                authenticationCode = "375731",
            )
            val user = relaxedMock<User>()

            beforeEach {
                every { userRepository.findByCredentialEmailAddress("sm_lim2@caredoc.kr") } returns user
            }

            afterEach { clearAllMocks() }

            fun behavior() = userService.handleLogin(credential)

            then("이메일 주소로 유저를 조회한다.") {
                behavior()

                verify {
                    userRepository.findByCredentialEmailAddress("sm_lim2@caredoc.kr")
                }
            }

            then("조회된 유저에게 크레덴셜이 유효한지 확인한다.") {
                behavior()

                verify {
                    user.authenticateUsingCode(
                        withArg {
                            it shouldBe "375731"
                        }
                    )
                }
            }

            and("하지만 로그인을 시도한 유저가 존재하지 않다면") {
                beforeEach {
                    every { userRepository.findByCredentialEmailAddress("sm_lim2@caredoc.kr") } returns null
                }

                afterEach { clearAllMocks() }

                then("UserNotFoundByEmailAddressException 이 발생한다.") {
                    val actualException = shouldThrow<UserNotFoundByEmailAddressException> { behavior() }

                    actualException.enteredEmailAddress shouldBe "sm_lim2@caredoc.kr"
                }
            }
        }

        `when`("사용자의 이메일로 사용자를 조회하면") {
            val query = UserByEmailQuery(email = "boris@caredoc.kr")
            val registeredUser = relaxedMock<User>()

            beforeEach {
                every { userRepository.findTopByCredentialEmailAddress("boris@caredoc.kr") } returns registeredUser
            }

            afterEach { clearAllMocks() }

            fun behavior() = userService.getUser(query)

            then("리포지토리로부터 사용자를 조회합니다.") {
                behavior()

                verify {
                    userRepository.findTopByCredentialEmailAddress("boris@caredoc.kr")
                }
            }

            then("조회된 사용자를 반환합니다.") {
                val actualResult = behavior()

                actualResult shouldBe registeredUser
            }
        }

        `when`("존재하지 않는 사용자의 이메일로 사용자를 조회하면") {
            val query = UserByEmailQuery(email = "boris@caredoc.kr")

            beforeEach {
                every { userRepository.findTopByCredentialEmailAddress("boris@caredoc.kr") } returns null
            }

            afterEach { clearAllMocks() }

            fun behavior() = userService.getUser(query)

            then("UserNotFoundByEmailAddressException이 발생합니다.") {
                val thrownException = shouldThrow<UserNotFoundByEmailAddressException> { behavior() }

                thrownException.enteredEmailAddress shouldBe "boris@caredoc.kr"
            }
        }

        `when`("사용자의 이메일 주소를 이미 다른 사용자가 사용중인 이메일로 수정하면") {
            val query = UserByIdQuery(userId = "01GP0C6MA2ECPYD3EVWXRFZ471")
            val command = UserEditingCommand(
                email = Patches.ofValue("alreadyUsed@caredoc.kr"),
                subject = generateInternalCaregivingManagerSubject(),
            )

            val targetUser = relaxedMock<User>()

            beforeEach {
                every { targetUser.emailAddress } returns "origin@caredoc.kr"

                with(userRepository) {
                    every { findByIdOrNull("01GP0C6MA2ECPYD3EVWXRFZ471") } returns targetUser
                    every { existsByCredentialEmailAddress("alreadyUsed@caredoc.kr") } returns true
                }
            }

            afterEach { clearAllMocks() }

            fun behavior() = userService.editUser(query, command)

            then("AlreadyExistsUserEmailAddressException이 발생합니다.") {
                val thrownException = shouldThrow<AlreadyExistsUserEmailAddressException> { behavior() }

                thrownException.emailAddress shouldBe "alreadyUsed@caredoc.kr"
            }
        }
    }
})
