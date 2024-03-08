package kr.caredoc.careinsurance.reception.caregivingstartmessage

import com.github.guepardoapps.kulid.ULID
import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.alimtalk.AlimtalkSender
import kr.caredoc.careinsurance.alimtalk.BulkAlimtalkMessage
import kr.caredoc.careinsurance.alimtalk.SendingResultOfMessage
import kr.caredoc.careinsurance.caregiving.CaregivingRoundModified
import kr.caredoc.careinsurance.caregiving.CaregivingRoundStarted
import kr.caredoc.careinsurance.message.SendingStatus
import kr.caredoc.careinsurance.reception.Reception
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQuery
import kr.caredoc.careinsurance.reception.ReceptionsByIdsQueryHandler
import kr.caredoc.careinsurance.reception.ReferenceReceptionNotExistsException
import kr.caredoc.careinsurance.reception.exception.ReceptionNotFoundByIdException
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.security.encryption.Decryptor
import kr.caredoc.careinsurance.security.hash.PepperedHasher
import kr.caredoc.careinsurance.security.personaldata.IncludingPersonalData
import kr.caredoc.careinsurance.withSort
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CaregivingStartMessageService(
    private val receptionsByIdsQueryHandler: ReceptionsByIdsQueryHandler,
    private val caregivingStartMessageSummaryRepository: CaregivingStartMessageSummaryRepository,
    private val caregivingStartMessageSendingHistoryRepository: CaregivingStartMessageSendingHistoryRepository,
    private val alimtalkSender: AlimtalkSender,
    private val decryptor: Decryptor,
    private val patientNameHasher: PepperedHasher,
) : CaregivingStartMessageSummariesByFilterQueryHandler,
    CaregivingStartMessageSummarySearchQueryHandler,
    CaregivingStartMessageSendingCommandHandler {
    @Transactional
    override fun getCaregivingStartMessageSummaries(
        query: CaregivingStartMessageSummariesByFilterQuery,
        pageRequest: Pageable
    ): Page<CaregivingStartMessageSummary> {
        CaregivingStartMessageSummaryAccessPolicy.check(query.subject, query, Object.Empty)
        return if (query.filter.sendingStatus == null) {
            caregivingStartMessageSummaryRepository.findByExpectedSendingDate(
                query.filter.date,
                pageRequest.withDefaultSummarySort(),
            )
        } else {
            caregivingStartMessageSummaryRepository.findByExpectedSendingDateAndSendingStatus(
                query.filter.date,
                query.filter.sendingStatus,
                pageRequest.withDefaultSummarySort(),
            )
        }
    }

    private fun Pageable.withDefaultSummarySort() = this.withSort(
        Sort.by(Sort.Order.desc(CaregivingStartMessageSummaryRepository.RECEPTION_RECEIVED_DATE_TIME_ORDERING)),
    )

    @Transactional
    override fun searchCaregivingStartMessageSummary(
        query: CaregivingStartMessageSummarySearchQuery,
        pageRequest: Pageable
    ): Page<CaregivingStartMessageSummary> {
        CaregivingStartMessageSummaryAccessPolicy.check(query.subject, query, Object.Empty)
        return if (query.filter.sendingStatus == null) {
            when (query.searchCondition.searchingProperty) {
                CaregivingStartMessageSummarySearchQuery.SearchingProperty.ACCIDENT_NUMBER -> caregivingStartMessageSummaryRepository.searchByExpectedSendingDateAndAccidentNumberKeyword(
                    query.filter.date,
                    query.searchCondition.keyword,
                    pageRequest.withDefaultSummarySort(),
                )

                CaregivingStartMessageSummarySearchQuery.SearchingProperty.PATIENT_NAME -> caregivingStartMessageSummaryRepository.searchByExpectedSendingDateAndHashedPatientName(
                    query.filter.date,
                    patientNameHasher.hashAsHex(query.searchCondition.keyword),
                    pageRequest.withDefaultSummarySort(),
                )
            }
        } else {
            when (query.searchCondition.searchingProperty) {
                CaregivingStartMessageSummarySearchQuery.SearchingProperty.ACCIDENT_NUMBER -> caregivingStartMessageSummaryRepository.searchByExpectedSendingDateAndSendingStatusAndAccidentNumberKeyword(
                    query.filter.date,
                    query.filter.sendingStatus,
                    query.searchCondition.keyword,
                    pageRequest.withDefaultSummarySort(),
                )

                CaregivingStartMessageSummarySearchQuery.SearchingProperty.PATIENT_NAME -> caregivingStartMessageSummaryRepository.searchByExpectedSendingDateAndSendingStatusAndHashedPatientName(
                    query.filter.date,
                    query.filter.sendingStatus,
                    patientNameHasher.hashAsHex(query.searchCondition.keyword),
                    pageRequest.withDefaultSummarySort(),
                )
            }
        }
    }

    @Transactional
    @EventListener(CaregivingRoundModified::class)
    fun handleCaregivingRoundModified(@IncludingPersonalData event: CaregivingRoundModified) {
        if (event.caregivingRoundNumber != 1) {
            // 간병 시작 메시지는 첫번째 회차만 다룹니다.
            return
        }

        val startDateModification = event.startDateTime.map { it?.toLocalDate() }
        if (!startDateModification.hasChanged || startDateModification.current == null) {
            // 시작시간이 존재하거나 변경된 경우에만 시작 메시지에 영향을 줍니다.
            return
        }

        val messageSummaries = caregivingStartMessageSummaryRepository.findByReceptionId(
            event.receptionId
        )
        messageSummaries.forEach { it.handleCaregivingRoundModified(event) }
    }

    @Transactional
    @EventListener(CaregivingRoundStarted::class)
    fun handleCaregivingRoundStarted(event: CaregivingRoundStarted) {
        if (event.caregivingRoundNumber != 1) {
            // 간병 시작 메시지는 첫번째 회차만 다룹니다.
            return
        }

        val messageSummaries = caregivingStartMessageSummaryRepository.findByReceptionId(
            event.receptionId
        )
        if (messageSummaries.isEmpty()) {
            caregivingStartMessageSummaryRepository.save(
                CaregivingStartMessageSummary(
                    id = ULID.random(),
                    receptionId = event.receptionId,
                    firstCaregivingRoundId = event.caregivingRoundId,
                    caregivingRoundStartDate = event.startDateTime.toLocalDate(),
                )
            )
        }
    }

    @Transactional
    override fun sendCaregivingStartMessage(command: CaregivingStartMessageSendingCommand) {
        CaregivingStartMessageAccessPolicy.check(command.subject, command, Object.Empty)
        if (command.targetReceptionIds.isEmpty()) {
            return
        }

        val receptions = try {
            receptionsByIdsQueryHandler.getReceptions(
                ReceptionsByIdsQuery(
                    command.targetReceptionIds,
                    command.subject,
                )
            )
        } catch (e: ReceptionNotFoundByIdException) {
            throw ReferenceReceptionNotExistsException(e.receptionId, e)
        }

        val results = sendCaregivingStartMessages(receptions, command.subject)

        val now = Clock.now()
        for (result in results) {
            recordMessageHistory(result, now)
            updateSummary(result, now)
        }
    }

    private fun sendCaregivingStartMessages(
        targetReceptions: Collection<Reception>,
        subject: Subject,
    ): List<SendingResultOfMessage> {
        val messageParameters = targetReceptions.map {
            BulkAlimtalkMessage.MessageParameter(
                id = it.id,
                recipient = it.inDecryptionContext(decryptor, subject) { decryptPrimaryContact() },
                templateData = listOf(),
            )
        }

        return alimtalkSender.send(
            BulkAlimtalkMessage(
                templateCode = "M-20230823-3",
                messageParameters = messageParameters,
            )
        )
    }

    private fun recordMessageHistory(sendingResultOfMessage: SendingResultOfMessage, now: LocalDateTime) {
        caregivingStartMessageSendingHistoryRepository.save(
            CaregivingStartMessageSendingHistory(
                id = ULID.random(),
                receptionId = sendingResultOfMessage.messageId,
                attemptDateTime = now,
                result = if (sendingResultOfMessage.sentMessageId == null) {
                    CaregivingStartMessageSendingHistory.SendingResult.FAILED
                } else {
                    CaregivingStartMessageSendingHistory.SendingResult.SENT
                },
                messageId = sendingResultOfMessage.sentMessageId,
            )
        )
    }

    private fun updateSummary(sendingResultOfMessage: SendingResultOfMessage, now: LocalDateTime) {
        caregivingStartMessageSummaryRepository.findByReceptionId(sendingResultOfMessage.messageId).forEach {
            if (sendingResultOfMessage.sentMessageId == null) {
                it.updateSendingResult(
                    SendingStatus.FAILED,
                    null
                )
            } else {
                it.updateSendingResult(
                    SendingStatus.SENT,
                    now,
                )
            }
        }
    }
}
