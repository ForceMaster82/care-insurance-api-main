package kr.caredoc.careinsurance.coverage

import com.github.guepardoapps.kulid.ULID
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import kr.caredoc.careinsurance.generateExternalCaregivingOrganizationManagerSubject
import kr.caredoc.careinsurance.generateGuestSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.search.SearchCondition
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.AccessDeniedException

@SpringBootTest
class CoverageServiceTest(
    @Autowired
    private val cacheCoverageRepository: CoverageRepository,
) : BehaviorSpec({
    given("coverage service") {
        val coverageRepository = relaxedMock<CoverageRepository>()
        val coverageService = CoverageService(coverageRepository)

        fun createCoverageWithArguments(
            name: String = "질병 3년형 (2022)",
            targetSubscriptionYear: Int = 2022,
            annualCoveredCaregivingCharges: List<Coverage.AnnualCoveredCaregivingCharge> =
                listOf(
                    Coverage.AnnualCoveredCaregivingCharge(
                        targetAccidentYear = 2022,
                        caregivingCharge = 90000,
                    ),
                    Coverage.AnnualCoveredCaregivingCharge(
                        targetAccidentYear = 2023,
                        caregivingCharge = 100000,
                    ),
                ),
            renewalType: RenewalType = RenewalType.TEN_YEAR,
            subject: Subject = generateInternalCaregivingManagerSubject(),
        ) = CoverageCreationCommand(
            name = name,
            targetSubscriptionYear = targetSubscriptionYear,
            annualCoveredCaregivingCharges = annualCoveredCaregivingCharges,
            renewalType = renewalType,
            subject = subject,
        ).let { command ->
            coverageService.createCoverage(command)
        }

        beforeEach {
            with(coverageRepository) {
                val pageableSlot = slot<Pageable>()
                every { findAll(capture(pageableSlot)) } answers {
                    val capturedPageable = pageableSlot.captured
                    PageImpl(
                        listOf(),
                        capturedPageable,
                        0,
                    )
                }

                val savingEntitySlot = slot<Coverage>()
                every { save(capture(savingEntitySlot)) } answers { savingEntitySlot.captured }

                every { findByIdOrNull(any()) } returns null
            }
        }

        afterEach { clearAllMocks() }

        and("registered coverages") {
            val coverages = listOf<Coverage>(
                relaxedMock(),
                relaxedMock(),
            )

            beforeEach {
                with(coverages[0]) {
                    every { id } returns "01GPD5EE21TGK5A5VCYWQ9Z73W"
                    every { name } returns "질병 3년형 (2022)"
                    every { targetSubscriptionYear } returns 2022
                    every { renewalType } returns RenewalType.TEN_YEAR
                }
                with(coverages[1]) {
                    every { id } returns "01GP2R2WN0DDQ2H715V2NEHDPB"
                    every { name } returns "상해 3년형 (2032)"
                    every { targetSubscriptionYear } returns 2032
                    every { renewalType } returns RenewalType.TEN_YEAR
                }

                with(coverageRepository) {
                    every {
                        findAll(
                            match<Pageable> {
                                it.pageNumber == 0 && it.pageSize == 2
                            }
                        )
                    } returns PageImpl(
                        coverages,
                        PageRequest.of(0, 2),
                        4,
                    )

                    every {
                        findByIdOrNull("01GPD5EE21TGK5A5VCYWQ9Z73W")
                    } returns coverages[0]

                    val searchKeywordSlot = slot<String>()
                    val pageableSlot = slot<Pageable>()
                    every {
                        findByNameContaining(capture(searchKeywordSlot), capture(pageableSlot))
                    } answers {
                        val capturedKeyword = searchKeywordSlot.captured
                        val matchedCoverages = coverages.filter { it.name.contains(capturedKeyword) }

                        PageImpl(
                            matchedCoverages,
                            pageableSlot.captured,
                            matchedCoverages.size.toLong(),
                        )
                    }

                    every {
                        findByIdOrNull("01GP2R2WN0DDQ2H715V2NEHDPB")
                    } returns coverages[1]

                    val nameParameterSlot = slot<String>()
                    every {
                        existsByName(capture(nameParameterSlot))
                    } answers {
                        val capturedNameParameter = nameParameterSlot.captured
                        coverages.find { it.name == capturedNameParameter } != null
                    }

                    val renewalTypeParameterSlot = slot<RenewalType>()
                    val subscriptionYearParameterSlot = slot<Int>()
                    every {
                        existsByRenewalTypeAndTargetSubscriptionYear(
                            capture(renewalTypeParameterSlot),
                            capture(subscriptionYearParameterSlot),
                        )
                    } answers {
                        val capturedRenewalTypeParameter = renewalTypeParameterSlot.captured
                        val capturedSubscriptionYearParameter = subscriptionYearParameterSlot.captured
                        coverages.find {
                            it.renewalType == capturedRenewalTypeParameter &&
                                it.targetSubscriptionYear == capturedSubscriptionYearParameter
                        } != null
                    }
                }
            }

            afterEach {
                clearAllMocks()
            }

            `when`("getting paged all coverages") {
                val query = AllCoveragesQuery(generateInternalCaregivingManagerSubject())

                fun behavior() = coverageService.getCoverages(query, PageRequest.of(0, 2))

                then("returns paged all coverages") {
                    val actualResult = behavior()

                    actualResult.totalElements shouldBe 4
                    actualResult.totalPages shouldBe 2
                    actualResult.pageable.pageNumber shouldBe 0
                    actualResult.content shouldContainExactly coverages
                }
            }

            `when`("getting a coverage with mapper") {
                val query = CoverageByIdQuery(
                    coverageId = "01GPD5EE21TGK5A5VCYWQ9Z73W",
                    subject = generateInternalCaregivingManagerSubject()
                )

                fun behavior() = coverageService.getCoverage(query) { coverage -> coverage.name }

                then("returns mapped coverage") {
                    val actualResult = behavior()

                    actualResult shouldBe "질병 3년형 (2022)"
                }

                then("query coverage using repository") {
                    behavior()
                    verify {
                        coverageRepository.findByIdOrNull("01GPD5EE21TGK5A5VCYWQ9Z73W")
                    }
                }
            }

            `when`("editing coverage") {
                val command = CoverageEditingCommand(
                    coverageId = "01GPD5EE21TGK5A5VCYWQ9Z73W",
                    name = "질병 3년형 - 특약 (2022)",
                    targetSubscriptionYear = 2022,
                    annualCoveredCaregivingCharges = listOf(
                        Coverage.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2022,
                            caregivingCharge = 90000
                        ),
                        Coverage.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2023,
                            caregivingCharge = 100000
                        ),
                        Coverage.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2023,
                            caregivingCharge = 110000
                        )
                    ),
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = coverageService.editCoverage(command)

                then("coverage should be edited") {
                    behavior()
                    verify {
                        coverages[0].editMetaData(command)
                    }
                }
            }

            `when`("editing coverage without internal user attribute") {
                val command = CoverageEditingCommand(
                    coverageId = "01GPD5EE21TGK5A5VCYWQ9Z73W",
                    name = "질병 3년형 - 특약 (2022)",
                    targetSubscriptionYear = 2022,
                    annualCoveredCaregivingCharges = listOf(
                        Coverage.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2022,
                            caregivingCharge = 90000
                        ),
                        Coverage.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2023,
                            caregivingCharge = 100000
                        ),
                        Coverage.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2023,
                            caregivingCharge = 110000
                        )
                    ),
                    subject = generateGuestSubject(),
                )

                fun behavior() = coverageService.editCoverage(command)

                then("throws AccessDeniedException") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }

            `when`("ensuring a coverage") {
                val query = CoverageByIdQuery(
                    coverageId = "01GPD5EE21TGK5A5VCYWQ9Z73W",
                    subject = generateInternalCaregivingManagerSubject()
                )

                fun behavior() = coverageService.ensureCoverageExists(query)

                then("just runs") {
                    shouldNotThrowAny { behavior() }
                }
            }

            `when`("가입 담보를 이름으로 검색하면") {
                val query = CoveragesBySearchConditionQuery(
                    searchCondition = SearchCondition(
                        searchingProperty = CoveragesBySearchConditionQuery.SearchingProperty.NAME,
                        keyword = "질병",
                    ),
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = coverageService.getCoverages(query, PageRequest.of(0, 2))

                then("리포지토리로부터 가입 담보를 검색합니다.") {
                    behavior()

                    verify {
                        coverageRepository.findByNameContaining(
                            "질병",
                            PageRequest.of(0, 2),
                        )
                    }
                }

                then("검색 결과를 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.content shouldContainExactlyInAnyOrder setOf(coverages[0])
                }
            }

            `when`("내부 사용자 권한 없이 가입 담보를 검색하면") {
                val query = CoveragesBySearchConditionQuery(
                    searchCondition = SearchCondition(
                        searchingProperty = CoveragesBySearchConditionQuery.SearchingProperty.NAME,
                        keyword = "질병",
                    ),
                    subject = generateExternalCaregivingOrganizationManagerSubject("01GPWXVJB2WPDNXDT5NE3B964N")
                )

                fun behavior() = coverageService.getCoverages(query, PageRequest.of(0, 2))

                then("AccessDeniedException이 발생합니다.") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }

            `when`("등록된 가입 담보와 동일한 이름을 가진 가입 담보를 등록하면") {
                fun behavior() = createCoverageWithArguments(
                    name = "질병 3년형 (2022)",
                    targetSubscriptionYear = 2023,
                )

                then("CoverageNameDuplicatedException이 발생합니다.") {
                    shouldThrow<CoverageNameDuplicatedException> { behavior() }
                }
            }

            `when`("등록된 가입 담보와 기준 연도를 가진 가입 담보를 등록하면") {
                fun behavior() = createCoverageWithArguments(
                    name = "질병 5년형 (2022)",
                    targetSubscriptionYear = 2022,
                )

                then("SubscriptionYearDuplicatedException이 발생합니다.") {
                    shouldThrow<SubscriptionYearDuplicatedException> { behavior() }
                }
            }

            `when`("기존에 등록된 가입 담보와 동일한 이름으로 다른 가입 담보를 수정하면") {
                val command = CoverageEditingCommand(
                    coverageId = "01GPD5EE21TGK5A5VCYWQ9Z73W",
                    name = "상해 3년형 (2032)",
                    targetSubscriptionYear = 2022,
                    annualCoveredCaregivingCharges = listOf(
                        Coverage.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2022,
                            caregivingCharge = 90000
                        ),
                        Coverage.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2023,
                            caregivingCharge = 100000
                        ),
                        Coverage.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2023,
                            caregivingCharge = 110000
                        )
                    ),
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = coverageService.editCoverage(command)

                then("CoverageNameDuplicatedException이 발생합니다.") {
                    shouldThrow<CoverageNameDuplicatedException> { behavior() }
                }
            }
        }

        `when`("getting paged all coverages") {
            val request = AllCoveragesQuery(generateInternalCaregivingManagerSubject())

            fun behavior() = coverageService.getCoverages(request, PageRequest.of(0, 2))

            then("empty page") {
                val actualResult = behavior()

                actualResult.content shouldBe listOf()
            }
        }

        `when`("getting paged all coverages without internal user attribute") {
            val request = AllCoveragesQuery(generateGuestSubject())

            fun behavior() = coverageService.getCoverages(request, PageRequest.of(0, 2))

            then("throws AccessDeniedException") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }

        `when`("creating coverage") {
            fun behavior() = createCoverageWithArguments()

            beforeEach {
                mockkObject(ULID)
                every { ULID.random() } returns "01GPCPRCFSCZK219TERJK9B0H0"
            }

            afterEach { clearAllMocks() }

            then("persist coverage entity") {
                behavior()

                verify {
                    coverageRepository.save(
                        withArg {
                            it.name shouldBe "질병 3년형 (2022)"
                            it.targetSubscriptionYear shouldBe 2022
                            it.renewalType shouldBe RenewalType.TEN_YEAR
                            it.annualCoveredCaregivingCharges shouldContainExactlyInAnyOrder listOf(
                                Coverage.AnnualCoveredCaregivingCharge(
                                    targetAccidentYear = 2022,
                                    caregivingCharge = 90000,
                                ),
                                Coverage.AnnualCoveredCaregivingCharge(
                                    targetAccidentYear = 2023,
                                    caregivingCharge = 100000,
                                ),
                            )
                        }
                    )
                }
            }

            then("returns creation result") {
                val actualResult = behavior()

                actualResult.createdCoverageId shouldBe "01GPCPRCFSCZK219TERJK9B0H0"
            }
        }

        `when`("creating coverage without internal user attribute") {
            fun behavior() = createCoverageWithArguments(subject = generateGuestSubject())

            then("throws AccessDeniedException") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }

        `when`("creating coverage having three year renewal type") {
            fun behavior() = createCoverageWithArguments(renewalType = RenewalType.THREE_YEAR)

            then("throws IllegalRenewalTypeEnteredException") {
                val thrownException = shouldThrow<IllegalRenewalTypeEnteredException> { behavior() }

                thrownException.enteredRenewalType shouldBe RenewalType.THREE_YEAR
            }
        }

        `when`("getting a coverage with mapper") {
            val query = CoverageByIdQuery(
                coverageId = "01GPD5EE21TGK5A5VCYWQ9Z73W",
                subject = generateInternalCaregivingManagerSubject()
            )

            fun behavior() = coverageService.getCoverage(query) { coverage -> coverage.name }

            then("throws CoverageNotFoundByIdException") {
                val thrownException = shouldThrow<CoverageNotFoundByIdException> { behavior() }

                thrownException.coverageId shouldBe "01GPD5EE21TGK5A5VCYWQ9Z73W"
            }
        }

        `when`("ensuring a coverage") {
            val query = CoverageByIdQuery(
                coverageId = "01GPD5EE21TGK5A5VCYWQ9Z73W",
                subject = generateInternalCaregivingManagerSubject()
            )

            fun behavior() = coverageService.ensureCoverageExists(query)

            then("throws CoverageNotFoundByIdException") {
                val thrownException = shouldThrow<CoverageNotFoundByIdException> { behavior() }

                thrownException.coverageId shouldBe "01GPD5EE21TGK5A5VCYWQ9Z73W"
            }
        }

        and("엔티티 테스트할 때") {
            val coverage = Coverage(
                id = "01HA49VPVW5TYNKNNDZR1WXCQB",
                name = "질병 3년형 (2032)",
                targetSubscriptionYear = 2032,
                renewalType = RenewalType.TEN_YEAR,
                annualCoveredCaregivingCharges = listOf(
                    Coverage.AnnualCoveredCaregivingCharge(
                        targetAccidentYear = 2031,
                        caregivingCharge = 90000,
                    ),
                    Coverage.AnnualCoveredCaregivingCharge(
                        targetAccidentYear = 2032,
                        caregivingCharge = 100000,
                    ),
                )
            )
            `when`("저장을 요청하면") {
                fun behavior() = cacheCoverageRepository.save(coverage)
                then("저장이 됩니다.") {
                    behavior()
                }
            }
            `when`("조회를 요청하면") {
                val id = "01HA49VPVW5TYNKNNDZR1WXCQB"

                fun behavior() = cacheCoverageRepository.findByIdOrNull(id)
                then("조회가 됩니다.") {
                    behavior()
                }
            }
        }
    }
})
