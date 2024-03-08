package kr.caredoc.careinsurance.web.caregiving

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainAll
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.caregiving.exception.ReferenceCaregivingRoundNotExistException
import kr.caredoc.careinsurance.caregiving.progressmessage.CaregivingProgressMessageSendingCommandHandler
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@CareInsuranceWebMvcTest(CaregivingProgressMessageController::class)
class CaregivingProgressMessageControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean(relaxed = true)
    private val caregivingProgressMessageSendingCommandHandler: CaregivingProgressMessageSendingCommandHandler,
) : ShouldSpec({
    context("간병 진행 메시지 발송을 요청하면") {
        val request = post("/api/v1/caregiving-progress-messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    [
                     {
                        "caregivingRoundId": "01H04CNJ25N530WHFCH8WH56QV"
                     },
                     {
                        "caregivingRoundId": "01H04CNZCSSK17KQ304DPHJM5M"
                     },
                     {
                        "caregivingRoundId": "01H04CPCC6GE09A8QD9FHZQ15Y"
                     }
                    ]
                """.trimIndent()
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        should("204 No Content로 응답합니다.") {
            expectResponse(status().isNoContent())
        }

        should("내용 없는 페이로드로 응답합니다.") {
            expectResponse(content().string(""))
        }

        should("간병 진행 메시지 발송을 도메인 영역에 위임합니다.") {
            mockMvc.perform(request)

            verify {
                caregivingProgressMessageSendingCommandHandler.sendCaregivingProgressMessages(
                    withArg {
                        it.targetCaregivingRoundIds shouldContainAll setOf(
                            "01H04CNJ25N530WHFCH8WH56QV",
                            "01H04CNZCSSK17KQ304DPHJM5M",
                            "01H04CPCC6GE09A8QD9FHZQ15Y",
                        )
                    }
                )
            }
        }

        context("간병 진행 메시지를 발송할 간병 회차를 찾을 수 없다면") {
            beforeEach {
                every {
                    caregivingProgressMessageSendingCommandHandler.sendCaregivingProgressMessages(
                        match { it.targetCaregivingRoundIds.contains("01H04CNJ25N530WHFCH8WH56QV") }
                    )
                } throws ReferenceCaregivingRoundNotExistException(
                    referenceCaregivingRoundId = "01H04CNJ25N530WHFCH8WH56QV",
                )
            }

            afterEach { clearAllMocks() }

            should("422 Unprocessable Entity로 응답합니다.") {
                expectResponse(status().isUnprocessableEntity())
            }

            should("에러 메시지와 에러 타입과 에러 데이터를 페이로드에 포함하여 응답합니다.") {
                expectResponse(
                    content().json(
                        """
                            {
                              "message": "조회하고자 하는 간병 정보가 존재하지 않습니다.",
                              "errorType": "REFERENCE_CAREGIVING_ROUND_NOT_EXISTS",
                              "data": {
                                "enteredCaregivingRoundId" : "01H04CNJ25N530WHFCH8WH56QV"
                              }
                            }
                        """.trimIndent()
                    )
                )
            }
        }
    }
})
