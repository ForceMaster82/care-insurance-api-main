package kr.caredoc.careinsurance.billing

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import kr.caredoc.careinsurance.caregiving.CaregivingCharge
import kr.caredoc.careinsurance.caregiving.CaregivingChargeConfirmStatus
import kr.caredoc.careinsurance.caregiving.CaregivingChargeModified
import kr.caredoc.careinsurance.caregiving.CaregivingRoundModified
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.generateSystemSubject
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.reception.CaregivingManagerInfo
import kr.caredoc.careinsurance.reception.OrganizationType
import kr.caredoc.careinsurance.reception.ReceptionModified
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.security.accesscontrol.ObjectAttribute
import kr.caredoc.careinsurance.transaction.TransactionType
import kr.caredoc.careinsurance.withFixedClock
import java.time.LocalDate
import java.time.LocalDateTime

class BillingTest : BehaviorSpec({
    given("청구 생성의 인자들이 잘 주어졌을 때") {
        `when`("가입담보 10년형으로 추가 시간없이 청구가 생성이 되면") {
            fun behavior() = createBillingForRenewalTenYear(
                LocalDateTime.of(2023, 3, 23, 9, 30, 15),
                LocalDateTime.of(2023, 3, 25, 9, 30, 15),
            )

            then("청구의 구성요소들이 계산됩니다.") {
                val billing = behavior()

                billing.basicAmounts.size shouldBe 2
                billing.basicAmounts[0].targetAccidentYear shouldBe 2022
                billing.basicAmounts[0].dailyCaregivingCharge shouldBe 100000
                billing.basicAmounts[0].caregivingDays shouldBe 1
                billing.basicAmounts[0].totalAmount shouldBe 100000
                billing.basicAmounts[1].targetAccidentYear shouldBe 2023
                billing.basicAmounts[1].dailyCaregivingCharge shouldBe 200000
                billing.basicAmounts[1].caregivingDays shouldBe 1
                billing.basicAmounts[1].totalAmount shouldBe 200000
            }

            then("청구 추가 시간이 계산된다.") {
                val billing = behavior()

                billing.additionalHours shouldBe 0
            }

            then("청구 추가 금액이 계산된다.") {
                val billing = behavior()

                billing.additionalAmount shouldBe 0
            }

            then("청구 총 금액이 계산된다.") {
                val billing = behavior()

                billing.totalAmount shouldBe 300000
            }

            then("청구가 생성되었음을 알리는 이벤트가 등록됩니다.") {
                val billing = behavior()

                val occurredEvent = billing.domainEvents.find { it is BillingGenerated } as BillingGenerated

                occurredEvent.caregivingRoundId shouldBe "01GW692NXFNWT7S85RJPYR9WVZ"
                occurredEvent.progressingStatus shouldBe BillingProgressingStatus.WAITING_FOR_BILLING
            }
        }

        and("가입담보 10년형으로 추가 시간이 4시간 미만으로 청구가 생성이 되면") {
            fun behavior() = createBillingForRenewalTenYear(
                LocalDateTime.of(2023, 3, 23, 9, 30, 15),
                LocalDateTime.of(2023, 3, 25, 12, 30, 15),
            )
            then("청구의 구성요소들이 계산됩니다.") {
                val billing = behavior()

                billing.basicAmounts.size shouldBe 2
                billing.basicAmounts[0].targetAccidentYear shouldBe 2022
                billing.basicAmounts[0].dailyCaregivingCharge shouldBe 100000
                billing.basicAmounts[0].caregivingDays shouldBe 1
                billing.basicAmounts[0].totalAmount shouldBe 100000
                billing.basicAmounts[1].targetAccidentYear shouldBe 2023
                billing.basicAmounts[1].dailyCaregivingCharge shouldBe 200000
                billing.basicAmounts[1].caregivingDays shouldBe 1
                billing.basicAmounts[1].totalAmount shouldBe 200000
            }
            then("청구 추가 시간이 계산된다.") {
                val billing = behavior()

                billing.additionalHours shouldBe 3
            }
            then("청구 추가 금액이 계산된다.") {
                val billing = behavior()

                billing.additionalAmount shouldBe 60000
            }
            then("청구 총 금액이 계산된다.") {
                val billing = behavior()

                billing.totalAmount shouldBe 360000
            }
        }
        and("가입담보 10년형으로 추가 시간이 4시간 이상으로 청구가 생성이 되면") {
            fun behavior() = createBillingForRenewalTenYear(
                LocalDateTime.of(2023, 3, 23, 9, 30, 15),
                LocalDateTime.of(2023, 3, 25, 13, 30, 15),
            )
            then("청구의 구성요소들이 계산됩니다.") {
                val billing = behavior()

                billing.basicAmounts.size shouldBe 2
                billing.basicAmounts[0].targetAccidentYear shouldBe 2022
                billing.basicAmounts[0].dailyCaregivingCharge shouldBe 100000
                billing.basicAmounts[0].caregivingDays shouldBe 1
                billing.basicAmounts[0].totalAmount shouldBe 100000
                billing.basicAmounts[1].targetAccidentYear shouldBe 2023
                billing.basicAmounts[1].dailyCaregivingCharge shouldBe 200000
                billing.basicAmounts[1].caregivingDays shouldBe 1
                billing.basicAmounts[1].totalAmount shouldBe 200000
            }
            then("청구 추가 시간이 계산된다.") {
                val billing = behavior()

                billing.additionalHours shouldBe 4
            }
            then("청구 추가 금액이 계산된다.") {
                val billing = behavior()

                billing.additionalAmount shouldBe 200000
            }
            then("청구 총 금액이 계산된다.") {
                val billing = behavior()

                billing.totalAmount shouldBe 500000
            }
        }
        and("가입담보 10년형으로 추가 시간이 4시간 이상으로 24시간에 가깝게 청구가 생성이 되면") {
            fun behavior() = createBillingForRenewalTenYear(
                LocalDateTime.of(2023, 3, 10, 1, 30, 15),
                LocalDateTime.of(2023, 3, 11, 23, 30, 15),
            )
            then("청구의 구성요소들이 계산됩니다.") {
                val billing = behavior()

                billing.basicAmounts.size shouldBe 1
                billing.basicAmounts[0].targetAccidentYear shouldBe 2022
                billing.basicAmounts[0].dailyCaregivingCharge shouldBe 100000
                billing.basicAmounts[0].caregivingDays shouldBe 1
                billing.basicAmounts[0].totalAmount shouldBe 100000
            }
            then("청구 추가 시간이 계산된다.") {
                val billing = behavior()

                billing.additionalHours shouldBe 22
            }
            then("청구 추가 금액이 계산된다.") {
                val billing = behavior()

                billing.additionalAmount shouldBe 100000
            }
            then("청구 총 금액이 계산된다.") {
                val billing = behavior()

                billing.totalAmount shouldBe 200000
            }
        }
        and("가입담보 3년형으로 추가 시간없이 청구가 생성이 되면") {
            fun behavior() = createBillingForRenewalThreeYear(
                LocalDateTime.of(2013, 3, 23, 9, 30, 15),
                LocalDateTime.of(2013, 3, 25, 9, 30, 15),
            )
            then("청구의 구성요소들이 계산됩니다.") {
                val billing = behavior()

                billing.basicAmounts.size shouldBe 1
                billing.basicAmounts[0].targetAccidentYear shouldBe 2012
                billing.basicAmounts[0].dailyCaregivingCharge shouldBe 50000
                billing.basicAmounts[0].caregivingDays shouldBe 2
                billing.basicAmounts[0].totalAmount shouldBe 100000
            }
            then("청구 추가 시간이 계산된다.") {
                val billing = behavior()

                billing.additionalHours shouldBe 0
            }
            then("청구 추가 금액이 계산된다.") {
                val billing = behavior()

                billing.additionalAmount shouldBe 0
            }
            then("청구 총 금액이 계산된다.") {
                val billing = behavior()

                billing.totalAmount shouldBe 100000
            }
        }

        and("가입담보 3년형으로 추가 시간이 4시간 미만으로 청구가 생성이 되면") {
            fun behavior() = createBillingForRenewalThreeYear(
                LocalDateTime.of(2013, 3, 23, 9, 30, 15),
                LocalDateTime.of(2013, 3, 25, 12, 30, 15),
            )
            then("청구의 구성요소들이 계산됩니다.") {
                val billing = behavior()

                billing.basicAmounts.size shouldBe 1
                billing.basicAmounts[0].targetAccidentYear shouldBe 2012
                billing.basicAmounts[0].dailyCaregivingCharge shouldBe 50000
                billing.basicAmounts[0].caregivingDays shouldBe 2
                billing.basicAmounts[0].totalAmount shouldBe 100000
            }
            then("청구 추가 시간이 계산된다.") {
                val billing = behavior()

                billing.additionalHours shouldBe 3
            }
            then("청구 추가 금액이 계산된다.") {
                val billing = behavior()

                billing.additionalAmount shouldBe 60000
            }
            then("청구 총 금액이 계산된다.") {
                val billing = behavior()

                billing.totalAmount shouldBe 160000
            }
        }
        and("가입담보 3년형으로 추가 시간이 4시간 이상으로 청구가 생성이 되면") {
            fun behavior() = createBillingForRenewalThreeYear(
                LocalDateTime.of(2013, 3, 23, 9, 30, 15),
                LocalDateTime.of(2013, 3, 25, 13, 30, 15),
            )
            then("청구의 구성요소들이 계산됩니다.") {
                val billing = behavior()

                billing.basicAmounts.size shouldBe 1
                billing.basicAmounts[0].targetAccidentYear shouldBe 2012
                billing.basicAmounts[0].dailyCaregivingCharge shouldBe 50000
                billing.basicAmounts[0].caregivingDays shouldBe 2
                billing.basicAmounts[0].totalAmount shouldBe 100000
            }
            then("청구 추가 시간이 계산된다.") {
                val billing = behavior()

                billing.additionalHours shouldBe 4
            }
            then("청구 추가 금액이 계산된다.") {
                val billing = behavior()

                billing.additionalAmount shouldBe 50000
            }
            then("청구 총 금액이 계산된다.") {
                val billing = behavior()

                billing.totalAmount shouldBe 150000
            }
        }
        and("가입담보 3년형으로 추가 시간이 4시간 이상으로 청약 갱신일에 간병 종료가 되도록 청구가 생성이 되면") {
            fun behavior() = createBillingForRenewalThreeYear(
                LocalDateTime.of(2015, 3, 21, 9, 30, 15),
                LocalDateTime.of(2015, 3, 24, 7, 30, 15),
            )
            then("청구의 구성요소들이 계산됩니다.") {
                val billing = behavior()

                billing.basicAmounts.size shouldBe 1
                billing.basicAmounts[0].targetAccidentYear shouldBe 2012
                billing.basicAmounts[0].dailyCaregivingCharge shouldBe 50000
                billing.basicAmounts[0].caregivingDays shouldBe 2
                billing.basicAmounts[0].totalAmount shouldBe 100000
            }

            then("청구 추가 시간이 계산된다.") {
                val billing = behavior()

                billing.additionalHours shouldBe 22
            }
            then("청구 추가 금액이 계산된다.") {
                val billing = behavior()

                billing.additionalAmount shouldBe 50000
            }
            then("청구 총 금액이 계산된다.") {
                val billing = behavior()

                billing.totalAmount shouldBe 150000
            }
        }
        and("가입담보 3년형으로 추가 시간없이 적용 연도가 2개에 포함되도록 청구가 생성이 되면") {
            fun behavior() = createBillingForRenewalThreeYear(
                LocalDateTime.of(2015, 3, 23, 9, 30, 15),
                LocalDateTime.of(2015, 3, 25, 9, 30, 15),
            )
            then("청구의 구성요소들이 계산됩니다.") {
                val billing = behavior()

                billing.basicAmounts.size shouldBe 2
                billing.basicAmounts[0].targetAccidentYear shouldBe 2012
                billing.basicAmounts[0].dailyCaregivingCharge shouldBe 50000
                billing.basicAmounts[0].caregivingDays shouldBe 1
                billing.basicAmounts[0].totalAmount shouldBe 50000
                billing.basicAmounts[1].targetAccidentYear shouldBe 2015
                billing.basicAmounts[1].dailyCaregivingCharge shouldBe 80000
                billing.basicAmounts[1].caregivingDays shouldBe 1
                billing.basicAmounts[1].totalAmount shouldBe 80000
            }
            then("청구 추가 시간이 계산된다.") {
                val billing = behavior()

                billing.additionalHours shouldBe 0
            }
            then("청구 추가 금액이 계산된다.") {
                val billing = behavior()

                billing.additionalAmount shouldBe 0
            }
            then("청구 총 금액이 계산된다.") {
                val billing = behavior()

                billing.totalAmount shouldBe 130000
            }
        }

        and("가입담보 3년형으로 추가 시간이 4시간 미만으로 적용 연도가 2개에 포함되도록 청구가 생성이 되면") {
            fun behavior() = createBillingForRenewalThreeYear(
                LocalDateTime.of(2015, 3, 23, 9, 30, 15),
                LocalDateTime.of(2015, 3, 25, 12, 30, 15),
            )
            then("청구의 구성요소들이 계산됩니다.") {
                val billing = behavior()


                billing.basicAmounts.size shouldBe 2
                billing.basicAmounts[0].targetAccidentYear shouldBe 2012
                billing.basicAmounts[0].dailyCaregivingCharge shouldBe 50000
                billing.basicAmounts[0].caregivingDays shouldBe 1
                billing.basicAmounts[0].totalAmount shouldBe 50000
                billing.basicAmounts[1].targetAccidentYear shouldBe 2015
                billing.basicAmounts[1].dailyCaregivingCharge shouldBe 80000
                billing.basicAmounts[1].caregivingDays shouldBe 1
                billing.basicAmounts[1].totalAmount shouldBe 80000
            }
            then("청구 추가 시간이 계산된다.") {
                val billing = behavior()

                billing.additionalHours shouldBe 3
            }
            then("청구 추가 금액이 계산된다.") {
                val billing = behavior()

                billing.additionalAmount shouldBe 60000
            }
            then("청구 총 금액이 계산된다.") {
                val billing = behavior()

                billing.totalAmount shouldBe 190000
            }
        }
        and("가입담보 3년형으로 추가 시간이 4시간 이상으로 적용 연도가 2개에 포함되도록 청구가 생성이 되면") {
            fun behavior() = createBillingForRenewalThreeYear(
                LocalDateTime.of(2015, 3, 23, 9, 30, 15),
                LocalDateTime.of(2015, 3, 25, 13, 30, 15),
            )
            then("청구의 구성요소들이 계산됩니다.") {
                val billing = behavior()

                billing.basicAmounts.size shouldBe 2
                billing.basicAmounts[0].targetAccidentYear shouldBe 2012
                billing.basicAmounts[0].dailyCaregivingCharge shouldBe 50000
                billing.basicAmounts[0].caregivingDays shouldBe 1
                billing.basicAmounts[0].totalAmount shouldBe 50000
                billing.basicAmounts[1].targetAccidentYear shouldBe 2015
                billing.basicAmounts[1].dailyCaregivingCharge shouldBe 80000
                billing.basicAmounts[1].caregivingDays shouldBe 1
                billing.basicAmounts[1].totalAmount shouldBe 80000
            }
            then("청구 추가 시간이 계산된다.") {
                val billing = behavior()

                billing.additionalHours shouldBe 4
            }
            then("청구 추가 금액이 계산된다.") {
                val billing = behavior()

                billing.additionalAmount shouldBe 80000
            }
            then("청구 총 금액이 계산된다.") {
                val billing = behavior()

                billing.totalAmount shouldBe 210000
            }
        }
        and("도착 후 취소 여부가 참인 청구가 시작일자와 종료일자가 같은 날로 생성이 되면") {
            fun behavior() = createBillingForIsCancelAfterArrived(
                startDateTime = LocalDateTime.of(2015, 3, 24, 12, 0, 0),
                endDateTime = LocalDateTime.of(2015, 3, 24, 12, 30, 0),
            )

            then("청구의 구성요소들에 계산됩니다.") {
                val billing = behavior()

                billing.basicAmounts.size shouldBe 1
                billing.basicAmounts[0].targetAccidentYear shouldBe 2015
                billing.basicAmounts[0].dailyCaregivingCharge shouldBe 80000
                billing.basicAmounts[0].caregivingDays shouldBe 1
                billing.basicAmounts[0].totalAmount shouldBe 80000
            }

            then("청구 추가 시간이 없습니다.") {
                val billing = behavior()

                billing.additionalHours shouldBe 0
            }
            then("청구 추가 금액이 없습니다.") {
                val billing = behavior()

                billing.additionalAmount shouldBe 0
            }
            then("청구 총 금액이 계산됩니다.") {
                val billing = behavior()

                billing.totalAmount shouldBe 80000
            }
        }

        and("도착 후 취소 여부가 참인 청구가 시작일자와 종료일자가 두개의 가입담보 기간에 걸쳐서 생성이 되면") {
            fun behavior() = createBillingForIsCancelAfterArrived(
                startDateTime = LocalDateTime.of(2015, 3, 21, 12, 0, 0),
                endDateTime = LocalDateTime.of(2015, 3, 25, 12, 30, 0),
            )

            then("청구의 구성요소들에 계산됩니다.") {
                val billing = behavior()

                billing.basicAmounts.size shouldBe 1
                billing.basicAmounts[0].targetAccidentYear shouldBe 2015
                billing.basicAmounts[0].dailyCaregivingCharge shouldBe 80000
                billing.basicAmounts[0].caregivingDays shouldBe 1
                billing.basicAmounts[0].totalAmount shouldBe 80000
            }

            then("청구 추가 시간이 없습니다.") {
                val billing = behavior()

                billing.additionalHours shouldBe 0
            }
            then("청구 추가 금액이 없습니다.") {
                val billing = behavior()

                billing.additionalAmount shouldBe 0
            }
            then("청구 총 금액이 계산됩니다.") {
                val billing = behavior()

                billing.totalAmount shouldBe 80000
            }

            then("생성된 청구는 ASSIGNED_ORGANIZATION_ID 접근 대상 속성이 비어있습니다..") {
                val settlement = behavior()

                settlement[ObjectAttribute.ASSIGNED_ORGANIZATION_ID].size shouldBe 0
            }
        }

        `when`("간병 관리자를 포함하여 정산을 생성하면") {
            fun behavior() = createBillingForIsCancelAfterArrived(
                startDateTime = LocalDateTime.of(2015, 3, 21, 12, 0, 0),
                endDateTime = LocalDateTime.of(2015, 3, 25, 12, 30, 0),
                caregivingManagerInfo = CaregivingManagerInfo(
                    organizationType = OrganizationType.ORGANIZATION,
                    organizationId = "01GSVWS32PWXHXD500V3FKRT6K",
                    managingUserId = "01GSVWSE5N26T9FY0NA6CMZFZS"
                )
            )

            then("생성된 청구는 ASSIGNED_ORGANIZATION_ID 접근 대상 속성을 가집니다.") {
                val settlement = behavior()

                settlement[ObjectAttribute.ASSIGNED_ORGANIZATION_ID] shouldContain "01GSVWS32PWXHXD500V3FKRT6K"
            }
        }
    }

    given("Billing 이 주어졌을 때") {
        lateinit var billing: Billing

        beforeEach {
            billing = createBillingForRenewalThreeYear(
                startDateTime = LocalDateTime.of(2023, 3, 23, 9, 30, 15),
                endDateTime = LocalDateTime.of(2023, 3, 25, 12, 30, 15),
            )
        }

        afterEach {
            clearAllMocks()
        }

        and("시작일시와 종료일시가 변경되지 않은 간병 회차 수정 사항 또한 주어졌을때") {
            val event = relaxedMock<CaregivingRoundModified>()

            beforeEach {
                with(event) {
                    every { startDateTime } returns Modification(
                        LocalDateTime.of(2023, 3, 23, 9, 30, 15),
                        LocalDateTime.of(2023, 3, 23, 9, 30, 15),
                    )
                    every { endDateTime } returns Modification(
                        LocalDateTime.of(2023, 3, 25, 12, 30, 15),
                        LocalDateTime.of(2023, 3, 25, 12, 30, 15),
                    )
                }
            }

            afterEach { clearAllMocks() }

            `when`("간병 회차 수정사항이 청구에 영향을 줄지 확인하면") {
                fun behavior() = billing.willBeAffectedBy(event)

                then("false를 반환합니다.") {
                    val expectedResult = false
                    val actualResult = behavior()

                    actualResult shouldBe expectedResult
                }
            }
        }

        and("시작일시가 변경된 간병 회차 수정 사항 또한 주어졌을때") {
            val event = relaxedMock<CaregivingRoundModified>()

            beforeEach {
                with(event) {
                    every { startDateTime } returns Modification(
                        LocalDateTime.of(2023, 3, 23, 9, 30, 15),
                        LocalDateTime.of(2023, 3, 22, 9, 30, 15),
                    )
                    every { endDateTime } returns Modification(
                        LocalDateTime.of(2023, 3, 25, 12, 30, 15),
                        LocalDateTime.of(2023, 3, 25, 12, 30, 15),
                    )
                }
            }

            afterEach { clearAllMocks() }

            `when`("간병 회차 수정사항이 청구에 영향을 줄지 확인하면") {
                fun behavior() = billing.willBeAffectedBy(event)

                then("true를 반환합니다.") {
                    val expectedResult = true
                    val actualResult = behavior()

                    actualResult shouldBe expectedResult
                }
            }
        }

        and("종료일시가 변경된 간병 회차 수정 사항 또한 주어졌을때") {
            val event = relaxedMock<CaregivingRoundModified>()

            beforeEach {
                with(event) {
                    every { startDateTime } returns Modification(
                        LocalDateTime.of(2023, 3, 23, 9, 30, 15),
                        LocalDateTime.of(2023, 3, 23, 9, 30, 15),
                    )
                    every { endDateTime } returns Modification(
                        LocalDateTime.of(2023, 3, 25, 12, 30, 15),
                        LocalDateTime.of(2023, 3, 26, 12, 30, 15),
                    )
                }
            }

            afterEach { clearAllMocks() }

            `when`("간병 회차 수정사항이 청구에 영향을 줄지 확인하면") {
                fun behavior() = billing.willBeAffectedBy(event)

                then("true를 반환합니다.") {
                    val expectedResult = true
                    val actualResult = behavior()

                    actualResult shouldBe expectedResult
                }
            }
        }

        and("시작일시와 종료일시가 변경된 간병 회차 수정 사항 또한 주어졌을때") {
            val event = relaxedMock<CaregivingRoundModified>()

            beforeEach {
                with(event) {
                    every { startDateTime } returns Modification(
                        LocalDateTime.of(2023, 3, 23, 9, 30, 15),
                        LocalDateTime.of(2023, 3, 22, 9, 30, 15),
                    )
                    every { endDateTime } returns Modification(
                        LocalDateTime.of(2023, 3, 25, 12, 30, 15),
                        LocalDateTime.of(2023, 3, 26, 9, 30, 15),
                    )
                }
            }

            afterEach { clearAllMocks() }

            `when`("간병 회차 수정사항이 청구에 영향을 줄지 확인하면") {
                fun behavior() = billing.willBeAffectedBy(event)

                then("true를 반환합니다.") {
                    val expectedResult = true
                    val actualResult = behavior()

                    actualResult shouldBe expectedResult
                }
            }

            `when`("수정사항과 가입담보 정보를 반영하면") {
                fun behavior() = billing.handleCaregivingRoundModified(
                    event,
                    CoverageInfo(
                        targetSubscriptionYear = 2012,
                        renewalType = CoverageInfo.RenewalType.THREE_YEAR,
                        annualCoveredCaregivingCharges = listOf(
                            CoverageInfo.AnnualCoveredCaregivingCharge(
                                targetAccidentYear = 2012,
                                caregivingCharge = 50000,
                            ),
                            CoverageInfo.AnnualCoveredCaregivingCharge(
                                targetAccidentYear = 2015,
                                caregivingCharge = 80000,
                            ),
                            CoverageInfo.AnnualCoveredCaregivingCharge(
                                targetAccidentYear = 2018,
                                caregivingCharge = 90000,
                            ),
                            CoverageInfo.AnnualCoveredCaregivingCharge(
                                targetAccidentYear = 2021,
                                caregivingCharge = 100000,
                            ),
                        )
                    ),
                )

                then("시작일자와 종료일자를 변경을 반영합니다.") {
                    billing.caregivingRoundInfo.startDateTime shouldBe LocalDateTime.of(2023, 3, 23, 9, 30, 15)
                    billing.caregivingRoundInfo.endDateTime shouldBe LocalDateTime.of(2023, 3, 25, 12, 30, 15)

                    behavior()

                    billing.caregivingRoundInfo.startDateTime shouldBe LocalDateTime.of(2023, 3, 22, 9, 30, 15)
                    billing.caregivingRoundInfo.endDateTime shouldBe LocalDateTime.of(2023, 3, 26, 9, 30, 15)
                }

                then("청구 금액을 계산합니다.") {
                    billing.totalAmount shouldBe 260000
                    billing.additionalHours shouldBe 3
                    billing.additionalAmount shouldBe 60000

                    behavior()

                    billing.totalAmount shouldBe 400000
                    billing.additionalHours shouldBe 0
                    billing.additionalAmount shouldBe 0
                }

                then("청구 금액 변경으로 이벤트를 발생시킵니다.") {
                    behavior()

                    val occurredEvent = billing.domainEvents.find { it is BillingModified } as BillingModified

                    occurredEvent.totalAmount.current shouldBe 400000
                }
            }
        }

        `when`("Billing 을 진행합니다.") {
            fun behavior() = billing.waitDeposit()

            then("Billing 의 billingProgressingStatus 를 미수로 변경합니다.") {
                billing.billingProgressingStatus shouldBe BillingProgressingStatus.WAITING_FOR_BILLING

                behavior()

                billing.billingProgressingStatus shouldBe BillingProgressingStatus.WAITING_DEPOSIT
            }

            then("Billing 의 청구 일자를 기록합니다.") {
                billing.billingDate shouldBe null

                withFixedClock(LocalDateTime.of(2023, 3, 27, 0, 0, 0)) {
                    behavior()
                }

                billing.billingDate shouldBe LocalDate.of(2023, 3, 27)
            }
            then("billing 의 수정 이벤트가 발생합니다.") {
                behavior()

                val occurredEvent = billing.domainEvents.find { it is BillingModified } as BillingModified

                occurredEvent.progressingStatus shouldBe Modification(
                    BillingProgressingStatus.WAITING_FOR_BILLING, BillingProgressingStatus.WAITING_DEPOSIT
                )
            }
        }
        `when`("입/출금 내역을 추가될 때") {
            val transactionSubjectId = "01GXQWSTWDHZW1H8B46Y5K2WT4"
            val subject = generateInternalCaregivingManagerSubject()

            beforeEach {
                billing.waitDeposit()
            }

            afterEach {
                clearAllMocks()
            }

            `when`("출금 내역이 추가되면") {
                fun behavior() = billing.recordTransaction(
                    BillingTransactionRecordingCommand(
                        transactionType = TransactionType.WITHDRAWAL,
                        amount = 50000,
                        transactionDate = LocalDate.of(2023, 4, 11),
                        transactionSubjectId = transactionSubjectId,
                        subject = subject,
                    )
                )

                afterEach { clearAllMocks() }

                then("출금 내역이 생성됩니다.") {
                    withFixedClock(LocalDateTime.of(2023, 4, 11, 12, 22, 33)) {
                        behavior()
                    }

                    billing.transactions[0].transactionType shouldBe TransactionType.WITHDRAWAL
                    billing.transactions[0].amount shouldBe 50000
                    billing.transactions[0].transactionDate shouldBe LocalDate.of(2023, 4, 11)
                    billing.transactions[0].enteredDateTime shouldBe LocalDateTime.of(2023, 4, 11, 12, 22, 33)
                    billing.transactions[0].transactionSubjectId shouldBe transactionSubjectId
                }

                then("총 출금 금액에 출금 금액이 증가합니다.") {
                    billing.totalWithdrawalAmount shouldBe 0

                    behavior()

                    billing.totalWithdrawalAmount shouldBe 50000
                }

                then("마지막 입출금 날짜가 현재 날짜로 변경됩니다.") {
                    billing.lastTransactionDate shouldBe null

                    withFixedClock(LocalDateTime.of(2023, 4, 11, 0, 0, 0)) {
                        behavior()
                    }

                    billing.lastTransactionDate shouldBe LocalDate.of(2023, 4, 11)
                }

                then("입출금 이벤트가 발생합니다.") {
                    behavior()

                    val occurredEvent = billing.domainEvents.find {
                        it is BillingTransactionRecorded
                    } as BillingTransactionRecorded

                    occurredEvent.amount shouldBe 50000
                    occurredEvent.transactionType shouldBe TransactionType.WITHDRAWAL
                    occurredEvent.receptionId shouldBe billing.receptionInfo.receptionId
                    occurredEvent.caregivingRoundId shouldBe billing.caregivingRoundInfo.caregivingRoundId
                }

                then("출금 금액에 따라 청구 상태를 반영합니다.") {
                    billing.billingProgressingStatus shouldBe BillingProgressingStatus.WAITING_DEPOSIT

                    behavior()

                    billing.billingProgressingStatus shouldBe BillingProgressingStatus.UNDER_DEPOSIT
                }
                then("청구 상태 변경 이벤트가 발생합니다.") {
                    behavior()

                    val occurredEvent = billing.domainEvents.find { it is BillingModified } as BillingModified

                    occurredEvent.receptionId shouldBe billing.receptionInfo.receptionId
                    occurredEvent.caregivingRoundId shouldBe billing.caregivingRoundInfo.caregivingRoundId
                    occurredEvent.progressingStatus.current shouldBe BillingProgressingStatus.UNDER_DEPOSIT // ModificationTracker 생성 시점 previous 의 값이 유지되어 임시 처리
                    occurredEvent.totalAmount.current shouldBe 260000
                }
            }
            `when`("청구 금액보다 작은 입금 내역이 추가되면") {
                fun behavior() = billing.recordTransaction(
                    BillingTransactionRecordingCommand(
                        transactionType = TransactionType.DEPOSIT,
                        amount = 50000,
                        transactionDate = LocalDate.of(2023, 4, 11),
                        transactionSubjectId = transactionSubjectId,
                        subject = subject,
                    )
                )

                afterEach { clearAllMocks() }

                then("입금 내역이 생성됩니다.") {
                    withFixedClock(LocalDateTime.of(2023, 4, 11, 12, 22, 33)) {
                        behavior()
                    }

                    billing.transactions[0].transactionType shouldBe TransactionType.DEPOSIT
                    billing.transactions[0].amount shouldBe 50000
                    billing.transactions[0].transactionDate shouldBe LocalDate.of(2023, 4, 11)
                    billing.transactions[0].enteredDateTime shouldBe LocalDateTime.of(2023, 4, 11, 12, 22, 33)
                    billing.transactions[0].transactionSubjectId shouldBe transactionSubjectId
                }

                then("총 입금 금액에 입금 금액이 증가합니다.") {
                    billing.totalDepositAmount shouldBe 0

                    behavior()

                    billing.totalDepositAmount shouldBe 50000
                }

                then("마지막 입출금 날짜가 현재 날짜로 변경됩니다.") {
                    billing.lastTransactionDate shouldBe null

                    withFixedClock(LocalDateTime.of(2023, 4, 11, 0, 0, 0)) {
                        behavior()
                    }

                    billing.lastTransactionDate shouldBe LocalDate.of(2023, 4, 11)
                }

                then("입출금 이벤트가 발생합니다.") {
                    behavior()

                    val occurredEvent = billing.domainEvents.find {
                        it is BillingTransactionRecorded
                    } as BillingTransactionRecorded

                    occurredEvent.amount shouldBe 50000
                    occurredEvent.transactionType shouldBe TransactionType.DEPOSIT
                    occurredEvent.receptionId shouldBe billing.receptionInfo.receptionId
                    occurredEvent.caregivingRoundId shouldBe billing.caregivingRoundInfo.caregivingRoundId
                }

                then("입금 금액에 따라 청구 상태를 반영합니다.") {
                    billing.billingProgressingStatus shouldBe BillingProgressingStatus.WAITING_DEPOSIT

                    behavior()

                    billing.billingProgressingStatus shouldBe BillingProgressingStatus.UNDER_DEPOSIT
                }

                then("청구 상태 변경 이벤트가 발생합니다.") {
                    behavior()

                    val occurredEvent = billing.domainEvents.find { it is BillingModified } as BillingModified

                    occurredEvent.receptionId shouldBe billing.receptionInfo.receptionId
                    occurredEvent.caregivingRoundId shouldBe billing.caregivingRoundInfo.caregivingRoundId
                    occurredEvent.progressingStatus.current shouldBe BillingProgressingStatus.UNDER_DEPOSIT // ModificationTracker 의 최초 previous 의 값이 유지되어 임시 처리
                    occurredEvent.totalAmount.current shouldBe 260000
                }
            }
            `when`("청구 금액만큼 입금 내역이 추가되면") {
                fun behavior() = billing.recordTransaction(
                    BillingTransactionRecordingCommand(
                        transactionType = TransactionType.DEPOSIT,
                        amount = 260000,
                        transactionDate = LocalDate.of(2023, 4, 11),
                        transactionSubjectId = transactionSubjectId,
                        subject = subject,
                    )
                )

                afterEach { clearAllMocks() }

                then("입금 내역이 생성됩니다.") {
                    behavior()

                    billing.transactions[0].transactionType shouldBe TransactionType.DEPOSIT
                    billing.transactions[0].amount shouldBe 260000
                    billing.transactions[0].transactionDate shouldBe LocalDate.of(2023, 4, 11)
                    billing.transactions[0].transactionSubjectId shouldBe transactionSubjectId
                }

                then("총 입금 금액에 입금 금액이 증가합니다.") {
                    billing.totalDepositAmount shouldBe 0

                    behavior()

                    billing.totalDepositAmount shouldBe 260000
                }

                then("입출금 이벤트가 발생합니다.") {
                    behavior()

                    val occurredEvent = billing.domainEvents.find {
                        it is BillingTransactionRecorded
                    } as BillingTransactionRecorded

                    occurredEvent.amount shouldBe 260000
                    occurredEvent.transactionType shouldBe TransactionType.DEPOSIT
                    occurredEvent.receptionId shouldBe billing.receptionInfo.receptionId
                    occurredEvent.caregivingRoundId shouldBe billing.caregivingRoundInfo.caregivingRoundId
                }

                then("입금 금액에 따라 청구 상태를 반영합니다.") {
                    billing.billingProgressingStatus shouldBe BillingProgressingStatus.WAITING_DEPOSIT

                    behavior()

                    billing.billingProgressingStatus shouldBe BillingProgressingStatus.COMPLETED_DEPOSIT
                }

                then("청구 상태 변경 이벤트가 발생합니다.") {
                    behavior()

                    val occurredEvent = billing.domainEvents.find { it is BillingModified } as BillingModified

                    occurredEvent.receptionId shouldBe billing.receptionInfo.receptionId
                    occurredEvent.caregivingRoundId shouldBe billing.caregivingRoundInfo.caregivingRoundId
                    occurredEvent.progressingStatus.current shouldBe BillingProgressingStatus.COMPLETED_DEPOSIT // ModificationTracker 생성 시점 previous 의 값이 유지되어 임시 처리
                    occurredEvent.totalAmount.current shouldBe 260000
                }
            }
            `when`("청구 금액보다 많은 입금 내역이 추가되면") {
                fun behavior() = billing.recordTransaction(
                    BillingTransactionRecordingCommand(
                        transactionType = TransactionType.DEPOSIT,
                        amount = 300000,
                        transactionDate = LocalDate.of(2023, 4, 11),
                        transactionSubjectId = transactionSubjectId,
                        subject = subject,
                    )
                )

                afterEach { clearAllMocks() }

                then("입금 내역이 생성됩니다.") {
                    behavior()

                    billing.transactions[0].transactionType shouldBe TransactionType.DEPOSIT
                    billing.transactions[0].amount shouldBe 300000
                    billing.transactions[0].transactionDate shouldBe LocalDate.of(2023, 4, 11)
                    billing.transactions[0].transactionSubjectId shouldBe transactionSubjectId
                }

                then("총 입금 금액에 입금 금액이 증가합니다.") {
                    billing.totalDepositAmount shouldBe 0

                    behavior()

                    billing.totalDepositAmount shouldBe 300000
                }

                then("입출금 이벤트가 발생합니다.") {
                    behavior()

                    val occurredEvent = billing.domainEvents.find {
                        it is BillingTransactionRecorded
                    } as BillingTransactionRecorded

                    occurredEvent.amount shouldBe 300000
                    occurredEvent.transactionType shouldBe TransactionType.DEPOSIT
                    occurredEvent.receptionId shouldBe billing.receptionInfo.receptionId
                    occurredEvent.caregivingRoundId shouldBe billing.caregivingRoundInfo.caregivingRoundId
                }

                then("입금 금액에 따라 청구 상태를 반영합니다.") {
                    billing.billingProgressingStatus shouldBe BillingProgressingStatus.WAITING_DEPOSIT

                    behavior()

                    billing.billingProgressingStatus shouldBe BillingProgressingStatus.OVER_DEPOSIT
                }

                then("청구 상태 변경 이벤트가 발생합니다.") {
                    behavior()

                    val occurredEvent = billing.domainEvents.find { it is BillingModified } as BillingModified

                    occurredEvent.receptionId shouldBe billing.receptionInfo.receptionId
                    occurredEvent.caregivingRoundId shouldBe billing.caregivingRoundInfo.caregivingRoundId
                    occurredEvent.progressingStatus.current shouldBe BillingProgressingStatus.OVER_DEPOSIT // ModificationTracker 생성 시점 previous 의 값이 유지되어 임시 처리
                    occurredEvent.totalAmount.current shouldBe 260000
                }
            }
        }

        `when`("도착 후 취소 여부가 수정되면") {
            fun behavior() = billing.handleCaregivingChargeModified(
                CaregivingChargeModified(
                    receptionId = "01GYY5GE4S8D31QJTC8GQ3ZHGC",
                    caregivingRoundId = "01GYY5GN80SDFDAV16K34PJSCP",
                    caregivingRoundNumber = 3,
                    basicAmount = 100000,
                    additionalAmount = 20000,
                    totalAmount = 120000,
                    expectedSettlementDate = Modification(LocalDate.of(2023, 4, 23), LocalDate.of(2023, 4, 26)),
                    confirmStatus = CaregivingChargeConfirmStatus.CONFIRMED,
                    editingSubject = generateSystemSubject(),
                    isCancelAfterArrived = Modification(previous = false, current = true),
                    additionalHoursCharge = Modification(20000, 40000),
                    mealCost = Modification(0, 10000),
                    transportationFee = Modification(30000, 400000),
                    holidayCharge = Modification(3, 4),
                    caregiverInsuranceFee = Modification(50000, 40000),
                    commissionFee = Modification(100000, 200000),
                    vacationCharge = Modification(0, 2),
                    patientConditionCharge = Modification(2, 4),
                    covid19TestingCost = Modification(150000, 25000),
                    additionalCharges = Modification(
                        previous = listOf(
                            CaregivingCharge.AdditionalCharge(
                                name = "추가비용1",
                                amount = 124000,
                            )
                        ),
                        current = listOf(
                            CaregivingCharge.AdditionalCharge(
                                name = "추가비용1",
                                amount = 124000,
                            ),
                            CaregivingCharge.AdditionalCharge(
                                name = "추가비용2",
                                amount = 240000,
                            ),
                        )
                    ),
                    outstandingAmount = Modification(35000, 155000),
                ),
                coverageInfo = CoverageInfo(
                    targetSubscriptionYear = 2012,
                    renewalType = CoverageInfo.RenewalType.THREE_YEAR,
                    annualCoveredCaregivingCharges = listOf(
                        CoverageInfo.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2012,
                            caregivingCharge = 50000,
                        ),
                        CoverageInfo.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2015,
                            caregivingCharge = 80000,
                        ),
                        CoverageInfo.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2018,
                            caregivingCharge = 90000,
                        ),
                        CoverageInfo.AnnualCoveredCaregivingCharge(
                            targetAccidentYear = 2021,
                            caregivingCharge = 100000,
                        ),
                    )
                )
            )

            then("도착 후 취소 여부를 반영합니다.") {
                billing.isCancelAfterArrived shouldBe false

                behavior()

                billing.isCancelAfterArrived shouldBe true
            }

            then("청구 금액을 계산합니다.") {
                billing.totalAmount shouldBe 260000
                billing.additionalHours shouldBe 3
                billing.additionalAmount shouldBe 60000

                behavior()

                billing.totalAmount shouldBe 100000
                billing.additionalHours shouldBe 0
                billing.additionalAmount shouldBe 0
            }

            then("청구 금액 변경으로 이벤트를 발생시킵니다.") {
                behavior()

                val occurredEvent = billing.domainEvents.find { it is BillingModified } as BillingModified

                occurredEvent.totalAmount.current shouldBe 100000
            }
        }

        `when`("접수정보가 변경되면") {
            val event = relaxedMock<ReceptionModified>()

            beforeEach {
                with(event) {
                    every { accidentInfo } returns Modification(
                        relaxedMock {
                            every { accidentNumber } returns "01GW6918R2H7NART1C8ACWCRD8"
                        },
                        relaxedMock {
                            every { accidentNumber } returns "2022-3333333"
                        }
                    )
                    every { insuranceInfo } returns Modification(
                        relaxedMock {
                            every { subscriptionDate } returns LocalDate.of(2012, 3, 24)
                        },
                        relaxedMock {
                            every { subscriptionDate } returns LocalDate.of(2012, 3, 25)
                        }
                    )
                    every { caregivingManagerInfo } returns Modification(
                        null,
                        CaregivingManagerInfo(
                            organizationType = OrganizationType.ORGANIZATION,
                            organizationId = "01GSVWS32PWXHXD500V3FKRT6K",
                            managingUserId = "01GSVWSE5N26T9FY0NA6CMZFZS"
                        )
                    )
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = billing.handleReceptionModified(event)

            then("청구의 접수정보가 변경됩니다.") {
                handling()

                billing.receptionInfo.accidentNumber shouldBe "2022-3333333"
                billing.receptionInfo.subscriptionDate shouldBe LocalDate.of(2012, 3, 25)
            }

            then("ASSIGNED_ORGANIZATION_ID 접근 대상 속성이 갱신됩니다.") {
                billing[ObjectAttribute.ASSIGNED_ORGANIZATION_ID].size shouldBe 0

                handling()

                billing[ObjectAttribute.ASSIGNED_ORGANIZATION_ID] shouldContain "01GSVWS32PWXHXD500V3FKRT6K"
            }
        }
    }
})

