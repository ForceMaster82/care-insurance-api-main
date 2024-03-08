package kr.caredoc.careinsurance.caregiving.progressmessage

import com.github.guepardoapps.kulid.ULID
import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.alimtalk.AlimtalkSender
import kr.caredoc.careinsurance.alimtalk.BulkAlimtalkMessage
import kr.caredoc.careinsurance.alimtalk.SendingResultOfMessage
import kr.caredoc.careinsurance.caregiving.CaregivingRound
import kr.caredoc.careinsurance.caregiving.CaregivingRoundModified
import kr.caredoc.careinsurance.caregiving.CaregivingRoundStarted
import kr.caredoc.careinsurance.caregiving.CaregivingRoundsByIdsQuery
import kr.caredoc.careinsurance.caregiving.CaregivingRoundsByIdsQueryHandler
import kr.caredoc.careinsurance.caregiving.exception.CaregivingRoundNotFoundByIdException
import kr.caredoc.careinsurance.caregiving.exception.ReferenceCaregivingRoundNotExistException
import kr.caredoc.careinsurance.message.SendingStatus
import kr.caredoc.careinsurance.reception.ReceptionByIdQuery
import kr.caredoc.careinsurance.reception.ReceptionByIdQueryHandler
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.security.encryption.Decryptor
import kr.caredoc.careinsurance.security.hash.PepperedHasher
import kr.caredoc.careinsurance.withSort
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CaregivingProgressMessageService(
    private val receptionByIdQueryHandler: ReceptionByIdQueryHandler,
    private val caregivingRoundsByIdsQueryHandler: CaregivingRoundsByIdsQueryHandler,
    private val caregivingProgressMessageSummaryRepository: CaregivingProgressMessageSummaryRepository,
    private val caregivingProgressMessageSendingHistoryRepository: CaregivingProgressMessageSendingHistoryRepository,
    private val alimtalkSender: AlimtalkSender,
    private val decryptor: Decryptor,
    private val patientNameHasher: PepperedHasher,
) : CaregivingProgressMessageSummariesByFilterQueryHandler,
    CaregivingProgressMessageSummariesSearchQueryHandler,
    CaregivingProgressMessageSendingCommandHandler {

    companion object {
        private val DEFAULT_SORT = Sort.by(
            Sort.Order.desc(CaregivingProgressMessageSummaryRepository.RECEPTION_RECEIVED_DATE_TIME_ORDERING)
        )
    }

    @Transactional(readOnly = true)
    override fun searchCaregivingProgressMessageSummaries(
        query: CaregivingProgressMessageSummariesSearchQuery,
        pageRequest: Pageable
    ): Page<CaregivingProgressMessageSummary> {
        CaregivingProgressMessageSummaryAccessPolicy.check(query.subject, query, Object.Empty)

        return if (query.filter.sendingStatus == null) {
            when (query.searchCondition.searchingProperty) {
                CaregivingProgressMessageSummariesSearchQuery.SearchingProperty.ACCIDENT_NUMBER -> caregivingProgressMessageSummaryRepository.getCaregivingProgressMessageSummaryByDateAndAccidentNumber(
                    expectedSendingDate = query.filter.date,
                    accidentNumberKeyword = query.searchCondition.keyword,
                    pageable = pageRequest.withSort(DEFAULT_SORT),
                )

                CaregivingProgressMessageSummariesSearchQuery.SearchingProperty.PATIENT_NAME -> caregivingProgressMessageSummaryRepository.getCaregivingProgressMessageSummaryByDateAndHashedPatientName(
                    expectedSendingDate = query.filter.date,
                    hashedPatientName = patientNameHasher.hashAsHex(query.searchCondition.keyword),
                    pageable = pageRequest.withSort(DEFAULT_SORT),
                )
            }
        } else {
            when (query.searchCondition.searchingProperty) {
                CaregivingProgressMessageSummariesSearchQuery.SearchingProperty.ACCIDENT_NUMBER -> caregivingProgressMessageSummaryRepository.getCaregivingProgressMessageSummaryByDateAndSendingStatusAndAccidentNumber(
                    expectedSendingDate = query.filter.date,
                    sendingStatus = query.filter.sendingStatus,
                    accidentNumberKeyword = query.searchCondition.keyword,
                    pageable = pageRequest.withSort(DEFAULT_SORT),
                )

                CaregivingProgressMessageSummariesSearchQuery.SearchingProperty.PATIENT_NAME -> caregivingProgressMessageSummaryRepository.getCaregivingProgressMessageSummaryByDateAndSendingStatusAndHashedPatientName(
                    expectedSendingDate = query.filter.date,
                    sendingStatus = query.filter.sendingStatus,
                    hashedPatientName = patientNameHasher.hashAsHex(query.searchCondition.keyword),
                    pageable = pageRequest.withSort(DEFAULT_SORT),
                )
            }
        }
    }

    @Transactional(readOnly = true)
    override fun getCaregivingProgressMessageSummaries(
        query: CaregivingProgressMessageSummariesByFilterQuery,
        pageRequest: Pageable
    ): Page<CaregivingProgressMessageSummary> {
        CaregivingProgressMessageSummaryAccessPolicy.check(query.subject, query, Object.Empty)

        return if (query.filter.sendingStatus == null) {
            caregivingProgressMessageSummaryRepository.getCaregivingProgressMessageSummaryByDate(
                expectedSendingDate = query.filter.date,
                pageable = pageRequest.withSort(DEFAULT_SORT),
            )
        } else {
            caregivingProgressMessageSummaryRepository.getCaregivingProgressMessageSummaryByDateAndSendingStatus(
                expectedSendingDate = query.filter.date,
                sendingStatus = query.filter.sendingStatus,
                pageable = pageRequest.withSort(DEFAULT_SORT),
            )
        }
    }

    @Transactional
    @EventListener(CaregivingRoundStarted::class)
    fun handleCaregivingRoundStarted(event: CaregivingRoundStarted) {
        if (caregivingProgressMessageSummaryRepository.existsByCaregivingRoundId(event.caregivingRoundId)) {
            return
        }

        caregivingProgressMessageSummaryRepository.save(
            CaregivingProgressMessageSummary(
                id = ULID.random(),
                caregivingRoundId = event.caregivingRoundId,
                caregivingRoundNumber = event.caregivingRoundNumber,
                startDateTime = event.startDateTime,
                receptionId = event.receptionId,
            )
        )
    }

    @Transactional
    override fun sendCaregivingProgressMessages(command: CaregivingProgressMessageSendingCommand) {
        CaregivingProgressMessageAccessPolicy.check(command.subject, command, Object.Empty)

        if (command.targetCaregivingRoundIds.isEmpty()) {
            return
        }

        val caregivingRounds = try {
            caregivingRoundsByIdsQueryHandler.getCaregivingRounds(
                CaregivingRoundsByIdsQuery(
                    command.targetCaregivingRoundIds,
                    command.subject,
                )
            )
        } catch (e: CaregivingRoundNotFoundByIdException) {
            throw ReferenceCaregivingRoundNotExistException(e.caregivingRoundId, e)
        }

        sendFirstCaregivingRoundProgressMessages(caregivingRounds, command.subject)

        sendNonFirstCaregivingRoundProgressMessages(caregivingRounds, command.subject)
    }

    private fun sendFirstCaregivingRoundProgressMessages(caregivingRounds: List<CaregivingRound>, subject: Subject) {
        val targets = caregivingRounds.filter { it.caregivingRoundNumber == 1 }
        if (targets.isEmpty()) {
            return
        }
        val firstRoundMessageParameters =
            targets.map {
                val reception = receptionByIdQueryHandler.getReception(
                    ReceptionByIdQuery(
                        it.receptionInfo.receptionId,
                        subject,
                    )
                )
                BulkAlimtalkMessage.MessageParameter(
                    id = it.id,
                    recipient = reception.inDecryptionContext(decryptor, subject) { decryptPrimaryContact() },
                    templateData = mutableListOf(
                        "고객명" to reception.inDecryptionContext(decryptor, subject) { decryptPatientName() },
                    ),
                )
            }

        val results = alimtalkSender.send(
            BulkAlimtalkMessage(
                templateCode = "M-20230823",
                messageParameters = firstRoundMessageParameters,
            )
        )

        val now = Clock.now()
        for (result in results) {
            recordMessageSendingHistory(result, now)
            updateMessageSummary(result, now)
        }
    }

    private fun sendNonFirstCaregivingRoundProgressMessages(caregivingRounds: List<CaregivingRound>, subject: Subject) {
        val targets = caregivingRounds.filter { it.caregivingRoundNumber != 1 }
        if (targets.isEmpty()) {
            return
        }
        val nonFirstRoundMessageParameters =
            targets.map {
                val reception = receptionByIdQueryHandler.getReception(
                    ReceptionByIdQuery(
                        it.receptionInfo.receptionId,
                        subject,
                    )
                )
                BulkAlimtalkMessage.MessageParameter(
                    id = it.id,
                    recipient = reception.inDecryptionContext(decryptor, subject) { decryptPrimaryContact() },
                    templateData = mutableListOf(
                        "고객명" to reception.inDecryptionContext(decryptor, subject) { decryptPatientName() },
                    ),
                )
            }

        val results = alimtalkSender.send(
            BulkAlimtalkMessage(
                templateCode = "M-20230823-1",
                messageParameters = nonFirstRoundMessageParameters,
            )
        )

        val now = Clock.now()
        for (result in results) {
            recordMessageSendingHistory(result, now)
            updateMessageSummary(result, now)
        }
    }

    private fun recordMessageSendingHistory(sendingResultOfMessage: SendingResultOfMessage, now: LocalDateTime) {
        caregivingProgressMessageSendingHistoryRepository.save(
            CaregivingProgressMessageSendingHistory(
                id = ULID.random(),
                caregivingRoundId = sendingResultOfMessage.messageId,
                attemptDateTime = now,
                result = if (sendingResultOfMessage.sentMessageId == null) {
                    CaregivingProgressMessageSendingHistory.SendingResult.FAILED
                } else {
                    CaregivingProgressMessageSendingHistory.SendingResult.SENT
                },
                messageId = sendingResultOfMessage.sentMessageId
            )
        )
    }

    private fun updateMessageSummary(sendingResultOfMessage: SendingResultOfMessage, now: LocalDateTime) {
        caregivingProgressMessageSummaryRepository.findByCaregivingRoundId(sendingResultOfMessage.messageId).forEach {
            if (sendingResultOfMessage.sentMessageId == null) {
                it.updateSendingResult(
                    SendingStatus.FAILED,
                    null,
                )
            } else {
                it.updateSendingResult(
                    SendingStatus.SENT,
                    now,
                )
            }
        }
    }

    @Transactional
    @EventListener(CaregivingRoundModified::class)
    fun handleCaregivingModified(event: CaregivingRoundModified) {
        caregivingProgressMessageSummaryRepository.findByCaregivingRoundId(event.caregivingRoundId)
            .forEach {
                if (it.willBeAffectedBy(event)) {
                    it.handleCaregivingModified(event)
                    caregivingProgressMessageSummaryRepository.save(it)
                }
            }
    }
}
