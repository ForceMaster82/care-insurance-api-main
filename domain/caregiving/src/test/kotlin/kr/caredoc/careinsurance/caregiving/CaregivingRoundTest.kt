package kr.caredoc.careinsurance.caregiving

import com.github.guepardoapps.kulid.ULID
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkObject
import kr.caredoc.careinsurance.account.AccountInfo
import kr.caredoc.careinsurance.billing.BillingGenerated
import kr.caredoc.careinsurance.billing.BillingModified
import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import kr.caredoc.careinsurance.caregiving.state.CancellationReason
import kr.caredoc.careinsurance.caregiving.state.FinishingReason
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.patient.Sex
import kr.caredoc.careinsurance.reception.CaregivingManagerInfo
import kr.caredoc.careinsurance.reception.OrganizationType
import kr.caredoc.careinsurance.reception.ReceptionModified
import kr.caredoc.careinsurance.reception.ReceptionProgressingStatus
import kr.caredoc.careinsurance.reconciliation.ReconciliationClosed
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.settlement.SettlementGenerated
import kr.caredoc.careinsurance.settlement.SettlementModified
import kr.caredoc.careinsurance.settlement.SettlementProgressingStatus
import kr.caredoc.careinsurance.withFixedClock
import org.mockito.ArgumentMatchers.any
import java.time.LocalDate
import java.time.LocalDateTime