private fun createBillingForRenewalTenYear(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    caregivingManagerInfo: CaregivingManagerInfo? = null,
) = Billing(
    id = "01GW68NM47FX1C8KE6GCFYBBDK",
    receptionInfo = Billing.ReceptionInfo(
        receptionId = "01GW690V6Q14VKHH4PYE3M6FBY",
        accidentNumber = "01GW6918R2H7NART1C8ACWCRD8",
        subscriptionDate = LocalDate.of(2015, 3, 24),
    ),
    caregivingRoundInfo = Billing.CaregivingRoundInfo(
        caregivingRoundId = "01GW692NXFNWT7S85RJPYR9WVZ",
        roundNumber = 3,
        startDateTime = startDateTime,
        endDateTime = endDateTime,
    ),
    billingProgressingStatus = BillingProgressingStatus.WAITING_FOR_BILLING,
    coverageInfo = CoverageInfo(
        targetSubscriptionYear = 2012,
        renewalType = CoverageInfo.RenewalType.TEN_YEAR,
        annualCoveredCaregivingCharges = listOf(
            CoverageInfo.AnnualCoveredCaregivingCharge(
                targetAccidentYear = 2022, caregivingCharge = 100000
            ),
            CoverageInfo.AnnualCoveredCaregivingCharge(
                targetAccidentYear = 2023, caregivingCharge = 200000
            ),
        )
    ),
    isCancelAfterArrived = false,
    caregivingManagerInfo = caregivingManagerInfo,
)

