package kr.caredoc.careinsurance.settlement.statistics

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
import kr.caredoc.careinsurance.generateExternalCaregivingOrganizationManagerSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.settlement.SettlementTransactionRecorded
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.access.AccessDeniedException
import java.time.LocalDate

class SettlementStatisticsServiceTest : BehaviorSpec({
    given("정산 통계 서비스가 주어졌을때") {
        val dailySettlementTransactionStatisticsRepository =
            relaxedMock<DailySettlementTransactionStatisticsRepository>()
        val dailyCaregivingRoundSettlementTransactionStatisticsRepository =
            relaxedMock<DailyCaregivingRoundSettlementTransactionStatisticsRepository>()
        val service = SettlementStatisticsService(
            dailySettlementTransactionStatisticsRepository = dailySettlementTransactionStatisticsRepository,
            dailyCaregivingRoundSettlementTransactionStatisticsRepository = dailyCaregivingRoundSettlementTransactionStatisticsRepository,
        )

        beforeEach {
            every {
                dailySettlementTransactionStatisticsRepository.findByDate(LocalDate.of(2023, 1, 30))
            } returns listOf()

            with(dailyCaregivingRoundSettlementTransactionStatisticsRepository) {
                every {
                    findTopByDateAndCaregivingRoundId(
                        any(),
                        any(),
                    )
                } returns null

                val savingEntitySlot = slot<DailyCaregivingRoundSettlementTransactionStatistics>()

                every {
                    save(capture(savingEntitySlot))
                } answers {
                    savingEntitySlot.captured
                }
            }

            mockkConstructor(DailySettlementTransactionStatistics::class)
            justRun { anyConstructed<DailySettlementTransactionStatistics>().handleSettlementTransactionRecorded(any()) }

            mockkConstructor(DailyCaregivingRoundSettlementTransactionStatistics::class)
            justRun {
                anyConstructed<DailyCaregivingRoundSettlementTransactionStatistics>().handleSettlementTransactionRecorded(
                    any()
                )
            }
        }

        afterEach { clearAllMocks() }

        and("날짜별 정산 입출금 통계 또한 주어졌을때") {
            val registeredSettlementStatistics = relaxedMock<DailySettlementTransactionStatistics>()

            beforeEach {
                every {
                    dailySettlementTransactionStatisticsRepository.findByDate(LocalDate.of(2023, 1, 30))
                } returns listOf(
                    registeredSettlementStatistics
                )

                with(registeredSettlementStatistics) {
                    every { date } returns LocalDate.of(2023, 1, 30)
                }
            }

            `when`("날짜로 날짜별 정산 입출금 통계를 조회하면") {
                val query = DailySettlementTransactionStatisticsByDateQuery(
                    date = LocalDate.of(2023, 1, 30),
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = service.getDailySettlementTransactionStatistics(query)

                then("날짜별 정산 입출금 통계를 리포지토리로부터 조회합니다.") {
                    behavior()

                    verify {
                        dailySettlementTransactionStatisticsRepository.findByDate(LocalDate.of(2023, 1, 30))
                    }
                }

                then("조회된 정산 입출금 통계를 반환합니다.") {
                    val actualResult = behavior()

                    actualResult shouldBe registeredSettlementStatistics
                }
            }

            `when`("정산의 입출금 내역이 입력되면") {
                val event = relaxedMock<SettlementTransactionRecorded>()

                beforeEach {
                    every { event.transactionDate } returns LocalDate.of(2023, 1, 30)
                }

                afterEach { clearAllMocks() }

                fun handling() = service.handleSettlementTransactionRecorded(event)

                then("입출금일의 날짜별 정산 입출금 통계를 리포지토리로부터 조회합니다.") {
                    handling()

                    verify {
                        dailySettlementTransactionStatisticsRepository.findByDate(LocalDate.of(2023, 1, 30))
                    }
                }

                then("입출금일의 날짜별 정산 입출금 통계에 이벤트를 전달합니다.") {
                    handling()

                    verify {
                        registeredSettlementStatistics.handleSettlementTransactionRecorded(event)
                    }
                }
            }
        }

        `when`("날짜로 날짜별 정산 입출금 통계를 조회하면") {
            val query = DailySettlementTransactionStatisticsByDateQuery(
                date = LocalDate.of(2023, 1, 30),
                subject = generateInternalCaregivingManagerSubject(),
            )

            fun behavior() = service.getDailySettlementTransactionStatistics(query)

            then("날짜별 정산 입출금 통계를 리포지토리로부터 조회합니다.") {
                behavior()

                verify {
                    dailySettlementTransactionStatisticsRepository.findByDate(LocalDate.of(2023, 1, 30))
                }
            }

            then("null을 반환합니다.") {
                val actualResult = behavior()

                actualResult shouldBe null
            }
        }

        `when`("내부 사용자 권한 없이 날짜별 정산 입출금 통계를 조회하면") {
            val query = DailySettlementTransactionStatisticsByDateQuery(
                date = LocalDate.of(2023, 1, 30),
                subject = generateExternalCaregivingOrganizationManagerSubject("01GRWDRWTM6ENFSQXTDN9HDDWK"),
            )

            fun behavior() = service.getDailySettlementTransactionStatistics(query)

            then("AccessDeniedException이 발생합니다.") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }

        `when`("정산의 입출금 내역이 입력되면") {
            val event = relaxedMock<SettlementTransactionRecorded>()

            beforeEach {
                every { event.transactionDate } returns LocalDate.of(2023, 1, 30)
                every { event.caregivingRoundId } returns "01GSM0PQ5G8HW2GKYXH3VGGMZG"
            }

            afterEach {
                clearAllMocks()
            }

            fun handling() = service.handleSettlementTransactionRecorded(event)

            then("입출금일의 날짜별 정산 입출금 통계를 리포지토리로부터 조회합니다.") {
                handling()

                verify {
                    dailySettlementTransactionStatisticsRepository.findByDate(LocalDate.of(2023, 1, 30))
                }
            }

            then("새롭게 생성된 통계에 정산 입출금을 반영합니다.") {
                handling()

                verify {
                    anyConstructed<DailySettlementTransactionStatistics>().handleSettlementTransactionRecorded(event)
                }
            }

            then("입출금일의 날짜/간병회차별 정산 입출금 통계를 리포지토리로부터 조회합니다.") {
                handling()

                verify {
                    dailyCaregivingRoundSettlementTransactionStatisticsRepository.findTopByDateAndCaregivingRoundId(
                        LocalDate.of(2023, 1, 30),
                        "01GSM0PQ5G8HW2GKYXH3VGGMZG",
                    )
                }
            }

            then("새롭게 생성된 날짜/간병회차별 정산 입출금 통계에 이벤트를 전달합니다.") {
                handling()

                verify {
                    anyConstructed<DailyCaregivingRoundSettlementTransactionStatistics>().handleSettlementTransactionRecorded(
                        event
                    )
                }
            }
        }

        and("날짜/간병회차별 정산 입출금 통계 또한 주어졌을때") {
            val registeredDailyCaregivingRoundSettlementTransactionStatistics =
                listOf<DailyCaregivingRoundSettlementTransactionStatistics>(
                    relaxedMock(),
                    relaxedMock(),
                )

            beforeEach {
                with(dailyCaregivingRoundSettlementTransactionStatisticsRepository) {
                    val pageableSlot = slot<Pageable>()
                    every {
                        findByDate(
                            LocalDate.of(2023, 1, 30),
                            capture(pageableSlot),
                        )
                    } answers {
                        PageImpl(
                            registeredDailyCaregivingRoundSettlementTransactionStatistics,
                            pageableSlot.captured,
                            registeredDailyCaregivingRoundSettlementTransactionStatistics.size.toLong(),
                        )
                    }

                    every {
                        findTopByDateAndCaregivingRoundId(
                            LocalDate.of(2023, 1, 30),
                            "01GSM0PQ5G8HW2GKYXH3VGGMZG",
                        )
                    } returns registeredDailyCaregivingRoundSettlementTransactionStatistics[0]
                }
            }

            afterEach { clearAllMocks() }

            `when`("날짜/간병회차별 정산 입출금 통계를 조회하면") {
                val query = DailyCaregivingRoundSettlementTransactionStatisticsByDateQuery(
                    date = LocalDate.of(2023, 1, 30),
                    subject = generateInternalCaregivingManagerSubject(),
                )
                val pageable = PageRequest.of(0, 20)

                fun behavior() = service.getDailyCaregivingRoundSettlementTransactionStatistics(
                    query = query,
                    pageRequest = pageable,
                )

                then("날짜/간병회차별 정산 입출금 통계를 리포지토리로부터 조회합니다.") {
                    behavior()

                    verify {
                        dailyCaregivingRoundSettlementTransactionStatisticsRepository.findByDate(
                            withArg {
                                it shouldBe LocalDate.of(2023, 1, 30)
                            },
                            withArg {
                                it.pageSize shouldBe 20
                                it.pageNumber shouldBe 0
                                it.sort shouldBe Sort.by(
                                    Sort.Order.desc(
                                        DailyCaregivingRoundSettlementTransactionStatistics::lastEnteredDateTime.name
                                    )
                                )
                            }
                        )
                    }
                }

                then("조회한 입출금 통계를 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.content shouldContainExactlyInAnyOrder registeredDailyCaregivingRoundSettlementTransactionStatistics
                }
            }

            `when`("내부 사용자 권한 없이 날짜/간병회차별 정산 입출금 통계를 조회하면") {
                val query = DailyCaregivingRoundSettlementTransactionStatisticsByDateQuery(
                    date = LocalDate.of(2023, 1, 30),
                    subject = generateExternalCaregivingOrganizationManagerSubject("01GRWDRWTM6ENFSQXTDN9HDDWK"),
                )
                val pageable = PageRequest.of(0, 20)

                fun behavior() = service.getDailyCaregivingRoundSettlementTransactionStatistics(
                    query = query,
                    pageRequest = pageable,
                )

                then("AccessDeniedException이 발생합니다.") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }

            `when`("정산의 입출금 내역이 입력되면") {
                val event = relaxedMock<SettlementTransactionRecorded>()

                beforeEach {
                    with(event) {
                        every { transactionDate } returns LocalDate.of(2023, 1, 30)
                        every { caregivingRoundId } returns "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                    }
                }

                afterEach { clearAllMocks() }

                fun handling() = service.handleSettlementTransactionRecorded(event)

                then("입출금일의 날짜별 정산 입출금 통계를 리포지토리로부터 조회합니다.") {
                    handling()

                    verify {
                        dailyCaregivingRoundSettlementTransactionStatisticsRepository.findTopByDateAndCaregivingRoundId(
                            LocalDate.of(2023, 1, 30),
                            "01GSM0PQ5G8HW2GKYXH3VGGMZG",
                        )
                    }
                }

                then("날짜/간병회차별 정산 입출금 통계에 이벤트를 전달합니다.") {
                    handling()

                    verify {
                        registeredDailyCaregivingRoundSettlementTransactionStatistics[0].handleSettlementTransactionRecorded(
                            event
                        )
                    }
                }
            }
        }
    }
})