class CaregivingRoundTest : BehaviorSpec({
    given("간병 회차가 주어졌을때") {
        lateinit var caregivingRound: CaregivingRound

        beforeEach {
            caregivingRound = CaregivingRound(
                id = "01GVATYRYEPXMNDP9MBC540T1E",
                caregivingRoundNumber = 1,
                receptionInfo = CaregivingRound.ReceptionInfo(
                    receptionId = "01GVAV4T8AP2SWE71SXZMWB1Z9",
                    insuranceNumber = "12345-12345",
                    accidentNumber = "2023-1234567",
                    maskedPatientName = "김*자",
                    receptionProgressingStatus = ReceptionProgressingStatus.RECEIVED,
                    expectedCaregivingStartDate = null,
                    caregivingManagerInfo = CaregivingManagerInfo(
                        organizationType = OrganizationType.INTERNAL,
                        organizationId = null,
                        managingUserId = "01GQ23MVTBAKS526S0WGS9CS0A"
                    )
                ),
            )
        }

        afterEach { clearAllMocks() }

        `when`("수정된 접수 데이터가 접수되면") {
            val event = relaxedMock<ReceptionModified> {
                every { insuranceInfo.current.insuranceNumber } returns "22222-11111"
                every { accidentInfo.current.accidentNumber } returns "2022-2222222"
                every { patientInfo.current.name.masked } returns "방*영"
                every { progressingStatus } returns Modification(
                    ReceptionProgressingStatus.MATCHING,
                    ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS,
                )
                every { expectedCaregivingStartDate.current } returns LocalDate.of(2023, 3, 12)
                every { caregivingManagerInfo.current } returns relaxedMock {
                    every { organizationType } returns OrganizationType.AFFILIATED
                    every { organizationId } returns "01GPWXVJB2WPDNXDT5NE3B964N"
                    every { managingUserId } returns "01GR8BNHFPYQW55PNGKHAKBNS6"
                }
            }

            fun handling() = caregivingRound.handleReceptionModified(event)

            then("간병 회차의 접수 데이터가 수정된다.") {
                handling()

                caregivingRound.receptionInfo.insuranceNumber shouldBe "22222-11111"
                caregivingRound.receptionInfo.accidentNumber shouldBe "2022-2222222"
                caregivingRound.receptionInfo.maskedPatientName shouldBe "방*영"
                caregivingRound.receptionInfo.receptionProgressingStatus shouldBe ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS
                caregivingRound.receptionInfo.expectedCaregivingStartDate shouldBe LocalDate.of(2023, 3, 12)
                caregivingRound.receptionInfo.caregivingManagerInfo.organizationType shouldBe OrganizationType.AFFILIATED
                caregivingRound.receptionInfo.caregivingManagerInfo.organizationId shouldBe "01GPWXVJB2WPDNXDT5NE3B964N"
                caregivingRound.receptionInfo.caregivingManagerInfo.managingUserId shouldBe "01GR8BNHFPYQW55PNGKHAKBNS6"
            }
        }

        `when`("메모를 수정하면") {
            fun behavior() = caregivingRound.updateRemarks("메모", generateInternalCaregivingManagerSubject())

            then("메모가 수정됩니다.") {
                caregivingRound.remarks shouldBe ""

                behavior()

                caregivingRound.remarks shouldBe "메모"
            }
        }

        and("청구 진행상태 변경사항 또한 주어졌을때") {
            val event = relaxedMock<BillingModified>()

            beforeEach {
                with(event) {
                    every { progressingStatus } returns Modification(
                        BillingProgressingStatus.NOT_STARTED,
                        BillingProgressingStatus.WAITING_FOR_BILLING,
                    )
                }
            }

            afterEach { clearAllMocks() }

            `when`("간병 회차에 영향을 주는지 확인하면") {
                fun behavior() = caregivingRound.willBeAffectedBy(event)

                then("true를 반환합니다.") {
                    val expectedResult = true
                    val actualResult = behavior()

                    actualResult shouldBe expectedResult
                }
            }

            `when`("변경사항을 간병 회차에 반영하면") {
                fun handling() = caregivingRound.handleBillingModified(event)

                then("간병 회차의 청구 상태가 변경됩니다.") {
                    handling()

                    caregivingRound.billingProgressingStatus shouldBe BillingProgressingStatus.WAITING_FOR_BILLING
                }
            }
        }

        and("청구 상태가 변경되지 않은 청구 변경사항 또한 주어졌을때") {
            val event = relaxedMock<BillingModified>()

            beforeEach {
                with(event) {
                    every { progressingStatus } returns Modification(
                        BillingProgressingStatus.NOT_STARTED,
                        BillingProgressingStatus.NOT_STARTED,
                    )
                }
            }

            afterEach { clearAllMocks() }

            `when`("간병 회차에 영향을 주는지 확인하면") {
                fun behavior() = caregivingRound.willBeAffectedBy(event)

                then("false를 반환합니다.") {
                    val expectedResult = false
                    val actualResult = behavior()

                    actualResult shouldBe expectedResult
                }
            }
        }

        and("정산 진행상태 변경사항 또한 주어졌을때") {
            val event = relaxedMock<SettlementModified>()

            beforeEach {
                with(event) {
                    every { progressingStatus } returns Modification(
                        SettlementProgressingStatus.WAITING,
                        SettlementProgressingStatus.COMPLETED,
                    )
                }
            }

            afterEach { clearAllMocks() }

            `when`("간병 회차에 영향을 주는지 확인하면") {
                fun behavior() = caregivingRound.willBeAffectedBy(event)

                then("true를 반환합니다.") {
                    val expectedResult = true
                    val actualResult = behavior()

                    actualResult shouldBe expectedResult
                }
            }

            `when`("변경사항을 간병 회차에 반영하면") {
                fun handling() = caregivingRound.handleSettlementModified(event)

                then("간병 회차의 정산 상태가 변경됩니다.") {
                    handling()

                    caregivingRound.settlementProgressingStatus shouldBe SettlementProgressingStatus.COMPLETED
                }
            }
        }

        and("정산 진행상태가 변경되지 않은 정산 변경사항 또한 주어졌을때") {
            val event = relaxedMock<SettlementModified>()

            beforeEach {
                with(event) {
                    every { progressingStatus } returns Modification(
                        SettlementProgressingStatus.WAITING,
                        SettlementProgressingStatus.WAITING,
                    )
                }
            }

            afterEach { clearAllMocks() }

            `when`("간병 회차에 영향을 주는지 확인하면") {
                fun behavior() = caregivingRound.willBeAffectedBy(event)

                then("false를 반환합니다.") {
                    val expectedResult = false
                    val actualResult = behavior()

                    actualResult shouldBe expectedResult
                }
            }
        }
    }

    given("간병 회차를 생성하기 위한 인자들이 잘 주어졌을때") {
        val id = ULID.random()
        val caregivingRoundNumber = 1
        val receptionInfo = CaregivingRound.ReceptionInfo(
            receptionId = ULID.random(),
            insuranceNumber = "12345-12345",
            accidentNumber = "2023-1234567",
            maskedPatientName = "방*영",
            receptionProgressingStatus = ReceptionProgressingStatus.RECEIVED,
            expectedCaregivingStartDate = null,
            caregivingManagerInfo = CaregivingManagerInfo(
                organizationType = OrganizationType.INTERNAL,
                organizationId = null,
                managingUserId = "01GQ23MVTBAKS526S0WGS9CS0A"
            )
        )

        `when`("간병 회차를 생성하면") {

            fun behavior() = CaregivingRound(
                id = id,
                caregivingRoundNumber = caregivingRoundNumber,
                receptionInfo = receptionInfo,
            )

            then("문제없이 생성된다.") {
                val actualResult = behavior()
                actualResult.caregivingProgressingStatus shouldBe CaregivingProgressingStatus.NOT_STARTED
                actualResult.startDateTime shouldBe null
                actualResult.caregiverInfo shouldBe null
            }
        }
    }

    given("생성된 간병 회차가 존재할때") {
        lateinit var caregivingRound: CaregivingRound

        beforeEach {
            val subject = generateInternalCaregivingManagerSubject()

            caregivingRound = CaregivingRound(
                id = "01GVJ09BDH6SRXEM14RSZRBKH9",
                caregivingRoundNumber = 1,
                receptionInfo = CaregivingRound.ReceptionInfo(
                    receptionId = ULID.random(),
                    insuranceNumber = "12345-12345",
                    accidentNumber = "2023-1234567",
                    maskedPatientName = "방*영",
                    receptionProgressingStatus = ReceptionProgressingStatus.RECEIVED,
                    expectedCaregivingStartDate = null,
                    caregivingManagerInfo = CaregivingManagerInfo(
                        organizationType = OrganizationType.ORGANIZATION,
                        organizationId = "01GSVWS32PWXHXD500V3FKRT6K",
                        managingUserId = "01GSVWSE5N26T9FY0NA6CMZFZS"
                    )
                )
            )

            caregivingRound.assignCaregiver(
                CaregiverInfo(
                    caregiverOrganizationId = null,
                    name = "정만길",
                    sex = Sex.MALE,
                    phoneNumber = "01012341111",
                    birthDate = LocalDate.of(1984, 2, 14),
                    insured = false,
                    dailyCaregivingCharge = 150000,
                    commissionFee = 3000,
                    accountInfo = AccountInfo(
                        bank = "국민은행",
                        accountNumber = "110-110-1111333",
                        accountHolder = "정만길",
                    )
                ),
                subject
            )

            caregivingRound.startCaregiving(LocalDateTime.of(2023, 2, 16, 14, 0, 0), subject)
        }

        afterEach { clearAllMocks() }

        `when`("간병 회차의 시작 일시를 수정하면") {
            fun behavior() = caregivingRound.editCaregivingStartDateTime(
                LocalDateTime.of(2023, 3, 15, 3, 23, 0),
                generateInternalCaregivingManagerSubject()
            )

            then("간병회차가 수정되었음을 알리는 이벤트가 등록됩니다.") {
                withFixedClock(LocalDateTime.of(2023, 3, 15, 0, 0, 0)) { behavior() }

                val occurredEvent = caregivingRound.domainEvents.find {
                    it is CaregivingRoundModified
                } as CaregivingRoundModified

                occurredEvent.caregivingRoundId shouldBe "01GVJ09BDH6SRXEM14RSZRBKH9"
                occurredEvent.caregivingRoundNumber shouldBe 1
                occurredEvent.startDateTime shouldBe Modification(
                    any(),
                    LocalDateTime.of(2023, 3, 15, 3, 23, 0)
                )
            }
        }
    }

    given("종료된 간병회차가 주어졌을때") {
        lateinit var caregivingRound: CaregivingRound

        beforeEach {
            val subject = generateInternalCaregivingManagerSubject()

            caregivingRound = CaregivingRound(
                id = "01GVJ09BDH6SRXEM14RSZRBKH9",
                caregivingRoundNumber = 1,
                receptionInfo = CaregivingRound.ReceptionInfo(
                    receptionId = "01GVAV4T8AP2SWE71SXZMWB1Z9",
                    insuranceNumber = "12345-12345",
                    accidentNumber = "2023-1234567",
                    maskedPatientName = "방*영",
                    receptionProgressingStatus = ReceptionProgressingStatus.RECEIVED,
                    expectedCaregivingStartDate = null,
                    caregivingManagerInfo = CaregivingManagerInfo(
                        organizationType = OrganizationType.INTERNAL,
                        organizationId = null,
                        managingUserId = "01GQ23MVTBAKS526S0WGS9CS0A"
                    )
                ),
            )

            caregivingRound.assignCaregiver(
                CaregiverInfo(
                    caregiverOrganizationId = null,
                    name = "정만길",
                    sex = Sex.MALE,
                    phoneNumber = "01012341111",
                    birthDate = LocalDate.of(1984, 2, 14),
                    insured = false,
                    dailyCaregivingCharge = 150000,
                    commissionFee = 3000,
                    accountInfo = AccountInfo(
                        bank = "국민은행",
                        accountNumber = "110-110-1111333",
                        accountHolder = "정만길",
                    )
                ),
                subject
            )

            caregivingRound.startCaregiving(LocalDateTime.of(2023, 2, 16, 14, 0, 0), subject)

            caregivingRound.finish(
                LocalDateTime.of(2023, 2, 22, 14, 0, 0),
                FinishingReason.FINISHED_CONTINUE,
                generateInternalCaregivingManagerSubject()
            )
        }

        `when`("간병 회차 시작일을 종료일 이후로 수정하면") {
            fun behavior() = caregivingRound.editCaregivingStartDateTime(
                LocalDateTime.of(2023, 2, 24, 3, 23, 13),
                generateInternalCaregivingManagerSubject()
            )

            then("IllegalStartDateTimeEnteredException 이 발생한다.") {
                val thrownException = shouldThrow<IllegalCaregivingPeriodEnteredException> { behavior() }

                thrownException.targetCaregivingRoundId shouldBe "01GVJ09BDH6SRXEM14RSZRBKH9"
                thrownException.enteredStartDateTime shouldBe LocalDateTime.of(2023, 2, 24, 3, 23, 13)
            }
        }

        `when`("간병 회차의 시작일과 종료일이 같고 종료 시간이 시작 시간보다 이전으로 지정하면") {
            fun behavior() {
                caregivingRound.editCaregivingStartDateTime(
                    LocalDateTime.of(2023, 2, 17, 14, 0, 0),
                    generateInternalCaregivingManagerSubject()
                )
                caregivingRound.editCaregivingEndDateTime(
                    LocalDateTime.of(2023, 2, 17, 10, 0, 0),
                    generateInternalCaregivingManagerSubject()
                )
            }

            then("IllegalStartDateTimeEnteredException 이 발생한다.") {
                val thrownException = shouldThrow<IllegalCaregivingPeriodEnteredException> {
                    withFixedClock(LocalDateTime.of(2023, 2, 17, 16, 0, 0)) { behavior() }
                }

                thrownException.targetCaregivingRoundId shouldBe "01GVJ09BDH6SRXEM14RSZRBKH9"
                thrownException.enteredStartDateTime shouldBe LocalDateTime.of(2023, 2, 17, 14, 0, 0)
            }
        }
    }

    given("리매중인 간병이 주어졌을때") {
        lateinit var caregivingRound: CaregivingRound

        beforeEach {
            mockkObject(ULID)
            every { ULID.random() } returns "01H4V1C58BPMB3075JQEG21TF0"

            val subject = generateInternalCaregivingManagerSubject()

            caregivingRound = CaregivingRound(
                id = ULID.random(),
                caregivingRoundNumber = 2,
                receptionInfo = CaregivingRound.ReceptionInfo(
                    receptionId = "01GT8YRGJCZMKRVZ6BA2AA86A8",
                    insuranceNumber = "12345-12345",
                    accidentNumber = "2023-1234567",
                    maskedPatientName = "방*영",
                    receptionProgressingStatus = ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS,
                    expectedCaregivingStartDate = null,
                    caregivingManagerInfo = CaregivingManagerInfo(
                        organizationType = OrganizationType.ORGANIZATION,
                        organizationId = "01GSVWS32PWXHXD500V3FKRT6K",
                        managingUserId = "01GSVWSE5N26T9FY0NA6CMZFZS"
                    )
                ),
            )

            caregivingRound.assignCaregiver(
                CaregiverInfo(
                    caregiverOrganizationId = null,
                    name = "정만길",
                    sex = Sex.MALE,
                    phoneNumber = "01012341234",
                    birthDate = LocalDate.of(1964, 2, 14),
                    insured = true,
                    dailyCaregivingCharge = 150000,
                    commissionFee = 3000,
                    accountInfo = AccountInfo(
                        bank = "국민은행",
                        accountNumber = "110-110-1111111",
                        accountHolder = "정만길",
                    )
                ),
                subject
            )
        }

        afterEach { clearAllMocks() }

        `when`("리매칭중 취소로 수정하면") {
            fun behavior() = caregivingRound.cancel(
                CancellationReason.CANCELED_WHILE_REMATCHING,
                "리매칭중 취소로 처리합니다.",
                generateInternalCaregivingManagerSubject()
            )

            then("간병 회차가 수정된다.") {
                behavior()

                caregivingRound.caregivingRoundNumber shouldBe 2
                caregivingRound.caregivingProgressingStatus shouldBe CaregivingProgressingStatus.CANCELED_WHILE_REMATCHING
                caregivingRound.caregivingRoundClosingReasonType shouldBe ClosingReasonType.CANCELED_WHILE_REMATCHING
                caregivingRound.caregivingRoundClosingReasonDetail shouldBe "리매칭중 취소로 처리합니다."
            }
        }
    }

    given("진행중인 간병이 주어졌을때") {
        lateinit var caregivingRound: CaregivingRound

        beforeEach {
            mockkObject(ULID)
            every { ULID.random() } returns "01GYF306XMEMVZYG6RZMR47GSD"

            val subject = generateInternalCaregivingManagerSubject()

            caregivingRound = CaregivingRound(
                id = ULID.random(),
                caregivingRoundNumber = 1,
                receptionInfo = CaregivingRound.ReceptionInfo(
                    receptionId = "01GT8YRGJCZMKRVZ6BA2AA86A8",
                    insuranceNumber = "12345-12345",
                    accidentNumber = "2023-1234567",
                    maskedPatientName = "방*영",
                    receptionProgressingStatus = ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS,
                    expectedCaregivingStartDate = null,
                    caregivingManagerInfo = CaregivingManagerInfo(
                        organizationType = OrganizationType.ORGANIZATION,
                        organizationId = "01GSVWS32PWXHXD500V3FKRT6K",
                        managingUserId = "01GSVWSE5N26T9FY0NA6CMZFZS"
                    )
                ),
            )

            caregivingRound.assignCaregiver(
                CaregiverInfo(
                    caregiverOrganizationId = null,
                    name = "정만길",
                    sex = Sex.MALE,
                    phoneNumber = "01012341234",
                    birthDate = LocalDate.of(1964, 2, 14),
                    insured = true,
                    dailyCaregivingCharge = 150000,
                    commissionFee = 3000,
                    accountInfo = AccountInfo(
                        bank = "국민은행",
                        accountNumber = "110-110-1111111",
                        accountHolder = "정만길",
                    )
                ),
                subject
            )

            caregivingRound.startCaregiving(LocalDateTime.of(2023, 2, 17, 14, 0, 0), subject)
        }

        afterEach { clearAllMocks() }

        `when`("내부 직원이 간병 회차를 수정하면") {
            fun behavior() = withFixedClock(LocalDateTime.of(2023, 2, 17, 13, 0, 0)) {
                caregivingRound.finish(
                    LocalDateTime.of(2023, 2, 22, 14, 0, 0),
                    FinishingReason.FINISHED_CONTINUE,
                    generateInternalCaregivingManagerSubject()
                )
            }

            then("간병 회차가 수정된다.") {
                behavior()

                caregivingRound.caregivingRoundNumber shouldBe 1
                caregivingRound.caregivingProgressingStatus shouldBe CaregivingProgressingStatus.COMPLETED
                caregivingRound.caregivingRoundClosingReasonType shouldBe ClosingReasonType.FINISHED_CONTINUE
                caregivingRound.endDateTime shouldBe LocalDateTime.of(2023, 2, 22, 14, 0, 0)
            }

            and("시작일과 종료일이 수정되면") {
                beforeEach {
                    caregivingRound.clearEvents()
                }

                afterEach { /* nothing to do */ }

                val subject = generateInternalCaregivingManagerSubject()

                fun behavior() {
                    caregivingRound.editCaregivingStartDateTime(LocalDateTime.of(2023, 2, 15, 14, 0, 0), subject)
                }

                then("간병 회차가 수정되었음을 알리는 이벤트를 등록합니다.") {
                    behavior()

                    val occurredEvent = caregivingRound.domainEvents.find {
                        it is CaregivingRoundModified
                    } as CaregivingRoundModified

                    occurredEvent.caregivingRoundId shouldBe "01GYF306XMEMVZYG6RZMR47GSD"
                    occurredEvent.caregivingRoundNumber shouldBe 1
                    occurredEvent.receptionId shouldBe "01GT8YRGJCZMKRVZ6BA2AA86A8"
                    occurredEvent.startDateTime shouldBe Modification(
                        LocalDateTime.of(2023, 2, 17, 14, 0, 0),
                        LocalDateTime.of(2023, 2, 15, 14, 0, 0),
                    )
                    occurredEvent.cause shouldBe CaregivingRoundModified.Cause.DIRECT_EDIT
                    occurredEvent.editingSubject shouldBe subject
                }
            }
        }

        `when`("계속건을 추가하여 간병을 종료하면") {
            fun behavior() = caregivingRound.finish(
                LocalDateTime.of(2023, 2, 22, 14, 0, 0),
                FinishingReason.FINISHED_CONTINUE,
                generateInternalCaregivingManagerSubject()
            )

            then("다음 간병 회차가 진행중으로 생성된다.") {
                val actualResult = behavior().nextRound!!

                actualResult.caregivingRoundNumber shouldBe 2
                actualResult.caregivingProgressingStatus shouldBe CaregivingProgressingStatus.CAREGIVING_IN_PROGRESS
                actualResult.caregivingRoundClosingReasonType shouldBe null
                actualResult.startDateTime shouldBe LocalDateTime.of(2023, 2, 22, 14, 0, 0)
                actualResult.endDateTime shouldBe null
                actualResult.receptionInfo shouldBe CaregivingRound.ReceptionInfo(
                    receptionId = "01GT8YRGJCZMKRVZ6BA2AA86A8",
                    insuranceNumber = "12345-12345",
                    accidentNumber = "2023-1234567",
                    maskedPatientName = "방*영",
                    receptionProgressingStatus = ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS,
                    expectedCaregivingStartDate = null,
                    caregivingManagerInfo = CaregivingManagerInfo(
                        organizationType = OrganizationType.ORGANIZATION,
                        organizationId = "01GSVWS32PWXHXD500V3FKRT6K",
                        managingUserId = "01GSVWSE5N26T9FY0NA6CMZFZS"
                    )
                )
                actualResult.caregiverInfo shouldBe CaregiverInfo(
                    caregiverOrganizationId = null,
                    name = "정만길",
                    sex = Sex.MALE,
                    phoneNumber = "01012341234",
                    birthDate = LocalDate.of(1964, 2, 14),
                    insured = true,
                    dailyCaregivingCharge = 150000,
                    commissionFee = 3000,
                    accountInfo = AccountInfo(
                        bank = "국민은행",
                        accountNumber = "110-110-1111111",
                        accountHolder = "정만길",
                    )
                )
            }
        }

        `when`("해당 회차를 마지막 회차로 삼아 간병을 완전히 종료하면") {
            fun behavior() = caregivingRound.finish(
                LocalDateTime.of(2023, 2, 22, 14, 0, 0),
                FinishingReason.FINISHED,
                generateInternalCaregivingManagerSubject()
            )

            then("마지막 간병 회차가 종료되었음을 알리는 이벤트가 등록됩니다.") {
                behavior()

                val occurredEvent = caregivingRound.domainEvents.find {
                    it is LastCaregivingRoundFinished
                } as LastCaregivingRoundFinished

                occurredEvent.receptionId shouldBe "01GT8YRGJCZMKRVZ6BA2AA86A8"
                occurredEvent.lastCaregivingRoundId shouldBe caregivingRound.id
                occurredEvent.endDateTime shouldBe LocalDateTime.of(2023, 2, 22, 14, 0, 0)
            }
        }
    }

    given("중단 계속 상태의 간병이 주어졌을때") {
        lateinit var caregivingRound: CaregivingRound

        beforeEach {
            val subject = generateInternalCaregivingManagerSubject()

            caregivingRound =
                CaregivingRound(
                    id = ULID.random(),
                    caregivingRoundNumber = 1,
                    receptionInfo = CaregivingRound.ReceptionInfo(
                        receptionId = "01GT8YRGJCZMKRVZ6BA2AA86A8",
                        insuranceNumber = "12345-12345",
                        accidentNumber = "2023-1234567",
                        maskedPatientName = "방*영",
                        receptionProgressingStatus = ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS,
                        expectedCaregivingStartDate = null,
                        caregivingManagerInfo = CaregivingManagerInfo(
                            organizationType = OrganizationType.ORGANIZATION,
                            organizationId = "01GSVWS32PWXHXD500V3FKRT6K",
                            managingUserId = "01GSVWSE5N26T9FY0NA6CMZFZS"
                        )
                    ),
                )

            caregivingRound.assignCaregiver(
                CaregiverInfo(
                    caregiverOrganizationId = null,
                    name = "정만길",
                    sex = Sex.MALE,
                    phoneNumber = "01012341111",
                    birthDate = LocalDate.of(1984, 2, 14),
                    insured = false,
                    dailyCaregivingCharge = 150000,
                    commissionFee = 3000,
                    accountInfo = AccountInfo(
                        bank = "국민은행",
                        accountNumber = "110-110-1111333",
                        accountHolder = "정만길",
                    )
                ),
                subject
            )

            caregivingRound.startCaregiving(LocalDateTime.of(2023, 2, 17, 14, 0, 0), subject)

            caregivingRound.stop(LocalDateTime.of(2023, 2, 22, 14, 0, 0), subject)
        }

        afterEach { clearAllMocks() }

        `when`("계속건 추가로 간병을 종료하면") {
            fun behavior() = caregivingRound.finish(
                endDateTime = LocalDateTime.of(2023, 2, 22, 14, 0, 0),
                FinishingReason.FINISHED_CONTINUE,
                generateInternalCaregivingManagerSubject()
            )

            then("간병 회차가 수정된다.") {
                behavior()

                caregivingRound.caregivingRoundNumber shouldBe 1
                caregivingRound.caregivingProgressingStatus shouldBe CaregivingProgressingStatus.COMPLETED
                caregivingRound.caregivingRoundClosingReasonType shouldBe ClosingReasonType.FINISHED_CONTINUE
                caregivingRound.endDateTime shouldBe LocalDateTime.of(2023, 2, 22, 14, 0, 0)
            }
        }

        `when`("중단(계속)으로 간병을 종료하면") {
            fun behavior() = caregivingRound.finish(
                endDateTime = LocalDateTime.of(2023, 2, 22, 14, 0, 0),
                FinishingReason.FINISHED_RESTARTING,
                generateInternalCaregivingManagerSubject()
            )

            then("간병 회차가 수정된다.") {
                behavior()

                caregivingRound.caregivingRoundNumber shouldBe 1
                caregivingRound.caregivingProgressingStatus shouldBe CaregivingProgressingStatus.COMPLETED
                caregivingRound.caregivingRoundClosingReasonType shouldBe ClosingReasonType.FINISHED_RESTARTING
                caregivingRound.endDateTime shouldBe LocalDateTime.of(2023, 2, 22, 14, 0, 0)
            }
        }
    }

    given("완전히 종료된 간병이 주어졌을때") {
        lateinit var caregivingRound: CaregivingRound

        beforeEach {
            mockkObject(ULID)
            every { ULID.random() } returns "01GYF306XMEMVZYG6RZMR47GSD"
            val subject = generateInternalCaregivingManagerSubject()

            caregivingRound = CaregivingRound(
                id = ULID.random(),
                caregivingRoundNumber = 1,
                receptionInfo = CaregivingRound.ReceptionInfo(
                    receptionId = "01GT8YRGJCZMKRVZ6BA2AA86A8",
                    insuranceNumber = "12345-12345",
                    accidentNumber = "2023-1234567",
                    maskedPatientName = "방*영",
                    receptionProgressingStatus = ReceptionProgressingStatus.CAREGIVING_IN_PROGRESS,
                    expectedCaregivingStartDate = null,
                    caregivingManagerInfo = CaregivingManagerInfo(
                        organizationType = OrganizationType.ORGANIZATION,
                        organizationId = "01GSVWS32PWXHXD500V3FKRT6K",
                        managingUserId = "01GSVWSE5N26T9FY0NA6CMZFZS"
                    )
                ),
            )

            caregivingRound.assignCaregiver(
                CaregiverInfo(
                    caregiverOrganizationId = null,
                    name = "정만길",
                    sex = Sex.MALE,
                    phoneNumber = "01012341234",
                    birthDate = LocalDate.of(1964, 2, 14),
                    insured = true,
                    dailyCaregivingCharge = 150000,
                    commissionFee = 3000,
                    accountInfo = AccountInfo(
                        bank = "국민은행",
                        accountNumber = "110-110-1111111",
                        accountHolder = "정만길",
                    )
                ),
                subject
            )

            caregivingRound.startCaregiving(LocalDateTime.of(2023, 2, 17, 14, 0, 0), subject)

            caregivingRound.finish(
                LocalDateTime.of(2023, 2, 22, 14, 0, 0),
                FinishingReason.FINISHED,
                subject
            )

            caregivingRound.clearEvents()
        }

        afterEach { clearAllMocks() }

        `when`("마지막 간병 회차의 종료일을 수정하면") {
            fun behavior() = caregivingRound.editCaregivingEndDateTime(
                LocalDateTime.of(2023, 2, 23, 14, 0, 0),
                generateInternalCaregivingManagerSubject()
            )

            then("마지막 간병 회차가 수정되었음을 알리는 이벤트가 등록됩니다.") {
                behavior()

                val occurredEvent = caregivingRound.domainEvents.find {
                    it is LastCaregivingRoundModified
                } as LastCaregivingRoundModified

                occurredEvent.receptionId shouldBe "01GT8YRGJCZMKRVZ6BA2AA86A8"
                occurredEvent.lastCaregivingRoundId shouldBe caregivingRound.id
                occurredEvent.endDateTime shouldBe Modification(
                    LocalDateTime.of(2023, 2, 22, 14, 0, 0),
                    LocalDateTime.of(2023, 2, 23, 14, 0, 0)
                )
            }
        }

        `when`("정산이 생성되었음을 알리는 이벤트가 전달되면") {
            val event = relaxedMock<SettlementGenerated>()

            beforeEach {
                with(event) {
                    every { progressingStatus } returns SettlementProgressingStatus.CONFIRMED
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = caregivingRound.handleSettlementGenerated(event)

            then("간병 회차의 정산 상태가 변경됩니다.") {
                handling()

                caregivingRound.settlementProgressingStatus shouldBe SettlementProgressingStatus.CONFIRMED
            }
        }

        `when`("청구가 생성되었음을 알리는 이벤트가 전달되면") {
            val event = relaxedMock<BillingGenerated>()

            beforeEach {
                with(event) {
                    every { progressingStatus } returns BillingProgressingStatus.WAITING_FOR_BILLING
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = caregivingRound.handleBillingGenerated(event)

            then("간병 회차의 청구 상태가 변경됩니다.") {
                handling()

                caregivingRound.billingProgressingStatus shouldBe BillingProgressingStatus.WAITING_FOR_BILLING
            }
        }

        `when`("정산 대사 완료되었음을 알리는 이벤트가 전달되면") {
            val event = relaxedMock<ReconciliationClosed>()

            fun handling() = caregivingRound.handleReconciliationClosed(event)

            beforeEach {
                every { event.subject } returns generateInternalCaregivingManagerSubject()
            }

            afterEach {
                clearAllMocks()
            }

            then("간병 회차의 상태가 변경됩니다.") {
                handling()

                caregivingRound.caregivingProgressingStatus shouldBe CaregivingProgressingStatus.RECONCILIATION_COMPLETED
            }
        }
    }
})
