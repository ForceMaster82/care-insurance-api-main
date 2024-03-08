package kr.caredoc.careinsurance.externalapi

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import kr.caredoc.careinsurance.alimtalk.AlimtalkSender
import kr.caredoc.careinsurance.alimtalk.BulkAlimtalkMessage
import kr.caredoc.careinsurance.alimtalk.SendingResultOfMessage
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux

class ExternalApiAlimtalkService(private val client: WebClient) : AlimtalkSender {
    override fun send(message: BulkAlimtalkMessage): List<SendingResultOfMessage> {
        return runBlocking {
            Flux.merge(
                message.messageParameters.map { messageParameter ->
                    client.post().uri("/api/alim-talk/send").bodyValue(
                        AlimtalkSendRequest(
                            receivePhoneNumber = messageParameter.recipient,
                            templateCode = message.templateCode,
                            messageReplacements = messageParameter.templateData.map { it.intoReplacement() },
                            buttonReplacements = listOf(),
                        )
                    ).retrieve().bodyToMono<AlimtalkSendResponse>().map {
                        MessageSendingResultSet(
                            messageParameter.id,
                            it,
                        )
                    }
                }
            ).asFlow().map {
                SendingResultOfMessage(
                    messageId = it.messageId,
                    sentMessageId = it.response.data.trackingId.toString()
                )
            }.toList()
        }
    }

    private fun Pair<String, String>.intoReplacement() = AlimtalkSendRequest.Replacement(
        this.first,
        this.second,
    )

    private data class MessageSendingResultSet(val messageId: String, val response: AlimtalkSendResponse)
}
