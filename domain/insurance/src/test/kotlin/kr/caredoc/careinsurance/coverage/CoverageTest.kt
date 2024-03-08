package kr.caredoc.careinsurance.coverage

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kr.caredoc.careinsurance.generateGuestSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.withFixedClock
import org.springframework.security.access.AccessDeniedException
import java.time.LocalDateTime

class CoverageTest : BehaviorSpec({
    given("properties having duplicated annual coverage") {
        val id = "01GPD5EE21TGK5A5VCYWQ9Z73W"
        val name = "질병 3년형 (2022)"
        val targetSubscriptionYear = 2022
        val annualCoveredCaregivingCharges = listOf(
            Coverage.AnnualCoveredCaregivingCharge(
                targetAccidentYear = 2022,
                caregivingCharge = 90000,
            ),
            Coverage.AnnualCoveredCaregivingCharge(
                targetAccidentYear = 2022,
                caregivingCharge = 100000,
            ),
        )

        `when`("creating coverage with properties") {
            fun behavior() = Coverage(
                id = id,
                name = name,
                targetSubscriptionYear = targetSubscriptionYear,
                renewalType = RenewalType.THREE_YEAR,
                annualCoveredCaregivingCharges = annualCoveredCaregivingCharges,
            )

            then("throws AnnualCoverageDuplicatedException") {
                val thrownException = shouldThrow<AnnualCoverageDuplicatedException> { behavior() }

                thrownException.duplicatedYears shouldContainExactlyInAnyOrder listOf(2022)
            }
        }
    }

    given("coverage") {
        lateinit var coverage: Coverage

        beforeEach {
            coverage = withFixedClock(LocalDateTime.of(2023, 3, 18, 15, 36, 55)) {
                Coverage(
                    id = "01GPD5EE21TGK5A5VCYWQ9Z73W",
                    name = "질병 3년형 (2022)",
                    targetSubscriptionYear = 2022,
                    renewalType = RenewalType.TEN_YEAR,
                    annualCoveredCaregivingCharges = listOf(
                        Coverage.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2022,
                            caregivingCharge = 90000,
                        ),
                        Coverage.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2023,
                            caregivingCharge = 100000,
                        ),
                    ),
                )
            }
        }

        afterEach { /* nothing to do */ }

        `when`("editing coverage meta data") {
            fun behavior() = coverage.editMetaData(
                CoverageEditingCommand(
                    coverageId = "01GPD5EE21TGK5A5VCYWQ9Z73W",
                    name = "질병 3년형 (2023)",
                    targetSubscriptionYear = 2023,
                    annualCoveredCaregivingCharges = listOf(
                        Coverage.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2022,
                            caregivingCharge = 90000,
                        ),
                        Coverage.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2023,
                            caregivingCharge = 100000,
                        ),
                        Coverage.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2024,
                            caregivingCharge = 110000,
                        ),
                    ),
                    subject = generateInternalCaregivingManagerSubject()
                )
            )

            then("coverage properties are should be changed") {
                behavior()

                coverage.name shouldBe "질병 3년형 (2023)"
                coverage.targetSubscriptionYear shouldBe 2023
                coverage.annualCoveredCaregivingCharges shouldContainAll listOf(
                    Coverage.AnnualCoveredCaregivingCharge(
                        targetAccidentYear = 2022,
                        caregivingCharge = 90000,
                    ),
                    Coverage.AnnualCoveredCaregivingCharge(
                        targetAccidentYear = 2023,
                        caregivingCharge = 100000,
                    ),
                    Coverage.AnnualCoveredCaregivingCharge(
                        targetAccidentYear = 2024,
                        caregivingCharge = 110000,
                    ),
                )
            }

            then("최근 수정 일시가 현재 일시로 갱신됩니다.") {
                coverage.lastModifiedDateTime shouldBe LocalDateTime.of(2023, 3, 18, 15, 36, 55)

                withFixedClock(LocalDateTime.of(2023, 5, 19, 15, 36, 55)) {
                    behavior()
                }

                coverage.lastModifiedDateTime shouldBe LocalDateTime.of(2023, 5, 19, 15, 36, 55)
            }
        }

        `when`("기존 값과 완전히 동일한 값들로 가입 담보를 수정하면") {
            fun behavior() = coverage.editMetaData(
                CoverageEditingCommand(
                    coverageId = "01GPD5EE21TGK5A5VCYWQ9Z73W",
                    name = "질병 3년형 (2022)",
                    targetSubscriptionYear = 2022,
                    annualCoveredCaregivingCharges = listOf(
                        Coverage.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2022,
                            caregivingCharge = 90000,
                        ),
                        Coverage.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2023,
                            caregivingCharge = 100000,
                        ),
                    ),
                    subject = generateInternalCaregivingManagerSubject()
                )
            )

            then("마지막 수정 일시가 갱신되지 않습니다.") {
                coverage.lastModifiedDateTime shouldBe LocalDateTime.of(2023, 3, 18, 15, 36, 55)

                withFixedClock(LocalDateTime.of(2023, 5, 19, 15, 36, 55)) {
                    behavior()
                }

                coverage.lastModifiedDateTime shouldBe LocalDateTime.of(2023, 3, 18, 15, 36, 55)
            }
        }

        `when`("editing coverage using duplicated annual coverages") {
            fun behavior() = coverage.editMetaData(
                CoverageEditingCommand(
                    coverageId = "01GPD5EE21TGK5A5VCYWQ9Z73W",
                    name = "질병 3년형 (2023)",
                    targetSubscriptionYear = 2023,
                    annualCoveredCaregivingCharges = listOf(
                        Coverage.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2022,
                            caregivingCharge = 90000,
                        ),
                        Coverage.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2023,
                            caregivingCharge = 100000,
                        ),
                        Coverage.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2023,
                            caregivingCharge = 110000,
                        ),
                    ),
                    subject = generateInternalCaregivingManagerSubject(),
                )
            )

            then("throws AnnualCoverageDuplicatedException") {
                val thrownException = shouldThrow<AnnualCoverageDuplicatedException> { behavior() }

                thrownException.duplicatedYears shouldContainExactlyInAnyOrder listOf(2023)
            }
        }

        `when`("editing coverage without internal user attribute") {
            fun behavior() = coverage.editMetaData(
                CoverageEditingCommand(
                    coverageId = "01GPD5EE21TGK5A5VCYWQ9Z73W",
                    name = "질병 3년형 (2023)",
                    targetSubscriptionYear = 2023,
                    annualCoveredCaregivingCharges = listOf(
                        Coverage.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2022,
                            caregivingCharge = 90000,
                        ),
                        Coverage.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2023,
                            caregivingCharge = 100000,
                        ),
                        Coverage.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2024,
                            caregivingCharge = 110000,
                        ),
                    ),
                    subject = generateGuestSubject(),
                )
            )

            then("throws AccessDeniedException") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }
    }
})
