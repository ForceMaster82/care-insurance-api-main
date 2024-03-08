package kr.caredoc.careinsurance.caregiving

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import kr.caredoc.careinsurance.generateExternalCaregivingOrganizationManagerSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.relaxedMock
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.access.AccessDeniedException
import java.time.LocalDateTime

class CaregivingStatisticsServiceTest : BehaviorSpec({
    given("간병 통계 서비스가 주어졌을때") {
        val caregivingRoundRepository = relaxedMock<CaregivingRoundRepository>()
        val service = CaregivingStatisticsService(
            caregivingRoundRepository = caregivingRoundRepository,
        )

        `when`("년/월 조건으로 월별 지역별 간병 통계를 조회하면") {
            val query = MonthlyRegionalCaregivingStatisticsByFilterQuery(
                year = 2023,
                month = 11,
                stateFilter = null,
                cityFilter = null,
                subject = generateInternalCaregivingManagerSubject(),
            )

            val statisticsQueryingResult = relaxedMock<Page<MonthlyRegionalCaregivingStatistics>>()

            beforeEach {
                every {
                    caregivingRoundRepository.getCaregivingStatisticsByPeriodIntersect(
                        LocalDateTime.of(2023, 11, 1, 0, 0, 0),
                        LocalDateTime.of(2023, 12, 1, 0, 0, 0),
                        PageRequest.of(0, 2),
                    )
                } returns statisticsQueryingResult
            }

            afterEach { clearAllMocks() }

            fun behavior() = service.getMonthlyRegionalCaregivingStatistics(
                query,
                PageRequest.of(0, 2),
            )

            then("해당 월의 시작일과 다음달의 시작일로 지역별 간병 통계를 리포지토리로부터 조회한다.") {
                behavior()

                verify {
                    caregivingRoundRepository.getCaregivingStatisticsByPeriodIntersect(
                        withArg { it shouldBe LocalDateTime.of(2023, 11, 1, 0, 0, 0) },
                        withArg { it shouldBe LocalDateTime.of(2023, 12, 1, 0, 0, 0) },
                        withArg<Pageable> {
                            it.pageSize shouldBe 2
                            it.pageNumber shouldBe 0
                            it.sort.getOrderFor(CaregivingRoundRepository.HOSPITAL_STATE_ORDERING)?.direction shouldBe Sort.Direction.ASC
                            it.sort.getOrderFor(CaregivingRoundRepository.HOSPITAL_CITY_ORDERING)?.direction shouldBe Sort.Direction.ASC
                        },
                    )
                }
            }

            then("조회된 간병 통계를 반환한다.") {
                val actualResult = behavior()

                actualResult shouldBe statisticsQueryingResult
            }
        }

        `when`("년/월과 시/도 조건으로 월별 지역별 간병 통계를 조회하면") {
            val query = MonthlyRegionalCaregivingStatisticsByFilterQuery(
                year = 2023,
                month = 11,
                stateFilter = "서울특별시",
                cityFilter = null,
                subject = generateInternalCaregivingManagerSubject(),
            )

            val statisticsQueryingResult = relaxedMock<Page<MonthlyRegionalCaregivingStatistics>>()

            beforeEach {
                every {
                    caregivingRoundRepository.getCaregivingStatisticsByPeriodIntersectAndState(
                        LocalDateTime.of(2023, 11, 1, 0, 0, 0),
                        LocalDateTime.of(2023, 12, 1, 0, 0, 0),
                        "서울특별시",
                        PageRequest.of(0, 2),
                    )
                } returns statisticsQueryingResult
            }

            afterEach { clearAllMocks() }

            fun behavior() = service.getMonthlyRegionalCaregivingStatistics(
                query,
                PageRequest.of(0, 2),
            )

            then("해당 월의 시작일과 다음달의 시작일과 시/도 조건으로 지역별 간병 통계를 리포지토리로부터 조회한다.") {
                behavior()

                verify {
                    caregivingRoundRepository.getCaregivingStatisticsByPeriodIntersectAndState(
                        withArg { it shouldBe LocalDateTime.of(2023, 11, 1, 0, 0, 0) },
                        withArg { it shouldBe LocalDateTime.of(2023, 12, 1, 0, 0, 0) },
                        withArg { it shouldBe "서울특별시" },
                        withArg<Pageable> {
                            it.pageSize shouldBe 2
                            it.pageNumber shouldBe 0
                            it.sort.getOrderFor(CaregivingRoundRepository.HOSPITAL_STATE_ORDERING)?.direction shouldBe Sort.Direction.ASC
                            it.sort.getOrderFor(CaregivingRoundRepository.HOSPITAL_CITY_ORDERING)?.direction shouldBe Sort.Direction.ASC
                        },
                    )
                }
            }

            then("조회된 간병 통계를 반환한다.") {
                val actualResult = behavior()

                actualResult shouldBe statisticsQueryingResult
            }
        }

        `when`("년/월로 시/도, 시/군/구 조건으로월별 지역별 간병 통계를 조회하면") {
            val query = MonthlyRegionalCaregivingStatisticsByFilterQuery(
                year = 2023,
                month = 11,
                stateFilter = "서울특별시",
                cityFilter = "강남구",
                subject = generateInternalCaregivingManagerSubject(),
            )

            val statisticsQueryingResult = relaxedMock<Page<MonthlyRegionalCaregivingStatistics>>()

            beforeEach {
                every {
                    caregivingRoundRepository.getCaregivingStatisticsByPeriodIntersectAndStateAndCity(
                        LocalDateTime.of(2023, 11, 1, 0, 0, 0),
                        LocalDateTime.of(2023, 12, 1, 0, 0, 0),
                        "서울특별시",
                        "강남구",
                        PageRequest.of(0, 2),
                    )
                } returns statisticsQueryingResult
            }

            afterEach { clearAllMocks() }

            fun behavior() = service.getMonthlyRegionalCaregivingStatistics(
                query,
                PageRequest.of(0, 2),
            )

            then("해당 월의 시작일과 다음달의 시작일과 시/도, 시/군/구 조건으로 지역별 간병 통계를 리포지토리로부터 조회한다.") {
                behavior()

                verify {
                    caregivingRoundRepository.getCaregivingStatisticsByPeriodIntersectAndStateAndCity(
                        withArg { it shouldBe LocalDateTime.of(2023, 11, 1, 0, 0, 0) },
                        withArg { it shouldBe LocalDateTime.of(2023, 12, 1, 0, 0, 0) },
                        withArg { it shouldBe "서울특별시" },
                        withArg { it shouldBe "강남구" },
                        withArg<Pageable> {
                            it.pageSize shouldBe 2
                            it.pageNumber shouldBe 0
                            it.sort.getOrderFor(CaregivingRoundRepository.HOSPITAL_STATE_ORDERING)?.direction shouldBe Sort.Direction.ASC
                            it.sort.getOrderFor(CaregivingRoundRepository.HOSPITAL_CITY_ORDERING)?.direction shouldBe Sort.Direction.ASC
                        },
                    )
                }
            }

            then("조회된 간병 통계를 반환한다.") {
                val actualResult = behavior()

                actualResult shouldBe statisticsQueryingResult
            }
        }

        `when`("내부 사용자 권한 없이 년/월 조건으로 월별 지역별 간병 통계를 조회하면") {
            val query = MonthlyRegionalCaregivingStatisticsByFilterQuery(
                year = 2023,
                month = 11,
                stateFilter = null,
                cityFilter = null,
                subject = generateExternalCaregivingOrganizationManagerSubject("01GSEKXGET5JKFST29B5K0N4XH")
            )

            fun behavior() = service.getMonthlyRegionalCaregivingStatistics(
                query,
                PageRequest.of(0, 2),
            )

            then("AccessDeniedException이 발생합니다.") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }

        `when`("년/월 조건으로 월별 지역별 간병 통계를 CSV 형식으로 조회하면") {
            val query = MonthlyRegionalCaregivingStatisticsByFilterQuery(
                year = 2023,
                month = 11,
                stateFilter = null,
                cityFilter = null,
                subject = generateInternalCaregivingManagerSubject(),
            )

            beforeEach {
                every {
                    caregivingRoundRepository.getCaregivingStatisticsByPeriodIntersect(
                        LocalDateTime.of(2023, 11, 1, 0, 0, 0),
                        LocalDateTime.of(2023, 12, 1, 0, 0, 0),
                        Sort.by(
                            Sort.Order.asc(CaregivingRoundRepository.HOSPITAL_STATE_ORDERING),
                            Sort.Order.asc(CaregivingRoundRepository.HOSPITAL_CITY_ORDERING),
                        )
                    )
                } returns listOf(
                    relaxedMock {
                        every { state } returns "서울특별시"
                        every { city } returns "강남구"
                        every { receptionCount } returns 24265
                    },
                    relaxedMock {
                        every { state } returns "서울특별시"
                        every { city } returns "강동구"
                        every { receptionCount } returns 24265
                    },
                    relaxedMock {
                        every { state } returns "경기도"
                        every { city } returns "양주시"
                        every { receptionCount } returns 24265
                    },
                )
            }

            afterEach { clearAllMocks() }

            fun behavior() = service.getMonthlyRegionalCaregivingStatisticsAsCsv(query)

            then("해당 월의 시작일과 다음달의 시작일로 지역별 간병 통계를 리포지토리로부터 조회한다.") {
                behavior()

                verify {
                    caregivingRoundRepository.getCaregivingStatisticsByPeriodIntersect(
                        withArg { it shouldBe LocalDateTime.of(2023, 11, 1, 0, 0, 0) },
                        withArg { it shouldBe LocalDateTime.of(2023, 12, 1, 0, 0, 0) },
                        withArg<Sort> {
                            it.getOrderFor(CaregivingRoundRepository.HOSPITAL_STATE_ORDERING)?.direction shouldBe Sort.Direction.ASC
                            it.getOrderFor(CaregivingRoundRepository.HOSPITAL_CITY_ORDERING)?.direction shouldBe Sort.Direction.ASC
                        },
                    )
                }
            }

            then("통계를 바탕으로 CSV를 생성하여 반환한다.") {
                val actualResult = behavior()

                actualResult shouldBe """
                    시/도,시/군/구,돌봄환자 수
                    서울특별시,강남구,24265
                    서울특별시,강동구,24265
                    경기도,양주시,24265
                """.trimIndent()
            }
        }

        `when`("년/월과 시/도 조건으로 월별 지역별 간병 통계를 CSV 형식으로 조회하면") {
            val query = MonthlyRegionalCaregivingStatisticsByFilterQuery(
                year = 2023,
                month = 11,
                stateFilter = "서울특별시",
                cityFilter = null,
                subject = generateInternalCaregivingManagerSubject(),
            )

            beforeEach {
                every {
                    caregivingRoundRepository.getCaregivingStatisticsByPeriodIntersectAndState(
                        LocalDateTime.of(2023, 11, 1, 0, 0, 0),
                        LocalDateTime.of(2023, 12, 1, 0, 0, 0),
                        "서울특별시",
                        Sort.by(
                            Sort.Order.asc(CaregivingRoundRepository.HOSPITAL_STATE_ORDERING),
                            Sort.Order.asc(CaregivingRoundRepository.HOSPITAL_CITY_ORDERING),
                        )
                    )
                } returns listOf(
                    relaxedMock {
                        every { state } returns "서울특별시"
                        every { city } returns "강남구"
                        every { receptionCount } returns 24265
                    },
                    relaxedMock {
                        every { state } returns "서울특별시"
                        every { city } returns "강동구"
                        every { receptionCount } returns 24265
                    },
                )
            }

            afterEach { clearAllMocks() }

            fun behavior() = service.getMonthlyRegionalCaregivingStatisticsAsCsv(query)

            then("해당 월의 시작일과 다음달의 시작일과 시/도 조건으로 지역별 간병 통계를 리포지토리로부터 조회한다.") {
                behavior()

                verify {
                    caregivingRoundRepository.getCaregivingStatisticsByPeriodIntersectAndState(
                        withArg { it shouldBe LocalDateTime.of(2023, 11, 1, 0, 0, 0) },
                        withArg { it shouldBe LocalDateTime.of(2023, 12, 1, 0, 0, 0) },
                        withArg { it shouldBe "서울특별시" },
                        withArg<Sort> {
                            it.getOrderFor(CaregivingRoundRepository.HOSPITAL_STATE_ORDERING)?.direction shouldBe Sort.Direction.ASC
                            it.getOrderFor(CaregivingRoundRepository.HOSPITAL_CITY_ORDERING)?.direction shouldBe Sort.Direction.ASC
                        },
                    )
                }
            }

            then("통계를 바탕으로 CSV를 생성하여 반환한다.") {
                val actualResult = behavior()

                actualResult shouldBe """
                    시/도,시/군/구,돌봄환자 수
                    서울특별시,강남구,24265
                    서울특별시,강동구,24265
                """.trimIndent()
            }
        }

        `when`("년/월로 시/도, 시/군/구 조건으로월별 지역별 간병 통계를 CSV 형식으로 조회하면") {
            val query = MonthlyRegionalCaregivingStatisticsByFilterQuery(
                year = 2023,
                month = 11,
                stateFilter = "서울특별시",
                cityFilter = "강남구",
                subject = generateInternalCaregivingManagerSubject(),
            )

            beforeEach {
                every {
                    caregivingRoundRepository.getCaregivingStatisticsByPeriodIntersectAndStateAndCity(
                        LocalDateTime.of(2023, 11, 1, 0, 0, 0),
                        LocalDateTime.of(2023, 12, 1, 0, 0, 0),
                        "서울특별시",
                        "강남구",
                        Sort.by(
                            Sort.Order.asc(CaregivingRoundRepository.HOSPITAL_STATE_ORDERING),
                            Sort.Order.asc(CaregivingRoundRepository.HOSPITAL_CITY_ORDERING),
                        )
                    )
                } returns listOf(
                    relaxedMock {
                        every { state } returns "서울특별시"
                        every { city } returns "강남구"
                        every { receptionCount } returns 24265
                    },
                )
            }

            afterEach { clearAllMocks() }

            fun behavior() = service.getMonthlyRegionalCaregivingStatisticsAsCsv(query)

            then("해당 월의 시작일과 다음달의 시작일과 시/도, 시/군/구 조건으로 지역별 간병 통계를 리포지토리로부터 조회한다.") {
                behavior()

                verify {
                    caregivingRoundRepository.getCaregivingStatisticsByPeriodIntersectAndStateAndCity(
                        withArg { it shouldBe LocalDateTime.of(2023, 11, 1, 0, 0, 0) },
                        withArg { it shouldBe LocalDateTime.of(2023, 12, 1, 0, 0, 0) },
                        withArg { it shouldBe "서울특별시" },
                        withArg { it shouldBe "강남구" },
                        withArg<Sort> {
                            it.getOrderFor(CaregivingRoundRepository.HOSPITAL_STATE_ORDERING)?.direction shouldBe Sort.Direction.ASC
                            it.getOrderFor(CaregivingRoundRepository.HOSPITAL_CITY_ORDERING)?.direction shouldBe Sort.Direction.ASC
                        },
                    )
                }
            }

            then("통계를 바탕으로 CSV를 생성하여 반환한다.") {
                val actualResult = behavior()

                actualResult shouldBe """
                    시/도,시/군/구,돌봄환자 수
                    서울특별시,강남구,24265
                """.trimIndent()
            }
        }

        `when`("내부 사용자 권한 없이 년/월 조건으로 월별 지역별 간병 통계를 CSV 형식으로조회하면") {
            val query = MonthlyRegionalCaregivingStatisticsByFilterQuery(
                year = 2023,
                month = 11,
                stateFilter = null,
                cityFilter = null,
                subject = generateExternalCaregivingOrganizationManagerSubject("01GSEKXGET5JKFST29B5K0N4XH")
            )

            fun behavior() = service.getMonthlyRegionalCaregivingStatisticsAsCsv(query)

            then("AccessDeniedException이 발생합니다.") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }
    }
})
