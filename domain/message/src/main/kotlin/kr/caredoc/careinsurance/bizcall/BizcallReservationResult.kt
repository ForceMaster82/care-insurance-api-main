package kr.caredoc.careinsurance.bizcall

import java.time.LocalDateTime

data class BizcallReservationResult(
    val bizcallId: String,
    val scenarioId: String,
    val scenarioName: String,
    val priority: Int,
    val originalMdn: String,
    val changedMdn: String?,
    val voice: Voice,
    val voiceSpeed: Int,
    val bizcallName: String,
    val reservationInfo: ReservationInfo,
    val callStatus: CallStatus,
    val retry: RetryInfo,
    val totalRecipientCount: Int,
    val completionRecipientCount: Int,
    val createdDateTime: LocalDateTime,
    val modifiedDateTime: LocalDateTime,
) {
    data class ReservationInfo(
        val reservationDateTime: LocalDateTime,
        val reservationSequence: Int,
    )
    data class RetryInfo(
        val retryCount: Int,
        val retryInterval: Int,
        val replacedRetry: Boolean,
    )
}
