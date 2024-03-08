package kr.caredoc.careinsurance.user

import com.github.guepardoapps.kulid.ULID
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import kr.caredoc.careinsurance.generateExternalCaregivingOrganizationManagerSubject
import kr.caredoc.careinsurance.generateGuestSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.generateUserSubject
import kr.caredoc.careinsurance.patch.Patches
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.search.SearchCondition
import kr.caredoc.careinsurance.security.accesscontrol.ObjectAttribute
import kr.caredoc.careinsurance.user.exception.InternalCaregivingManagerNotFoundByIdException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.AccessDeniedException

class InternalCaregivingManagerServiceTest : BehaviorSpec({
    given("internal caregiving manager service") {
        val internalCaregivingManagerRepository = relaxedMock<InternalCaregivingManagerRepository>()
        val userCreationCommandHandler = relaxedMock<UserCreationCommandHandler>()
        val usersByEmailKeywordQueryHandler = relaxedMock<UsersByEmailKeywordQueryHandler>()
        val userEditingCommandHandler = relaxedMock<UserEditingCommandHandler>()
        val eventPublisher = relaxedMock< ApplicationEventPublisher>()
        val internalCaregivingManagerService = InternalCaregivingManagerService(
            internalCaregivingManagerRepository = internalCaregivingManagerRepository,
            userCreationCommandHandler = userCreationCommandHandler,
            usersByEmailKeywordQueryHandler = usersByEmailKeywordQueryHandler,
            userEditingCommandHandler = userEditingCommandHandler,
            eventPublisher = eventPublisher,
        )

        beforeEach {
            with(internalCaregivingManagerRepository) {
                val internalCaregivingManagerSlot = slot<InternalCaregivingManager>()
                every { save(capture(internalCaregivingManagerSlot)) } answers {
                    internalCaregivingManagerSlot.captured
                }

                every { findByUserId(any()) } returns null

                every { findByIdOrNull(any()) } returns null
            }
        }
        afterEach { clearAllMocks() }

        `when`("creating internal caregiving manager") {
            val createdInternalCaregivingManagerId = "01GPJT9CN73GMCBMV9RFCZ4KXY"
            val subject = generateInternalCaregivingManagerSubject()
            val command = InternalCaregivingManagerCreationCommand(
                email = "my_bang@caredoc.kr",
                name = "방미영",
                nickname = "Rena",
                phoneNumber = "010-4026-1111",
                role = "케어닥 백엔드 개발자",
                remarks = "메모입니다.",
                subject = subject,
            )

            fun behavior() = internalCaregivingManagerService.createInternalCaregivingManager(command)

            beforeEach {
                mockkObject(ULID)
                every { ULID.random() } returns "01GPJT9CN73GMCBMV9RFCZ4KXY"
            }

            afterEach { clearAllMocks() }

            then("creating user") {
                behavior()

                verify {
                    userCreationCommandHandler.createUser(
                        withArg {
                            it.name shouldBe "방미영"
                            it.emailAddressForLogin shouldBe "my_bang@caredoc.kr"
                        }
                    )
                }
            }

            then("persist internal caregiving manager entity") {
                behavior()

                verify {
                    internalCaregivingManagerRepository.save(
                        withArg {
                            it.id shouldBe createdInternalCaregivingManagerId
                            it.name shouldBe "방미영"
                            it.nickname shouldBe "Rena"
                            it.phoneNumber shouldBe "010-4026-1111"
                            it.role shouldBe "케어닥 백엔드 개발자"
                            it.remarks shouldBe "메모입니다."
                        }
                    )
                }
            }

            then("returns creation result") {
                val actualResult = behavior()
                actualResult.createdInternalCaregivingManagerId shouldBe "01GPJT9CN73GMCBMV9RFCZ4KXY"
            }
        }

        `when`("getting internal caregiving manager by user id") {
            fun behavior() = internalCaregivingManagerService.getInternalCaregivingManager(
                InternalCaregivingManagerByUserIdQuery(
                    userId = "01GDYB3M58TBBXG1A0DJ1B866V",
                    generateGuestSubject(),
                )
            )

            then("throws InternalCaregivingManagerNotFoundByUserId") {
                shouldThrow<InternalCaregivingManagerNotFoundByUserIdException> { behavior() }
            }
        }

        `when`("존재하지 않는 내부 간병 관리자의 ID로 존재 여부를 확인하면") {
            val query = InternalCaregivingManagerByIdQuery(
                internalCaregivingManagerId = "01GVCZ7W7MAYAC6C7JMJHSNEJR",
                subject = generateInternalCaregivingManagerSubject(),
            )

            fun behavior() = internalCaregivingManagerService.ensureInternalCaregivingManagerExists(query)

            then("throws InternalCaregivingManagerNotFoundById") {
                val thrownException = shouldThrow<InternalCaregivingManagerNotFoundByIdException> { behavior() }

                thrownException.internalCaregivingManagerId shouldBe "01GVCZ7W7MAYAC6C7JMJHSNEJR"
            }
        }

        and("existing internal caregiving manager") {
            val existingInternalCaregivingManagers = listOf<InternalCaregivingManager>(
                relaxedMock(),
                relaxedMock(),
            )
            beforeEach {
                with(internalCaregivingManagerRepository) {
                    every { findByUserId("01GDYB3M58TBBXG1A0DJ1B866V") } returns existingInternalCaregivingManagers[0]
                    val pageableSlot = slot<Pageable>()
                    every { findAll(capture(pageableSlot)) } answers {
                        PageImpl(
                            existingInternalCaregivingManagers,
                            pageableSlot.captured,
                            existingInternalCaregivingManagers.size.toLong(),
                        )
                    }

                    every { findByNameContains(match { "방미영".contains(it) }, capture(pageableSlot)) } answers {
                        PageImpl(
                            listOf(
                                existingInternalCaregivingManagers[1]
                            ),
                            pageableSlot.captured,
                            1,
                        )
                    }

                    every {
                        findByUserIdIn(match { it.contains("01GDYB3M58TBBXG1A0DJ1B866V") }, capture(pageableSlot))
                    } answers {
                        PageImpl(
                            listOf(existingInternalCaregivingManagers[0]),
                            pageableSlot.captured,
                            1,
                        )
                    }

                    every {
                        findByIdOrNull("01GVCZ7W7MAYAC6C7JMJHSNEJR")
                    } returns existingInternalCaregivingManagers[0]
                }

                every {
                    usersByEmailKeywordQueryHandler.getUsers(match { "boris@caredoc.kr".contains(it.emailKeyword) })
                } returns listOf(
                    relaxedMock {
                        every { id } returns "01GDYB3M58TBBXG1A0DJ1B866V"
                    }
                )

                with(existingInternalCaregivingManagers[0]) {
                    every { get(ObjectAttribute.OWNER_ID) } returns setOf("01GDYB3M58TBBXG1A0DJ1B866V")
                }
            }

            `when`("getting internal caregiving manager by user id") {
                fun behavior() = internalCaregivingManagerService.getInternalCaregivingManager(
                    InternalCaregivingManagerByUserIdQuery(
                        userId = "01GDYB3M58TBBXG1A0DJ1B866V",
                        generateInternalCaregivingManagerSubject(),
                    )
                )

                then("returns internal caregiving manager") {
                    val actualResult = behavior()

                    actualResult shouldBe existingInternalCaregivingManagers[0]
                }
            }

            `when`("본인의 내부 관리자 정보를 조회하면") {
                fun behavior() = internalCaregivingManagerService.getInternalCaregivingManager(
                    InternalCaregivingManagerByUserIdQuery(
                        userId = "01GDYB3M58TBBXG1A0DJ1B866V",
                        generateUserSubject("01GDYB3M58TBBXG1A0DJ1B866V"),
                    )
                )

                then("returns internal caregiving manager") {
                    val actualResult = behavior()

                    actualResult shouldBe existingInternalCaregivingManagers[0]
                }
            }

            `when`("내부 관리자 권한없이 본인이 아닌 내부 관리자 정보를 조회하면") {
                fun behavior() = internalCaregivingManagerService.getInternalCaregivingManager(
                    InternalCaregivingManagerByUserIdQuery(
                        userId = "01GDYB3M58TBBXG1A0DJ1B866V",
                        subject = generateExternalCaregivingOrganizationManagerSubject("01GVMQXE9SFT6F199WG99T0R7X"),
                    )
                )

                then("AccessDeniedException을 발생시킵니다.") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }

            `when`("getting all internal caregiving managers with page request") {
                val query = GetAllInternalCaregivingManagersQuery(
                    subject = generateInternalCaregivingManagerSubject()
                )
                val pageRequest = PageRequest.of(0, 10)

                fun behavior() = internalCaregivingManagerService.getInternalCaregivingManagers(query, pageRequest)

                then("returns paged caregiving managers") {
                    val actualResult = behavior()

                    actualResult.content shouldContainAll setOf(
                        existingInternalCaregivingManagers[0],
                        existingInternalCaregivingManagers[1],
                    )
                }
            }

            `when`("getting all internal caregiving managers without internal user attribute") {
                val query = GetAllInternalCaregivingManagersQuery(
                    subject = generateExternalCaregivingOrganizationManagerSubject("01GPWXVJB2WPDNXDT5NE3B964N"),
                )
                val pageRequest = PageRequest.of(0, 10)

                fun behavior() = internalCaregivingManagerService.getInternalCaregivingManagers(query, pageRequest)

                then("throws AccessDeniedException") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }

            `when`("searching internal caregiving managers by name") {
                val query = InternalCaregivingManagersBySearchConditionQuery(
                    searchCondition = SearchCondition(
                        searchingProperty = InternalCaregivingManagersBySearchConditionQuery.SearchingProperty.NAME,
                        keyword = "방미",
                    ),
                    subject = generateInternalCaregivingManagerSubject()
                )
                val pageRequest = PageRequest.of(0, 10)

                fun behavior() = internalCaregivingManagerService.getInternalCaregivingManagers(query, pageRequest)

                then("query internal caregiving managers using name keyword") {
                    behavior()

                    verify {
                        internalCaregivingManagerRepository.findByNameContains(
                            withArg {
                                it shouldBe "방미"
                            },
                            withArg {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 10
                            },
                        )
                    }
                }

                then("returns paged search result") {
                    val actualResult = behavior()

                    actualResult.content shouldContainAll setOf(
                        existingInternalCaregivingManagers[1],
                    )
                }
            }

            `when`("searching internal caregiving managers by name without internal user attribute") {
                val query = InternalCaregivingManagersBySearchConditionQuery(
                    searchCondition = SearchCondition(
                        searchingProperty = InternalCaregivingManagersBySearchConditionQuery.SearchingProperty.NAME,
                        keyword = "방미",
                    ),
                    subject = generateExternalCaregivingOrganizationManagerSubject("01GPWXVJB2WPDNXDT5NE3B964N"),
                )
                val pageRequest = PageRequest.of(0, 10)

                fun behavior() = internalCaregivingManagerService.getInternalCaregivingManagers(query, pageRequest)

                then("throws AccessDeniedException") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }

            `when`("searching internal caregiving managers by email address") {
                val query = InternalCaregivingManagersBySearchConditionQuery(
                    searchCondition = SearchCondition(
                        searchingProperty = InternalCaregivingManagersBySearchConditionQuery.SearchingProperty.EMAIL,
                        keyword = "boris",
                    ),
                    subject = generateInternalCaregivingManagerSubject()
                )
                val pageRequest = PageRequest.of(0, 10)

                fun behavior() = internalCaregivingManagerService.getInternalCaregivingManagers(query, pageRequest)

                then("query user using email keyword") {
                    behavior()

                    verify {
                        usersByEmailKeywordQueryHandler.getUsers(
                            withArg {
                                it.emailKeyword shouldBe "boris"
                            }
                        )
                    }
                }

                then("query internal caregiving manager using user info") {
                    behavior()

                    verify {
                        internalCaregivingManagerRepository.findByUserIdIn(
                            withArg {
                                it shouldContainExactly setOf("01GDYB3M58TBBXG1A0DJ1B866V")
                            },
                            withArg {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 10
                            },
                        )
                    }
                }

                then("returns paged search result") {
                    val actualResult = behavior()

                    actualResult.content shouldContainAll setOf(
                        existingInternalCaregivingManagers[0],
                    )
                }
            }

            `when`("내부 간병 관리자의 ID로 존재 여부를 확인하면") {
                val query = InternalCaregivingManagerByIdQuery(
                    internalCaregivingManagerId = "01GVCZ7W7MAYAC6C7JMJHSNEJR",
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = internalCaregivingManagerService.ensureInternalCaregivingManagerExists(query)

                then("아무런 예외도 발생하지 않습니다.") {
                    shouldNotThrowAny { behavior() }
                }
            }

            `when`("아무런 권한이 없는 외부 간병 관리자가 내부 간병 관리자의 ID로 존재 여부를 확인하면") {
                val query = InternalCaregivingManagerByIdQuery(
                    internalCaregivingManagerId = "01GVCZ7W7MAYAC6C7JMJHSNEJR",
                    subject = generateExternalCaregivingOrganizationManagerSubject("01GVMQXE9SFT6F199WG99T0R7X"),
                )

                fun behavior() = internalCaregivingManagerService.ensureInternalCaregivingManagerExists(query)

                then("AccessDeniedException이 발생합니다.") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }

            `when`("내부 관리자를 수정하면") {
                val subject = generateInternalCaregivingManagerSubject()
                val internalCaregivingManagerId = "01GVCZ7W7MAYAC6C7JMJHSNEJR"
                val targetUserId = "01GY7D74K8H7XM5P5F5SSQK1D5"

                val suspended = Patches.ofValue(true)
                val email = Patches.ofValue("eddy@caredoc.kr")
                val internalCaregivingManagerByIdQuery = InternalCaregivingManagerByIdQuery(
                    internalCaregivingManagerId = internalCaregivingManagerId,
                    subject = generateInternalCaregivingManagerSubject(),
                )
                val internalCaregivingManagerEditingCommand = InternalCaregivingManagerEditingCommand(
                    suspended = suspended,
                    email = email,
                    subject = subject,
                )

                val userByIdQuery = UserByIdQuery(
                    userId = targetUserId,
                )

                val userEditingCommand = UserEditingCommand(
                    suspended = suspended,
                    email = email,
                    subject = subject,
                )

                fun behavior() = internalCaregivingManagerService.editInternalCaregivingManager(
                    query = internalCaregivingManagerByIdQuery,
                    command = internalCaregivingManagerEditingCommand,
                )

                beforeEach {
                    every { userEditingCommandHandler.editUser(userByIdQuery, userEditingCommand) } returns Unit
                    every { internalCaregivingManagerRepository.findByIdOrNull(match { it == internalCaregivingManagerId }) } returns relaxedMock {
                        every { id } returns internalCaregivingManagerId
                        every { userId } returns targetUserId
                    }
                }

                afterEach { clearAllMocks() }

                then("대상이 되는 내부 관리자의 정보를 조회합니다.") {
                    behavior()

                    verify {
                        internalCaregivingManagerRepository.findByIdOrNull(
                            withArg {
                                it shouldBe internalCaregivingManagerId
                            }
                        )
                    }
                }

                then("대상이 되는 내부 관리자의 유저 정보 수정을 요청합니다.") {
                    behavior()

                    verify {
                        userEditingCommandHandler.editUser(
                            withArg {
                                it.userId shouldBe targetUserId
                            },
                            withArg {
                                it.suspended shouldBe suspended
                                it.email shouldBe email
                            }
                        )
                    }
                }
            }
        }
    }
})
