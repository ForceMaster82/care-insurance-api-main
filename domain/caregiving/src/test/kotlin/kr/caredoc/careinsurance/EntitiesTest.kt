package kr.caredoc.careinsurance

import io.kotest.core.spec.style.BehaviorSpec
import kr.caredoc.careinsurance.bizcall.BizcallReservation
import kr.caredoc.careinsurance.bizcall.BizcallReservationRepository
import kr.caredoc.careinsurance.caregiving.CaregivingCharge
import kr.caredoc.careinsurance.caregiving.CaregivingChargeRepository
import kr.caredoc.careinsurance.caregiving.progressmessage.CaregivingProgressMessageSendingHistory
import kr.caredoc.careinsurance.caregiving.progressmessage.CaregivingProgressMessageSendingHistoryRepository
import kr.caredoc.careinsurance.caregiving.progressmessage.CaregivingProgressMessageSummary
import kr.caredoc.careinsurance.caregiving.progressmessage.CaregivingProgressMessageSummaryRepository
import kr.caredoc.careinsurance.email.EmailSendingLog
import kr.caredoc.careinsurance.email.EmailSendingLogRepository
import kr.caredoc.careinsurance.email.SenderProfile
import kr.caredoc.careinsurance.reception.caregivingstartmessage.CaregivingStartMessageSendingHistory
import kr.caredoc.careinsurance.reception.caregivingstartmessage.CaregivingStartMessageSendingHistoryRepository
import kr.caredoc.careinsurance.reception.history.ReceptionModificationHistory
import kr.caredoc.careinsurance.reception.history.ReceptionModificationHistoryRepository
import kr.caredoc.careinsurance.reception.modification.CaregivingChargeModificationSummary
import kr.caredoc.careinsurance.reception.modification.CaregivingChargeModificationSummaryRepository
import kr.caredoc.careinsurance.reception.modification.CaregivingRoundModificationSummary
import kr.caredoc.careinsurance.reception.modification.CaregivingRoundModificationSummaryRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
class EntitiesTest(
    @Autowired
    private val cacheCaregivingChargeRepository: CaregivingChargeRepository,
    @Autowired
    private val cacheCaregivingProgressMessageSummaryRepository: CaregivingProgressMessageSummaryRepository,
    @Autowired
    private val cacheCaregivingStartMessageSendingHistoryRepository: CaregivingStartMessageSendingHistoryRepository,
    @Autowired
    private val cacheCaregivingProgressMessageSendingHistoryRepository: CaregivingProgressMessageSendingHistoryRepository,
    @Autowired
    private val cacheReceptionModificationHistoryRepository: ReceptionModificationHistoryRepository,
    @Autowired
    private val cacheCaregivingChargeModificationSummaryRepository: CaregivingChargeModificationSummaryRepository,
    @Autowired
    private val cacheCaregivingRoundModificationSummaryRepository: CaregivingRoundModificationSummaryRepository,
    @Autowired
    private val cacheBizcallReservationRepository: BizcallReservationRepository,
    @Autowired
    private val cacheEmailSendingLogRepository: EmailSendingLogRepository,
) : BehaviorSpec({

    given("정산비 산정의 엔티티 테스트를 할 때") {
        val caregivingCharge = CaregivingCharge(
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
        caregivingCharge.clearEvents()

        `when`("저장을 요청하면") {
            fun behavior() = cacheCaregivingChargeRepository.save(caregivingCharge)
            then("저장이 됩니다.") {
                behavior()
            }
        }

        `when`("조회를 요청하면") {
            fun behavior() = cacheCaregivingChargeRepository.findByIdOrNull("01GVD27R0YSGY0V48EXR2T0FNM")
            then("조회가 됩니다.") {
                behavior()
            }
        }
    }

    given("진행 알림톡 목록 요약 엔티티 테스트를 할 때") {
        val caregivingProgressMessageSummary = CaregivingProgressMessageSummary(
            id = "01H0HGMYED6NNRS8GQDB0N23AA",
            caregivingRoundId = "01H0CZNTEGESA5HRBF9ZP8NV4X",
            caregivingRoundNumber = 2,
            startDateTime = LocalDateTime.of(2023, 5, 13, 14, 20, 0),
            receptionId = "01H0D0NB66HJ8V2YZ0XKTKBG16",
        )

        `when`("저장을 요청하면") {
            fun behavior() = cacheCaregivingProgressMessageSummaryRepository.save(caregivingProgressMessageSummary)
            then("저장이 됩니다.") {
                behavior()
            }
        }

        `when`("조회를 요청하면") {
            fun behavior() = cacheCaregivingProgressMessageSummaryRepository.findByIdOrNull("01H0HGMYED6NNRS8GQDB0N23AA")
            then("조회가 됩니다.") {
                behavior()
            }
        }
    }

    given("간병 시작 메시지 발송 기록 엔티티를 테스트할 때") {
        val caregivingStartMessageSendingHsitory = CaregivingStartMessageSendingHistory(
            id = "01HF3TBJ8G48D071XHR4CS6C13",
            receptionId = "01HF3TC2RQB25TYH6GECSMT1CK",
            attemptDateTime = LocalDateTime.of(2023, 11, 13, 12, 0, 0),
            result = CaregivingStartMessageSendingHistory.SendingResult.SENT,
            messageId = "01HF3TD66Z01M3W8C3APQZFGVH",
        )
        `when`("저장을 요청하면") {
            fun behavior() = cacheCaregivingStartMessageSendingHistoryRepository.save(caregivingStartMessageSendingHsitory)
            then("저장이 됩니다.") {
                behavior()
            }
        }

        `when`("조회를 요청하면") {
            fun behavior() = cacheCaregivingStartMessageSendingHistoryRepository.findByIdOrNull("01HF3TBJ8G48D071XHR4CS6C13")
            then("조회가 됩니다.") {
                behavior()
            }
        }
    }

    given("간병 진행 메시지 발송 기록 엔티티를 테스트할 때") {
        val caregivingProgressMessageSendingHistory = CaregivingProgressMessageSendingHistory(
            id = "01HF3SE0SP5ZCEYQBMYTFBYFEF",
            caregivingRoundId = "01HF3SEETX1W3D6NRDA1KSQ28C",
            attemptDateTime = LocalDateTime.of(2023, 11, 13, 12, 0, 0),
            result = CaregivingProgressMessageSendingHistory.SendingResult.SENT,
            messageId = "01HF3SG3YCN49R4E327ECBA08X",
        )

        `when`("저장을 요청하면") {
            fun behavior() = cacheCaregivingProgressMessageSendingHistoryRepository.save(caregivingProgressMessageSendingHistory)
            then("저장이 됩니다.") {
                behavior()
            }
        }

        `when`("조회를 요청하면") {
            fun behavior() = cacheCaregivingProgressMessageSendingHistoryRepository.findByIdOrNull("01HF3SE0SP5ZCEYQBMYTFBYFEF")
            then("조회가 됩니다.") {
                behavior()
            }
        }
    }

    given("접수 수정 내역 엔티티 테스트할 때") {

        val receptionModificationHsitory = ReceptionModificationHistory(
            id = "01HF3VN1PYMT3E1PY94XC958H3",
            receptionId = "01HF3VNDM8WZ4QPYGR9YS620FW",
            modifiedProperty = ReceptionModificationHistory.ModificationProperty.ACCIDENT_NUMBER,
            previous = "1111-1111",
            modified = "222-2222",
            modifierId = "01HF3VQPMEE3TM92P1HARQ0Y49",
            modifiedDateTime = LocalDateTime.of(2023, 11, 13, 12, 30, 0),
        )
        `when`("저장을 요청하면") {
            fun behavior() = cacheReceptionModificationHistoryRepository.save(receptionModificationHsitory)
            then("저장이 됩니다.") {
                behavior()
            }
        }
        `when`("조회를 요청하면") {
            fun behavior() = cacheReceptionModificationHistoryRepository.findByIdOrNull("01HF3VN1PYMT3E1PY94XC958H3")
            then("조회가 됩니다.") {
                behavior()
            }
        }
    }

    given("간병비 산정 수정 내역 요약 엔티티 테스트할 때") {
        val caregivingChargeModificationSummary = CaregivingChargeModificationSummary(
            id = "01HF3YQZD8AJC967H39154SJGT",
            receptionId = "01HF3YRE8EP9ZP6XY8Y4S623GY",
        )
        `when`("저장을 요청하면") {
            fun behavior() = cacheCaregivingChargeModificationSummaryRepository.save(caregivingChargeModificationSummary)
            then("저장이 됩니다.") {
                behavior()
            }
        }
        `when`("조회를 요청하면") {
            fun behavior() = cacheCaregivingChargeModificationSummaryRepository.findByIdOrNull("01HF3YQZD8AJC967H39154SJGT")
            then("조회가 됩니다.") {
                behavior()
            }
        }
    }

    given("간병 회차 수정 내역 요약 엔티티 테스트할 때") {
        val caregivingRoundModificationSummary = CaregivingRoundModificationSummary(
            id = "01HFN9WS71Q4S2RF8BWJG0J6GE",
            receptionId = "01HFN9WWGHDCXR1B6RS7KT9TQP",
        )
        `when`("저장을 요청하면") {
            fun behavior() = cacheCaregivingRoundModificationSummaryRepository.save(caregivingRoundModificationSummary)
            then("저장이 됩니다.") {
                behavior()
            }
        }
        `when`("조회를 요청하면") {
            fun behavior() = cacheCaregivingRoundModificationSummaryRepository.findByIdOrNull("01HFN9WS71Q4S2RF8BWJG0J6GE")
            then("조회가 됩니다.") {
                behavior()
            }
        }
    }

    given("비즈콜 예약 엔티티를 테스트할 때") {
        val bizcallReservation = BizcallReservation(
            id = "01HFQR2SNB5W2KN44RSTKF27XN",
            sendingDateTime = LocalDateTime.of(2023, 11, 21, 8, 15, 34),
            bizcallId = "64bdd9c044449c0aa9ba9713",
        )
        `when`("저장을 요청하면") {
            fun behavior() = cacheBizcallReservationRepository.save(bizcallReservation)
            then("저장이 됩니다.") {
                behavior()
            }
        }

        `when`("조회를 요청하면") {
            fun behavior() = cacheBizcallReservationRepository.findByIdOrNull("01HFQR2SNB5W2KN44RSTKF27XN")
            then("조희가 됩니다.") {
                behavior()
            }
        }
    }

    given("이메일 전송 로그 엔티티를 테스트할 때") {
        val emailSendingLog = EmailSendingLog.Companion.ofSent(
            id = "01HFQRNAW2BMKSMPF4NJBXENN5",
            recipientAddress = "recipient@caredoc.kr",
            senderAddress = "test@caredoc.kr",
            senderProfile = SenderProfile.INFO.name,
            title = "Test",
            sentDateTime = LocalDateTime.of(2023, 11, 21, 4, 50, 44),
        )
        `when`("저장을 요청하면") {
            fun behavior() = cacheEmailSendingLogRepository.save(emailSendingLog)
            then("저장이 됩니다.") {
                behavior()
            }
        }

        `when`("조회를 요청하면") {
            fun behavior() = cacheEmailSendingLogRepository.findByIdOrNull("01HFQRNAW2BMKSMPF4NJBXENN5")
            then("조희가 됩니다.") {
                behavior()
            }
        }
    }
})