private fun createBillingForRenewalThreeYear(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    caregivingManagerInfo: CaregivingManagerInfo? = null,
) = Billing(
    id = "01GW68NM47FX1C8KE6GCFYBBDK",
    receptionInfo = Billing.ReceptionInfo(
        receptionId = "01GW690V6Q14VKHH4PYE3M6FBY",
        accidentNumber = "01GW6918R2H7NART1C8ACWCRD8",
        subscriptionDate = LocalDate.of(2012, 3, 24),
    ),
    caregivingRoundInfo = Billing.CaregivingRoundInfo(
        caregivingRoundId = "01GW692NXFNWT7S85RJPYR9WVZ",
        roundNumber = 3,
        startDateTime = startDateTime,
        endDateTime = endDateTime,
    ),
    billingProgressingStatus = BillingProgressingStatus.WAITING_FOR_BILLING,
    coverageInfo = CoverageInfo(
        targetSubscriptionYear = 2012,
        renewalType = CoverageInfo.RenewalType.THREE_YEAR,
        annualCoveredCaregivingCharges = listOf(
            CoverageInfo.AnnualCoveredCaregivingCharge(
                targetAccidentYear = 2012,
                caregivingCharge = 50000,
            ),
            CoverageInfo.AnnualCoveredCaregivingCharge(
                targetAccidentYear = 2015,
                caregivingCharge = 80000,
            ),
            CoverageInfo.AnnualCoveredCaregivingCharge(
                targetAccidentYear = 2018,
                caregivingCharge = 90000,
            ),
            CoverageInfo.AnnualCoveredCaregivingCharge(
                targetAccidentYear = 2021,
                caregivingCharge = 100000,
            ),
        )
    ),
    isCancelAfterArrived = false,
    caregivingManagerInfo = caregivingManagerInfo,
)

