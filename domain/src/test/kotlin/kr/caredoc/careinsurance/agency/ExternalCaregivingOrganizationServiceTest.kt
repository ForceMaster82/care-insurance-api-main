package kr.caredoc.careinsurance.agency

import com.github.guepardoapps.kulid.ULID
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.floats.shouldBeBetween
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import kr.caredoc.careinsurance.account.AccountInfo
import kr.caredoc.careinsurance.file.FileByUrlQueryHandler
import kr.caredoc.careinsurance.file.FileSavingCommandHandler
import kr.caredoc.careinsurance.file.FileSavingResult
import kr.caredoc.careinsurance.generateExternalCaregivingOrganizationManagerSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.search.SearchCondition
import kr.caredoc.careinsurance.security.accesscontrol.ObjectAttribute
import kr.caredoc.careinsurance.security.accesscontrol.SystemUser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import java.io.FileInputStream

@SpringBootTest
class ExternalCaregivingOrganizationServiceTest(
    @Autowired
    private val cacheExternalCaregivingOrganizationRepository: ExternalCaregivingOrganizationRepository,
) : BehaviorSpec({
    given("external caregiving organization service") {
        val fileSavingCommandHandler = relaxedMock<FileSavingCommandHandler>()
        val fileByUrlQueryHandler = relaxedMock<FileByUrlQueryHandler>()
        val externalCaregivingOrganizationRepository = relaxedMock<ExternalCaregivingOrganizationRepository>()
        val externalCaregivingOrganizationService = ExternalCaregivingOrganizationService(
            fileSavingCommandHandler,
            fileByUrlQueryHandler,
            externalCaregivingOrganizationRepository,
            businessLicenseBucket = "careinsurance-business-license-dev"
        )

        beforeEach {
            val externalCaregivingOrganizationSlot = slot<ExternalCaregivingOrganization>()
            every {
                externalCaregivingOrganizationRepository.save(capture(externalCaregivingOrganizationSlot))
            } answers {
                externalCaregivingOrganizationSlot.captured
            }
        }

        afterEach { clearAllMocks() }

        `when`("creating external caregiving organization") {
            val createdExternalCaregivingOrganizationId = "01GPWXVJB2WPDNXDT5NE3B964N"
            val subject = generateInternalCaregivingManagerSubject()
            val accountInfo = AccountInfo(
                bank = "국민은행",
                accountNumber = "085300-04-111424",
                accountHolder = "박한결",
            )
            val command = ExternalCaregivingOrganizationCreationCommand(
                name = "케어라인",
                externalCaregivingOrganizationType = ExternalCaregivingOrganizationType.ORGANIZATION,
                address = "서울시 강남구 삼성동 109-1",
                contractName = "김라인",
                phoneNumber = "010-4026-1111",
                profitAllocationRatio = 0.6f,
                accountInfo = accountInfo,
                subject = subject,
            )

            fun behavior() = externalCaregivingOrganizationService.createExternalCaregivingOrganization(command)

            beforeEach {
                mockkObject(ULID)
                every { ULID.random() } returns "01GPWXVJB2WPDNXDT5NE3B964N"
            }

            afterEach { clearAllMocks() }

            then("persist external caregiving organization entity") {
                behavior()

                verify {
                    externalCaregivingOrganizationRepository.save(
                        withArg {
                            it.id shouldBe createdExternalCaregivingOrganizationId
                            it.name shouldBe "케어라인"
                            it.externalCaregivingOrganizationType shouldBe ExternalCaregivingOrganizationType.ORGANIZATION
                            it.address shouldBe "서울시 강남구 삼성동 109-1"
                            it.contractName shouldBe "김라인"
                            it.phoneNumber shouldBe "010-4026-1111"
                            it.profitAllocationRatio.shouldBeBetween(0.5999f, 0.6001f, 0.0001f)
                            it.accountInfo?.bank shouldBe "국민은행"
                            it.accountInfo?.accountNumber shouldBe "085300-04-111424"
                            it.accountInfo?.accountHolder shouldBe "박한결"
                        }
                    )
                }
            }

            then("returns creation result") {
                val actualResult = behavior()
                actualResult.createdExternalCaregivingOrganizationId shouldBe "01GPWXVJB2WPDNXDT5NE3B964N"
            }
        }

        `when`("creating external caregiving organization account info is null ") {
            val createdExternalCaregivingOrganizationId = "01GPWXVJB2WPDNXDT5NE3B964N"
            val subject = generateInternalCaregivingManagerSubject()

            val command = ExternalCaregivingOrganizationCreationCommand(
                name = "케어라인",
                externalCaregivingOrganizationType = ExternalCaregivingOrganizationType.ORGANIZATION,
                address = "서울시 강남구 삼성동 109-1",
                contractName = "김라인",
                phoneNumber = "010-4026-1111",
                profitAllocationRatio = 0.6f,
                accountInfo = AccountInfo(
                    bank = null,
                    accountNumber = null,
                    accountHolder = null,
                ),
                subject = subject,
            )

            fun behavior() = externalCaregivingOrganizationService.createExternalCaregivingOrganization(command)

            beforeEach {
                mockkObject(ULID)
                every { ULID.random() } returns "01GPWXVJB2WPDNXDT5NE3B964N"
            }

            afterEach { clearAllMocks() }

            then("persist external caregiving organization entity") {
                behavior()

                verify {
                    externalCaregivingOrganizationRepository.save(
                        withArg {
                            it.id shouldBe createdExternalCaregivingOrganizationId
                            it.name shouldBe "케어라인"
                            it.externalCaregivingOrganizationType shouldBe ExternalCaregivingOrganizationType.ORGANIZATION
                            it.address shouldBe "서울시 강남구 삼성동 109-1"
                            it.contractName shouldBe "김라인"
                            it.phoneNumber shouldBe "010-4026-1111"
                            it.profitAllocationRatio.shouldBeBetween(0.5999f, 0.6001f, 0.0001f)
                            it.accountInfo?.bank shouldBe null
                            it.accountInfo?.accountNumber shouldBe null
                            it.accountInfo?.accountHolder shouldBe null
                        }
                    )
                }
            }

            then("returns creation result") {
                val actualResult = behavior()
                actualResult.createdExternalCaregivingOrganizationId shouldBe "01GPWXVJB2WPDNXDT5NE3B964N"
            }
        }

        and("external caregiving organization exists") {
            val externalCaregivingOrganizationId = "01GPWXVJB2WPDNXDT5NE3B964N"
            val externalCaregivingOrganization = mockk<ExternalCaregivingOrganization>(relaxed = true)
            beforeEach {
                with(externalCaregivingOrganizationRepository) {
                    every { findByIdOrNull(externalCaregivingOrganizationId) } returns externalCaregivingOrganization
                    every { findByIdIn(match { it.contains("01GPWXVJB2WPDNXDT5NE3B964N") }) } returns listOf(
                        externalCaregivingOrganization
                    )
                }

                with(externalCaregivingOrganization) {
                    every { this@with[ObjectAttribute.ID] } returns setOf(externalCaregivingOrganizationId)
                }
            }
            afterEach { clearAllMocks() }

            `when`("getting external caregiving organization") {
                val query = ExternalCaregivingOrganizationByIdQuery(
                    id = externalCaregivingOrganizationId,
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = externalCaregivingOrganizationService.getExternalCaregivingOrganization(query)

                then("returns external caregiving organization") {
                    val actualResult = behavior()
                    actualResult shouldBe externalCaregivingOrganization
                }
            }

            `when`("시스템 사용자 권한으로 외부 간병 업체를 조회하면") {
                val query = ExternalCaregivingOrganizationByIdQuery(
                    id = externalCaregivingOrganizationId,
                    subject = SystemUser,
                )

                fun behavior() = externalCaregivingOrganizationService.getExternalCaregivingOrganization(query)

                then("아무 문제없이 응답한다.") {
                    shouldNotThrowAny { behavior() }
                }
            }

            `when`("외부 간병 업체 사용자 권한으로 본인이 소속된 간병 업체를 조회하면") {
                val query = ExternalCaregivingOrganizationByIdQuery(
                    id = externalCaregivingOrganizationId,
                    subject = generateExternalCaregivingOrganizationManagerSubject(externalCaregivingOrganizationId),
                )

                fun behavior() = externalCaregivingOrganizationService.getExternalCaregivingOrganization(query)

                then("아무 문제없이 응답한다.") {
                    shouldNotThrowAny { behavior() }
                }
            }

            `when`("아이디 목록으로 외부 간병 협회 목록을 조회하면") {
                val query = ExternalCaregivingOrganizationsByIdsQuery(
                    externalCaregivingOrganizationIds = listOf("01GPWXVJB2WPDNXDT5NE3B964N"),
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = externalCaregivingOrganizationService.getExternalCaregivingOrganizations(query)

                then("아이디가 일치하는 외부 간병 협회들을 리포지토리로부터 조회합니다.") {
                    behavior()

                    verify {
                        externalCaregivingOrganizationRepository.findByIdIn(
                            withArg {
                                it shouldContain "01GPWXVJB2WPDNXDT5NE3B964N"
                            }
                        )
                    }
                }

                then("아이디가 일치하는 외부 간병 협회들을 반환합니다.") {
                    val actualResult = behavior()
                    actualResult shouldContain externalCaregivingOrganization
                }
            }
        }

        and("external caregiving organization not exists") {
            beforeEach {
                with(externalCaregivingOrganizationRepository) {
                    every { findByIdOrNull("01GPWXVJB2WPDNXDT5NE3B964N") } returns null
                }
            }
            afterEach { clearAllMocks() }

            `when`("getting external caregiving organization") {
                val externalCaregivingOrganizationId = "01GPWXVJB2WPDNXDT5NE3B964N"
                val query = ExternalCaregivingOrganizationByIdQuery(
                    id = externalCaregivingOrganizationId,
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = externalCaregivingOrganizationService.getExternalCaregivingOrganization(query)

                then("throws ExternalCaregivingOrganizationNotFoundByIdException") {
                    val thrownException =
                        shouldThrow<ExternalCaregivingOrganizationNotFoundByIdException> { behavior() }
                    thrownException.externalCaregivingOrganizationId shouldBe query.id
                }
            }
        }

        `when`("ensuring external caregiving organization") {
            val query = ExternalCaregivingOrganizationByIdQuery(
                id = "01GPWXVJB2WPDNXDT5NE3B964N",
                subject = generateInternalCaregivingManagerSubject(),
            )

            fun behavior() = externalCaregivingOrganizationService.ensureExternalCaregivingOrganizationExists(query)

            and("external caregiving organization exists") {

                beforeEach {
                    with(externalCaregivingOrganizationRepository) {
                        every { existsById(query.id) } returns true
                    }
                }
                afterEach { clearAllMocks() }

                then("nothing happened") {
                    shouldNotThrowAny { behavior() }
                }
            }

            and("external caregiving organization not exists") {
                beforeEach {
                    with(externalCaregivingOrganizationRepository) {
                        every { existsById(query.id) } returns false
                    }
                }
                afterEach { clearAllMocks() }

                then("throws ExternalCaregivingOrganizationNotFoundByIdException") {
                    val thrownException =
                        shouldThrow<ExternalCaregivingOrganizationNotFoundByIdException> { behavior() }
                    thrownException.externalCaregivingOrganizationId shouldBe query.id
                }
            }
        }

        `when`("saving business license file") {

            val fileInputStream = FileInputStream("src/test/resources/business licenses/케어닥 사업자등록증.pdf")
            val businessLicenseSavingCommand = BusinessLicenseSavingCommand(
                externalCaregivingOrganizationId = "01GPWXVJB2WPDNXDT5NE3B964N",
                businessLicenseFile = fileInputStream,
                businessLicenseFileName = "케어라인 사업자등록증.pdf",
                mime = "application/pdf",
                contentLength = 1L,
                subject = generateInternalCaregivingManagerSubject(),
            )

            val savedBusinessLicenseFileUrl =
                "https://bik-service-business-license-dev.s3.ap-northeast-2.amazonaws.com/01GQH40N9C4MQPH9YV9CH95BHV"

            val externalCaregivingOrganization = mockk<ExternalCaregivingOrganization>(relaxed = true)

            beforeEach {
                val fileSavingResult: FileSavingResult = mockk(relaxed = true)
                every { fileSavingResult.savedFileId } returns "01GQH3XWSK1SQSDZTX373YW3V1"
                every { fileSavingResult.savedFileUrl } returns savedBusinessLicenseFileUrl

                every {
                    fileSavingCommandHandler.saveFile(any())
                } returns FileSavingResult(
                    savedFileId = "01GQH3XWSK1SQSDZTX373YW3V1",
                    savedFileUrl = savedBusinessLicenseFileUrl,
                )

                with(externalCaregivingOrganizationRepository) {
                    every { existsById("01GPWXVJB2WPDNXDT5NE3B964N") } returns true
                }

                with(externalCaregivingOrganizationRepository) {
                    every { findByIdOrNull("01GPWXVJB2WPDNXDT5NE3B964N") } returns externalCaregivingOrganization
                }
            }

            afterEach {
                clearAllMocks()
            }

            fun behavior() = externalCaregivingOrganizationService.saveBusinessLicenseFile(businessLicenseSavingCommand)

            then("saving business license using S3 service") {
                behavior()

                verify {
                    fileSavingCommandHandler.saveFile(
                        withArg {
                            it.mime shouldBe "application/pdf"
                            it.contentLength shouldBe 1L
                            it.bucketName shouldBe "careinsurance-business-license-dev"
                        }
                    )
                }
            }

            then("returns saving business license url") {
                val expectedResult = BusinessLicenseSavingResult(
                    savedBusinessLicenseFileUrl = savedBusinessLicenseFileUrl
                )
                val actualResult = behavior()

                actualResult shouldBe expectedResult
            }

            and("update business license info transaction processing failed") {

                val fileUrl =
                    "https://bik-service-business-license-dev.s3.ap-northeast-2.amazonaws.com/01GQH40N9C4MQPH9YV9CH95BHV"

                beforeEach {
                    every {
                        externalCaregivingOrganization.updateBusinessLicenseInfo(
                            SavedBusinessLicenseFileData(
                                businessLicenseSavingCommand.businessLicenseFileName,
                                fileUrl,
                            )
                        )
                    } throws Exception("error saved business license file data")
                }

                afterEach { clearAllMocks() }

                then("should delete business license") {
                    behavior()
                    verify {
                        fileByUrlQueryHandler.deleteFile(
                            withArg {
                                it.url shouldBe fileUrl
                            }
                        )
                    }
                }
            }
        }

        `when`("updating business license") {
            val externalCaregivingOrganization = mockk<ExternalCaregivingOrganization>(relaxed = true)

            val name = "케어라인 사업자등록증.pdf"
            val url =
                "https://careinsurance-business-license-dev.s3.ap-northeast-2.amazonaws.com/01GQP0PQCA68T6CMKV7AV0TPVV"

            fun behavior() = externalCaregivingOrganization.updateBusinessLicenseInfo(
                SavedBusinessLicenseFileData(
                    name,
                    url,
                )
            )
            then("update business license info of external caregiving organization") {
                behavior()

                verify {
                    externalCaregivingOrganization.updateBusinessLicenseInfo(
                        withArg {
                            it.businessLicenseFileName shouldBe name
                            it.businessLicenseFileUrl shouldBe url
                        }
                    )
                }
            }
        }

        and("external caregiving organizations") {
            val externalCaregivingOrganizations = listOf<ExternalCaregivingOrganization>(
                mockk(relaxed = true),
                mockk(relaxed = true),
                mockk(relaxed = true),
                mockk(relaxed = true),
            )
            val externalCaregivingOrganizationIds = listOf(
                "01GR34Z09TK5DZK27HCM0FEV54",
                "01GR3562NY5XZNQENNWZVRPSSN",
                "01GR36V45J2MJP8W68CTCT0BB5",
                "01GYYCMP45T4GJSNVMK1H6V6ZZ",
            )

            beforeEach {
                with(externalCaregivingOrganizations[0]) {
                    every { id } returns externalCaregivingOrganizationIds[0]
                    every { name } returns "케어라인"
                    every { externalCaregivingOrganizationType } returns ExternalCaregivingOrganizationType.AFFILIATED
                }
                with(externalCaregivingOrganizations[1]) {
                    every { id } returns externalCaregivingOrganizationIds[1]
                    every { name } returns "울산천사"
                    every { externalCaregivingOrganizationType } returns ExternalCaregivingOrganizationType.ORGANIZATION
                }
                with(externalCaregivingOrganizations[2]) {
                    every { id } returns externalCaregivingOrganizationIds[2]
                    every { name } returns "대구간병협회"
                    every { externalCaregivingOrganizationType } returns ExternalCaregivingOrganizationType.ORGANIZATION
                }
                with(externalCaregivingOrganizations[3]) {
                    every { id } returns externalCaregivingOrganizationIds[3]
                    every { name } returns "서울산업"
                    every { externalCaregivingOrganizationType } returns ExternalCaregivingOrganizationType.AFFILIATED
                }

                with(externalCaregivingOrganizationRepository) {
                    val pageableSlot = slot<Pageable>()
                    every {
                        findAll(capture(pageableSlot))
                    } answers {
                        PageImpl(
                            externalCaregivingOrganizations,
                            pageableSlot.captured,
                            externalCaregivingOrganizations.size.toLong()
                        )
                    }

                    val externalCaregivingOrganizationNameSlot = slot<String>()
                    every {
                        findByNameContains(capture(externalCaregivingOrganizationNameSlot), capture(pageableSlot))
                    } answers {
                        val matchedExternalCaregivingOrganizations =
                            externalCaregivingOrganizations.filter {
                                it.name.contains(
                                    externalCaregivingOrganizationNameSlot.captured
                                )
                            }
                        PageImpl(
                            matchedExternalCaregivingOrganizations,
                            pageableSlot.captured,
                            matchedExternalCaregivingOrganizations.size.toLong()
                        )
                    }

                    val organizationTypeSlot = slot<ExternalCaregivingOrganizationType>()
                    every {
                        findByExternalCaregivingOrganizationType(capture(organizationTypeSlot), capture(pageableSlot))
                    } answers {
                        val matchedExternalCaregivingOrganizations =
                            externalCaregivingOrganizations.filter {
                                it.externalCaregivingOrganizationType == organizationTypeSlot.captured
                            }
                        PageImpl(
                            matchedExternalCaregivingOrganizations,
                            pageableSlot.captured,
                            matchedExternalCaregivingOrganizations.size.toLong()
                        )
                    }

                    every {
                        findByExternalCaregivingOrganizationTypeAndNameContains(
                            capture(organizationTypeSlot),
                            capture(externalCaregivingOrganizationNameSlot),
                            capture(pageableSlot),
                        )
                    } answers {
                        val matchedExternalCaregivingOrganizations =
                            externalCaregivingOrganizations.filter {
                                it.externalCaregivingOrganizationType == organizationTypeSlot.captured &&
                                    it.name.contains(externalCaregivingOrganizationNameSlot.captured)
                            }
                        PageImpl(
                            matchedExternalCaregivingOrganizations,
                            pageableSlot.captured,
                            matchedExternalCaregivingOrganizations.size.toLong()
                        )
                    }
                }
            }

            afterEach { clearAllMocks() }

            `when`("getting external caregiving organizations") {
                val query = ExternalCaregivingOrganizationsByFilterQuery(
                    searchCondition = null,
                    organizationType = null,
                    subject = generateInternalCaregivingManagerSubject(),
                )
                val pageRequest = PageRequest.of(0, 10)

                fun behavior() =
                    externalCaregivingOrganizationService.getExternalCaregivingOrganizations(query, pageRequest)

                then("returns all external caregiving organizations") {
                    val actualResult = behavior()

                    actualResult.totalElements shouldBe 4
                    actualResult.totalPages shouldBe 1
                    actualResult.pageable.pageNumber shouldBe 0
                    actualResult.pageable.pageSize shouldBe 10
                    actualResult.content shouldContainExactly externalCaregivingOrganizations
                }
            }

            `when`("searching external caregiving organizations by name") {
                val query = ExternalCaregivingOrganizationsByFilterQuery(
                    searchCondition = SearchCondition(
                        searchingProperty = ExternalCaregivingOrganizationsByFilterQuery.SearchingProperty.EXTERNAL_CAREGIVING_ORGANIZATION_NAME,
                        keyword = "케어",
                    ),
                    organizationType = null,
                    subject = generateInternalCaregivingManagerSubject(),
                )
                val pageRequest = PageRequest.of(0, 1)

                fun behavior() =
                    externalCaregivingOrganizationService.getExternalCaregivingOrganizations(query, pageRequest)

                then("query external caregiving organizations using external caregiving organization repository") {
                    behavior()

                    verify {
                        externalCaregivingOrganizationRepository.findByNameContains(
                            withArg {
                                it shouldBe "케어"
                            },
                            withArg {
                                it.pageNumber shouldBe 0
                                it.pageSize shouldBe 1
                                it.sort shouldBe Sort.by(Sort.Order.desc("id"))
                            }
                        )
                    }
                }

                then("returns search result") {
                    val actualResult = behavior()

                    actualResult.content shouldContainExactly setOf(externalCaregivingOrganizations[0])
                }
            }

            `when`("업체 구분을 필터로 주어 외부 간병 업체 목록을 조회하면") {
                val query = ExternalCaregivingOrganizationsByFilterQuery(
                    searchCondition = null,
                    organizationType = ExternalCaregivingOrganizationType.AFFILIATED,
                    subject = generateInternalCaregivingManagerSubject(),
                )
                val pageRequest = PageRequest.of(0, 10)

                fun behavior() = externalCaregivingOrganizationService.getExternalCaregivingOrganizations(
                    query,
                    pageRequest,
                )

                then("필터링된 간병 업체 목록을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.totalElements shouldBe 2
                    actualResult.totalPages shouldBe 1
                    actualResult.pageable.pageNumber shouldBe 0
                    actualResult.pageable.pageSize shouldBe 10
                    actualResult.content shouldContainExactly setOf(
                        externalCaregivingOrganizations[0],
                        externalCaregivingOrganizations[3]
                    )
                }
            }

            `when`("업체 구분을 필터로 준 상태로 키워드로 검색하면") {
                val query = ExternalCaregivingOrganizationsByFilterQuery(
                    searchCondition = SearchCondition(
                        searchingProperty = ExternalCaregivingOrganizationsByFilterQuery.SearchingProperty.EXTERNAL_CAREGIVING_ORGANIZATION_NAME,
                        keyword = "울산",
                    ),
                    organizationType = ExternalCaregivingOrganizationType.ORGANIZATION,
                    subject = generateInternalCaregivingManagerSubject(),
                )
                val pageRequest = PageRequest.of(0, 10)

                fun behavior() = externalCaregivingOrganizationService.getExternalCaregivingOrganizations(
                    query,
                    pageRequest,
                )

                then("필터링과 검색을 거친 간병 업체 목록을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.totalElements shouldBe 1
                    actualResult.totalPages shouldBe 1
                    actualResult.pageable.pageNumber shouldBe 0
                    actualResult.pageable.pageSize shouldBe 10
                    actualResult.content shouldContainExactly setOf(externalCaregivingOrganizations[1])
                }
            }
        }

        and("엔티티 테스트할 때") {
            val externalCaregivingOrganization = ExternalCaregivingOrganization(
                id = "01GQNZQ5V3CZBXJ6B95JJRYMWN",
                name = "케어라인",
                externalCaregivingOrganizationType = ExternalCaregivingOrganizationType.AFFILIATED,
                address = "서울시 강남구 삼성동 109-1",
                contractName = "김라인",
                phoneNumber = "010-1234-1234",
                profitAllocationRatio = 0.0f,
                accountInfo = AccountInfo(
                    bank = "국민은행",
                    accountNumber = "085300-04-111424",
                    accountHolder = "박한결",
                )
            )
            `when`("저장을 요청하면") {
                fun behavior() = cacheExternalCaregivingOrganizationRepository.save(externalCaregivingOrganization)
                then("저장이 됩니다.") {
                    behavior()
                }
            }

            `when`("조회를 요청하면") {
                fun behavior() = cacheExternalCaregivingOrganizationRepository.findByIdOrNull("01GQNZQ5V3CZBXJ6B95JJRYMWN")
                then("조회가 됩니다.") {
                    behavior()
                }
            }
        }
    }
})
