package kr.caredoc.careinsurance.web.reception

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainAll
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.reception.ReferenceReceptionNotExistsException
import kr.caredoc.careinsurance.reception.caregivingstartmessage.CaregivingStartMessageSendingCommandHandler
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@CareInsuranceWebMvcTest(CaregivingStartMessageController::class)
class CaregivingStartMessageControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean(relaxed = true)
    private val caregivingStartMessageSendingCommandHandler: CaregivingStartMessageSendingCommandHandler,
) : ShouldSpec({
    context("간병 시작 메시지 발송을 요청하면") {
        val request = post("/api/v1/caregiving-start-messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    [
                      {
                        "receptionId": "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                      },
                      {
                        "receptionId": "01GZGGR525R36YJ3S3XN4VJFJV"
                      },
                      {
                        "receptionId": "01GZGGR525455DS5GHV6TF2QZ8"
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

        should("간병 시작 메시지 발송을 도메인 영역에 위임합니다.") {
            mockMvc.perform(request)

            verify {
                caregivingStartMessageSendingCommandHandler.sendCaregivingStartMessage(
                    withArg {
                        it.targetReceptionIds shouldContainAll setOf(
                            "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
                            "01GZGGR525R36YJ3S3XN4VJFJV",
                            "01GZGGR525455DS5GHV6TF2QZ8",
                        )
                    }
                )
            }
        }

        context("간병 시작 메시지를 발송할 간병 접수를 찾을 수 없다면") {
            beforeEach {
                every {
                    caregivingStartMessageSendingCommandHandler.sendCaregivingStartMessage(
                        match { it.targetReceptionIds.contains("01GPJMK7ZPBBKTY3TP0NN5JWCJ") }
                    )
                } throws ReferenceReceptionNotExistsException(
                    referenceReceptionId = "01GPJMK7ZPBBKTY3TP0NN5JWCJ",
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
                              "message": "요청에 포함된 간병 접수가 존재하지 않습니다.",
                              "errorType": "REFERENCE_RECEPTION_NOT_EXISTS",
                              "data": {
                                "enteredReceptionId": "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                              }
                            }
                        """.trimIndent()
                    )
                )
            }
        }
    }
})
