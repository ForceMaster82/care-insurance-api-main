package kr.caredoc.careinsurance.user.authorizationlogging

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerByIdQueryHandler
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerByUserIdQueryHandler
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerGenerated
import kr.caredoc.careinsurance.user.InternalCaregivingManagerByIdQueryHandler
import kr.caredoc.careinsurance.user.InternalCaregivingManagerByUserIdQueryHandler
import kr.caredoc.careinsurance.user.InternalCaregivingManagerGenerated
import kr.caredoc.careinsurance.user.UserModified
import java.time.LocalDateTime

class UserAuthorizationLoggingServiceTest : BehaviorSpec({
    given("유저 접근권한 로깅 서비스가 주어 졌을때") {
        val userAuthorizationLoggingRepository = relaxedMock<UserAuthorizationLoggingRepository>()
        val externalCaregivingManagerByIdQueryHandler = relaxedMock<ExternalCaregivingManagerByIdQueryHandler>()
        val internalCaregivingManagerByIdQueryHandler = relaxedMock<InternalCaregivingManagerByIdQueryHandler>()
        val externalCaregivingManagerByUserIdQueryHandler = relaxedMock<ExternalCaregivingManagerByUserIdQueryHandler>()
        val internalCaregivingManagerByUserIdQueryHandler = relaxedMock<InternalCaregivingManagerByUserIdQueryHandler>()
        val userAuthorizationLoggingService = UserAuthorizationLoggingService(
            userAuthorizationLoggingRepository,
            externalCaregivingManagerByIdQueryHandler,
            internalCaregivingManagerByIdQueryHandler,
            externalCaregivingManagerByUserIdQueryHandler,
            internalCaregivingManagerByUserIdQueryHandler,
        )

        beforeEach {
            val savingEntitySlot = slot<UserAuthorizationLogging>()
            every {
                userAuthorizationLoggingRepository.save(capture(savingEntitySlot))
            } answers {
                savingEntitySlot.captured
            }
        }

        afterEach { clearAllMocks() }

        `when`("내부 관리자가 생성 되었을 때") {
            val event = relaxedMock<InternalCaregivingManagerGenerated>()
            beforeEach {
                with(event) {
                    every { internalCaregivingManagerId } returns "01H38R6KB7C9CQV33WWH0WHE24"
                    every { userId } returns "01H38R778JH2WGV1677856JZP7"
                    every { grantedDateTime } returns LocalDateTime.of(2023, 6, 19, 11, 33, 22)
                    every { subject } returns generateInternalCaregivingManagerSubject()
                }

                every {
                    internalCaregivingManagerByIdQueryHandler.getInternalCaregivingManager(
                        match { it.internalCaregivingManagerId == "01H38R6KB7C9CQV33WWH0WHE24" }
                    )
                } returns relaxedMock {
                    every { id } returns "01H38R6KB7C9CQV33WWH0WHE24"
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = userAuthorizationLoggingService.handleInternalCaregivingManagerGenerated(event)

            then("유저 접근권한 로깅 자료를 생성합니다.") {
                handling()

                verify {
                    userAuthorizationLoggingRepository.save(
                        withArg {
                            it.grantedUserId shouldBe "01H38R778JH2WGV1677856JZP7"
                            it.grantedRoles shouldBe GrantedRoles.INTERNAL_USER
                            it.grantedType shouldBe GrantedType.GRANTED
                            it.grantedDateTime shouldBe LocalDateTime.of(2023, 6, 19, 11, 33, 22)
                        }
                    )
                }
            }
        }

        `when`("제휴사 사용자가 생성 되었을 때") {
            val event = relaxedMock<ExternalCaregivingManagerGenerated>()
            beforeEach {
                with(event) {
                    every { externalCaregivingManagerId } returns "01H38WM21Y6K71CKT3YSQV1AKW"
                    every { userId } returns "01H38WN1ZTZDNR8H51G6TRKDMY"
                    every { grantedDateTime } returns LocalDateTime.of(2023, 6, 19, 12, 53, 33)
                    every { subject } returns generateInternalCaregivingManagerSubject()
                }

                every {
                    externalCaregivingManagerByIdQueryHandler.getExternalCaregivingManager(
                        match { it.externalCaregivingManagerId == "01H38WM21Y6K71CKT3YSQV1AKW" }
                    )
                } returns relaxedMock {
                    every { id } returns "01H38WM21Y6K71CKT3YSQV1AKW"
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = userAuthorizationLoggingService.handleExternalCaregivingManagerGenerated(event)

            then("유저 접근권한 로깅 자료를 생성합니다.") {
                handling()

                verify {
                    userAuthorizationLoggingRepository.save(
                        withArg {
                            it.grantedUserId shouldBe "01H38WN1ZTZDNR8H51G6TRKDMY"
                            it.grantedRoles shouldBe GrantedRoles.EXTERNAL_USER
                            it.grantedType shouldBe GrantedType.GRANTED
                            it.grantedDateTime shouldBe LocalDateTime.of(2023, 6, 19, 12, 53, 33)
                        }
                    )
                }
            }
        }

        `when`("유저 정보가 수정 되었을 때") {
            val event = relaxedMock<UserModified>()
            beforeEach {
                with(event) {
                    every { userId } returns "01H3BCHP0VX7KG12PJ9E0VB7WJ"
                    every { suspended } returns Modification(
                        previous = false,
                        current = true,
                    )
                    every { editSubject } returns generateInternalCaregivingManagerSubject()
                    every { modifiedDateTime } returns LocalDateTime.of(2023, 6, 20, 12, 51, 33)
                }
                every {
                    internalCaregivingManagerByUserIdQueryHandler.existInternalCaregivingManager(
                        match { it.userId == "01H3BCHP0VX7KG12PJ9E0VB7WJ" }
                    )
                } returns true

                every {
                    externalCaregivingManagerByUserIdQueryHandler.existExternalCaregivingManager(
                        match { it.userId == "01H3BCHP0VX7KG12PJ9E0VB7WJ" }
                    )
                } returns false
            }

            afterEach { clearAllMocks() }

            fun handling() = userAuthorizationLoggingService.handleUserModified(event)

            then("유저 접근권한 로깅 자료가 생성됩니다.") {
                handling()

                verify {
                    userAuthorizationLoggingRepository.save(
                        withArg {
                            it.grantedUserId shouldBe "01H3BCHP0VX7KG12PJ9E0VB7WJ"
                            it.grantedRoles shouldBe GrantedRoles.INTERNAL_USER
                            it.grantedType shouldBe GrantedType.REVOKED
                            it.grantedDateTime shouldBe LocalDateTime.of(2023, 6, 20, 12, 51, 33)
                        }
                    )
                }
            }
        }
    }
})
