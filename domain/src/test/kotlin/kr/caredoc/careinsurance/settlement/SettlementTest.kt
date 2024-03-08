package kr.caredoc.careinsurance.settlement

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import kr.caredoc.careinsurance.caregiving.CaregivingChargeConfirmStatus
import kr.caredoc.careinsurance.caregiving.CaregivingChargeModified
import kr.caredoc.careinsurance.generateExternalCaregivingOrganizationManagerSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.patch.Patches
import kr.caredoc.careinsurance.reception.CaregivingManagerInfo
import kr.caredoc.careinsurance.reception.OrganizationType
import kr.caredoc.careinsurance.reception.ReceptionModified
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.security.accesscontrol.ObjectAttribute
import kr.caredoc.careinsurance.transaction.TransactionType
import kr.caredoc.careinsurance.withFixedClock
import org.springframework.security.access.AccessDeniedException
import java.time.LocalDate
import java.time.LocalDateTime

class SettlementTest : BehaviorSpec({
    given("정산을 생성하기 위한 인자들이 주어졌을때") {
        val id = "01GVCX47T2590S6RYTTFDGJQP6"
        val receptionId = "01GVD2HS5FMX9012BN28VHDPW3"
        val caregivingRoundId = "01GSM0PQ5G8HW2GKYXH3VGGMZG"
        val caregivingRoundNumber = 2
        val accidentNumber = "2022-1111111"
        val dailyCaregivingCharge = 121000
        val basicAmount = 605000
        val additionalAmount = 20000
        val totalAmount = 625000
        val lastCalculationDateTime = LocalDateTime.of(2022, 1, 30, 5, 21, 31)
        val expectedSettlementDate = LocalDate.of(2022, 1, 30)

        `when`("정산을 생성하면") {
            fun behavior() = Settlement(
                id = id,
                receptionId = receptionId,
                caregivingRoundId = caregivingRoundId,
                caregivingRoundNumber = caregivingRoundNumber,
                accidentNumber = accidentNumber,
                dailyCaregivingCharge = dailyCaregivingCharge,
                basicAmount = basicAmount,
                additionalAmount = additionalAmount,
                totalAmount = totalAmount,
                lastCalculationDateTime = lastCalculationDateTime,
                expectedSettlementDate = expectedSettlementDate,
                caregivingManagerInfo = CaregivingManagerInfo(
                    organizationType = OrganizationType.ORGANIZATION,
                    organizationId = "01GSVWS32PWXHXD500V3FKRT6K",
                    managingUserId = "01GSVWSE5N26T9FY0NA6CMZFZS"
                ),
            )

            then("정산이 생성되었음을 알리는 이벤트가 등록됩니다.") {
                val settlement = behavior()

                val occurredEvent = settlement.domainEvents.find { it is SettlementGenerated } as SettlementGenerated

                occurredEvent.caregivingRoundId shouldBe "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                occurredEvent.progressingStatus shouldBe SettlementProgressingStatus.CONFIRMED
                occurredEvent.settlementId shouldBe id
                occurredEvent.totalAmount shouldBe totalAmount
            }

            then("생성된 정산은 ASSIGNED_ORGANIZATION_ID 접근 대상 속성을 가집니다.") {
                val settlement = behavior()

                settlement[ObjectAttribute.ASSIGNED_ORGANIZATION_ID] shouldContain "01GSVWS32PWXHXD500V3FKRT6K"
            }
        }
    }

    given("산정됨 상태의 정산이 주어졌을때") {
        lateinit var settlement: Settlement

        beforeEach {
            settlement = Settlement(
                id = "01GVCX47T2590S6RYTTFDGJQP6",
                receptionId = "01GVD2HS5FMX9012BN28VHDPW3",
                caregivingRoundId = "01GSM0PQ5G8HW2GKYXH3VGGMZG",
                caregivingRoundNumber = 2,
                accidentNumber = "2022-1111111",
                dailyCaregivingCharge = 121000,
                basicAmount = 605000,
                additionalAmount = 20000,
                totalAmount = 625000,
                lastCalculationDateTime = LocalDateTime.of(2022, 1, 30, 5, 21, 31),
                expectedSettlementDate = LocalDate.of(2022, 1, 30),
            )
        }

        afterEach { clearAllMocks() }

        `when`("정산에 간병비가 수정되었음을 알리면") {
            val event = relaxedMock<CaregivingChargeModified>()

            beforeEach {
                with(event) {
                    every { caregivingRoundId } returns "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                    every { basicAmount } returns 750000
                    every { additionalAmount } returns 54500
                    every { totalAmount } returns 804500
                    every { expectedSettlementDate } returns Modification(
                        LocalDate.of(2023, 4, 17),
                        LocalDate.of(2023, 4, 17),
                    )
                    every { calculatedDateTime } returns LocalDateTime.of(2023, 4, 16, 12, 3, 4)
                    every { confirmStatus } returns CaregivingChargeConfirmStatus.NOT_STARTED
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = settlement.handleCaregivingChargeModified(event)

            then("정산에 입력된 간병비와 정산 예정일과 마지막 계산일시가 수정됩니다.") {
                handling()

                event.basicAmount shouldBe 750000
                event.additionalAmount shouldBe 54500
                event.totalAmount shouldBe 804500
                event.expectedSettlementDate shouldBe Modification(
                    LocalDate.of(2023, 4, 17),
                    LocalDate.of(2023, 4, 17),
                )
                event.calculatedDateTime shouldBe LocalDateTime.of(2023, 4, 16, 12, 3, 4)
            }
        }

        `when`("간병비가 확정되었음을 알리면") {
            val event = relaxedMock<CaregivingChargeModified>()

            beforeEach {
                with(event) {
                    every { confirmStatus } returns CaregivingChargeConfirmStatus.CONFIRMED
                    every { expectedSettlementDate.current } returns LocalDate.of(2023, 4, 17)
                    every { totalAmount } returns 125500
                }
            }

            afterEach { clearAllMocks() }

            fun handling() = settlement.handleCaregivingChargeModified(event)

            then("정산이 정산 대기상태로 전환됩니다.") {
                handling()

                settlement.progressingStatus shouldBe SettlementProgressingStatus.WAITING
            }

            then("정산이 변경되었음을 알리는 이벤트가 등록됩니다.") {
                handling()

                val occurredEvent = settlement.domainEvents.find { it is SettlementModified } as SettlementModified

                occurredEvent.progressingStatus shouldBe Modification(
                    SettlementProgressingStatus.CONFIRMED,
                    SettlementProgressingStatus.WAITING,
                )
                occurredEvent.settlementId shouldBe "01GVCX47T2590S6RYTTFDGJQP6"
                occurredEvent.totalAmount shouldBe 125500
                occurredEvent.totalDepositAmount shouldBe 0
                occurredEvent.totalWithdrawalAmount shouldBe 0
            }
        }

        `when`("접수 정보가 변경되면") {
            val event = relaxedMock<ReceptionModified>()

            beforeEach {
                with(event) {
                    every { accidentInfo } returns Modification(
                        relaxedMock {
                            every { accidentNumber } returns "2022-1111111"
                        },
                        relaxedMock {
                            every { accidentNumber } returns "2022-3333333"
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

            fun handling() = settlement.handleReceptionModified(event)

            then("사고, 환자 정보를 갱신합니다.") {
                handling()

                settlement.accidentNumber shouldBe "2022-3333333"
            }

            then("ASSIGNED_ORGANIZATION_ID 접근 대상 속성이 갱신됩니다.") {
                settlement[ObjectAttribute.ASSIGNED_ORGANIZATION_ID].size shouldBe 0

                handling()

                settlement[ObjectAttribute.ASSIGNED_ORGANIZATION_ID] shouldContain "01GSVWS32PWXHXD500V3FKRT6K"
            }
        }
    }

    given("대기 상태의 정산이 주어졌을때") {
        lateinit var settlement: Settlement

        beforeEach {
            settlement = withFixedClock(LocalDateTime.of(2022, 1, 30, 16, 5, 21)) {
                Settlement(
                    id = "01GVCX47T2590S6RYTTFDGJQP6",
                    receptionId = "01GVD2HS5FMX9012BN28VHDPW3",
                    caregivingRoundId = "01GSM0PQ5G8HW2GKYXH3VGGMZG",
                    caregivingRoundNumber = 2,
                    accidentNumber = "2022-1111111",
                    dailyCaregivingCharge = 121000,
                    basicAmount = 605000,
                    additionalAmount = 20000,
                    totalAmount = 625000,
                    lastCalculationDateTime = LocalDateTime.of(2022, 1, 30, 5, 21, 31),
                    expectedSettlementDate = LocalDate.of(2022, 1, 30),
                )
            }

            settlement.edit(
                SettlementEditingCommand(
                    progressingStatus = Patches.ofValue(SettlementProgressingStatus.WAITING),
                    subject = generateInternalCaregivingManagerSubject(),
                )
            )
        }

        afterEach {
            clearAllMocks()
        }

        `when`("정산금 출금 내역을 기록하면") {
            val subject = generateInternalCaregivingManagerSubject()
            val command = SettlementTransactionRecordingCommand(
                transactionType = TransactionType.WITHDRAWAL,
                amount = 5000,
                transactionDate = LocalDate.of(2022, 1, 30),
                transactionSubjectId = "01GW1160R5ZC9E3P5V57TYQX0E",
                subject = subject,
            )

            fun behavior() = settlement.recordTransaction(command)

            then("정산금 입출금 내역에 기록합니다.") {
                behavior()

                settlement.transactions.last().transactionType shouldBe TransactionType.WITHDRAWAL
                settlement.transactions.last().amount shouldBe 5000
                settlement.transactions.last().transactionDate shouldBe LocalDate.of(2022, 1, 30)
                settlement.transactions.last().transactionSubjectId shouldBe "01GW1160R5ZC9E3P5V57TYQX0E"
            }

            then("총 출금 금액이 갱신됩니다.") {
                settlement.totalWithdrawalAmount shouldBe 0

                behavior()

                settlement.totalWithdrawalAmount shouldBe 5000
            }

            then("마지막 입출금날짜가 현재 시각으로 변경됩니다.") {
                settlement.lastTransactionDatetime shouldBe null

                withFixedClock(LocalDateTime.of(2022, 1, 30, 16, 5, 21)) {
                    behavior()
                }

                settlement.lastTransactionDatetime shouldBe LocalDateTime.of(2022, 1, 30, 16, 5, 21)
            }

            then("정산 출금이 기록되었음을 이벤트로 알립니다.") {
                withFixedClock(LocalDateTime.of(2022, 1, 30, 16, 5, 21)) {
                    behavior()
                }

                val occurredEvent =
                    settlement.domainEvents.find { it is SettlementTransactionRecorded } as SettlementTransactionRecorded

                occurredEvent.receptionId shouldBe "01GVD2HS5FMX9012BN28VHDPW3"
                occurredEvent.caregivingRoundId shouldBe "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                occurredEvent.settlementId shouldBe "01GVCX47T2590S6RYTTFDGJQP6"
                occurredEvent.amount shouldBe 5000
                occurredEvent.transactionDate shouldBe LocalDate.of(2022, 1, 30)
                occurredEvent.transactionType shouldBe TransactionType.WITHDRAWAL
                occurredEvent.enteredDateTime shouldBe LocalDateTime.of(2022, 1, 30, 16, 5, 21)
                occurredEvent.order shouldBe 0
                occurredEvent.progressingStatus shouldBe SettlementProgressingStatus.WAITING
                occurredEvent.totalAmount shouldBe 625000
                occurredEvent.totalDepositAmount shouldBe 0
                occurredEvent.totalWithdrawalAmount shouldBe 5000
            }
        }

        `when`("정산금 출금 내역을 두번 기록하면") {
            val subject = generateInternalCaregivingManagerSubject()
            val command = SettlementTransactionRecordingCommand(
                transactionType = TransactionType.WITHDRAWAL,
                amount = 5000,
                transactionDate = LocalDate.of(2022, 1, 30),
                transactionSubjectId = "01GW1160R5ZC9E3P5V57TYQX0E",
                subject = subject,
            )

            fun behavior() {
                settlement.recordTransaction(command)
                settlement.recordTransaction(command)
            }

            then("두번째 정산 출금 기록은 순서가 1로 기록됩니다.") {
                behavior()

                val occurredEvent =
                    settlement.domainEvents.findLast { it is SettlementTransactionRecorded } as SettlementTransactionRecorded

                occurredEvent.order shouldBe 1
            }
        }

        `when`("정산금 입금 내역을 기록하면") {
            val subject = generateInternalCaregivingManagerSubject()
            val command = SettlementTransactionRecordingCommand(
                transactionType = TransactionType.DEPOSIT,
                amount = 5000,
                transactionDate = LocalDate.of(2022, 1, 30),
                transactionSubjectId = "01GW1160R5ZC9E3P5V57TYQX0E",
                subject = subject,
            )

            fun behavior() = settlement.recordTransaction(command)

            then("정산금 입출금 내역에 기록합니다.") {
                behavior()

                settlement.transactions.last().transactionType shouldBe TransactionType.DEPOSIT
                settlement.transactions.last().amount shouldBe 5000
                settlement.transactions.last().transactionDate shouldBe LocalDate.of(2022, 1, 30)
                settlement.transactions.last().transactionSubjectId shouldBe "01GW1160R5ZC9E3P5V57TYQX0E"
            }

            then("총 입금 금액이 갱신됩니다.") {
                settlement.totalDepositAmount shouldBe 0

                behavior()

                settlement.totalDepositAmount shouldBe 5000
            }

            then("마지막 입출금날짜가 현재 시각으로 변경됩니다.") {
                settlement.lastTransactionDatetime shouldBe null

                withFixedClock(LocalDateTime.of(2022, 1, 30, 16, 5, 21)) {
                    behavior()
                }

                settlement.lastTransactionDatetime shouldBe LocalDateTime.of(2022, 1, 30, 16, 5, 21)
            }

            then("정산 입금이 기록되었음을 이벤트로 알립니다.") {
                withFixedClock(LocalDateTime.of(2022, 1, 30, 16, 5, 21)) {
                    behavior()
                }

                val occurredEvent =
                    settlement.domainEvents.find { it is SettlementTransactionRecorded } as SettlementTransactionRecorded

                occurredEvent.receptionId shouldBe "01GVD2HS5FMX9012BN28VHDPW3"
                occurredEvent.caregivingRoundId shouldBe "01GSM0PQ5G8HW2GKYXH3VGGMZG"
                occurredEvent.settlementId shouldBe "01GVCX47T2590S6RYTTFDGJQP6"
                occurredEvent.amount shouldBe 5000
                occurredEvent.transactionDate shouldBe LocalDate.of(2022, 1, 30)
                occurredEvent.transactionType shouldBe TransactionType.DEPOSIT
                occurredEvent.enteredDateTime shouldBe LocalDateTime.of(2022, 1, 30, 16, 5, 21)
                occurredEvent.order shouldBe 0
            }
        }

        `when`("내부 사용자 권한 없이 정산금 입출금 내역을 입력하면") {
            val subject = generateExternalCaregivingOrganizationManagerSubject("01GVG25A2P5V6MVQG2SPKQ4D50")
            val command = SettlementTransactionRecordingCommand(
                transactionType = TransactionType.WITHDRAWAL,
                amount = 5000,
                transactionDate = LocalDate.of(2022, 1, 30),
                transactionSubjectId = "01GW1160R5ZC9E3P5V57TYQX0E",
                subject = subject,
            )

            fun behavior() = settlement.recordTransaction(command)

            then("AccessDeniedException이 발생합니다.") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }

        `when`("완료 상태로 변경하면") {
            val command = SettlementEditingCommand(
                progressingStatus = Patches.ofValue(SettlementProgressingStatus.COMPLETED),
                settlementManagerId = Patches.ofValue("01GVCZ7W7MAYAC6C7JMJHSNEJR"),
                subject = generateInternalCaregivingManagerSubject(),
            )

            fun behavior() = settlement.edit(command)

            then("정산의 진행 상태가 완료로 변경됩니다.") {
                behavior()

                settlement.progressingStatus shouldBe SettlementProgressingStatus.COMPLETED
            }

            then("지급된 금액이 지급할 총 금액으로 변경됩니다.") {
                behavior()

                settlement.totalWithdrawalAmount shouldBe 625000
            }

            then("마지막 입출금날짜가 현재 시각으로 변경됩니다.") {
                settlement.lastTransactionDatetime shouldBe null

                withFixedClock(LocalDateTime.of(2022, 1, 30, 16, 5, 21)) {
                    behavior()
                }

                settlement.lastTransactionDatetime shouldBe LocalDateTime.of(2022, 1, 30, 16, 5, 21)
            }

            then("정산 관리자의 명의로 지급할 총 금액만큼 출금이 기록됩니다.") {
                withFixedClock(LocalDateTime.of(2022, 1, 30, 16, 5, 21)) {
                    behavior()
                }

                settlement.transactions[0] shouldBe Settlement.TransactionRecord(
                    transactionType = TransactionType.WITHDRAWAL,
                    amount = 625000,
                    transactionDate = LocalDate.of(2022, 1, 30),
                    enteredDateTime = LocalDateTime.of(2022, 1, 30, 16, 5, 21),
                    transactionSubjectId = "01GVCZ7W7MAYAC6C7JMJHSNEJR",
                )
            }

            then("정산 완료일이 현재 시각으로 변경됩니다.") {
                settlement.settlementCompletionDateTime shouldBe null

                withFixedClock(LocalDateTime.of(2022, 1, 30, 16, 5, 21)) {
                    behavior()
                }

                settlement.settlementCompletionDateTime shouldBe LocalDateTime.of(2022, 1, 30, 16, 5, 21)
            }

            then("정산을 완료한 정산 관리자가 정산의 정산 관리자로 입력됩니다.") {
                behavior()

                settlement.settlementManagerId shouldBe "01GVCZ7W7MAYAC6C7JMJHSNEJR"
            }

            then("정산이 변경되었음을 알리는 이벤트가 등록됩니다.") {
                behavior()

                val occurredEvent = settlement.domainEvents.find { it is SettlementModified } as SettlementModified

                occurredEvent.progressingStatus shouldBe Modification(
                    SettlementProgressingStatus.WAITING,
                    SettlementProgressingStatus.COMPLETED,
                )
            }
        }
    }

    given("확인 상태의 정산이 주어졌을때") {
        lateinit var settlement: Settlement

        beforeEach {
            settlement = Settlement(
                id = "01GVCX47T2590S6RYTTFDGJQP6",
                receptionId = "01GVD2HS5FMX9012BN28VHDPW3",
                caregivingRoundId = "01GSM0PQ5G8HW2GKYXH3VGGMZG",
                caregivingRoundNumber = 2,
                accidentNumber = "2022-1111111",
                dailyCaregivingCharge = 121000,
                basicAmount = 605000,
                additionalAmount = 20000,
                totalAmount = 625000,
                lastCalculationDateTime = LocalDateTime.of(2022, 1, 30, 5, 21, 31),
                expectedSettlementDate = LocalDate.of(2022, 1, 30),
            )
        }

        afterEach { clearAllMocks() }

        `when`("완료 상태로 변경하면") {
            val command = SettlementEditingCommand(
                progressingStatus = Patches.ofValue(SettlementProgressingStatus.COMPLETED),
                settlementManagerId = Patches.ofValue("01GVCZ7W7MAYAC6C7JMJHSNEJR"),
                subject = generateInternalCaregivingManagerSubject(),
            )

            fun behavior() = settlement.edit(command)

            then("InvalidSettlementProgressingStatusTransitionException이 발생합니다.") {
                val thrownException = shouldThrow<InvalidSettlementProgressingStatusTransitionException> { behavior() }

                thrownException.currentSettlementProgressingStatus shouldBe SettlementProgressingStatus.CONFIRMED
                thrownException.enteredSettlementProgressingStatus shouldBe SettlementProgressingStatus.COMPLETED
            }
        }
    }
})
