package kr.caredoc.careinsurance.bizcallapi

import kr.caredoc.careinsurance.bizcall.BizcallBadRequestException
import kr.caredoc.careinsurance.bizcall.BizcallErrorResult
import kr.caredoc.careinsurance.bizcall.BizcallRecipient
import kr.caredoc.careinsurance.bizcall.BizcallRecipientResult
import kr.caredoc.careinsurance.bizcall.BizcallReservationRequest
import kr.caredoc.careinsurance.bizcall.BizcallReservationResult
import kr.caredoc.careinsurance.bizcall.BizcallSender
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

class BizcallApiService(
    private val client: WebClient
) : BizcallSender {
    override fun reserve(bizCallReservationRequest: BizcallReservationRequest): BizcallReservationResult {
        return client.post()
            .uri("/reservation")
            .bodyValue(bizCallReservationRequest)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono<BizcallReservationResult>()
            .onErrorResume(WebClientResponseException::class.java) {
                val result = it.getResponseBodyAs(BizcallErrorResult::class.java)
                throw BizcallBadRequestException(result)
            }
            .block()!!
    }

    override fun additionalRecipientByReservation(bizcallId: String, bizcallRecipientList: List<BizcallRecipient>): BizcallRecipientResult? {
        return client.post()
            .uri("/reservations/$bizcallId/recipient")
            .bodyValue(bizcallRecipientList)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono<BizcallRecipientResult>()
            .block()
    }
}
