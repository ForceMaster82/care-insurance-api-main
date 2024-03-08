package kr.caredoc.careinsurance.reconciliation.statistics

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import kr.caredoc.careinsurance.generateExternalCaregivingOrganizationManagerSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.reconciliation.ClosingStatus
import kr.caredoc.careinsurance.reconciliation.ReconciliationRepository
import kr.caredoc.careinsurance.relaxedMock
import org.springframework.security.access.AccessDeniedException

class ReconciliationStatisticsServiceTest : BehaviorSpec({
    given("대사 통계 서비스가 주어졌을때") {
        val reconciliationRepository = relaxedMock<ReconciliationRepository>()
        val service = ReconciliationStatisticsService(
            reconciliationRepository
        )

        `when`("연/월로 특정된 대사 통계를 조회하면") {
            val query = MonthlyReconciliationStatisticsByYearAndMonthQuery(
                year = 2023,
                month = 11,
                subject = generateInternalCaregivingManagerSubject(),
            )

            fun behavior() = service.getMonthlyReconciliationStatistics(query)

            beforeEach {
                with(reconciliationRepository) {
                    every {
                        countDistinctReceptionIdByReconciledYearAndReconciledMonthAndClosingStatus(
                            2023,
                            11,
                            ClosingStatus.CLOSED,
                        )
                    } returns 74

                    every {
                        countDistinctReceptionCaregiverPhoneNumberCountByReconciledYearAndReconciledMonthAndClosingStatus(
                            2023,
                            11,
                            ClosingStatus.CLOSED,
                        )
                    } returns 78

                    every {
                        sumLatestReconciliationCaregivingSecondsByReconciledYearAndReconciledMonthAndClosingStatus(
                            2023,
                            11,
                            ClosingStatus.CLOSED,
                        )
                    } returns 51626473

                    every {
                        reconciliationRepository.sumReconciliationFinancialPropertiesByReconciledYearAndReconciledMonthAndClosingStatus(
                            2023,
                            11,
                            ClosingStatus.CLOSED,
                        )
                    } returns relaxedMock {
                        every { totalBillingAmount } returns 76997000
                        every { totalSettlementAmount } returns 75175100
                        every { totalSales } returns 1821900
                        every { totalDistributedProfit } returns 1093140
                    }
                }
            }

            afterEach { clearAllMocks() }

            then("해당 월의 중복을 제외한 접수 아이디 개수를 조회합니다.") {
                behavior()

                verify {
                    reconciliationRepository.countDistinctReceptionIdByReconciledYearAndReconciledMonthAndClosingStatus(
                        2023,
                        11,
                        ClosingStatus.CLOSED,
                    )
                }
            }

            then("해당 월의 중복을 제외한 접수별 간병인 전화번호 개수를 조회합니다.") {
                behavior()

                verify {
                    reconciliationRepository.countDistinctReceptionCaregiverPhoneNumberCountByReconciledYearAndReconciledMonthAndClosingStatus(
                        2023,
                        11,
                        ClosingStatus.CLOSED,
                    )
                }
            }

            then("간병회차별로 마지막으로 등록된 대사 자료의 간병 기간 합계를 조회합니다.") {
                behavior()

                verify {
                    reconciliationRepository.sumLatestReconciliationCaregivingSecondsByReconciledYearAndReconciledMonthAndClosingStatus(
                        2023,
                        11,
                        ClosingStatus.CLOSED,
                    )
                }
            }

            then("대사자료의 현금 흐름 및 통계 합계를 조회합니다.") {
                behavior()

                verify {
                    reconciliationRepository.sumReconciliationFinancialPropertiesByReconciledYearAndReconciledMonthAndClosingStatus(
                        2023,
                        11,
                        ClosingStatus.CLOSED,
                    )
                }
            }

            then("접수의 수, 간병인의 수, 청구금 합계, 정산금 합계, 이익 합계, 분배 이익 합계를 그대로 반영하여 반환합니다.") {
                val actualResult = behavior()

                actualResult?.receptionCount shouldBe 74
                actualResult?.caregiverCount shouldBe 78
                actualResult?.totalCaregivingPeriod shouldBe 597
                actualResult?.totalBillingAmount shouldBe 76997000
                actualResult?.totalSettlementAmount shouldBe 75175100
                actualResult?.totalSales shouldBe 1821900
                actualResult?.totalDistributedProfit shouldBe 1093140
            }

            then("총 간병기간(초)를 일단위로 나눠 나머지를 절사한 값을 반환합니다.") {
                val actualResult = behavior()

                actualResult?.totalCaregivingPeriod shouldBe 597
            }
        }

        `when`("내부 사용자 권한 없이 대사 통계를 조회하면") {
            val query = MonthlyReconciliationStatisticsByYearAndMonthQuery(
                year = 2023,
                month = 11,
                subject = generateExternalCaregivingOrganizationManagerSubject("01GPWXVJB2WPDNXDT5NE3B964N")
            )

            fun behavior() = service.getMonthlyReconciliationStatistics(query)

            then("AccessDeniedException이 발생합니다.") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }
    }
})
