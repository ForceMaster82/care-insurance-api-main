package kr.caredoc.careinsurance.caregiving

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import kr.caredoc.careinsurance.caregiving.exception.CaregivingAdditionalChargeNameDuplicatedException
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.reception.CaregivingManagerInfo
import kr.caredoc.careinsurance.reception.OrganizationType
import kr.caredoc.careinsurance.reception.ReceptionModified
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.security.accesscontrol.ObjectAttribute
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import java.time.LocalDate
import java.time.LocalDateTime

class CaregivingChargeTest : BehaviorSpec({
    fun generateCaregivingCharge() = CaregivingCharge(
        id = "01GVD27R0YSGY0V48EXR2T0FNM",
        caregivingRoundInfo = CaregivingCharge.CaregivingRoundInfo(
            caregivingRoundId = "01GVD2HS5FMX9012BN28VHDPW3",
            caregivingRoundNumber = 1,
            startDateTime = LocalDateTime.of(2023, 3, 6, 14, 30, 0),
            endDateTime = LocalDateTime.of(2023, 3, 11, 16, 30, 0),
            dailyCaregivingCharge = 150000,
            receptionId = "01GVFY259Y6Z3Y5TZRVTAQD8T0",
        ),
        additionalHoursCharge = 0,
        mealCost = 0,
        transportationFee = 0,
        holidayCharge = 0,
        caregiverInsuranceFee = 0,
        commissionFee = 0,
        vacationCharge = 0,
        patientConditionCharge = 0,
        covid19TestingCost = 0,
        outstandingAmount = 0,
        additionalCharges = listOf(
            CaregivingCharge.AdditionalCharge(
                name = "특별 보상비",
                amount = 5000,
            ),
            CaregivingCharge.AdditionalCharge(
                name = "고객 보상비",
                amount = -10000,
            ),
        ),
        isCancelAfterArrived = true,
        expectedSettlementDate = LocalDate.of(2023, 4, 17),
    )

    lateinit var caregivingCharge: CaregivingCharge

    beforeEach {
        caregivingCharge = generateCaregivingCharge()
    }

    afterEach {
        clearAllMocks()
    }

    fun generateCaregivingChargeEditingCommand(
        subject: Subject = generateInternalCaregivingManagerSubject(),
        additionalCharges: List<CaregivingCharge.AdditionalCharge> = listOf(),
        confirmStatus: CaregivingChargeConfirmStatus = CaregivingChargeConfirmStatus.NOT_STARTED,
    ) = CaregivingChargeEditingCommand(
        additionalHoursCharge = 10000,
        mealCost = 0,
        transportationFee = 1500,
        holidayCharge = 50000,
        caregiverInsuranceFee = 15000,
        commissionFee = -5000,
        vacationCharge = 30000,
        patientConditionCharge = 50000,
        covid19TestingCost = 4500,
        outstandingAmount = 5000,
        additionalCharges = additionalCharges,
        isCancelAfterArrived = false,
        expectedSettlementDate = LocalDate.of(2023, 4, 18),
        caregivingChargeConfirmStatus = confirmStatus,
        subject = subject,
    )

    given("접수 수정사항이 주어졌을 때") {
        val event = relaxedMock<ReceptionModified>()

        beforeEach {
            every { event.caregivingManagerInfo } returns Modification(
                null,
                CaregivingManagerInfo(
                    organizationType = OrganizationType.ORGANIZATION,
                    organizationId = "01GSVWS32PWXHXD500V3FKRT6K",
                    managingUserId = "01GSVWSE5N26T9FY0NA6CMZFZS"
                )
            )
        }

        afterEach { clearAllMocks() }

        `when`("수정사항을 간병비 산정에 반영하면") {
            fun behavior() = caregivingCharge.handleReceptionModified(event)

            then("간병비 산정의 ASSIGNED_ORGANIZATION_ID 접근 대상 속성이 갱신됩니다.") {
                caregivingCharge[ObjectAttribute.ASSIGNED_ORGANIZATION_ID] shouldBe setOf()

                behavior()

                caregivingCharge[ObjectAttribute.ASSIGNED_ORGANIZATION_ID] shouldBe setOf("01GSVWS32PWXHXD500V3FKRT6K")
            }
        }
    }

    given("간병비 산정을 위한 인자들이 잘 주어졌을 때") {
        `when`("간병비 산정을 하면") {
            fun behavior() = generateCaregivingCharge()

            then("문제없이 생성된다.") {
                val actualResult = behavior()
                actualResult.caregivingChargeConfirmStatus shouldBe CaregivingChargeConfirmStatus.NOT_STARTED
                actualResult.basicAmount shouldBe 750000
                actualResult.additionalAmount shouldBe -5000
                actualResult.totalAmount shouldBe 745000
            }

            then("간병비가 산정되었음을 이벤트로 알린다.") {
                val generatedCaregivingCharge = behavior()

                val occurredEvent = generatedCaregivingCharge.domainEvents.find {
                    it is CaregivingChargeCalculated
                } as CaregivingChargeCalculated

                occurredEvent.receptionId shouldBe "01GVFY259Y6Z3Y5TZRVTAQD8T0"
                occurredEvent.caregivingRoundId shouldBe "01GVD2HS5FMX9012BN28VHDPW3"
                occurredEvent.roundNumber shouldBe 1
                occurredEvent.dailyCaregivingCharge shouldBe 150000
                occurredEvent.basicAmount shouldBe 750000
                occurredEvent.additionalAmount shouldBe -5000
                occurredEvent.totalAmount shouldBe 745000
                occurredEvent.expectedSettlementDate shouldBe LocalDate.of(2023, 4, 17)
                occurredEvent.isCancelAfterArrived shouldBe true
            }
        }

        `when`("내부 직원이 간병비 산정을 수정하면") {
            val sampleAdditionalCharges = listOf(
                CaregivingCharge.AdditionalCharge(
                    name = "특별 보상비",
                    amount = 5000,
                ),
                CaregivingCharge.AdditionalCharge(
                    name = "고객 보상비",
                    amount = -10000,
                ),
                CaregivingCharge.AdditionalCharge(
                    name = "세탁비",
                    amount = 15000,
                ),
            )

            val command = generateCaregivingChargeEditingCommand(
                additionalCharges = sampleAdditionalCharges,
            )

            fun behavior() = caregivingCharge.edit(command)

            then("간병비 산정은 수정된다.") {
                behavior()

                caregivingCharge.additionalHoursCharge shouldBe 10000
                caregivingCharge.mealCost shouldBe 0
                caregivingCharge.transportationFee shouldBe 1500
                caregivingCharge.holidayCharge shouldBe 50000
                caregivingCharge.caregiverInsuranceFee shouldBe 15000
                caregivingCharge.commissionFee shouldBe -5000
                caregivingCharge.vacationCharge shouldBe 30000
                caregivingCharge.patientConditionCharge shouldBe 50000
                caregivingCharge.covid19TestingCost shouldBe 4500
                caregivingCharge.outstandingAmount shouldBe 5000
                caregivingCharge.additionalCharges shouldBe listOf(
                    CaregivingCharge.AdditionalCharge(
                        name = "특별 보상비",
                        amount = 5000,
                    ),
                    CaregivingCharge.AdditionalCharge(
                        name = "고객 보상비",
                        amount = -10000,
                    ),
                    CaregivingCharge.AdditionalCharge(
                        name = "세탁비",
                        amount = 15000,
                    ),
                )
                caregivingCharge.isCancelAfterArrived shouldBe false
                caregivingCharge.expectedSettlementDate shouldBe LocalDate.of(2023, 4, 18)
                caregivingCharge.caregivingChargeConfirmStatus shouldBe CaregivingChargeConfirmStatus.NOT_STARTED
            }

            then("간병비 산정 금액들이 계상된다.") {
                behavior()

                caregivingCharge.basicAmount shouldBe 750000
                caregivingCharge.additionalAmount shouldBe 171000
                caregivingCharge.totalAmount shouldBe 921000
            }

            then("간병비가 재산정되었음을 알리는 이벤트가 등록된다.") {
                behavior()

                val occurredEvent = caregivingCharge.domainEvents.find {
                    it::class == CaregivingChargeModified::class
                } as CaregivingChargeModified

                occurredEvent.receptionId shouldBe "01GVFY259Y6Z3Y5TZRVTAQD8T0"
                occurredEvent.caregivingRoundId shouldBe "01GVD2HS5FMX9012BN28VHDPW3"
                occurredEvent.caregivingRoundNumber shouldBe 1
                occurredEvent.basicAmount shouldBe 750000
                occurredEvent.additionalAmount shouldBe 171000
                occurredEvent.totalAmount shouldBe 921000
                occurredEvent.additionalHoursCharge shouldBe Modification(0, 10000)
                occurredEvent.transportationFee shouldBe Modification(0, 1500)
                occurredEvent.holidayCharge shouldBe Modification(0, 50000)
                occurredEvent.caregiverInsuranceFee shouldBe Modification(0, 15000)
                occurredEvent.commissionFee shouldBe Modification(0, -5000)
                occurredEvent.vacationCharge shouldBe Modification(0, 30000)
                occurredEvent.patientConditionCharge shouldBe Modification(0, 50000)
                occurredEvent.covid19TestingCost shouldBe Modification(0, 4500)
                occurredEvent.additionalCharges shouldBe Modification(
                    listOf(
                        CaregivingCharge.AdditionalCharge(
                            name = "특별 보상비",
                            amount = 5000,
                        ),
                        CaregivingCharge.AdditionalCharge(
                            name = "고객 보상비",
                            amount = -10000,
                        ),
                    ),
                    listOf(
                        CaregivingCharge.AdditionalCharge(
                            name = "특별 보상비",
                            amount = 5000,
                        ),
                        CaregivingCharge.AdditionalCharge(
                            name = "고객 보상비",
                            amount = -10000,
                        ),
                        CaregivingCharge.AdditionalCharge(
                            name = "세탁비",
                            amount = 15000,
                        ),
                    ),
                )
                occurredEvent.outstandingAmount shouldBe Modification(0, 5000)
                occurredEvent.expectedSettlementDate shouldBe Modification(
                    LocalDate.of(2023, 4, 17),
                    LocalDate.of(2023, 4, 18)
                )
                occurredEvent.isCancelAfterArrived shouldBe Modification(previous = true, current = false)
            }
        }

        `when`("간병비 산정을 확정하면") {
            val command = generateCaregivingChargeEditingCommand(
                confirmStatus = CaregivingChargeConfirmStatus.CONFIRMED,
            )

            fun behavior() = caregivingCharge.edit(command)

            then("간병비가 확정되었음을 알리는 이벤트가 등록된다.") {
                behavior()

                val occurredEvent = caregivingCharge.domainEvents.find {
                    it::class == CaregivingChargeModified::class
                } as CaregivingChargeModified

                occurredEvent.confirmStatus shouldBe CaregivingChargeConfirmStatus.CONFIRMED
            }
        }
    }

    given("간병비 산정을 위한 인자들 중 추가 간병비 계정과목명이 중복으로 주어졌을 때") {
        `when`("간병비 산정을 수정 하면") {
            val duplicateAdditionalCharges = listOf(
                CaregivingCharge.AdditionalCharge(
                    name = "기타 비용 1",
                    amount = 5000,
                ),
                CaregivingCharge.AdditionalCharge(
                    name = "기타 비용 1",
                    amount = -10000,
                ),
            )

            val command = generateCaregivingChargeEditingCommand(
                additionalCharges = duplicateAdditionalCharges,
            )

            fun behavior() = caregivingCharge.edit(command)

            then("CaregivingAdditionalChargeNameDuplicatedException 발생 한다.") {
                val thrownException = shouldThrow<CaregivingAdditionalChargeNameDuplicatedException> { behavior() }

                thrownException.duplicatedAdditionalChargeNames shouldContainExactlyInAnyOrder listOf("기타 비용 1")
            }
        }
    }
})
