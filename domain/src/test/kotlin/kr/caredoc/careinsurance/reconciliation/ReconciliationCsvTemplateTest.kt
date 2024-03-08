package kr.caredoc.careinsurance.reconciliation

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationsByIdsQueryHandler
import kr.caredoc.careinsurance.caregiving.CaregivingRoundsByIdsQueryHandler
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.relaxedMock

class ReconciliationCsvTemplateTest : BehaviorSpec({
    given("대사 CSV 템플릿이 주어졌을때") {
        val caregivingRoundsByIdsQueryHandler = relaxedMock<CaregivingRoundsByIdsQueryHandler>()
        val externalCaregivingOrganizationsByIdsQueryHandler =
            relaxedMock<ExternalCaregivingOrganizationsByIdsQueryHandler>()
        val template = ReconciliationCsvTemplate(
            caregivingRoundsByIdsQueryHandler,
            externalCaregivingOrganizationsByIdsQueryHandler,
        )

        and("또한 템플리팅 할 대사 목록과 관련 도메인 객체들이 주어졌을때") {
            val reconciliations = listOf<Reconciliation>(
                relaxedMock(),
                relaxedMock(),
            )

            beforeEach {
                with(reconciliations[0]) {
                    every { caregivingRoundId } returns "01GW692NXFNWT7S85RJPYR9WVZ"
                    every { billingAmount } returns 625000
                    every { settlementAmount } returns 590000
                    every { settlementWithdrawalAmount } returns 0
                    every { settlementDepositAmount } returns 0
                    every { profit } returns 35000
                    every { distributedProfit } returns 21000
                }
                with(reconciliations[1]) {
                    every { caregivingRoundId } returns "01GW692NXFNWT7S85RJPYR9WVZ"
                    every { billingAmount } returns -70000
                    every { settlementAmount } returns 0
                    every { settlementWithdrawalAmount } returns 0
                    every { settlementDepositAmount } returns 0
                    every { profit } returns -70000
                    every { distributedProfit } returns -42000
                }

                with(caregivingRoundsByIdsQueryHandler) {
                    every {
                        getCaregivingRounds(match { it.caregivingRoundIds.contains("01GW692NXFNWT7S85RJPYR9WVZ") })
                    } returns listOf(
                        relaxedMock {
                            every { id } returns "01GW692NXFNWT7S85RJPYR9WVZ"
                            every { receptionInfo.accidentNumber } returns "2022-1234567"
                            every { receptionInfo.maskedPatientName } returns "홍*동"
                            every { receptionInfo.caregivingManagerInfo.organizationId } returns "01GPWXVJB2WPDNXDT5NE3B964N"
                            every { caregivingRoundNumber } returns 3
                            every { caregiverInfo?.name } returns "오간병"
                        }
                    )
                }

                with(externalCaregivingOrganizationsByIdsQueryHandler) {
                    every {
                        getExternalCaregivingOrganizations(match { it.externalCaregivingOrganizationIds.contains("01GPWXVJB2WPDNXDT5NE3B964N") })
                    } returns listOf(
                        relaxedMock {
                            every { id } returns "01GPWXVJB2WPDNXDT5NE3B964N"
                            every { name } returns "케어라인"
                        }
                    )
                }
            }

            afterEach { clearAllMocks() }

            `when`("대사 목록을 템플리팅하면") {
                fun behavior() = template.generate(reconciliations, generateInternalCaregivingManagerSubject())

                then("대사해야 하는 간병 회차 목록을 조회합니다.") {
                    behavior()

                    verify {
                        caregivingRoundsByIdsQueryHandler.getCaregivingRounds(
                            withArg {
                                it.caregivingRoundIds shouldContain "01GW692NXFNWT7S85RJPYR9WVZ"
                            }
                        )
                    }
                }

                then("간병 회차를 담당하는 외부 간병 업체 목록을 조회합니다.") {
                    behavior()

                    verify {
                        externalCaregivingOrganizationsByIdsQueryHandler.getExternalCaregivingOrganizations(
                            withArg {
                                it.externalCaregivingOrganizationIds shouldContain "01GPWXVJB2WPDNXDT5NE3B964N"
                            }
                        )
                    }
                }

                then("대사 목록을 CSV 형식으로 생성하여 응답합니다.") {
                    val actualResult = behavior()

                    actualResult shouldBe """
                        사고번호,고객명,간병차수,청구금액,간병인명,정산금액,출금액,입금액,회차수익,케어닥수익,제휴사,분배수익
                        2022-1234567,홍*동,3,625000,오간병,590000,0,0,35000,14000,케어라인,21000
                        2022-1234567,홍*동,3,-70000,오간병,0,0,0,-70000,-28000,케어라인,-42000
                    """.trimIndent()
                }
            }
        }
    }
})
