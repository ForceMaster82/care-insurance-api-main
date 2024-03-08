package kr.caredoc.careinsurance.user

import com.github.guepardoapps.kulid.ULID
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkObject
import kr.caredoc.careinsurance.generateUserSubject
import kr.caredoc.careinsurance.security.accesscontrol.SubjectAttribute
import kr.caredoc.careinsurance.security.password.IllegalPasswordException
import kr.caredoc.careinsurance.user.exception.CredentialNotMatchedException
import kr.caredoc.careinsurance.withFixedClock
import org.springframework.security.access.AccessDeniedException
import java.time.LocalDateTime
import kotlin.random.Random

class UserTest : BehaviorSpec({
    given("사용자를 생성할 수 있는 인자들이 주어졌을때") {
        val id = "01GNTT0KX6QSCG9R4E2322SA2P"
        val name = "보리스"
        val credential = User.EmailPasswordCredential(
            emailAddress = "sm_lim2@caredoc.kr",
            password = "1Q2w3e4r!!",
        )
        `when`("사용자를 생성하면") {
            fun behavior() = withFixedClock(LocalDateTime.of(2023, 1, 27, 16, 39, 0)) {
                User(
                    id = id,
                    name = name,
                    credential = credential
                )
            }

            then("사용자의 비밀번호는 만료되어 있습니다.") {
                val createdUser = behavior()

                createdUser.passwordExpired shouldBe true
            }
        }
    }

    given("비밀번호가 만료된 사용자가 주어졌을때") {
        lateinit var user: User

        beforeEach {
            user = withFixedClock(LocalDateTime.of(2023, 1, 27, 16, 39, 0)) {
                User(
                    id = "01GNTT0KX6QSCG9R4E2322SA2P",
                    name = "보리스",
                    credential = User.EmailPasswordCredential(
                        emailAddress = "sm_lim2@caredoc.kr",
                        password = "1Q2w3e4r!!",
                    )
                )
            }
        }

        afterEach { /* nothing to do */ }

        `when`("CREDENTIAL_EXPIRED 접근 주체 속성을 조회하면") {
            fun behavior() = user[SubjectAttribute.CREDENTIAL_EXPIRED]

            then("문자열 true를 포함한 속성 셋을 반환합니다.") {
                val actualAttribute = behavior()

                actualAttribute shouldContain "true"
            }
        }
    }

    given("비밀번호가 만료되지 않은 사용자가 주어졌을때") {
        lateinit var user: User

        beforeEach {
            user = withFixedClock(LocalDateTime.of(2023, 1, 27, 16, 39, 0)) {
                User(
                    id = "01GNTT0KX6QSCG9R4E2322SA2P",
                    name = "보리스",
                    credential = User.EmailPasswordCredential(
                        emailAddress = "sm_lim2@caredoc.kr",
                        password = "1Q2w3e4r!!",
                    )
                )
            }
            user.edit(
                UserEditingCommand(
                    passwordModification = UserEditingCommand.PasswordModification(
                        currentPassword = "1Q2w3e4r!!",
                        newPassword = "4Q3w2e1r!!",
                    ),
                    subject = user,
                )
            )
        }

        afterEach { /* nothing to do */ }

        `when`("비밀번호를 초기화 하면") {
            fun behavior() = user.resetPassword(
                UserPasswordResetCommand(
                    subject = user,
                )
            )

            then("비밀번호가 만료됩니다.") {
                user.passwordExpired shouldBe false

                behavior()

                user.passwordExpired shouldBe true
            }
        }

        `when`("CREDENTIAL_EXPIRED 접근 주체 속성을 조회하면") {
            fun behavior() = user[SubjectAttribute.CREDENTIAL_EXPIRED]

            then("문자열 false를 포함한 속성 셋을 반환합니다.") {
                val actualAttribute = behavior()

                actualAttribute shouldContain "false"
            }
        }
    }

    given("user") {
        lateinit var user: User

        beforeEach {
            user = withFixedClock(LocalDateTime.of(2023, 1, 27, 16, 39, 0)) {
                User(
                    id = "01GNTT0KX6QSCG9R4E2322SA2P",
                    name = "보리스",
                    credential = User.EmailPasswordCredential(
                        emailAddress = "sm_lim2@caredoc.kr",
                        password = "1Q2w3e4r!!",
                    )
                )
            }
        }

        afterEach { /* nothing to do */ }

        `when`("login using correct password") {
            fun behavior() = user.login("1Q2w3e4r!!")

            then("last login datetime should be updated") {
                user.lastLoginDateTime shouldBe LocalDateTime.of(2023, 1, 27, 16, 39, 0)

                withFixedClock(LocalDateTime.of(2023, 2, 27, 16, 39, 0)) {
                    behavior()
                }

                user.lastLoginDateTime shouldBe LocalDateTime.of(2023, 2, 27, 16, 39, 0)
            }
        }

        `when`("login as user using wrong credential") {
            fun behavior() = user.login("1Q2W3e4r!!")

            then("throws CredentialNotMatchedException") {
                val thrownException = shouldThrow<CredentialNotMatchedException> {
                    behavior()
                }

                thrownException.userId shouldBe "01GNTT0KX6QSCG9R4E2322SA2P"
            }
        }

        `when`("ensuring user activated") {
            fun behavior() = user.ensureUserActivated()

            then("nothing happens") {
                shouldNotThrowAny { behavior() }
            }
        }

        `when`("비밀번호를 수정하면") {
            fun behavior() = user.edit(
                UserEditingCommand(
                    passwordModification = UserEditingCommand.PasswordModification(
                        currentPassword = "1Q2w3e4r!!",
                        newPassword = "4Q3w2e1r!!",
                    ),
                    subject = user,
                )
            )

            then("비밀번호가 수정된다.") {
                behavior()

                shouldNotThrowAny { user.login("4Q3w2e1r!!") }
            }

            then("인증 수단 리비전을 갱신합니다.") {
                val revisionBeforeBehavior = user.credentialRevision

                behavior()

                user.credentialRevision shouldNotBe revisionBeforeBehavior
            }

            then("비밀번호 만료 상태가 해지됩니다.") {
                user.passwordExpired shouldBe true

                behavior()

                user.passwordExpired shouldBe false
            }
        }

        `when`("잘못된 현재 비밀번호를 입력하여 비밀번호를 수정하면") {
            fun behavior() = user.edit(
                UserEditingCommand(
                    passwordModification = UserEditingCommand.PasswordModification(
                        currentPassword = "4Q3w2e1r!!",
                        newPassword = "1Q2w3e4r!!",
                    ),
                    subject = user,
                )
            )

            then("CredentialNotMatchedException 예외가 발생한다.") {
                val thrownException = shouldThrow<CredentialNotMatchedException> { behavior() }

                thrownException.userId shouldBe "01GNTT0KX6QSCG9R4E2322SA2P"
            }
        }

        `when`("비밀번호 정책에 맞지 않는 비밀번호로 비밀번호를 변경하려고 하면") {
            fun behavior() = user.edit(
                UserEditingCommand(
                    passwordModification = UserEditingCommand.PasswordModification(
                        currentPassword = "1Q2w3e4r!!",
                        newPassword = "4Q3w2e1r",
                    ),
                    subject = user,
                )
            )

            then("IllegalPasswordException 예외가 발생한다.") {
                shouldThrow<IllegalPasswordException> { behavior() }
            }
        }

        `when`("본인이 아닌 사용자가 비밀번호를 수정하려고 하면") {
            fun behavior() = user.edit(
                UserEditingCommand(
                    passwordModification = UserEditingCommand.PasswordModification(
                        currentPassword = "1Q2w3e4r!!",
                        newPassword = "4Q3w2e1r",
                    ),
                    subject = generateUserSubject("01GV4TY6D5X3QKP4TCVD68TQNM"),
                )
            )

            then("AccessDeniedException 예외가 발생한다.") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }

        `when`("비밀번호를 초기화하면") {
            fun behavior() = user.resetPassword(
                UserPasswordResetCommand(
                    subject = user,
                )
            )

            then("기존 비밀번호로는 로그인이 불가하다.") {
                behavior()

                shouldThrow<CredentialNotMatchedException> { user.login("1Q2w3e4r!!") }
            }

            then("결과로 새롭게 생성된 비밀번호를 반환한다.") {
                val actualResult = behavior()

                actualResult.newPassword shouldNotBe "1Q2w3e4r!!"
            }

            then("새롭게 생성된 비밀번호로만 로그인이 가능하다.") {
                val actualResult = behavior()

                shouldNotThrowAny { user.login(actualResult.newPassword) }
            }

            then("인증 수단 리비전을 갱신합니다.") {
                val revisionBeforeBehavior = user.credentialRevision

                behavior()

                user.credentialRevision shouldNotBe revisionBeforeBehavior
            }

            then("비밀번호가 만료됩니다.") {
                behavior()

                user.passwordExpired shouldBe true
            }
        }

        `when`("본인이 아닌 사용자가 비밀번호를 초기화하려고 하면") {
            fun behavior() = user.resetPassword(
                UserPasswordResetCommand(
                    subject = generateUserSubject("01GV4TY6D5X3QKP4TCVD68TQNM"),
                )
            )

            then("AccessDeniedException 예외가 발생한다.") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }

        `when`("임시 인증번호를 발급하면") {
            fun behavior() = user.issueTemporalAuthenticationCode()

            beforeEach {
                mockkObject(Random)
                every { Random.nextInt(0, 1000000) } returns 10101
            }

            afterEach { clearAllMocks() }

            then("발급된 인증번호를 반환한다.") {
                val actualResult = behavior()

                actualResult.generatedAuthenticationCode shouldBe "010101"
            }

            then("발급된 인증번호를 이용하여 인증을 수행할 수 있다.") {
                behavior()

                shouldNotThrowAny { user.authenticateUsingCode("010101") }
            }

            then("인증 수단 리비전을 갱신합니다.") {
                val revisionBeforeBehavior = user.credentialRevision

                behavior()

                user.credentialRevision shouldNotBe revisionBeforeBehavior
            }
        }

        `when`("발급된 인증번호와 다른 번호로 사용자 인증을 시도하면") {
            fun behavior() = user.authenticateUsingCode("101010")

            beforeEach {
                mockkObject(Random)
                every { Random.nextInt(0, 1000000) } returns 10101

                user.issueTemporalAuthenticationCode()
            }

            afterEach { clearAllMocks() }

            then("CredentialNotMatchedException이 발생한다.") {
                shouldThrow<CredentialNotMatchedException> { behavior() }
            }
        }

        `when`("현재 리비전과 다른 리비전으로 크레덴셜 리비전이 일치하는지 확인하면") {
            var differentCredentialRevision = ULID.random()
            while (user.credentialRevision == differentCredentialRevision) {
                differentCredentialRevision = ULID.random()
            }

            fun behavior() = user.ensureCredentialRevisionMatched(differentCredentialRevision)

            then("CredentialNotMatchedException이 발생합니다.") {
                shouldThrow<CredentialNotMatchedException> { behavior() }
            }
        }

        `when`("현재 리비전과 일치하는 크레덴셜 리비전으로 크레덴셜 리비전이 일치하는지 확인하면") {
            fun behavior() = user.ensureCredentialRevisionMatched(user.credentialRevision)

            then("아무일도 일어나지 않습니다.") {
                shouldNotThrowAny { behavior() }
            }
        }
    }
})
