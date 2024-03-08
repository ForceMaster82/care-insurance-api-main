package kr.caredoc.careinsurance.web.reception

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.reception.modification.CaregivingRoundModificationSummaryByReceptionIdQueryHandler
import kr.caredoc.careinsurance.relaxedMock
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@CareInsuranceWebMvcTest(CaregivingRoundModificationController::class)
class CaregivingRoundModificationControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean
    private val caregivingRoundModificationSummaryByReceptionIdQueryHandler: CaregivingRoundModificationSummaryByReceptionIdQueryHandler,
) : ShouldSpec({
    context("간병 접수의 간병비 산정 수정 개요를 조회하면") {
        val request = get("/api/v1/receptions/01GPJMK7ZPBBKTY3TP0NN5JWCJ/caregiving-round-modification")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                caregivingRoundModificationSummaryByReceptionIdQueryHandler.getCaregivingRoundModificationSummary(
                    match { it.receptionId == "01GPJMK7ZPBBKTY3TP0NN5JWCJ" }
                )
            } returns relaxedMock {
                every { lastModifiedDateTime } returns LocalDateTime.of(2022, 10, 1, 17, 51, 22)
                every { lastModifierId } returns "01GNXBZAQ0J9J8DD8K461RDF26"
                every { modificationCount } returns 25
            }
        }

        afterEach { clearAllMocks() }

        should("200 Ok로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("간병 접수의 간병 회차 수정 개요를 페이로드에 포함하여 응답합니다.") {
            expectResponse(
                content().json(
                    """
                        {
                          "lastModifiedDateTime": "2022-10-01T08:51:22Z",
                          "lastModifierId": "01GNXBZAQ0J9J8DD8K461RDF26",
                          "modificationCount": 25
                        }
                    """.trimIndent()
                )
            )
        }

        should("수정 개요 조회를 도메인 영역에 위임합니다.") {
            mockMvc.perform(request)

            verify {
                caregivingRoundModificationSummaryByReceptionIdQueryHandler.getCaregivingRoundModificationSummary(
                    withArg {
                        it.receptionId shouldBe "01GPJMK7ZPBBKTY3TP0NN5JWCJ"
                    }
                )
            }
        }
    }
})