private fun createBillingForIsCancelAfterArrived(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    caregivingManagerInfo: CaregivingManagerInfo? = null,
) = Billing(
    id = "01GYS2Y7HCXJKY85DNTPRGYB3D",
    receptionInfo = Billing.ReceptionInfo(
        receptionId = "01GW690V6Q14VKHH4PYE3M6FBY",
        accidentNumber = "01GW6918R2H7NART1C8ACWCRD8",
        subscriptionDate = LocalDate.of(2012, 3, 24),
    ),
    caregivingRoundInfo = Billing.CaregivingRoundInfo(
        caregivingRoundId = "01GW692NXFNWT7S85RJPYR9WVZ",
        roundNumber = 3,
        startDateTime = startDateTime,
        endDateTime = endDateTime,
    ),
    billingProgressingStatus = BillingProgressingStatus.WAITING_FOR_BILLING,
    coverageInfo = CoverageInfo(
        targetSubscriptionYear = 2012,
        renewalType = CoverageInfo.RenewalType.THREE_YEAR,
        annualCoveredCaregivingCharges = listOf(
            CoverageInfo.AnnualCoveredCaregivingCharge(
                targetAccidentYear = 2012,
                caregivingCharge = 50000,
            ),
            CoverageInfo.AnnualCoveredCaregivingCharge(
                targetAccidentYear = 2015,
                caregivingCharge = 80000,
            ),
            CoverageInfo.AnnualCoveredCaregivingCharge(
                targetAccidentYear = 2018,
                caregivingCharge = 90000,
            ),
            CoverageInfo.AnnualCoveredCaregivingCharge(
                targetAccidentYear = 2021,
                caregivingCharge = 100000,
            ),
        )
    ),
    isCancelAfterArrived = true,
    caregivingManagerInfo = caregivingManagerInfo,
)
