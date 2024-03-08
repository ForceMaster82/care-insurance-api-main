package kr.caredoc.careinsurance.reception.history

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.slot
import io.mockk.verify
import kr.caredoc.careinsurance.generateExternalCaregivingOrganizationManagerSubject
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.generateUserSubject
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.patient.Sex
import kr.caredoc.careinsurance.reception.ClaimType
import kr.caredoc.careinsurance.reception.OrganizationType
import kr.caredoc.careinsurance.reception.ReceptionApplicationFileInfo
import kr.caredoc.careinsurance.reception.ReceptionByIdQueryHandler
import kr.caredoc.careinsurance.reception.ReceptionModified
import kr.caredoc.careinsurance.reception.ReceptionReceived
import kr.caredoc.careinsurance.relaxedMock
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.security.access.AccessDeniedException
import java.time.LocalDate
import java.time.LocalDateTime

class ReceptionModificationHistoryServiceTest : BehaviorSpec({
    given("접수 수정 이력 서비스가 주어졌을 때") {
        val receptionModificationHistoryRepository = relaxedMock<ReceptionModificationHistoryRepository>()
        val receptionByIdQueryHandler = relaxedMock<ReceptionByIdQueryHandler>()
        val receptionModificationSummaryRepository = relaxedMock<ReceptionModificationSummaryRepository>()
        val service = ReceptionModificationHistoryService(
            receptionModificationHistoryRepository = receptionModificationHistoryRepository,
            receptionByIdQueryHandler = receptionByIdQueryHandler,
            receptionModificationSummaryRepository = receptionModificationSummaryRepository,
        )

        beforeEach {
            justRun {
                receptionByIdQueryHandler.ensureReceptionExists(
                    match { it.receptionId == "01GYXEQ9FS8K3PCFXWG09FM0F1" }
                )
            }

            with(receptionModificationHistoryRepository) {
                val savingHistorySlot = slot<ReceptionModificationHistory>()

                every {
                    save(capture(savingHistorySlot))
                } answers {
                    savingHistorySlot.captured
                }
            }
            with(receptionModificationSummaryRepository) {
                every { findTopByReceptionId(any()) } returns null
                val savingSummarySlot = slot<ReceptionModificationSummary>()
                every { save(capture(savingSummarySlot)) } answers { savingSummarySlot.captured }
            }
        }

        afterEach {
            clearAllMocks()
        }

        and("이미 등록된 접수 수정 내역이 존재할 때") {
            val registeredReceptionModificationHistorites = listOf<ReceptionModificationHistory>(
                relaxedMock(),
                relaxedMock(),
                relaxedMock(),
            )
            val registeredReceptionModificationSummary = relaxedMock<ReceptionModificationSummary>()

            beforeEach {
                val pageableSlot = slot<Pageable>()
                every {
                    receptionModificationHistoryRepository.findByReceptionId(
                        match { it == "01GYXEQ9FS8K3PCFXWG09FM0F1" },
                        capture(pageableSlot),
                    )
                } answers {
                    PageImpl(
                        registeredReceptionModificationHistorites,
                        pageableSlot.captured,
                        registeredReceptionModificationHistorites.size.toLong(),
                    )
                }
                every {
                    receptionModificationSummaryRepository.findTopByReceptionId("01GYXEQ9FS8K3PCFXWG09FM0F1")
                } returns registeredReceptionModificationSummary
            }

            afterEach { clearAllMocks() }

            `when`("접수의 여러 개의 항목들이 직접 수정되면") {
                val event = relaxedMock<ReceptionModified>()

                beforeEach {
                    with(event) {
                        every { receptionId } returns "01GYXEQ9FS8K3PCFXWG09FM0F1"
                        every { insuranceInfo } returns Modification(
                            relaxedMock {
                                every { insuranceNumber } returns "2023-12345"
                                every { subscriptionDate } returns LocalDate.of(2023, 3, 12)
                                every { coverageId } returns "01GYV6XZT5252KS0VY0D7469EA"
                                every { caregivingLimitPeriod } returns 100
                            },
                            relaxedMock {
                                every { insuranceNumber } returns "2023-56789"
                                every { subscriptionDate } returns LocalDate.of(2023, 4, 12)
                                every { coverageId } returns "01GZ3H1WDENE96WFS52ZCSSTK6"
                                every { caregivingLimitPeriod } returns 180
                            },
                        )
                        every { accidentInfo } returns Modification(
                            relaxedMock {
                                every { accidentNumber } returns "2023-1111111"
                                every { accidentDateTime } returns LocalDateTime.of(2023, 4, 12, 18, 7, 55)
                                every { claimType } returns ClaimType.ACCIDENT
                                every { patientDescription } returns "자력으로 호흡 불가능"
                                every { admissionDateTime } returns LocalDateTime.of(2023, 4, 12, 21, 3, 12)
                                every { hospitalAndRoomInfo.state } returns null
                                every { hospitalAndRoomInfo.city } returns null
                                every { hospitalAndRoomInfo.hospitalAndRoom } returns "케어닥 병원 304호실"
                            },
                            relaxedMock {
                                every { accidentNumber } returns "2023-1111112"
                                every { accidentDateTime } returns LocalDateTime.of(2023, 4, 12, 18, 27, 55)
                                every { claimType } returns ClaimType.SICKNESS
                                every { patientDescription } returns "자력으로 호흡 불가능하고 거동이 불편함"
                                every { admissionDateTime } returns LocalDateTime.of(2023, 4, 12, 21, 23, 12)
                                every { hospitalAndRoomInfo.state } returns "서울특별시"
                                every { hospitalAndRoomInfo.city } returns "강남구"
                                every { hospitalAndRoomInfo.hospitalAndRoom } returns "케어닥 병원 504호실"
                            },
                        )
                        every { patientInfo } returns Modification(
                            relaxedMock {
                                every { name.masked } returns "방*영"
                                every { nickname } returns "레나"
                                every { age } returns 59
                                every { height } returns null
                                every { weight } returns null
                                every { sex } returns Sex.FEMALE
                                every { primaryContact.maskedPhoneNumber } returns "010****2222"
                                every { primaryContact.relationshipWithPatient } returns "본인"
                                every { secondaryContact?.maskedPhoneNumber } returns "010****5555"
                                every { secondaryContact?.relationshipWithPatient } returns "자매"
                            },
                            relaxedMock {
                                every { name.masked } returns "임*민"
                                every { nickname } returns "레나"
                                every { age } returns 59
                                every { height } returns 163
                                every { weight } returns 49
                                every { sex } returns Sex.FEMALE
                                every { primaryContact.maskedPhoneNumber } returns "010****2222"
                                every { primaryContact.relationshipWithPatient } returns "본인"
                                every { secondaryContact?.maskedPhoneNumber } returns "010****4444"
                                every { secondaryContact?.relationshipWithPatient } returns "남편"
                            },
                        )
                        every { caregivingManagerInfo } returns Modification(
                            relaxedMock {
                                every { organizationType } returns OrganizationType.INTERNAL
                                every { organizationId } returns null
                                every { managingUserId } returns "01GR8BNHFPYQW55PNGKHAKBNS6"
                            },
                            relaxedMock {
                                every { organizationType } returns OrganizationType.AFFILIATED
                                every { organizationId } returns "01GPWXVJB2WPDNXDT5NE3B964N"
                                every { managingUserId } returns "01GZD52ZVE40E6NGQ0A1T016Z5"
                            },
                        )
                        every { applicationFileInfo } returns Modification(
                            ReceptionApplicationFileInfo("[메리츠] SHENSHUNFU_간병인신청서.pdf", "https://url.to/file1"),
                            ReceptionApplicationFileInfo("여행경비정산표.pdf", "https://url.to/file2"),
                        )
                        every { desiredCaregivingStartDate } returns Modification(
                            LocalDate.of(2023, 3, 14),
                            LocalDate.of(2023, 4, 14),
                        )
                        every { expectedCaregivingStartDate } returns Modification(
                            null,
                            LocalDate.of(2023, 4, 12),
                        )
                        every { notifyCaregivingProgress } returns Modification(
                            previous = true,
                            current = false,
                        )
                        every { expectedCaregivingLimitDate } returns Modification(
                            LocalDate.of(2023, 3, 24),
                            LocalDate.of(2023, 4, 24),
                        )
                        every { desiredCaregivingPeriod } returns Modification(
                            2,
                            180,
                        )
                        every { additionalRequests } returns Modification(
                            "",
                            "잘 부탁 드립니다. "
                        )
                        every { event.cause } returns ReceptionModified.Cause.DIRECT_EDIT
                        every { editingSubject } returns generateUserSubject("01GYS7MER33TDH6PJFQC7XYA90")
                        every { event.modifiedDateTime } returns LocalDateTime.of(2023, 4, 25, 12, 37, 29)
                    }
                }

                afterEach { clearAllMocks() }

                fun handling() = service.handleReceptionModified(event)

                then("수정된 항목들을 기록으로 저장합니다.") {
                    handling()

                    fun verifyModification(
                        modifiedProperty: ReceptionModificationHistory.ModificationProperty,
                        previous: String?,
                        modified: String?,
                    ) {
                        verify {
                            receptionModificationHistoryRepository.save(
                                withArg {
                                    it.receptionId shouldBe "01GYXEQ9FS8K3PCFXWG09FM0F1"
                                    it.modifierId shouldBe "01GYS7MER33TDH6PJFQC7XYA90"
                                    it.modifiedDateTime shouldBe LocalDateTime.of(2023, 4, 25, 12, 37, 29)
                                    it.modifiedProperty shouldBe modifiedProperty
                                    it.previous shouldBe previous
                                    it.modified shouldBe modified
                                }
                            )
                        }
                    }

                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.INSURANCE_NUMBER,
                        "2023-12345",
                        "2023-56789",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.SUBSCRIPTION_DATE,
                        "2023-03-12",
                        "2023-04-12",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.COVERAGE_ID,
                        "01GYV6XZT5252KS0VY0D7469EA",
                        "01GZ3H1WDENE96WFS52ZCSSTK6",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.CAREGIVING_LIMIT_PERIOD,
                        "100",
                        "180",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.CLAIM_TYPE,
                        "ACCIDENT",
                        "SICKNESS",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.ADMISSION_DATE_TIME,
                        "2023-04-12T21:03:12",
                        "2023-04-12T21:23:12",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.ACCIDENT_NUMBER,
                        "2023-1111111",
                        "2023-1111112",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.HOSPITAL_STATE,
                        null,
                        "서울특별시",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.HOSPITAL_CITY,
                        null,
                        "강남구",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.HOSPITAL_AND_ROOM,
                        "케어닥 병원 304호실",
                        "케어닥 병원 504호실",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.ACCIDENT_DATE_TIME,
                        "2023-04-12T18:07:55",
                        "2023-04-12T18:27:55",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.PATIENT_DESCRIPTION,
                        "자력으로 호흡 불가능",
                        "자력으로 호흡 불가능하고 거동이 불편함",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.PATIENT_HEIGHT,
                        null,
                        "163",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.PATIENT_WEIGHT,
                        null,
                        "49",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.PATIENT_SECONDARY_PHONE_NUMBER,
                        "010****5555",
                        "010****4444",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.PATIENT_SECONDARY_RELATIONSHIP,
                        "자매",
                        "남편",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.CAREGIVING_ORGANIZATION_TYPE,
                        "INTERNAL",
                        "AFFILIATED",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.CAREGIVING_ORGANIZATION_ID,
                        null,
                        "01GPWXVJB2WPDNXDT5NE3B964N",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.CAREGIVING_MANAGING_USER_ID,
                        "01GR8BNHFPYQW55PNGKHAKBNS6",
                        "01GZD52ZVE40E6NGQ0A1T016Z5",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.DESIRED_CAREGIVING_START_DATE,
                        "2023-03-14",
                        "2023-04-14",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.EXPECTED_CAREGIVING_START_DATE,
                        null,
                        "2023-04-12",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.EXPECTED_CAREGIVING_LIMIT_DATE,
                        "2023-03-24",
                        "2023-04-24",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.DESIRED_CAREGIVING_PERIOD,
                        "2",
                        "180",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.ADDITIONAL_REQUESTS,
                        "",
                        "잘 부탁 드립니다. ",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.PATIENT_NAME,
                        "방*영",
                        "임*민",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.NOTIFY_CAREGIVING_PROGRESS,
                        "true",
                        "false",
                    )
                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.RECEPTION_APPLICATION_FILE_NAME,
                        "[메리츠] SHENSHUNFU_간병인신청서.pdf",
                        "여행경비정산표.pdf",
                    )
                }

                then("접수 수정 개요를 수정합니다.") {
                    handling()

                    verify {
                        receptionModificationSummaryRepository.findTopByReceptionId(
                            withArg {
                                it shouldBe "01GYXEQ9FS8K3PCFXWG09FM0F1"
                            }
                        )
                    }

                    verify {
                        registeredReceptionModificationSummary.handleReceptionModified(event)
                    }
                }
            }
            and("접수의 간병인 신청서 파일이 없는 상태에서 생성된다면") {
                val event = relaxedMock<ReceptionModified>()

                beforeEach {
                    with(event) {
                        every { applicationFileInfo } returns relaxedMock {
                            every { previous } returns null
                            every { current } returns relaxedMock {
                                every { receptionApplicationFileName } returns "간병인 신청서.pdf"
                            }
                        }
                    }
                }

                afterEach { clearAllMocks() }

                fun handling() = service.handleReceptionModified(event)

                then("아무일도 일어나지 않습니다.") {
                    handling()

                    fun verifyModification(
                        modifiedProperty: ReceptionModificationHistory.ModificationProperty,
                        previous: String?,
                        modified: String?,
                    ) {
                        verify(exactly = 0) {
                            receptionModificationHistoryRepository.save(
                                withArg {
                                    it.receptionId shouldBe "01GYXEQ9FS8K3PCFXWG09FM0F1"
                                    it.modifierId shouldBe "01GYS7MER33TDH6PJFQC7XYA90"
                                    it.modifiedDateTime shouldBe LocalDateTime.of(2023, 4, 25, 12, 37, 29)
                                    it.modifiedProperty shouldBe modifiedProperty
                                    it.previous shouldBe previous
                                    it.modified shouldBe modified
                                }
                            )
                        }
                    }

                    verifyModification(
                        ReceptionModificationHistory.ModificationProperty.RECEPTION_APPLICATION_FILE_NAME,
                        null,
                        "간병인 신청서.pdf",
                    )
                }
            }

            `when`("접수 수정 내역을 조회하면") {
                val query = ReceptionModificationHistoriesByReceptionIdQuery(
                    receptionId = "01GYXEQ9FS8K3PCFXWG09FM0F1",
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = service.getReceptionModificationHistories(
                    query = query,
                    pageRequest = PageRequest.of(0, 10),
                )

                then("접수 정보가 존재하며 접수에 접근 가능한지 검증합니다.") {
                    behavior()

                    verify {
                        receptionByIdQueryHandler.ensureReceptionExists(
                            withArg {
                                it.receptionId shouldBe "01GYXEQ9FS8K3PCFXWG09FM0F1"
                            }
                        )
                    }
                }

                then("접수 수정 내역을 리포지토리로부터 조회합니다.") {
                    behavior()

                    verify {
                        receptionModificationHistoryRepository.findByReceptionId(
                            withArg { it shouldBe "01GYXEQ9FS8K3PCFXWG09FM0F1" },
                            withArg {
                                it.pageSize shouldBe 10
                                it.pageNumber shouldBe 0
                            }
                        )
                    }
                }

                then("조회한 수정 내역을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult.content shouldContainExactlyInAnyOrder registeredReceptionModificationHistorites
                }
            }

            `when`("내부 사용자 권한 없이 접수 수정 내역을 조회하면") {
                val query = ReceptionModificationHistoriesByReceptionIdQuery(
                    receptionId = "01GYXEQ9FS8K3PCFXWG09FM0F1",
                    subject = generateExternalCaregivingOrganizationManagerSubject("01H1972AQ7M5MMMWBKVZ4G1DQV"),
                )

                fun behavior() = service.getReceptionModificationHistories(
                    query = query,
                    pageRequest = PageRequest.of(0, 10),
                )

                then("throws AccessDeniedException") {
                    shouldThrow<AccessDeniedException> { behavior() }
                }
            }

            `when`("접수 아이디로 접수 수정 개요를 조회하면") {
                val query = ReceptionModificationSummaryByReceptionIdQuery(
                    receptionId = "01GYXEQ9FS8K3PCFXWG09FM0F1",
                    subject = generateInternalCaregivingManagerSubject(),
                )

                fun behavior() = service.getReceptionModificationSummary(query)

                then("접수가 존재하며 접수에 접근 가능한지 검증합니다.") {
                    behavior()

                    verify {
                        receptionByIdQueryHandler.ensureReceptionExists(
                            withArg {
                                it.receptionId shouldBe "01GYXEQ9FS8K3PCFXWG09FM0F1"
                            }
                        )
                    }
                }

                then("리포지토리로부터 접수 아이디를 기준으로 접수 수정 개요를 조회합니다.") {
                    behavior()

                    verify {
                        receptionModificationSummaryRepository.findTopByReceptionId("01GYXEQ9FS8K3PCFXWG09FM0F1")
                    }
                }

                then("조회한 접수 수정 개요를 반환합니다.") {
                    val actualResult = behavior()

                    actualResult shouldBe registeredReceptionModificationSummary
                }
            }

            `when`("접수가 직접 수정 외의 다른 이유로 수정되면") {
                val event = relaxedMock<ReceptionModified>()

                beforeEach {
                    every { event.receptionId } returns "01GYXEQ9FS8K3PCFXWG09FM0F1"
                    every { event.cause } returns ReceptionModified.Cause.ETC
                }

                afterEach { clearAllMocks() }

                fun handling() = service.handleReceptionModified(event)

                then("아무것도 안합니다.") {
                    handling()

                    verify(exactly = 0) {
                        registeredReceptionModificationSummary.handleReceptionModified(any())
                        receptionModificationSummaryRepository.save(any())
                        receptionModificationHistoryRepository.save(any())
                    }
                }
            }
        }

        `when`("내부 사용자 권한 없이 접수 아이디로 접수 수정 개요를 조회하면") {
            val query = ReceptionModificationSummaryByReceptionIdQuery(
                receptionId = "01GYXEQ9FS8K3PCFXWG09FM0F1",
                subject = generateExternalCaregivingOrganizationManagerSubject("01H1972AQ7M5MMMWBKVZ4G1DQV"),
            )

            fun behavior() = service.getReceptionModificationSummary(query)

            then("throws AccessDeniedException") {
                shouldThrow<AccessDeniedException> { behavior() }
            }
        }

        `when`("접수 아이디로 접수 수정 개요를 조회하면") {
            val query = ReceptionModificationSummaryByReceptionIdQuery(
                receptionId = "01GYXEQ9FS8K3PCFXWG09FM0F1",
                subject = generateInternalCaregivingManagerSubject(),
            )

            fun behavior() = service.getReceptionModificationSummary(query)

            then("ReceptionModificationSummaryNotFoundByReceptionIdException이 발생합니다.") {
                shouldThrow<ReceptionModificationSummaryNotFoundByReceptionIdException> { behavior() }
            }
        }

        `when`("접수가 등록되면") {
            val event = relaxedMock<ReceptionReceived>()

            beforeEach {
                every { event.receptionId } returns "01GYXEQ9FS8K3PCFXWG09FM0F1"
            }

            afterEach { clearAllMocks() }

            fun handling() = service.handleReceptionReceived(event)

            then("접수 수정 개요를 저장합니다.") {
                handling()

                verify {
                    receptionModificationSummaryRepository.save(
                        withArg {
                            it.receptionId shouldBe "01GYXEQ9FS8K3PCFXWG09FM0F1"
                            it.lastModifiedDateTime shouldBe null
                            it.lastModifierId shouldBe null
                            it.modificationCount shouldBe 0
                        }
                    )
                }
            }
        }
    }
})
