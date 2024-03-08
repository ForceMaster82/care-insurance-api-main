package kr.caredoc.careinsurance.user

import com.github.guepardoapps.kulid.ULID
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import kr.caredoc.careinsurance.generateExternalCaregivingOrganizationManagerSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.generateUserSubject
import kr.caredoc.careinsurance.patch.Patches
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.search.SearchCondition
import kr.caredoc.careinsurance.security.accesscontrol.ObjectAttribute
import kr.caredoc.careinsurance.user.exception.AlreadyExistsUserEmailAddressException
import kr.caredoc.careinsurance.user.exception.ReferenceExternalCaregivingManagerNotExistsException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.AccessDeniedException

class ExternalCaregivingManagerServiceTest : BehaviorSpec({
    given("external caregiving manager service") {
        val externalCaregivingManagerRepository = relaxedMock<ExternalCaregivingManagerRepository>()
        val userCreationCommandHandler = relaxedMock<UserCreationCommandHandler>()
        val userEditingCommandHandler = relaxedMock<UserEditingCommandHandler>()
        val eventPublisher = relaxedMock<ApplicationEventPublisher>()
        val externalCaregivingManagerService = ExternalCaregivingManagerService(
            externalCaregivingManagerRepository,
            userCreationCommandHandler,
            userEditingCommandHandler,
            eventPublisher,
        )

        beforeEach {
            mockkObject(ULID)
            every { ULID.random() } returns "01GRTXJMJQPT5VW5796PVK6KWC"

            with(externalCaregivingManagerRepository) {
                val slot = slot<ExternalCaregivingManager>()
                every { save(capture(slot)) } answers {
                    slot.captured
                }
            }
        }

        afterEach { clearAllMocks() }

        `when`("created external caregiving manager") {
            beforeEach {
                every { externalCaregivingManagerRepository.findByEmail(any()) } returns null
                every { userCreationCommandHandler.createUser(any()) } returns relaxedMock {
                    every { createdUserId } returns "01GS9ZS7NXXHEX1ST1B5NKTBQD"
                }
            }
            afterEach { clearAllMocks() }
            val subject = generateInternalCaregivingManagerSubject()
            val externalCaregivingManagerId = "01GRTXJMJQPT5VW5796PVK6KWC"
            val command = ExternalCaregivingManagerCreationCommand(
                externalCaregivingOrganizationId = "01GRTYNAMXGDH9D3BRVQHWK467",
                email = "eddy@caredoc.kr",
                name = "eddy",
                phoneNumber = "01012345678",
                remarks = "잘 부탁드립니다.",
                subject = subject,
            )
            val createdUserId = "01GS9ZS7NXXHEX1ST1B5NKTBQD"

            fun behavior() = externalCaregivingManagerService.createExternalCaregivingManager(command)

            then("check save of user") {
                behavior()

                verify(exactly = 1) {
                    userCreationCommandHandler.createUser(
                        withArg {
                            it.name shouldBe command.name
                            it.emailAddressForLogin shouldBe command.email
                        }
                    )
                }
            }
            then("check save of external caregiving manager") {
                behavior()

                verify {
                    externalCaregivingManagerRepository.save(
                        withArg {
                            it.id shouldBe externalCaregivingManagerId
                            it.email shouldBe command.email
                            it.name shouldBe command.name
                            it.phoneNumber shouldBe command.phoneNumber
                            it.remarks shouldBe command.remarks
                            it.externalCaregivingOrganizationId shouldBe command.externalCaregivingOrganizationId
                            it.userId shouldBe createdUserId
                        }
                    )
                }
            }
            then("check created external caregiving manager id") {
                val result = behavior()

                result.externalCaregivingManagerId shouldBe "01GRTXJMJQPT5VW5796PVK6KWC"
            }
        }
        and("but already exists external caregiving manager email") {
            beforeEach {
                every { userCreationCommandHandler.createUser(any()) } throws AlreadyExistsUserEmailAddressException("eddy@caredoc.kr")
            }

            afterEach { clearAllMocks() }
            val subject = generateInternalCaregivingManagerSubject()
            val command = ExternalCaregivingManagerCreationCommand(
                externalCaregivingOrganizationId = "01GRTYNAMXGDH9D3BRVQHWK467",
                email = "eddy@caredoc.kr",
                name = "eddy",
                phoneNumber = "01012345678",
                remarks = "잘 부탁드립니다.",
                subject = subject,
            )

            then("AlreadyExistsUserEmailAddressException 발생합니다.") {
                val exception = shouldThrow<AlreadyExistsUserEmailAddressException> {
                    externalCaregivingManagerService.createExternalCaregivingManager(command)
                }

                exception.emailAddress shouldBe "eddy@caredoc.kr"
            }
        }

        `when`("external caregiving manager find by userId") {
            beforeEach {
                every { externalCaregivingManagerRepository.findByUserId(any()) } returns relaxedMock {
                    every { userId } returns "01GSF49ZGME4ZC15KVAD8317MQ"
                }
            }

            afterEach { clearAllMocks() }
            val subject = generateInternalCaregivingManagerSubject()
            val userId = "01GSF49ZGME4ZC15KVAD8317MQ"
            val query = ExternalCaregivingManagerByUserIdQuery(
                userId,
                subject
            )

            fun behavior() = externalCaregivingManagerService.getExternalCaregivingManager(query)

            then("check get external caregiving manager user id") {
                val result = behavior()

                result.userId shouldBe userId
            }
        }

        `when`("본인의 외부 관리자 정보를 조회하면") {
            beforeEach {
                every { externalCaregivingManagerRepository.findByUserId("01GSF49ZGME4ZC15KVAD8317MQ") } returns relaxedMock {
                    every { userId } returns "01GSF49ZGME4ZC15KVAD8317MQ"
                    every { get(ObjectAttribute.OWNER_ID) } returns setOf("01GSF49ZGME4ZC15KVAD8317MQ")
                }
            }

            afterEach { clearAllMocks() }
            val subject = generateUserSubject("01GSF49ZGME4ZC15KVAD8317MQ")
            val userId = "01GSF49ZGME4ZC15KVAD8317MQ"
            val query = ExternalCaregivingManagerByUserIdQuery(
                userId,
                subject
            )

            fun behavior() = externalCaregivingManagerService.getExternalCaregivingManager(query)

            then("외부 관리자를 반환합니다.") {
                val result = behavior()

                result.userId shouldBe userId
            }
        }

        `when`("본인이 속한 단체의 외부 관리자 정보를 조회하면") {
            beforeEach {
                every { externalCaregivingManagerRepository.findByUserId("01GSF49ZGME4ZC15KVAD8317MQ") } returns relaxedMock {
                    every { userId } returns "01GSF49ZGME4ZC15KVAD8317MQ"
                    every { get(ObjectAttribute.OWNER_ID) } returns setOf("01GSF49ZGME4ZC15KVAD8317MQ")
                    every { get(ObjectAttribute.BELONGING_ORGANIZATION_ID) } returns setOf("01GPWXVJB2WPDNXDT5NE3B964N")
                }
            }

            afterEach { clearAllMocks() }
            val subject = generateExternalCaregivingOrganizationManagerSubject("01GPWXVJB2WPDNXDT5NE3B964N")
            val userId = "01GSF49ZGME4ZC15KVAD8317MQ"
            val query = ExternalCaregivingManagerByUserIdQuery(
                userId,
                subject
            )

            fun behavior() = externalCaregivingManagerService.getExternalCaregivingManager(query)

            then("외부 관리자를 반환합니다.") {
                val result = behavior()

                result.userId shouldBe userId
            }
        }

        `when`("아무 관련 없는 사용자가 외부 관리자 정보를 조회하면") {
            beforeEach {
                every { externalCaregivingManagerRepository.findByUserId("01GSF49ZGME4ZC15KVAD8317MQ") } returns relaxedMock {
                    every { userId } returns "01GSF49ZGME4ZC15KVAD8317MQ"
                    every { get(ObjectAttribute.OWNER_ID) } returns setOf("01GSF49ZGME4ZC15KVAD8317MQ")
                    every { get(ObjectAttribute.BELONGING_ORGANIZATION_ID) } returns setOf("01GPWXVJB2WPDNXDT5NE3B964N")
                }
            }

            afterEach { clearAllMocks() }
            val subject = generateExternalCaregivingOrganizationManagerSubject("01GVMQXE9SFT6F199WG99T0R7X")
            val userId = "01GSF49ZGME4ZC15KVAD8317MQ"
            val query = ExternalCaregivingManagerByUserIdQuery(
                userId,
                subject
            )

            fun behavior() = externalCaregivingManagerService.getExternalCaregivingManager(query)

            then("AccessDeniedException이 발생합니다.") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }

        and("이미 external caregiving manager 가 등록되어 있을때") {
            beforeEach {
                every { externalCaregivingManagerRepository.findByIdOrNull(any()) } returns relaxedMock {
                    every { id } returns "01GSVDEYJ7TD9P853GBT6CWZ0J"
                    every { email } returns "eddy@caredoc.kr"
                    every { name } returns "eddy"
                    every { phoneNumber } returns "01012345678"
                    every { remarks } returns null
                    every { externalCaregivingOrganizationId } returns "01GSVHRATV7W5SMKP27XZYFJ0P"
                }
            }

            afterEach { clearAllMocks() }

            `when`("external caregiving manager 의 상세 정보를 조회할때") {
                val subject = generateInternalCaregivingManagerSubject()
                val externalCaregivingManagerId = "01GSVDEYJ7TD9P853GBT6CWZ0J"
                fun behavior() = externalCaregivingManagerService.getExternalCaregivingManager(
                    ExternalCaregivingManagerByIdQuery(
                        externalCaregivingManagerId = externalCaregivingManagerId,
                        subject = subject,
                    )
                )
                then("리포지토리에서 조회된 extenral caregiving manager를 반환합니다.") {
                    val result = behavior()

                    result.id shouldBe externalCaregivingManagerId
                }
            }
            `when`("external caregiving manager 를 수정할때") {
                val subject = generateInternalCaregivingManagerSubject()
                val externalCaregivingManagerId = "01GT3P0201DP6JAH0P1NYBBBCJ"
                val queryByExternalCaregivingManagerId = ExternalCaregivingManagerByIdQuery(
                    externalCaregivingManagerId = externalCaregivingManagerId,
                    subject = subject
                )
                val editExternalCaregivingManagerCommand = ExternalCaregivingManagerEditCommand(
                    email = Patches.ofValue("jerry@caredoc.kr"),
                    name = Patches.ofValue("jerry"),
                    phoneNumber = Patches.ofValue("01012345678"),
                    remarks = Patches.ofValue("수정 해주세요"),
                    suspended = Patches.ofValue(false),
                    externalCaregivingOrganizationId = Patches.ofValue("01GT3P4654ZH7XA9255XSEV08G"),
                    subject = subject,
                )

                val targetUserId = "01GT3P5NKV424YJX9FN0PJ9QK3"
                val userByIdQuery = UserByIdQuery(
                    userId = targetUserId
                )
                val userEditingCommand = UserEditingCommand(
                    email = editExternalCaregivingManagerCommand.email,
                    name = editExternalCaregivingManagerCommand.name,
                    suspended = editExternalCaregivingManagerCommand.suspended,
                    subject = subject,
                )

                lateinit var externalCaregivingManager: ExternalCaregivingManager
                beforeEach {
                    externalCaregivingManager = relaxedMock {
                        every { id } returns externalCaregivingManagerId
                        every { email } returns "eddy@caredoc.kr"
                        every { name } returns "eddy"
                        every { phoneNumber } returns "01012345678"
                        every { remarks } returns "잛 부탁드립니다."
                        every { externalCaregivingOrganizationId } returns "01GT3P4654ZH7XA9255XSEV08G"
                        every { userId } returns targetUserId
                    }
                    every {
                        externalCaregivingManagerService.getExternalCaregivingManager(
                            queryByExternalCaregivingManagerId
                        )
                    } returns externalCaregivingManager
                }

                afterEach { clearAllMocks() }

                fun behavior() = externalCaregivingManagerService.editExternalCaregivingManager(
                    queryByExternalCaregivingManagerId,
                    editExternalCaregivingManagerCommand
                )
                then("입력받은 external caregiving manager id로 쿼리합니다.") {
                    behavior()

                    verify(exactly = 1) {
                        externalCaregivingManagerService.getExternalCaregivingManager(queryByExternalCaregivingManagerId)
                    }
                }
                then("external caregiving manager 를 입력받은 커맨드로 수정합니다.") {
                    behavior()

                    verify(exactly = 1) {
                        userEditingCommandHandler.editUser(userByIdQuery, userEditingCommand)
                    }
                }
                then("external caregiving manager 에 수정을 요청합니다.") {
                    behavior()

                    verify(exactly = 1) {
                        externalCaregivingManager.edit(
                            withArg {
                                it.subject shouldBe subject
                                it.email shouldBe editExternalCaregivingManagerCommand.email
                                it.name shouldBe editExternalCaregivingManagerCommand.name
                                it.phoneNumber shouldBe editExternalCaregivingManagerCommand.phoneNumber
                                it.remarks shouldBe editExternalCaregivingManagerCommand.remarks
                                it.suspended shouldBe editExternalCaregivingManagerCommand.suspended
                                it.externalCaregivingOrganizationId shouldBe editExternalCaregivingManagerCommand.externalCaregivingOrganizationId
                            }
                        )
                    }
                    verify(exactly = 1) {
                        userEditingCommandHandler.editUser(
                            withArg {
                                it.userId shouldBe targetUserId
                            },
                            withArg {
                                it.suspended shouldBe editExternalCaregivingManagerCommand.suspended
                            }
                        )
                    }
                }
            }
            `when`("external caregiving manager 들의 사용 여부를 수정할 때") {
                lateinit var externalCaregivingManagerList: List<ExternalCaregivingManager>
                beforeEach {
                    externalCaregivingManagerList = listOf(
                        relaxedMock<ExternalCaregivingManager> {
                            every { id } returns "01GTP5FJQK1GYRWHAY5H9XW42D"
                            every { email } returns "eddy@caredoc.kr"
                            every { name } returns "eddy"
                            every { phoneNumber } returns "01012345678"
                            every { remarks } returns "잛 부탁드립니다."
                            every { externalCaregivingOrganizationId } returns "01GT3P4654ZH7XA9255XSEV08G"
                            every { userId } returns "01GTP65GTXXYNJ2Z6GPVBB9B9B"
                        },
                        relaxedMock<ExternalCaregivingManager> {
                            every { id } returns "01GTP5FR58RZRGV393C6CAT88K"
                            every { email } returns "second@caredoc.kr"
                            every { name } returns "second"
                            every { phoneNumber } returns "01022222222"
                            every { externalCaregivingOrganizationId } returns "01GTP67D8VE0GF73S7M7XV7SEP"
                            every { userId } returns "01GTP67H4M9ZFCX0YP2WDDTTTA"
                        },
                        relaxedMock<ExternalCaregivingManager> {
                            every { id } returns "01GTP5FZC5TNXRY3CRR9HVZ1MB"
                            every { email } returns "third@caredoc.kr"
                            every { name } returns "third"
                            every { phoneNumber } returns "01033333333"
                            every { externalCaregivingOrganizationId } returns "01GTP67YMYSFZZEGJJQZQ9S4C5"
                            every { userId } returns "01GTP68470FR53DCSTVV55GFME"
                        },
                        relaxedMock<ExternalCaregivingManager> {
                            every { id } returns "01GTP5G4KPDVYRSZTTH8GQY3VB"
                            every { email } returns "fourth@caredoc.kr"
                            every { name } returns "fourth"
                            every { phoneNumber } returns "01044444444"
                            every { externalCaregivingOrganizationId } returns "01GTP687PCT0C7CAE6B6EZYKAW"
                            every { userId } returns "01GTP68HZD382MYG4RGDX57Y03"
                        },
                    )
                    every { externalCaregivingManagerRepository.findByIdIn(any()) } returns externalCaregivingManagerList
                }

                afterEach { clearAllMocks() }

                val subject = generateInternalCaregivingManagerSubject()
                val command = mapOf(
                    "01GTP5FJQK1GYRWHAY5H9XW42D" to ExternalCaregivingManagerEditCommand(
                        suspended = Patches.ofValue(false),
                        subject = subject
                    ),
                    "01GTP5FR58RZRGV393C6CAT88K" to ExternalCaregivingManagerEditCommand(
                        suspended = Patches.ofValue(true),
                        subject = subject
                    ),
                    "01GTP5FZC5TNXRY3CRR9HVZ1MB" to ExternalCaregivingManagerEditCommand(
                        suspended = Patches.ofValue(false),
                        subject = subject
                    ),
                    "01GTP5G4KPDVYRSZTTH8GQY3VB" to ExternalCaregivingManagerEditCommand(
                        suspended = Patches.ofValue(false),
                        subject = subject
                    ),
                )

                fun behavior() = externalCaregivingManagerService.editExternalCaregivingManagers(command)
                then("external caregiving manager 들의 id 를 기준을로 조회합니다.") {
                    behavior()

                    verify(exactly = 1) {
                        externalCaregivingManagerRepository.findByIdIn(
                            withArg {
                                listOf(
                                    "01GTRCF22AY8481YS5DG1APMGH",
                                    "01GTP5FR58RZRGV393C6CAT88K",
                                    "01GTP5FZC5TNXRY3CRR9HVZ1MB",
                                    "01GTP5G4KPDVYRSZTTH8GQY3VB",
                                )
                            }
                        )
                    }
                }
                then("external caregiving manager 들 user 의 suspended 수정을 요청합니다.") {
                    behavior()

                    verify {
                        externalCaregivingManagerList.forEach { externalCaregivingManager ->
                            val target = command[externalCaregivingManager.id] ?: return@forEach
                            userEditingCommandHandler.editUser(
                                withArg {
                                    it.userId shouldBe Patches.ofValue(externalCaregivingManager.userId)
                                },
                                withArg {
                                    it.suspended shouldBe target.suspended
                                }
                            )
                        }
                    }
                }
                then("external caregiving manager 들의 수정을 요청합니다.") {
                    behavior()

                    verify {
                        externalCaregivingManagerList.forEach { externalCaregivingManager ->
                            val target = command[externalCaregivingManager.id] ?: return@forEach
                            externalCaregivingManager.edit(
                                withArg {
                                    it.subject shouldBe subject
                                    it.email shouldBe Patches.ofValue(target.email)
                                    it.name shouldBe Patches.ofValue(target.name)
                                    it.phoneNumber shouldBe Patches.ofValue(target.phoneNumber)
                                    it.remarks shouldBe Patches.ofValue(target.remarks)
                                    it.suspended shouldBe Patches.ofValue(target.suspended)
                                    it.externalCaregivingOrganizationId shouldBe Patches.ofValue(target.externalCaregivingOrganizationId)
                                }
                            )
                        }
                    }
                }
            }
            `when`("아무런 조건이 없이 external caregiving manager 목록을 조회할 때") {
                val subject = generateInternalCaregivingManagerSubject()
                val pageRequest = PageRequest.of(0, 2)

                fun behavior() = externalCaregivingManagerService.getExternalCaregivingManagers(
                    ExternalCaregivingManagersByFilterQuery(
                        externalCaregivingOrganizationId = null,
                        searchQuery = null,
                        subject = subject,
                    ),
                    pageRequest = pageRequest
                )

                then("모든 external caregiving manager 목록 조회를 요청합니다.") {
                    behavior()

                    verify(exactly = 1) {
                        externalCaregivingManagerRepository.findAll(
                            withArg<PageRequest> {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 2
                                it.sort shouldBe Sort.by(Direction.DESC, "id")
                            }
                        )
                    }
                }
            }
            `when`("external caregiving organization id를 조건으로 external caregiving manager 목록을 조회할 때") {
                val subject = generateInternalCaregivingManagerSubject()
                val pageRequest = PageRequest.of(0, 2)
                val externalCaregivingOrganizationId = "01GVCY8Y0BA3JD1DQGNXA5XCC7"

                fun behavior() = externalCaregivingManagerService.getExternalCaregivingManagers(
                    ExternalCaregivingManagersByFilterQuery(
                        externalCaregivingOrganizationId = externalCaregivingOrganizationId,
                        searchQuery = null,
                        subject = subject,
                    ),
                    pageRequest = pageRequest,
                )

                then("external caregiving organization id를 포함하는 목록 조회를 요청합니다.") {
                    behavior()

                    verify(exactly = 1) {
                        externalCaregivingManagerRepository.searchExternalCaregivingManagers(
                            withArg<ExternalCaregivingManagerSearchingRepository.SearchingCriteria> {
                                it.name shouldBe null
                                it.email shouldBe null
                                it.externalCaregivingOrganizationId shouldBe externalCaregivingOrganizationId
                            },
                            withArg<Pageable> {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 2
                            }
                        )
                    }
                }
            }
            `when`("email 이 query 조건으로 external caregiving manager 목록을 조회할 때") {
                val subject = generateInternalCaregivingManagerSubject()
                val pageRequest = PageRequest.of(0, 2)
                val keyword = "ed"

                fun behavior() = externalCaregivingManagerService.getExternalCaregivingManagers(
                    ExternalCaregivingManagersByFilterQuery(
                        externalCaregivingOrganizationId = null,
                        searchQuery = SearchCondition(
                            searchingProperty = ExternalCaregivingManagersByFilterQuery.SearchingProperty.EMAIL,
                            keyword = keyword,
                        ),
                        subject = subject,
                    ),
                    pageRequest = pageRequest
                )
                then("입력받은 email 을 포함하는 목록 조회를 요청합니다.") {
                    behavior()

                    verify(exactly = 1) {
                        externalCaregivingManagerRepository.searchExternalCaregivingManagers(
                            withArg<ExternalCaregivingManagerSearchingRepository.SearchingCriteria> {
                                it.name shouldBe null
                                it.email shouldBe keyword
                                it.externalCaregivingOrganizationId shouldBe null
                            },
                            withArg<Pageable> {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 2
                            }
                        )
                    }
                }
            }
            `when`("name 이 query 조건으로 external caregiving manager 목록을 조회할 때") {
                val subject = generateInternalCaregivingManagerSubject()
                val pageRequest = PageRequest.of(0, 2)
                val keyword = "ed"

                fun behavior() = externalCaregivingManagerService.getExternalCaregivingManagers(
                    ExternalCaregivingManagersByFilterQuery(
                        externalCaregivingOrganizationId = null,
                        searchQuery = SearchCondition(
                            searchingProperty = ExternalCaregivingManagersByFilterQuery.SearchingProperty.NAME,
                            keyword = keyword,
                        ),
                        subject = subject,
                    ),
                    pageRequest = pageRequest,
                )
                then("입력받은 name 을 포함하는 목록 조회를 요청합니다.") {
                    behavior()

                    verify(exactly = 1) {
                        externalCaregivingManagerRepository.searchExternalCaregivingManagers(
                            withArg<ExternalCaregivingManagerSearchingRepository.SearchingCriteria> {
                                it.name shouldBe keyword
                                it.email shouldBe null
                                it.externalCaregivingOrganizationId shouldBe null
                            },
                            withArg<Pageable> {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 2
                            }
                        )
                    }
                }
            }
            `when`("external caregiving organization id 와 email 이 query 조건으로 external caregiving manager 목록을 조회할 때") {
                val subject = generateInternalCaregivingManagerSubject()
                val pageRequest = PageRequest.of(0, 2)
                val externalCaregivingOrganizationId = "01GVCY8Y0BA3JD1DQGNXA5XCC7"
                val keyword = "ed"

                fun behavior() = externalCaregivingManagerService.getExternalCaregivingManagers(
                    ExternalCaregivingManagersByFilterQuery(
                        externalCaregivingOrganizationId = externalCaregivingOrganizationId,
                        searchQuery = SearchCondition(
                            searchingProperty = ExternalCaregivingManagersByFilterQuery.SearchingProperty.EMAIL,
                            keyword = keyword,
                        ),
                        subject = subject,
                    ),
                    pageRequest = pageRequest,
                )
                then("external caregiving organization id가 일치하고 입력받은 email 을 포함한 목록 조회를 요청합니다.") {
                    behavior()

                    verify(exactly = 1) {
                        externalCaregivingManagerRepository.searchExternalCaregivingManagers(
                            withArg<ExternalCaregivingManagerSearchingRepository.SearchingCriteria> {
                                it.name shouldBe null
                                it.email shouldBe keyword
                                it.externalCaregivingOrganizationId shouldBe externalCaregivingOrganizationId
                            },
                            withArg<Pageable> {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 2
                            }
                        )
                    }
                }
            }
            `when`("external caregiving organization id 와 name 이 query 조건으로 external caregiving manager 목록을 조회할 때") {
                val subject = generateInternalCaregivingManagerSubject()
                val pageRequest = PageRequest.of(0, 2)
                val keyword = "ed"
                val externalCaregivingOrganizationId = "01GVCY8Y0BA3JD1DQGNXA5XCC7"

                fun behavior() = externalCaregivingManagerService.getExternalCaregivingManagers(
                    ExternalCaregivingManagersByFilterQuery(
                        externalCaregivingOrganizationId = externalCaregivingOrganizationId,
                        searchQuery = SearchCondition(
                            searchingProperty = ExternalCaregivingManagersByFilterQuery.SearchingProperty.NAME,
                            keyword = keyword,
                        ),
                        subject = subject,
                    ),
                    pageRequest = pageRequest,
                )
                then("external caregiving organization id가 일치하고 입력받은 name 을 포함한 목록 조회를 요청합니다.") {
                    behavior()

                    verify(exactly = 1) {
                        externalCaregivingManagerRepository.searchExternalCaregivingManagers(
                            withArg<ExternalCaregivingManagerSearchingRepository.SearchingCriteria> {
                                it.name shouldBe keyword
                                it.email shouldBe null
                                it.externalCaregivingOrganizationId shouldBe externalCaregivingOrganizationId
                            },
                            withArg<Pageable> {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 2
                            }
                        )
                    }
                }
            }
        }

        and("external caregiving manager가 등록되어 있지 않을때") {
            beforeEach {
                every { externalCaregivingManagerRepository.findByIdOrNull(any()) } returns null
            }

            afterEach { clearAllMocks() }

            `when`("external caregiving manager 를 조회합니다.") {
                val subject = generateInternalCaregivingManagerSubject()
                val externalCaregivingManagerId = "01GSVDEYJ7TD9P853GBT6CWZ0J"
                fun behavior() = externalCaregivingManagerService.getExternalCaregivingManager(
                    ExternalCaregivingManagerByIdQuery(
                        externalCaregivingManagerId = externalCaregivingManagerId,
                        subject = subject,
                    )
                )
                then("ExternalCaregivingManagerNotExistsException 이 발생합니다.") {
                    val exception = shouldThrow<ExternalCaregivingManagerNotExistsException> {
                        behavior()
                    }
                    exception.externalCaregivingManagerId shouldBe externalCaregivingManagerId
                }
            }
            `when`("external caregiving manager 를 수정합니다.") {
                val externalCaregivingManagerId = "01GTBSX2AYZXS3SK11DFRKTGEA"
                val subject = generateInternalCaregivingManagerSubject()

                beforeEach {
                    every {
                        externalCaregivingManagerService.getExternalCaregivingManager(
                            ExternalCaregivingManagerByIdQuery(
                                externalCaregivingManagerId = externalCaregivingManagerId,
                                subject = subject,
                            )
                        )
                    } throws ExternalCaregivingManagerNotExistsException(externalCaregivingManagerId)
                }

                afterEach { clearAllMocks() }

                fun behavior() = externalCaregivingManagerService.editExternalCaregivingManager(
                    ExternalCaregivingManagerByIdQuery(
                        externalCaregivingManagerId = externalCaregivingManagerId,
                        subject = subject,
                    ),
                    ExternalCaregivingManagerEditCommand(
                        email = Patches.ofValue("jerry@caredoc.kr"),
                        name = Patches.ofValue("jerry"),
                        phoneNumber = Patches.ofValue("01012345678"),
                        remarks = Patches.ofValue("수정 해주세요"),
                        suspended = Patches.ofValue(false),
                        externalCaregivingOrganizationId = Patches.ofValue("01GSVDEYJ7TD9P853GBT6CWZ0J"),
                        subject = subject,
                    ),
                )
                then("ExternalCaregivingManagerNotExistsException 이 발생합니다.") {
                    val exception = shouldThrow<ExternalCaregivingManagerNotExistsException> {
                        behavior()
                    }
                    exception.externalCaregivingManagerId shouldBe externalCaregivingManagerId
                }
            }
            `when`("external caregiving manager 들의 사용 여부를 수정할 합니다") {
                val subject = generateInternalCaregivingManagerSubject()
                val command = mapOf(
                    "01GTP5FJQK1GYRWHAY5H9XW42D" to ExternalCaregivingManagerEditCommand(
                        suspended = Patches.ofValue(true),
                        subject = subject,
                    ),
                    "01GTP5FR58RZRGV393C6CAT88K" to ExternalCaregivingManagerEditCommand(
                        suspended = Patches.ofValue(false),
                        subject = subject,
                    ),
                    "01GTP5FZC5TNXRY3CRR9HVZ1MB" to ExternalCaregivingManagerEditCommand(
                        suspended = Patches.ofValue(false),
                        subject = subject,
                    ),
                    "01GTP5G4KPDVYRSZTTH8GQY3VB" to ExternalCaregivingManagerEditCommand(
                        suspended = Patches.ofValue(true),
                        subject = subject,
                    ),
                )

                beforeEach {
                    val externalCaregivingManagerList = listOf(
                        ExternalCaregivingManager(
                            id = "01GTP5FJQK1GYRWHAY5H9XW42D",
                            email = "first@caredoc.kr",
                            name = "first",
                            phoneNumber = "01011111111",
                            externalCaregivingOrganizationId = "01GTP65BGCT2NS8Q95J004EZ8M",
                            userId = "01GTP65GTXXYNJ2Z6GPVBB9B9B"
                        ),
                        ExternalCaregivingManager(
                            id = "01GTP5FR58RZRGV393C6CAT88K",
                            email = "second@caredoc.kr",
                            name = "second",
                            phoneNumber = "01022222222",
                            externalCaregivingOrganizationId = "01GTP67D8VE0GF73S7M7XV7SEP",
                            userId = "01GTP67H4M9ZFCX0YP2WDDTTTA"
                        ),
                        ExternalCaregivingManager(
                            id = "01GTP5FZC5TNXRY3CRR9HVZ1MB",
                            email = "third@caredoc.kr",
                            name = "third",
                            phoneNumber = "01033333333",
                            externalCaregivingOrganizationId = "01GTP67YMYSFZZEGJJQZQ9S4C5",
                            userId = "01GTP68470FR53DCSTVV55GFME"
                        ),
                    )
                    every { externalCaregivingManagerRepository.findByIdIn(any()) } returns externalCaregivingManagerList
                }

                fun behavior() = externalCaregivingManagerService.editExternalCaregivingManagers(command)
                then("ReferenceExternalCaregivingManagerNotExistsException 이 발생합니다.") {
                    val exception = shouldThrow<ReferenceExternalCaregivingManagerNotExistsException> {
                        behavior()
                    }
                    exception.enteredExternalCaregivingManagerId shouldBe "01GTP5G4KPDVYRSZTTH8GQY3VB"
                }
            }
        }
    }
})
