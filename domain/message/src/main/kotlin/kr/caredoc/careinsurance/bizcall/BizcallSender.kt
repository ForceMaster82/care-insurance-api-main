package kr.caredoc.careinsurance.bizcall

interface BizcallSender {
    fun reserve(bizCallReservationRequest: BizcallReservationRequest): BizcallReservationResult
    fun additionalRecipientByReservation(bizcallId: String, bizcallRecipientList: List<BizcallRecipient>): BizcallRecipientResult?
}
