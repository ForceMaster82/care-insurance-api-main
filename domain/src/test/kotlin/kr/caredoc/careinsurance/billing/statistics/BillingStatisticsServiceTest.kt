package kr.caredoc.careinsurance.billing.statistics

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.verify
import kr.caredoc.careinsurance.billing.BillingTransactionRecorded
import kr.caredoc.careinsurance.generateExternalCaregivingOrganizationManagerSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.relaxedMock
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.access.AccessDeniedException
import java.time.LocalDate

class BillingStatisticsServiceTest : BehaviorSpec({
    given("청구 통계 서비스가 주어졌을때") {
        val dailyBillingTransactionStatisticsRepository =
            relaxedMock<DailyBillingTransactionStatisticsRepository>()
        val dailyCaregivingRoundBillingTransactionStatisticsRepository = relaxedMock<DailyCaregivingRoundBillingTransactionStatisticsRepository>()
        val service = BillingStatisticsService(
            dailyBillingTransactionStatisticsRepository = dailyBillingTransactionStatisticsRepository,
            dailyCaregivingRoundBillingTransactionStatisticsRepository = dailyCaregivingRoundBillingTransactionStatisticsRepository,
        )

        beforeEach {
            every {
                dailyBillingTransactionStatisticsRepository.findByDate(LocalDate.of(2023, 1, 30))
            } returns listOf()

            mockkConstructor(DailyBillingTransactionStatistics::class)
            justRun { anyConstructed<DailyBillingTransactionStatistics>().handleBillingTransactionRecorded(any()) }

            with(dailyCaregivingRoundBillingTransactionStatisticsRepository) {
                every {
                    findTopByDateAndCaregivingRoundId(any(), any())
                } returns null

                val savingEntitySlot = slot<DailyCaregivingRoundBillingTransactionStatistics>()

                every {
                    save(capture(savingEntitySlot))
                } answers {
                    savingEntitySlot.captured
                }
            }

            mockkConstructor(DailyCaregivingRoundBillingTransactionStatistics::class)
            justRun {
                anyConstructed<DailyCaregivingRoundBillingTransactionStatistics>().handleBillingTransactionRecorded(any())
            }
        }

        afterEach { clearAllMocks() }

        and("날짜별 청구금 입출금 통계 또한 주어졌을때") {
            val registeredBillingStatistics = relaxedMock<DailyBillingTransactionStatistics>()

            beforeEach {
                every {
                    dailyBillingTransactionStatisticsRepository.findByDate(LocalDate.of(2023, 1, 30))
                } returns listOf(
                    registeredBillingStatistics
                )
            }

            `when`("날짜로 날짜별 청구금 입출금 통계를 조회하면") {
                val query = DailyBillingTransactionStatisticsByDateQuery(
                    date = LocalDate.of(2023, 1, 30),
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = service.getDailyBillingTransactionStatistics(query)

                then("날짜별 청구금 입출금 통계를 리포지토리로부터 조회합니다.") {
                    behavior()

                    verify {
                        dailyBillingTransactionStatisticsRepository.findByDate(LocalDate.of(2023, 1, 30))
                    }
                }

                then("조회된 청구금 입출금 통계를 반환합니다.") {
                    val actualResult = behavior()

                    actualResult shouldBe registeredBillingStatistics
                }

                `when`("청구의 입출금 내역이 입력되면") {
                    val event = relaxedMock<BillingTransactionRecorded>()

                    beforeEach {
                        every { event.transactionDate } returns LocalDate.of(2023, 1, 30)
                    }

                    afterEach { clearAllMocks() }

                    fun handling() = service.handleBillingTransactionRecorded(event)

                    then("입출금일의 날짜별 청구금 입출금 통계를 리포지토리로부터 조회합니다.") {
                        handling()

                        verify {
                            dailyBillingTransactionStatisticsRepository.findByDate(LocalDate.of(2023, 1, 30))
                        }
                    }

                    then("입출금일의 날짜별 청구금 입출금 통계에 이벤트를 전달합니다.") {
                        handling()

                        verify {
                            registeredBillingStatistics.handleBillingTransactionRecorded(event)
                        }
                    }
                }
            }
        }

        `when`("날짜로 날짜별 청구금 입출금 통계를 조회하면") {
            val query = DailyBillingTransactionStatisticsByDateQuery(
                date = LocalDate.of(2023, 1, 30),
                subject = generateInternalCaregivingManagerSubject(),
            )

            fun behavior() = service.getDailyBillingTransactionStatistics(query)

            then("날짜별 청구금 입출금 통계를 리포지토리로부터 조회합니다.") {
                behavior()

                verify {
                    dailyBillingTransactionStatisticsRepository.findByDate(LocalDate.of(2023, 1, 30))
                }
            }

            then("null을 반환합니다.") {
                val actualResult = behavior()

                actualResult shouldBe null
            }
        }

        `when`("내부 사용자 권한 없이 날짜별 청구금 입출금 통계를 조회하면") {
            val query = DailyBillingTransactionStatisticsByDateQuery(
                date = LocalDate.of(2023, 1, 30),
                subject = generateExternalCaregivingOrganizationManagerSubject("01GRWDRWTM6ENFSQXTDN9HDDWK"),
            )

            fun behavior() = service.getDailyBillingTransactionStatistics(query)

            then("AccessDeniedException이 발생합니다.") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }

        `when`("청구의 입출금 내역이 입력되면") {
            val event = relaxedMock<BillingTransactionRecorded>()

            beforeEach {
                every { event.transactionDate } returns LocalDate.of(2023, 1, 30)
            }

            afterEach {
                clearAllMocks()
            }

            fun handling() = service.handleBillingTransactionRecorded(event)

            then("입출금일의 날짜별 청구금 입출금 통계를 리포지토리로부터 조회합니다.") {
                handling()

                verify {
                    dailyBillingTransactionStatisticsRepository.findByDate(LocalDate.of(2023, 1, 30))
                }
            }

            then("새롭게 생성된 통계에 청구금 입출금을 반영합니다.") {
                handling()

                verify {
                    anyConstructed<DailyBillingTransactionStatistics>().handleBillingTransactionRecorded(event)
                }
            }
        }

        and("일자별 청구 입/출금 통계 또한 주어졌을때") {
            val dailyCaregivingRoundBillingTransactionStatistics =
                listOf<DailyCaregivingRoundBillingTransactionStatistics>(
                    relaxedMock(),
                    relaxedMock(),
                )

            beforeEach {
                with(dailyCaregivingRoundBillingTransactionStatisticsRepository) {
                    val pageableSlot = slot<Pageable>()
                    every {
                        findByDate(
                            LocalDate.of(2023, 4, 30),
                            capture(pageableSlot),
                        )
                    } answers {
                        PageImpl(
                            dailyCaregivingRoundBillingTransactionStatistics,
                            pageableSlot.captured,
                            dailyCaregivingRoundBillingTransactionStatistics.size.toLong(),
                        )
                    }

                    every {
                        findTopByDateAndCaregivingRoundId(
                            LocalDate.of(2023, 4, 30),
                            "01GSM0PQ5G8HW2GKYXH3VGGMZG",
                        )
                    } returns dailyCaregivingRoundBillingTransactionStatistics[0]
                }
            }

            afterEach { clearAllMocks() }

            `when`("일자별 청구 입/출금 통계를 조회하면") {
                val query = DailyCaregivingRoundBillingTransactionStatisticsByDateQuery(
                    date = LocalDate.of(2023, 4, 30),
                    subject = generateInternalCaregivingManagerSubject(),
                )
                val pageable = PageRequest.of(0, 2).withSort(
                    Sort.by(
                        Sort.Order.desc(
                            DailyCaregivingRoundBillingTransactionStatistics::lastEnteredDateTime.name
                        )
                    )
                )

                fun behavior() = service.getDailyCaregivingRoundBillingTransactionStatistics(
                    query = query,
                    pageRequest = pageable,
                )

                then("일자별 청구 입/출금 통계를 리포지토리로부터 조회합니다.") {
                    behavior()

                    verify {
                        dailyCaregivingRoundBillingTransactionStatisticsRepository.findByDate(
                            withArg {
                                it shouldBe LocalDate.of(2023, 4, 30)
                            },
                            withArg {
                                it.pageSize shouldBe 2
                                it.pageNumber shouldBe 0
                                it.sort shouldBe Sort.by(
                                    Sort.Order.desc(
                                        DailyCaregivingRoundBillingTransactionStatistics::lastEnteredDateTime.name
                                    )
                                )
                            }
                        )
                    }
                }

                then("조회한 입/출금 통계를 반환합니다.") {
                    val result = behavior()

                    result.content shouldContainExactlyInAnyOrder dailyCaregivingRoundBillingTransactionStatistics
                }
            }

            `when`("내부 사용자 권한 없이 일자별 청구 입/출금 통계를 조회하면") {
                val query = DailyCaregivingRoundBillingTransactionStatisticsByDateQuery(
                    date = LocalDate.of(2023, 1, 30),
                    subject = generateExternalCaregivingOrganizationManagerSubject("01GRWDRWTM6ENFSQXTDN9HDDWK"),
                )
                val pageable = PageRequest.of(0, 20)

                fun behavior() = service.getDailyCaregivingRoundBillingTransactionStatistics(
                    query = query,
                    pageRequest = pageable,
                )

                then("AccessDeniedException이 발생합니다.") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }

            `when`("청구의 입/출금 내역이 입력되면") {
                val event = relaxedMock<BillingTransactionRecorded>()

                beforeEach {
                    with(event) {
                        every { transactionDate } returns LocalDate.of(2023, 4, 30)
                        every { caregivingRoundId } returns "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                    }
                }

                afterEach { clearAllMocks() }

                fun handling() = service.handleBillingTransactionRecorded(event)

                then("입출금일의 일자별 청구 입/출금 통계를 리포지토리로부터 조회합니다.") {
                    handling()

                    verify {
                        dailyCaregivingRoundBillingTransactionStatisticsRepository.findTopByDateAndCaregivingRoundId(
                            LocalDate.of(2023, 4, 30),
                            "01GSM0PQ5G8HW2GKYXH3VGGMZG",
                        )
                    }
                }

                then("일자별 청구 입/출금 통계에 이벤트를 전달합니다.") {
                    handling()

                    verify {
                        dailyCaregivingRoundBillingTransactionStatistics[0].handleBillingTransactionRecorded(
                            event
                        )
                    }
                }
            }
        }
    }
})
