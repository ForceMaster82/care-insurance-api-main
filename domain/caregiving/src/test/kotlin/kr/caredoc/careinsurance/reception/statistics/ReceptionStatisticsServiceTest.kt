package kr.caredoc.careinsurance.reception.statistics

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import kr.caredoc.careinsurance.billing.BillingModified
import kr.caredoc.careinsurance.billing.BillingTransactionRecorded
import kr.caredoc.careinsurance.generateExternalCaregivingOrganizationManagerSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.reception.Reception
import kr.caredoc.careinsurance.reception.ReceptionByIdQueryHandler
import kr.caredoc.careinsurance.reception.ReceptionModified
import kr.caredoc.careinsurance.reception.ReceptionReceived
import kr.caredoc.careinsurance.relaxedMock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.AccessDeniedException
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
class ReceptionStatisticsServiceTest(
    @Autowired
    private val cacheReceptionStatisticsRepository: DailyReceptionStatisticsRepository,
) : BehaviorSpec({
    given("간병 접수 서비스가 주어졌을때") {
        val dailyReceptionStatisticsRepository = relaxedMock<DailyReceptionStatisticsRepository>()
        val receptionByIdQueryHandler = relaxedMock<ReceptionByIdQueryHandler>()
        val service = ReceptionStatisticsService(
            dailyReceptionStatisticsRepository = dailyReceptionStatisticsRepository,
            receptionByIdQueryHandler = receptionByIdQueryHandler,
        )

        val registeredDailyReceptionStatistics = listOf<DailyReceptionStatistics>(
            relaxedMock(),
            relaxedMock(),
        )

        beforeEach {
            with(registeredDailyReceptionStatistics[0]) {
                every { receivedDate } returns LocalDate.of(2022, 11, 1)
                every { receptionCount } returns 21
                every { canceledReceptionCount } returns 2
                every { canceledByPersonalCaregiverReceptionCount } returns 1
                every { canceledByMedicalRequestReceptionCount } returns 0
                every { requestedBillingCount } returns 79
                every { requestedBillingAmount } returns 44893000
                every { depositCount } returns 74
                every { depositAmount } returns 42809000
                every { withdrawalCount } returns 0
                every { withdrawalAmount } returns 0
                every { sameDayAssignmentReceptionCount } returns 6
                every { startedSameDayAssignmentReceptionCount } returns 5
                every { shortTermReceptionCount } returns 2
                every { startedShortTermReceptionCount } returns 2
            }
            with(registeredDailyReceptionStatistics[1]) {
                every { receivedDate } returns LocalDate.of(2022, 11, 2)
                every { receptionCount } returns 19
                every { canceledReceptionCount } returns 1
                every { canceledByPersonalCaregiverReceptionCount } returns 2
                every { canceledByMedicalRequestReceptionCount } returns 0
                every { requestedBillingCount } returns 22
                every { requestedBillingAmount } returns 13108000
                every { depositCount } returns 19
                every { depositAmount } returns 12333000
                every { withdrawalCount } returns 0
                every { withdrawalAmount } returns 0
                every { sameDayAssignmentReceptionCount } returns 7
                every { startedSameDayAssignmentReceptionCount } returns 5
                every { shortTermReceptionCount } returns 2
                every { startedShortTermReceptionCount } returns 2
            }

            with(dailyReceptionStatisticsRepository) {
                every {
                    findByReceivedDateBetweenOrderByReceivedDate(
                        match {
                            it.year == 2022 && it.monthValue == 11
                        },
                        match {
                            it.year == 2022 && it.monthValue == 11
                        },
                    )
                } returns registeredDailyReceptionStatistics

                every {
                    dailyReceptionStatisticsRepository.findByReceivedDateForUpdate(LocalDate.of(2022, 11, 1))
                } returns listOf(registeredDailyReceptionStatistics[0])

                every {
                    dailyReceptionStatisticsRepository.findByReceivedDateForUpdate(LocalDate.of(2022, 11, 2))
                } returns listOf(registeredDailyReceptionStatistics[1])
            }

            with(receptionByIdQueryHandler) {
                every { getReception(match { it.receptionId == "01GWVQRD04F6213P3QS3VCWJPA" }) } returns relaxedMock {
                    every { id } returns "01GWVQRD04F6213P3QS3VCWJPA"
                    every { receivedDateTime } returns LocalDateTime.of(2022, 11, 1, 20, 3, 12)
                }
            }
        }

        afterEach {
            clearAllMocks()
        }

        `when`("기간 내의 날짜별 간병 접수 통계를 조회하면") {
            val query = DailyReceptionStatisticsByDateRangeQuery(
                from = LocalDate.of(2022, 11, 1),
                until = LocalDate.of(2022, 11, 30),
                subject = generateInternalCaregivingManagerSubject()
            )

            fun behavior() = service.getDailyReceptionStatistics(query)

            then("리포지토리에서 날짜별 간병 접수를 조회합니다.") {
                behavior()

                verify {
                    dailyReceptionStatisticsRepository.findByReceivedDateBetweenOrderByReceivedDate(
                        from = LocalDate.of(2022, 11, 1),
                        until = LocalDate.of(2022, 11, 30),
                    )
                }
            }

            then("조회된 간병 접수 통계를 응답합니다.") {
                val actualResult = behavior()

                actualResult shouldBe registeredDailyReceptionStatistics
            }
        }

        `when`("기간 내의 날짜별 간병 접수 통계를 CSV 형식으로 조회하면") {
            val query = DailyReceptionStatisticsByDateRangeQuery(
                from = LocalDate.of(2022, 11, 1),
                until = LocalDate.of(2022, 11, 30),
                subject = generateInternalCaregivingManagerSubject()
            )

            fun behavior() = service.getDailyReceptionStatisticsAsCsv(query)

            then("리포지토리에서 날짜별 간병 접수를 조회합니다.") {
                behavior()

                verify {
                    dailyReceptionStatisticsRepository.findByReceivedDateBetweenOrderByReceivedDate(
                        from = LocalDate.of(2022, 11, 1),
                        until = LocalDate.of(2022, 11, 30),
                    )
                }
            }

            then("조회된 간병 접수 통계를 응답합니다.") {
                val actualResult = behavior()

                actualResult shouldBe """
                  구분,요일,전체 배정 건,간병인 파견 건,개인구인 건,석션 등 의료행위,취소 건,지급 요청 건,지급요청금액,지급받은 건,지급받은 금액,환수 건,환수금액,민원 건,당일배정요청 건,배정 건,단기 요청 건,배정 건
                  2022-11-01,화,21,19,1,0,2,79,44893000,74,42809000,0,0,0,6,5,2,2
                  2022-11-02,수,19,18,2,0,1,22,13108000,19,12333000,0,0,0,7,5,2,2
                """.trimIndent()
            }
        }

        `when`("내부 사용자가 아닌 사용자가 기간 내의 날짜별 간병 접수 통계를 조회하면") {
            val query = DailyReceptionStatisticsByDateRangeQuery(
                from = LocalDate.of(2022, 11, 1),
                until = LocalDate.of(2022, 11, 30),
                subject = generateExternalCaregivingOrganizationManagerSubject("01GRWDRWTM6ENFSQXTDN9HDDWK")
            )

            fun behavior() = service.getDailyReceptionStatistics(query)

            then("AccessDeniedException이 발생합니다.") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }

        `when`("ReceptionReceived 이벤트 발생을 감지하면") {
            val event = ReceptionReceived(
                receptionId = "01GWVQRD04F6213P3QS3VCWJPA",
                receivedDateTime = LocalDateTime.of(2022, 11, 1, 20, 3, 12),
                desiredCaregivingStartDate = LocalDate.of(2022, 11, 2),
                urgency = Reception.Urgency.URGENT,
                periodType = Reception.PeriodType.NORMAL,
            )

            fun handling() = service.handleReceptionReceived(event)

            then("접수가 발생한 날짜의 통계를 조회합니다.") {
                handling()

                verify {
                    dailyReceptionStatisticsRepository.findByReceivedDateForUpdate(LocalDate.of(2022, 11, 1))
                }
            }

            then("접수가 발생한 날짜의 통계에 이벤트를 전달합니다.") {
                handling()

                verify {
                    registeredDailyReceptionStatistics[0].handleReceptionReceived(event)
                }
            }
        }

        `when`("ReceptionModified 이벤트 발생을 감지하면") {
            val event = relaxedMock<ReceptionModified>()

            beforeEach {
                with(event) {
                    every { receivedDateTime } returns LocalDateTime.of(2022, 11, 1, 20, 3, 12)
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = service.handleReceptionModified(event)

            then("접수가 발생한 날짜의 통계를 조회합니다.") {
                handling()

                verify {
                    dailyReceptionStatisticsRepository.findByReceivedDateForUpdate(LocalDate.of(2022, 11, 1))
                }
            }

            then("접수가 발생한 날짜의 통계에 이벤트를 전달합니다.") {
                handling()

                verify {
                    registeredDailyReceptionStatistics[0].handleReceptionModified(event)
                }
            }
        }

        `when`("BillingModified 이벤트 발생을 감지하면") {
            val event = relaxedMock<BillingModified>()

            beforeEach {
                with(event) {
                    every { receptionId } returns "01GWVQRD04F6213P3QS3VCWJPA"
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = service.handleBillingModified(event)

            then("청구가 발생한 접수를 조회합니다.") {
                handling()

                verify {
                    receptionByIdQueryHandler.getReception(
                        withArg {
                            it.receptionId shouldBe "01GWVQRD04F6213P3QS3VCWJPA"
                        }
                    )
                }
            }

            then("접수가 발생한 날짜의 통계에 이벤트를 전달합니다.") {
                handling()

                verify {
                    registeredDailyReceptionStatistics[0].handleBillingModified(event)
                }
            }
        }

        `when`("BillingTransactionRecorded 이벤트 발생을 감지하면") {
            val event = relaxedMock<BillingTransactionRecorded>()

            beforeEach {
                with(event) {
                    every { receptionId } returns "01GWVQRD04F6213P3QS3VCWJPA"
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = service.handleBillingTransactionRecorded(event)

            then("청구가 발생한 접수를 조회합니다.") {
                handling()

                verify {
                    receptionByIdQueryHandler.getReception(
                        withArg {
                            it.receptionId shouldBe "01GWVQRD04F6213P3QS3VCWJPA"
                        }
                    )
                }
            }

            then("접수가 발생한 날짜의 통계에 이벤트를 전달합니다.") {
                handling()

                verify {
                    registeredDailyReceptionStatistics[0].handleBillingTransactionRecorded(event)
                }
            }
        }

        and("일별 접수 통계 엔티티 테스트할 때") {
            val receptionStatistics = DailyReceptionStatistics(
                id = "01HFNA9PXG978C3RABJHHAS38X",
                receivedDate = LocalDate.of(2023, 11, 20)
            )
            `when`("저장을 요청하면") {
                fun behavior() = cacheReceptionStatisticsRepository.save(receptionStatistics)
                then("저장이 됩니다.") {
                    behavior()
                }
            }

            `when`("조회를 요청하면") {
                fun behavior() = cacheReceptionStatisticsRepository.findByIdOrNull("01HFNA9PXG978C3RABJHHAS38X")
                then("조회가 됩니다.") {
                    behavior()
                }
            }
        }
    }
})
