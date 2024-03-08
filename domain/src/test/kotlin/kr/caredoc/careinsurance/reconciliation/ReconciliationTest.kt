package kr.caredoc.careinsurance.reconciliation

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kr.caredoc.careinsurance.generateExternalCaregivingOrganizationManagerSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.withFixedClock
import org.springframework.security.access.AccessDeniedException
import java.time.LocalDate
import java.time.LocalDateTime

class ReconciliationTest : BehaviorSpec({
    given("마감 전의 대사가 주어졌을때") {
        lateinit var reconciliation: Reconciliation

        beforeEach {
            reconciliation = Reconciliation(
                id = "01GXX57HXJY77ND6G57PDM3R1D",
                receptionId = "01GVD2HS5FMX9012BN28VHDPW3",
                caregivingRoundId = "01GSM0PQ5G8HW2GKYXH3VGGMZG",
                issuedDate = LocalDate.of(2023, 4, 13),
                issuedType = IssuedType.FINISH,
                billingAmount = 625000,
                settlementAmount = 590000,
                settlementDepositAmount = 0,
                settlementWithdrawalAmount = 0,
                profit = 35000,
                distributedProfit = 21000,
                caregiverPhoneNumberWhenIssued = "01011112222",
                actualCaregivingSecondsWhenIssued = 432001,
            )
        }

        afterEach { /* nothing to do */ }

        `when`("대사를 마감 전으로 되돌리면") {
            val command = ReconciliationEditingCommand(
                closingStatus = ClosingStatus.OPEN,
                subject = generateInternalCaregivingManagerSubject(),
            )

            fun behavior() = reconciliation.edit(command)

            then("InvalidReconciliationClosingStatusTransitionException이 발생합니다") {
                val thrownException = shouldThrow<InvalidReconciliationClosingStatusTransitionException> { behavior() }

                thrownException.currentReconciliationClosingStatus shouldBe ClosingStatus.OPEN
                thrownException.enteredReconciliationClosingStatus shouldBe ClosingStatus.OPEN
            }
        }

        `when`("대사를 마감하면") {
            val command = ReconciliationEditingCommand(
                closingStatus = ClosingStatus.CLOSED,
                subject = generateInternalCaregivingManagerSubject(),
            )

            fun behavior() = reconciliation.edit(command)

            then("대사의 마감 상태가 CLOSED로 변경됩니다.") {
                behavior()

                reconciliation.closingStatus shouldBe ClosingStatus.CLOSED
            }

            then("ReconciliationClosed 이벤트가 발생합니다.") {
                behavior()

                val occurredEvent = reconciliation.domainEvents.find {
                    it is ReconciliationClosed
                } as ReconciliationClosed

                occurredEvent.reconciliationId shouldBe "01GXX57HXJY77ND6G57PDM3R1D"
            }

            then("대사 기준 연월이 현재 날짜 기준으로 확정됩니다.") {
                withFixedClock(LocalDateTime.of(2023, 11, 3, 12, 37, 29)) {
                    behavior()
                }

                reconciliation.reconciledYear shouldBe 2023
                reconciliation.reconciledMonth shouldBe 11
            }

            then("마감 일시가 현재 일시를 기준으로 확정됩니다.") {
                withFixedClock(LocalDateTime.of(2023, 11, 3, 12, 37, 29)) {
                    behavior()
                }

                reconciliation.closedDateTime shouldBe LocalDateTime.of(2023, 11, 3, 12, 37, 29)
            }
        }

        `when`("내부 사용자 권한 없이 대사를 수정하면") {
            val command = ReconciliationEditingCommand(
                closingStatus = ClosingStatus.CLOSED,
                subject = generateExternalCaregivingOrganizationManagerSubject("01GVG25A2P5V6MVQG2SPKQ4D50"),
            )

            fun behavior() = reconciliation.edit(command)

            then("AccessDeniedException이 발생합니다.") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }
    }

    given("마감된 대사가 주어졌을때") {
        lateinit var reconciliation: Reconciliation

        beforeEach {
            reconciliation = Reconciliation(
                id = "01GXX57HXJY77ND6G57PDM3R1D",
                receptionId = "01GVD2HS5FMX9012BN28VHDPW3",
                caregivingRoundId = "01GSM0PQ5G8HW2GKYXH3VGGMZG",
                issuedDate = LocalDate.of(2023, 4, 13),
                issuedType = IssuedType.FINISH,
                billingAmount = 625000,
                settlementAmount = 590000,
                settlementDepositAmount = 0,
                settlementWithdrawalAmount = 0,
                profit = 35000,
                distributedProfit = 21000,
                caregiverPhoneNumberWhenIssued = "01011112222",
                actualCaregivingSecondsWhenIssued = 432001,
            )
            reconciliation.edit(
                ReconciliationEditingCommand(
                    closingStatus = ClosingStatus.CLOSED,
                    subject = generateInternalCaregivingManagerSubject(),
                )
            )
        }

        afterEach { /* nothing to do */ }

        `when`("대사를 마감 전으로 되돌리면") {
            val command = ReconciliationEditingCommand(
                closingStatus = ClosingStatus.OPEN,
                subject = generateInternalCaregivingManagerSubject(),
            )

            fun behavior() = reconciliation.edit(command)

            then("InvalidReconciliationClosingStatusTransitionException이 발생합니다") {
                val thrownException = shouldThrow<InvalidReconciliationClosingStatusTransitionException> { behavior() }

                thrownException.currentReconciliationClosingStatus shouldBe ClosingStatus.CLOSED
                thrownException.enteredReconciliationClosingStatus shouldBe ClosingStatus.OPEN
            }
        }

        `when`("대사를 마감하면") {
            val command = ReconciliationEditingCommand(
                closingStatus = ClosingStatus.CLOSED,
                subject = generateInternalCaregivingManagerSubject(),
            )

            fun behavior() = reconciliation.edit(command)

            then("InvalidReconciliationClosingStatusTransitionException이 발생합니다") {
                val thrownException = shouldThrow<InvalidReconciliationClosingStatusTransitionException> { behavior() }

                thrownException.currentReconciliationClosingStatus shouldBe ClosingStatus.CLOSED
                thrownException.enteredReconciliationClosingStatus shouldBe ClosingStatus.CLOSED
            }
        }
    }

    given("마감 기준 연월이 이미 결정된 대사가 주어졌을때") {
        lateinit var reconciliation: Reconciliation

        beforeEach {
            reconciliation = Reconciliation(
                id = "01GXX57HXJY77ND6G57PDM3R1D",
                receptionId = "01GVD2HS5FMX9012BN28VHDPW3",
                caregivingRoundId = "01GSM0PQ5G8HW2GKYXH3VGGMZG",
                issuedDate = LocalDate.of(2023, 4, 13),
                issuedType = IssuedType.FINISH,
                billingAmount = 625000,
                settlementAmount = 590000,
                settlementDepositAmount = 0,
                settlementWithdrawalAmount = 0,
                profit = 35000,
                distributedProfit = 21000,
                reconciledYear = 2023,
                reconciledMonth = 11,
                caregiverPhoneNumberWhenIssued = "01011112222",
                actualCaregivingSecondsWhenIssued = 432001,
            )
        }

        afterEach { /* nothing to do */ }

        `when`("대사를 마감하면") {
            val command = ReconciliationEditingCommand(
                closingStatus = ClosingStatus.CLOSED,
                subject = generateInternalCaregivingManagerSubject(),
            )

            fun behavior() = reconciliation.edit(command)

            then("대사 기준 연월은 최초에 기입한 연월로 유지됩니다.") {
                withFixedClock(LocalDateTime.of(2023, 12, 3, 0, 0, 0)) {
                    behavior()
                }

                reconciliation.reconciledYear shouldBe 2023
                reconciliation.reconciledMonth shouldBe 11
            }
        }
    }
})
