package kr.caredoc.careinsurance.web.user

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.patch.Patches
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.user.ExternalCaregivingManager
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerByIdQueryHandler
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerCreationCommandHandler
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerEditCommandHandler
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerNotExistsException
import kr.caredoc.careinsurance.user.ExternalCaregivingManagersByFilterQueryHandler
import kr.caredoc.careinsurance.user.User
import kr.caredoc.careinsurance.user.UserByIdQueryHandler
import kr.caredoc.careinsurance.user.UsersByIdsQueryHandler
import kr.caredoc.careinsurance.user.exception.AlreadyExistsUserEmailAddressException
import kr.caredoc.careinsurance.user.exception.ReferenceExternalCaregivingManagerNotExistsException
import kr.caredoc.careinsurance.web.agency.ExternalCaregivingManagerController
import kr.caredoc.careinsurance.web.search.IllegalSearchQueryException
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@CareInsuranceWebMvcTest(ExternalCaregivingManagerController::class)
class ExternalCaregivingManagerControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean(relaxed = true)
    private val externalCaregivingManagerCreationCommandHandler: ExternalCaregivingManagerCreationCommandHandler,
    @MockkBean
    private val userByIdQueryHandler: UserByIdQueryHandler,
    @MockkBean(relaxed = true)
    private val externalCaregivingManagerByIdQueryHandler: ExternalCaregivingManagerByIdQueryHandler,
    @MockkBean(relaxed = true)
    private val externalCaregivingManagerEditCommandHandler: ExternalCaregivingManagerEditCommandHandler,
    @MockkBean(relaxed = true)
    private val externalCaregivingManagerByFilterQueryHandler: ExternalCaregivingManagersByFilterQueryHandler,
    @MockkBean(relaxed = true)
    private val usersByIdsQueryHandler: UsersByIdsQueryHandler,
) : ShouldSpec({
    context("when create external caregiving manager") {
        val request = post("/api/v1/external-caregiving-managers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                            {
                              "externalCaregivingOrganizationId": "01GRNV2B26BY95M870RDBM0BJZ",
                              "email": "eddy@caredoc.kr",
                              "name": "에디",
                              "phoneNumber": "01012345678",
                              "remarks": "꼼꼼히 봐주세요"
                            }
                """.trimIndent()
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                externalCaregivingManagerCreationCommandHandler.createExternalCaregivingManager(any())
            } returns relaxedMock {
                every { externalCaregivingManagerId } returns "01GRTMM3VC03NXFJYWSQ2MCW2X"
            }
        }

        afterEach { clearAllMocks() }

        should("The status of the API response is Created.") {
            expectResponse(status().isCreated)
        }

        should("The id of the external caregiving manager is included in the response result header.") {
            expectResponse(
                header().string(
                    "location",
                    "http://localhost/api/v1/external-caregiving-managers/01GRTMM3VC03NXFJYWSQ2MCW2X"
                )
            )
        }

        context("but existed email") {
            beforeEach {
                every {
                    externalCaregivingManagerCreationCommandHandler.createExternalCaregivingManager(any())
                } throws AlreadyExistsUserEmailAddressException("eddy@caredoc.kr")
            }

            afterEach { clearAllMocks() }

            should("The status of the API response is BadRequest.") {
                expectResponse(status().isConflict)
            }

            should("There is an error message and type in the response result.") {
                expectResponse(
                    content().json(
                        """
                        {
                          "message": "입력한 이메일은 이미 존재하는 이메일입니다.",
                          "errorType": "ALREADY_EXISTS_EXTERNAL_CAREGIVING_MANGER_EMAIL"
                        }
                        """.trimIndent()
                    )
                )
            }

            should("There is an enteredEmail in data the response result") {
                expectResponse(
                    content().json(
                        """
                        {
                          "data": {
                            "enteredEmail": "eddy@caredoc.kr"
                          }
                        }
                        """.trimIndent()
                    )
                )
            }
        }
    }

    context("입력된 external caregiving manager 를 상세 조회를 합니다.") {
        val externalCaregivingManagerId = "01GSSCV24S0TRT3RN841MPZPY2"
        val request = get("/api/v1/external-caregiving-managers/$externalCaregivingManagerId")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every { externalCaregivingManagerByIdQueryHandler.getExternalCaregivingManager(any()) } returns relaxedMock {
                every { id } returns externalCaregivingManagerId
                every { email } returns "eddy@caredoc.kr"
                every { name } returns "eddy"
                every { phoneNumber } returns "01012345678"
                every { remarks } returns null
                every { userId } returns "01GSSEJWTVD1NX36H7V3AKEZ29"
                every { externalCaregivingOrganizationId } returns "01GSSEE5VNCRAP63TW807M0NAE"
            }

            every { userByIdQueryHandler.getUser(match { it.userId == "01GSSEJWTVD1NX36H7V3AKEZ29" }) } returns relaxedMock {
                every { id } returns "01GSSEJWTVD1NX36H7V3AKEZ29"
                every { lastLoginDateTime } returns LocalDateTime.of(2023, 2, 21, 15, 11, 30)
                every { suspended } returns false
            }
        }

        afterEach { clearAllMocks() }

        should("http status 응답은 200 입니다.") {
            expectResponse(status().isOk)
        }

        should("응답 본문은 조회하고자 하는 external caregiving manager의 상세 정보를 포함해야합니다.") {
            expectResponse(
                content().json(
                    """
                                {
                                "id": "01GSSCV24S0TRT3RN841MPZPY2", 
                                "email": "eddy@caredoc.kr", 
                                "name": "eddy", 
                                "phoneNumber": "01012345678", 
                                "remarks": null,
                                "lastLoginDateTime": "2023-02-21T06:11:30Z",
                                "suspended": false, 
                                "externalCaregivingOrganizationId": "01GSSEE5VNCRAP63TW807M0NAE"
                                }                            
                    """.trimIndent()
                )
            )
        }

        context("입력 된 external caregiving manager 가 존재하지 않습니다.") {
            beforeEach {
                every {
                    externalCaregivingManagerByIdQueryHandler.getExternalCaregivingManager(any())
                } throws ExternalCaregivingManagerNotExistsException(externalCaregivingManagerId)
            }

            afterEach { clearAllMocks() }

            should("http status 응답은 404 입니다.") {
                expectResponse(status().isNotFound)
            }

            should("응답 본문에 error message 와 errorType 정보를 포함해야합니다.") {
                expectResponse(
                    content().json(
                        """
                                    {
                                      "message": "조회하고자 하는 외부 제휴사(협회) 계정이 존재하지 않습니다.",
                                      "errorType": "EXTERNAL_CAREGIVING_MANAGER_NOT_EXISTS"
                                    }
                        """.trimIndent()
                    )
                )
            }

            should("응답 본문 data에 입력한 external caregiving manager id를 포함해야합니다.") {
                expectResponse(
                    content().json(
                        """
                                    {
                                      "data":{
                                        "enteredExternalCaregivingManagerId": "01GSSCV24S0TRT3RN841MPZPY2"
                                      }
                                    }
                        """.trimIndent()
                    )
                )
            }
        }
    }

    context("입력된 external caregiving manager 정보를 수정합니다.") {
        val externalCaregivingManagerId = "01GT3J675C6Y93KN7X6NRNS60P"
        val request = put("/api/v1/external-caregiving-managers/$externalCaregivingManagerId")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                            {
                              "email": "eddy@caredoc.kr",
                              "name": "eddy",
                              "phoneNumber": "01012345678",
                              "remarks": "수정 해주세요",
                              "suspended": false,
                              "externalCaregivingOrganizationId": "01GRNV2B26BY95M870RDBM0BJZ"
                            }
                """.trimIndent()
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        should("응답 결과로 http status는 204를 나타냅니다.") {
            expectResponse(status().isNoContent)
        }

        should("external caregiving manager를 수정하기 위해서 external caregivig manager handler를 사용해야 합니다.") {
            mockMvc.perform(request)

            verify {
                externalCaregivingManagerEditCommandHandler.editExternalCaregivingManager(
                    withArg {
                        it.externalCaregivingManagerId shouldBe externalCaregivingManagerId
                    },
                    withArg {
                        it.email shouldBe Patches.ofValue("eddy@caredoc.kr")
                        it.name shouldBe Patches.ofValue("eddy")
                        it.phoneNumber shouldBe Patches.ofValue("01012345678")
                        it.remarks shouldBe Patches.ofValue("수정 해주세요")
                        it.suspended shouldBe Patches.ofValue(false)
                        it.externalCaregivingOrganizationId shouldBe Patches.ofValue("01GRNV2B26BY95M870RDBM0BJZ")
                    }
                )
            }
        }

        context("입력 된 external caregiving manager 가 존재하지 않습니다.") {
            beforeEach {
                every {
                    externalCaregivingManagerEditCommandHandler.editExternalCaregivingManager(any(), any())
                } throws ExternalCaregivingManagerNotExistsException(externalCaregivingManagerId)
            }

            afterEach { clearAllMocks() }

            should("http status 응답은 404 입니다.") {
                mockMvc.perform(request)

                expectResponse(status().isNotFound)
            }

            should("응답 본문에 error message 와 errorType 정보를 포함해야합니다.") {
                expectResponse(
                    content().json(
                        """
                                    {
                                      "message": "조회하고자 하는 외부 제휴사(협회) 계정이 존재하지 않습니다.",
                                      "errorType": "EXTERNAL_CAREGIVING_MANAGER_NOT_EXISTS"
                                    }
                        """.trimIndent()
                    )
                )
            }

            should("응답 본문 data에 입력한 external caregiving manager id를 포함해야합니다.") {
                expectResponse(
                    content().json(
                        """
                                    {
                                      "data":{
                                        "enteredExternalCaregivingManagerId": "01GT3J675C6Y93KN7X6NRNS60P"
                                      }
                                    }
                        """.trimIndent()
                    )
                )
            }
        }

        context("만약 입력한 이메일이 다른 사용자에 의해 선점되어 있다면") {
            beforeEach {
                every {
                    externalCaregivingManagerEditCommandHandler.editExternalCaregivingManager(any(), any())
                } throws AlreadyExistsUserEmailAddressException("eddy@caredoc.kr")
            }

            afterEach { clearAllMocks() }

            should("409 Conflict로 응답합니다.") {
                expectResponse(status().isConflict)
            }

            should("에러 메시지와 타입, 데이터를 페이로드에 포함하여 응답합니다.") {
                expectResponse(
                    content().json(
                        """
                        {
                          "message": "입력한 이메일은 이미 존재하는 이메일입니다.",
                          "errorType": "ALREADY_EXISTS_EXTERNAL_CAREGIVING_MANGER_EMAIL",
                          "data": {
                            "enteredEmail": "eddy@caredoc.kr"
                          }
                        }
                        """.trimIndent()
                    )
                )
            }
        }
    }

    context("요청받은 external caregiving manager list 사용 여부를 수정합니다.") {
        val request = patch("/api/v1/external-caregiving-managers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    [
                      {
                        "id": "01GTK2HQ7M8SGG9CQWDX76DYYB",
                        "suspended": false
                      },
                      {
                        "id": "01GTK2MP02Y6M5EEP2C3S6JZ61",
                        "suspended": true 
                      },
                      {
                        "id": "01GTK2MW6WX69MY7K83SBBWP99",
                        "suspended": false
                      },
                      {
                        "id": "01GTK2NG4NVGWQ5DGN4KR5VP55",
                        "suspended": false
                      }
                    ]
                """.trimIndent()
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        should("응답 결과로 http status는 204를 나타냅니다.") {
            expectResponse(status().isNoContent)
        }
        should("요청받은 외부 제휴사(협회) 계정 list 의 사용 여부를 수정 요청합니다.") {
            mockMvc.perform(request)

            verify {
                externalCaregivingManagerEditCommandHandler.editExternalCaregivingManagers(
                    withArg {
                        it["01GTK2HQ7M8SGG9CQWDX76DYYB"]?.suspended shouldBe Patches.ofValue(false)
                        it["01GTK2MP02Y6M5EEP2C3S6JZ61"]?.suspended shouldBe Patches.ofValue(true)
                        it["01GTK2MW6WX69MY7K83SBBWP99"]?.suspended shouldBe Patches.ofValue(false)
                        it["01GTK2NG4NVGWQ5DGN4KR5VP55"]?.suspended shouldBe Patches.ofValue(false)
                    }
                )
            }
        }

        context("요청받은 외부 제휴사(협회) 계정 list 중 참조할 수 없는 외부 제휴사(협회) 계정이 있습니다. ") {

            beforeEach {
                every {
                    externalCaregivingManagerEditCommandHandler.editExternalCaregivingManagers(any())
                } throws ReferenceExternalCaregivingManagerNotExistsException("01GTK2NG4NVGWQ5DGN4KR5VP55")
            }

            afterEach { clearAllMocks() }

            should("응답 결과로 http status는 422를 나타냅니다.") {
                expectResponse(status().isUnprocessableEntity)
            }

            should("응답 본문에 error message 와 errorType 정보를 포함해야합니다.") {
                expectResponse(
                    content().json(
                        """
                                    {
                                      "message": "요청에 포함된 외부 제휴사(협회) 계정이 존재하지 않습니다.",
                                      "errorType": "REFERENCE_EXTERNAL_CAREGIVING_MANAGER_NOT_EXISTS"
                                    }
                        """.trimIndent()
                    )
                )
            }

            should("응답 본문 data에 참조에 실패한 external caregiving manager id를 포함해야합니다.") {
                expectResponse(
                    content().json(
                        """
                                    {
                                      "data":{
                                        "enteredExternalCaregivingManagerId": "01GTK2NG4NVGWQ5DGN4KR5VP55"
                                      }
                                    }
                        """.trimIndent()
                    )
                )
            }
        }
    }

    context("external caregiving manager 목록을 조회 합니다.") {
        val request = get("/api/v1/external-caregiving-managers")
            .queryParam("page-number", "1")
            .queryParam("page-size", "5")
            .queryParam("external-caregiving-organization-id", "01GVAG2D50Q7ZV78SBZ4QEBQWV")
            .queryParam("query", "email:eddy@caredoc.kr")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                externalCaregivingManagerByFilterQueryHandler.getExternalCaregivingManagers(
                    any(),
                    any()
                )
            } returns generateExternalCaregivingManagers()

            every { usersByIdsQueryHandler.getUsers(any()) } returns generateUsers()
        }

        afterEach { clearAllMocks() }

        should("http status 응답은 200 입니다.") {
            expectResponse(status().isOk)
        }
        should("응답에는 pagination 에 대한 내용이 있습니다.") {
            expectResponse(
                content().json(
                    """
                                {
                                   "currentPageNumber": 1,
                                   "lastPageNumber": 2,
                                   "totalItemCount": 4
                                }
                    """.trimIndent()
                )
            )
        }
        should("응답에는 external caregiving manager 목록 조회에 대한 결과가 있습니다.") {
            expectResponse(
                content().json(generateResponseContent())
            )
        }
    }

    context("조건 없이 external caregiving manager 목록을 조회 합니다.") {
        val request = get("/api/v1/external-caregiving-managers")
            .queryParam("page-number", "1")
            .queryParam("page-size", "5")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                externalCaregivingManagerByFilterQueryHandler.getExternalCaregivingManagers(
                    any(),
                    any()
                )
            } returns generateExternalCaregivingManagers()

            every { usersByIdsQueryHandler.getUsers(any()) } returns generateUsers()
        }

        afterEach { clearAllMocks() }

        should("http status 응답은 200 입니다.") {
            expectResponse(status().isOk)
        }
        should("응답에는 pagination 에 대한 내용이 있습니다.") {
            expectResponse(
                content().json(
                    """
                                {
                                   "currentPageNumber": 1,
                                   "lastPageNumber": 2,
                                   "totalItemCount": 4
                                }
                    """.trimIndent()
                )
            )
        }
        should("응답에는 external caregiving manager 목록 조회에 대한 결과가 있습니다.") {
            expectResponse(
                content().json(generateResponseContent())
            )
        }
    }

    context("query 가 빈 공백인 조건으로 external caregiving manager 목록을 조회 합니다.") {
        val request = get("/api/v1/external-caregiving-managers")
            .queryParam("page-number", "1")
            .queryParam("page-size", "5")
            .queryParam("query", "")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                externalCaregivingManagerByFilterQueryHandler.getExternalCaregivingManagers(
                    any(),
                    any()
                )
            } throws IllegalSearchQueryException("")
        }

        afterEach { clearAllMocks() }

        should("http status 응답은 400 입니다.") {
            expectResponse(status().isBadRequest)
        }
        should("응답에는 message 와 errorType 을 포함해야 합니다.") {
            expectResponse(
                content().json(
                    """
                                {
                                   "message": "해석할 수 없는 검색 조건입니다.",
                                   "errorType" : "ILLEGAL_SEARCH_QUERY"
                                }
                    """.trimIndent()
                )
            )
        }
    }
})

private fun generateExternalCaregivingManagers() = PageImpl(
    listOf(
        relaxedMock<ExternalCaregivingManager> {
            every { id } returns "01GV2H1FQ65N7H0YRFJFY05P25"
            every { externalCaregivingOrganizationId } returns "01GV2SDATEJ54VJPE9RA0RHQQF"
            every { email } returns "eddy@caredoc.kr"
            every { name } returns "eddy"
            every { userId } returns "01GSSEJWTVD1NX36H7V3AKEZ29"
        },
        relaxedMock<ExternalCaregivingManager> {
            every { id } returns "01GVACQVHXMRAWQZPF990NCXYZ"
            every { externalCaregivingOrganizationId } returns "01GVACRBET4M3AMJC2KVAGMXVB"
            every { email } returns "jerry@caredoc.kr"
            every { name } returns "jerry"
            every { userId } returns "01GVAK21VN9YK3YNVF8XNFRR99"
        },
        relaxedMock<ExternalCaregivingManager> {
            every { id } returns "01GVAG55N6W43YVYJBVYNT4JFB"
            every { externalCaregivingOrganizationId } returns "01GVAG5MMHHCDQTVAGWKGVGVHY"
            every { email } returns "rena@caredoc.kr"
            every { name } returns "rena"
            every { userId } returns "01GVAK5Y253K0K53FDV18C2VFW"
        },
        relaxedMock<ExternalCaregivingManager> {
            every { id } returns "01GVAG5EYJYT76J7Y7XCZA18Q1"
            every { externalCaregivingOrganizationId } returns "01GVAG65G7HQ37CFG38WWHPK8V"
            every { email } returns "boris@caredoc.kr"
            every { name } returns "boris"
            every { userId } returns "01GVAK7NJCTYDVA7VD4B71DXHP"
        },
    ),
    Pageable.ofSize(2),
    4
)

private fun generateUsers() = listOf<User>(
    relaxedMock {
        every { id } returns "01GSSEJWTVD1NX36H7V3AKEZ29"
        every { lastLoginDateTime } returns LocalDateTime.of(2023, 3, 9, 18, 0, 0)
        every { suspended } returns false
    },
    relaxedMock {
        every { id } returns "01GVAK21VN9YK3YNVF8XNFRR99"
        every { lastLoginDateTime } returns LocalDateTime.of(2023, 3, 11, 18, 0, 0)
        every { suspended } returns false
    },
    relaxedMock {
        every { id } returns "01GVAK5Y253K0K53FDV18C2VFW"
        every { lastLoginDateTime } returns LocalDateTime.of(2023, 3, 9, 18, 0, 0)
        every { suspended } returns false
    },
    relaxedMock {
        every { id } returns "01GVAK7NJCTYDVA7VD4B71DXHP"
        every { lastLoginDateTime } returns LocalDateTime.of(2023, 3, 11, 18, 0, 0)
        every { suspended } returns false
    },
)

private fun generateResponseContent() =
    """
        {
            "items": [                              
                {
                   "id": "01GV2H1FQ65N7H0YRFJFY05P25",
                   "externalCaregivingOrganizationId": "01GV2SDATEJ54VJPE9RA0RHQQF",
                   "email": "eddy@caredoc.kr",
                   "name": "eddy",
                   "lastLoginDateTime": "2023-03-09T09:00:00Z", 
                   "suspended": false
                },
                {
                   "id": "01GVACQVHXMRAWQZPF990NCXYZ",
                   "externalCaregivingOrganizationId": "01GVACRBET4M3AMJC2KVAGMXVB",
                   "email": "jerry@caredoc.kr",
                   "name": "jerry",
                   "lastLoginDateTime": "2023-03-11T09:00:00Z", 
                   "suspended": false
                },
                {
                   "id": "01GVAG55N6W43YVYJBVYNT4JFB",
                   "externalCaregivingOrganizationId": "01GVAG5MMHHCDQTVAGWKGVGVHY",
                   "email": "rena@caredoc.kr",
                   "name": "rena",
                   "lastLoginDateTime": "2023-03-09T09:00:00Z", 
                   "suspended": false
                },
                {
                   "id": "01GVAG5EYJYT76J7Y7XCZA18Q1",
                   "externalCaregivingOrganizationId": "01GVAG65G7HQ37CFG38WWHPK8V",
                   "email": "boris@caredoc.kr",
                   "name": "boris",
                   "lastLoginDateTime": "2023-03-11T09:00:00Z", 
                   "suspended": false
                }
            ]
        }
    """.trimIndent()
