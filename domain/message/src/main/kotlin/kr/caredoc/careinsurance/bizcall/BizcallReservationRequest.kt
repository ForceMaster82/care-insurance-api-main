package kr.caredoc.careinsurance.bizcall

data class BizcallReservationRequest(
    val scenarioId: String,
    val originalMdn: String,
    val changedMdn: String?,
    val priority: Int,
    val voiceSpeed: Int,
    val bizcallName: String,
    val voice: Voice,
    val reservationInfo: ReservationInfo,
    val retry: RetryInfo
) {
    data class ReservationInfo(
        val reservationDateTime: String,
    )

    data class RetryInfo(
        val retryCount: Int,
        val retryInterval: Int,
        val replacedRetry: Boolean,
    )
}
