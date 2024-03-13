package kr.caredoc.careinsurance.reception.caregivingsatisfactionsurvey

import com.github.guepardoapps.kulid.ULID
import io.sentry.Sentry
import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.bizcall.BizcallRecipient
import kr.caredoc.careinsurance.bizcall.BizcallReservation
import kr.caredoc.careinsurance.bizcall.BizcallReservationRepository
import kr.caredoc.careinsurance.bizcall.BizcallReservationRequest
import kr.caredoc.careinsurance.bizcall.BizcallSender
import kr.caredoc.careinsurance.bizcall.Voice
import kr.caredoc.careinsurance.caregiving.LastCaregivingRoundFinished
import kr.caredoc.careinsurance.caregiving.LastCaregivingRoundModified
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQuery
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQueryHandler
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.checkAll
import kr.caredoc.careinsurance.security.encryption.Decryptor
import kr.caredoc.careinsurance.security.hash.PepperedHasher
import kr.caredoc.careinsurance.withSort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter

@Service
class CaregivingSatisfactionSurveyService(
    private val caregivingSatisfactionSurveyStatusRepository: CaregivingSatisfactionSurveyStatusRepository,
    private val patientNameHasher: PepperedHasher,
    private val receptionsByIdsQueryHandler: ReceptionsByIdsQueryHandler,
    private val bizcallReservationRepository: BizcallReservationRepository,
    private val bizcallSender: BizcallSender,
    @Value("\${satisfaction-survey.scenario-id}")
    private val satisfactionSurveyBizcallScenarioId: String,
    @Value("\${satisfaction-survey.caller-number}")
    private val satisfactionSurveyBizcallCallerNumber: String,
    @Value("\${satisfaction-survey.display-number:#{null}}")
    private val satisfactionSurveyBizcallDisplayNumber: String?,
    private val decryptor: Decryptor,
) : CaregivingSatisfactionSurveyStatusesByFilterQueryHandler,
    CaregivingSatisfactionSurveyStatusSearchQueryHandler,
    CaregivingSatisfactionSurveyReserveCommandHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    override fun getCaregivingSatisfactionSurveyStatuses(
        query: CaregivingSatisfactionSurveyStatusesByFilterQuery,
        pageRequest: Pageable
    ): Page<CaregivingSatisfactionSurveyStatus> {
        CaregivingSatisfactionSurveyStatusAccessPolicy.check(query.subject, query, Object.Empty)
        return caregivingSatisfactionSurveyStatusRepository.findByExpectedSendingDate(
            query.filter.date,
            pageRequest.withSort(Sort.by(Sort.Order.desc(CaregivingSatisfactionSurveyStatus::expectedSendingDate.name))),
        )
    }

    @Transactional(readOnly = true)
    override fun searchCaregivingSatisfactionSurveyStatus(
        query: CaregivingSatisfactionSurveyStatusSearchQuery,
        pageRequest: Pageable
    ): Page<CaregivingSatisfactionSurveyStatus> {
        CaregivingSatisfactionSurveyStatusAccessPolicy.check(query.subject, query, Object.Empty)
        val pageable = pageRequest.withSort(
            Sort.by(Sort.Order.desc(CaregivingSatisfactionSurveyStatus::expectedSendingDate.name))
        )
        return when (query.searchCondition.searchingProperty) {
            CaregivingSatisfactionSurveyStatusSearchQuery.SearchingProperty.ACCIDENT_NUMBER -> caregivingSatisfactionSurveyStatusRepository.findByExpectedSendingDateAndAccidentNumberContaining(
                query.filter.date,
                query.searchCondition.keyword,
                pageable,
            )

            CaregivingSatisfactionSurveyStatusSearchQuery.SearchingProperty.PATIENT_NAME -> caregivingSatisfactionSurveyStatusRepository.findByExpectedSendingDateAndHashedPatientName(
                query.filter.date,
                patientNameHasher.hashAsHex(query.searchCondition.keyword),
                pageable,
            )

            CaregivingSatisfactionSurveyStatusSearchQuery.SearchingProperty.CAREGIVER_NAME -> caregivingSatisfactionSurveyStatusRepository.findByExpectedCaregiverNameLike(
                query.filter.date,
                query.searchCondition.keyword,
                pageable,
            )
        }
    }

    @Transactional
    @EventListener(LastCaregivingRoundFinished::class)
    fun handleLastCaregivingRoundFinished(event: LastCaregivingRoundFinished) {
        if (caregivingSatisfactionSurveyStatusRepository.existsByReceptionId(event.receptionId)) {
            return
        }

        caregivingSatisfactionSurveyStatusRepository.save(event.intoSurveyStatus())
    }

    private fun LastCaregivingRoundFinished.intoSurveyStatus(id: String = ULID.random()) =
        CaregivingSatisfactionSurveyStatus(
            id = id,
            receptionId = receptionId,
            caregivingRoundId = lastCaregivingRoundId,
            caregivingRoundEndDate = endDateTime.toLocalDate(),
        )

    @Transactional
    @EventListener(LastCaregivingRoundModified::class)
    fun handleLastCaregivingRoundModified(event: LastCaregivingRoundModified) {
        caregivingSatisfactionSurveyStatusRepository.findByReceptionId(event.receptionId).forEach {
            it.handleLastCaregivingRoundModified(event)
        }
    }

    @Transactional
    override fun reserveSatisfactionSurvey(command: CaregivingSatisfactionSurveyReserveCommand) {
        val receptions = receptionsByIdsQueryHandler.getReceptions(
            ReceptionsByIdsQuery(
                receptionIds = command.targetReceptionIds,
                subject = command.subject,
            )
        )
        val targetSurveys = caregivingSatisfactionSurveyStatusRepository.findByReceptionIdIn(receptions.map { it.id })

        CaregivingSatisfactionSurveyStatusAccessPolicy.checkAll(command.subject, command, targetSurveys)

        val sendingDateTime = Clock.today().atTime(16, 0, 0)

        val bizcallReservation = bizcallReservationRepository.findBySendingDateTime(sendingDateTime)
        val bizcallId = if (bizcallReservation == null) {
            val reservationResult = bizcallSender.reserve(
                BizcallReservationRequest(
                    scenarioId = satisfactionSurveyBizcallScenarioId,
                    originalMdn = satisfactionSurveyBizcallCallerNumber,
                    changedMdn = satisfactionSurveyBizcallDisplayNumber,
                    priority = 2,
                    voiceSpeed = 90,
                    bizcallName = "간병 서비스 만족도 조사",
                    voice = Voice.ARIA,
                    reservationInfo = BizcallReservationRequest.ReservationInfo(sendingDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
                    retry = BizcallReservationRequest.RetryInfo(
                        retryCount = 0,
                        retryInterval = 0,
                        replacedRetry = false,
                    ),
                )
            )

            bizcallReservationRepository.save(
                BizcallReservation(
                    id = ULID.random(),
                    sendingDateTime = sendingDateTime,
                    bizcallId = reservationResult.bizcallId
                )
            ).bizcallId
        } else {
            bizcallReservation.bizcallId
        }

        try {
            val result = bizcallSender.additionalRecipientByReservation(
                bizcallId,
                receptions.map {
                    BizcallRecipient(
                        it.inDecryptionContext(decryptor, command.subject) { decryptPrimaryContact() },
                        mapOf("대상자" to it.inDecryptionContext(decryptor, command.subject) { decryptPatientName() }),
                    )
                }
            )

            if (result?.validationResultList?.isNotEmpty() == true) {
                val message = result.validationResultList.map {
                    "[${it.errorCode}] ${it.errorMessage}(${it.references})"
                }.toString()
                logger.error("bizcall recipient failed: $message")
                Sentry.captureMessage(message)
            }
        } catch (e: Exception) {
            targetSurveys.forEach {
                it.markAsFailed(command.subject)
            }
            logger.error("bizcall sending failed: $e")
            Sentry.captureException(e)

            return
        }

        targetSurveys.forEach {
            it.markAsReserved(command.subject)
        }
    }
}
