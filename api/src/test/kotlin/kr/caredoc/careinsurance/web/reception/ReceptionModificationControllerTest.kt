package kr.caredoc.careinsurance.web.reception

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.reception.history.ReceptionModificationSummaryByReceptionIdQueryHandler
import kr.caredoc.careinsurance.relaxedMock
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@CareInsuranceWebMvcTest(ReceptionModificationController::class)
class ReceptionModificationControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean
    private val receptionModificationSummaryByReceptionIdQueryHandler: ReceptionModificationSummaryByReceptionIdQueryHandler,
) : ShouldSpec({
    context("접수 수정 개요를 조회하면") {
        val request = get("/api/v1/receptions/01GZDRCBK6XQ1TNN68DPKZPS3Q/reception-modification")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                receptionModificationSummaryByReceptionIdQueryHandler.getReceptionModificationSummary(
                    match { it.receptionId == "01GZDRCBK6XQ1TNN68DPKZPS3Q" }
                )
            } returns relaxedMock {
                every { lastModifiedDateTime } returns LocalDateTime.of(2023, 5, 2, 14, 3, 22)
                every { lastModifierId } returns "01GZDD73FT2RTTW5N6BC15622A"
                every { modificationCount } returns 5
            }
        }

        afterEach { clearAllMocks() }

        should("200 Ok로 응답합니다.") {
            expectResponse(status().isOk)
        }

        should("접수 수정 개요를 페이로드에 포함하여 반환합니다.") {
            expectResponse(
                content().json(
                    """
                       {
                         "lastModifiedDateTime": "2023-05-02T05:03:22Z",
                         "lastModifierId": "01GZDD73FT2RTTW5N6BC15622A",
                         "modificationCount": 5
                       } 
                    """.trimIndent()
                )
            )
        }

        should("접수 수정 개요 조회를 도메인 영역으로 위임합니다.") {
            mockMvc.perform(request)

            verify {
                receptionModificationSummaryByReceptionIdQueryHandler.getReceptionModificationSummary(
                    withArg {
                        it.receptionId shouldBe "01GZDRCBK6XQ1TNN68DPKZPS3Q"
                    }
                )
            }
        }
    }
})
