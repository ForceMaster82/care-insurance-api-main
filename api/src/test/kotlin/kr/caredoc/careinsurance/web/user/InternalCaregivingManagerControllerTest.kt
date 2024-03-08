package kr.caredoc.careinsurance.web.user

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.ShouldSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.slot
import kr.caredoc.careinsurance.CareInsuranceWebMvcTest
import kr.caredoc.careinsurance.ResponseMatcher
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.user.AllInternalCaregivingManagersQueryHandler
import kr.caredoc.careinsurance.user.InternalCaregivingManagerByIdQueryHandler
import kr.caredoc.careinsurance.user.InternalCaregivingManagerCreationCommandHandler
import kr.caredoc.careinsurance.user.InternalCaregivingManagerCreationResult
import kr.caredoc.careinsurance.user.InternalCaregivingManagerEditingCommandHandler
import kr.caredoc.careinsurance.user.InternalCaregivingManagersBySearchConditionQuery
import kr.caredoc.careinsurance.user.InternalCaregivingManagersBySearchConditionQueryHandler
import kr.caredoc.careinsurance.user.UserByIdQueryHandler
import kr.caredoc.careinsurance.user.UsersByIdsQueryHandler
import kr.caredoc.careinsurance.user.exception.AlreadyExistsUserEmailAddressException
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@CareInsuranceWebMvcTest(InternalCaregivingManagerController::class)
class InternalCaregivingManagerControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean(relaxed = true)
    private val internalCaregivingManagerCreationCommandHandler: InternalCaregivingManagerCreationCommandHandler,
    @MockkBean(relaxed = true)
    private val allInternalCaregivingManagersQueryHandler: AllInternalCaregivingManagersQueryHandler,
    @MockkBean(relaxed = true)
    private val internalCaregivingManagersBySearchConditionQueryHandler: InternalCaregivingManagersBySearchConditionQueryHandler,
    @MockkBean(relaxed = true)
    private val usersByIdsQueryHandler: UsersByIdsQueryHandler,
    @MockkBean(relaxed = true)
    private val internalCaregivingManagerByIdQueryHandler: InternalCaregivingManagerByIdQueryHandler,
    @MockkBean(relaxed = true)
    private val userByIdQueryHandler: UserByIdQueryHandler,
    @MockkBean(relaxed = true)
    private val internalCaregivingManagerEditingCommandHandler: InternalCaregivingManagerEditingCommandHandler,
) : ShouldSpec({
    context("POST /api/v1/internal-caregiving-managers") {
        val request = post("/api/v1/internal-caregiving-managers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                       "email": "my_bang@caredoc.kr",
                       "name": "방미영",
                       "nickname": "Rena",
                       "phoneNumber": "010-4026-1111",
                       "role": "케어닥 백엔드 개발자",
                       "remarks": "메모입니다."
                    }
                """.trimIndent()
            )
        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                internalCaregivingManagerCreationCommandHandler.createInternalCaregivingManager(
                    match {
                        listOf(
                            it.email == "my_bang@caredoc.kr",
                            it.name == "방미영",
                            it.nickname == "Rena",
                            it.phoneNumber == "010-4026-1111",
                            it.role == "케어닥 백엔드 개발자",
                            it.remarks == "메모입니다.",
                        ).all { predicate -> predicate }
                    }
                )
            } returns InternalCaregivingManagerCreationResult("01GPJT9CN73GMCBMV9RFCZ4KXY")
        }

        afterEach { clearAllMocks() }

        should("response status should be 201 Created") {
            expectResponse(status().isCreated)
        }

        should("response payload should be empty") {
            expectResponse(content().string(""))
        }
    }

    context("POST /api/v1/internal-caregiving-managers remarks is null") {
        val request = post("/api/v1/internal-caregiving-managers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                       "email": "my_bang@caredoc.kr",
                       "name": "방미영",
                       "nickname": "Rena",
                       "phoneNumber": "010-4026-1111",
                       "role": "케어닥 백엔드 개발자"
                    }
                """.trimIndent()
            )
        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                internalCaregivingManagerCreationCommandHandler.createInternalCaregivingManager(
                    match {
                        listOf(
                            it.email == "my_bang@caredoc.kr",
                            it.name == "방미영",
                            it.nickname == "Rena",
                            it.phoneNumber == "010-4026-1111",
                            it.role == "케어닥 백엔드 개발자",
                        ).all { predicate -> predicate }
                    }
                )
            } returns InternalCaregivingManagerCreationResult("01GPJT9CN73GMCBMV9RFCZ4KXY")
        }

        afterEach { clearAllMocks() }

        should("response status should be 201 Created") {
            expectResponse(status().isCreated)
        }

        should("response payload should be empty") {
            expectResponse(content().string(""))
        }
    }

    context("when getting internal caregiving managers") {
        val request = get("/api/v1/internal-caregiving-managers")
            .queryParam("page-number", "1")
            .queryParam("page-size", "10")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            val pageableSlot = slot<Pageable>()
            every {
                allInternalCaregivingManagersQueryHandler.getInternalCaregivingManagers(
                    any(),
                    capture(pageableSlot),
                )
            } answers {
                PageImpl(
                    listOf(
                        relaxedMock {
                            every { id } returns "01GRKBNEVCNMYDT629X93YGZRF"
                            every { userId } returns "01GRKBNXSSWRM08DDHGCH84YMA"
                            every { name } returns "임석민"
                            every { nickname } returns "보리스"
                            every { phoneNumber } returns "01011112222"
                        },
                        relaxedMock {
                            every { id } returns "01GRKBP1AFB90SV6XJ144KAJ03"
                            every { userId } returns "01GRKBP1AFWHARZWEB94079Q5K"
                            every { name } returns "방미영"
                            every { nickname } returns "레나"
                            every { phoneNumber } returns "01011113333"
                        },
                    ),
                    pageableSlot.captured,
                    2,
                )
            }

            every {
                usersByIdsQueryHandler.getUsers(
                    match {
                        it.userIds.containsAll(
                            setOf(
                                "01GRKBNXSSWRM08DDHGCH84YMA",
                                "01GRKBP1AFWHARZWEB94079Q5K",
                            )
                        )
                    }
                )
            } returns listOf(
                relaxedMock {
                    every { id } returns "01GRKBNXSSWRM08DDHGCH84YMA"
                    every { emailAddress } returns "boris@caredoc.kr"
                    every { lastLoginDateTime } returns LocalDateTime.of(2021, 1, 4, 6, 12, 54)
                    every { suspended } returns true
                },
                relaxedMock {
                    every { id } returns "01GRKBP1AFWHARZWEB94079Q5K"
                    every { emailAddress } returns "my_bang@caredoc.kr"
                    every { lastLoginDateTime } returns LocalDateTime.of(2021, 1, 4, 6, 12, 52)
                    every { suspended } returns false
                },
            )
        }

        afterEach { clearAllMocks() }

        should("response status should be 200 Ok") {
            expectResponse(status().isOk)
        }

        should("response payload should contains paging meta data") {
            expectResponse(
                content().json(
                    """
                        {
                          "currentPageNumber": 1,
                          "lastPageNumber": 1,
                          "totalItemCount": 2
                        }
                    """.trimIndent()
                )
            )
        }

        should("response payload should contains internal caregiving managers") {
            expectResponse(
                content().json(
                    """
                        {
                          "items": [
                            {
                              "id": "01GRKBNEVCNMYDT629X93YGZRF",
                              "userId": "01GRKBNXSSWRM08DDHGCH84YMA",
                              "email": "boris@caredoc.kr",
                              "name": "임석민",
                              "nickname": "보리스",
                              "phoneNumber": "01011112222",
                              "lastLoginDateTime": "2021-01-03T21:12:54Z",
                              "suspended": true
                            },
                            {
                              "id": "01GRKBP1AFB90SV6XJ144KAJ03",
                              "userId": "01GRKBP1AFWHARZWEB94079Q5K",
                              "email": "my_bang@caredoc.kr",
                              "name": "방미영",
                              "nickname": "레나",
                              "phoneNumber": "01011113333",
                              "lastLoginDateTime": "2021-01-03T21:12:52Z",
                              "suspended": false
                            }
                          ]
                        }
                    """.trimIndent()
                )
            )
        }
    }

    context("when searching internal caregiving managers by email") {
        val request = get("/api/v1/internal-caregiving-managers")
            .queryParam("page-number", "1")
            .queryParam("page-size", "10")
            .queryParam("query", "email:boris")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            val pageableSlot = slot<Pageable>()
            every {
                internalCaregivingManagersBySearchConditionQueryHandler.getInternalCaregivingManagers(
                    match {
                        it.searchCondition.searchingProperty == InternalCaregivingManagersBySearchConditionQuery.SearchingProperty.EMAIL &&
                            it.searchCondition.keyword == "boris"
                    },
                    capture(pageableSlot),
                )
            } answers {
                PageImpl(
                    listOf(
                        relaxedMock {
                            every { id } returns "01GRKBNEVCNMYDT629X93YGZRF"
                            every { userId } returns "01GRKBNXSSWRM08DDHGCH84YMA"
                            every { name } returns "임석민"
                            every { nickname } returns "보리스"
                            every { phoneNumber } returns "01011112222"
                        },
                    ),
                    pageableSlot.captured,
                    1,
                )
            }

            every {
                usersByIdsQueryHandler.getUsers(
                    match {
                        it.userIds.containsAll(
                            setOf(
                                "01GRKBNXSSWRM08DDHGCH84YMA",
                            )
                        )
                    }
                )
            } returns listOf(
                relaxedMock {
                    every { id } returns "01GRKBNXSSWRM08DDHGCH84YMA"
                    every { emailAddress } returns "boris@caredoc.kr"
                    every { lastLoginDateTime } returns LocalDateTime.of(2021, 1, 4, 6, 12, 54)
                    every { suspended } returns true
                },
            )
        }

        afterEach { clearAllMocks() }

        should("response status should be 200 Ok") {
            expectResponse(status().isOk)
        }

        should("response payload should contains internal caregiving managers") {
            expectResponse(
                content().json(
                    """
                        {
                          "items": [
                            {
                              "id": "01GRKBNEVCNMYDT629X93YGZRF",
                              "userId": "01GRKBNXSSWRM08DDHGCH84YMA",
                              "email": "boris@caredoc.kr",
                              "name": "임석민",
                              "nickname": "보리스",
                              "phoneNumber": "01011112222",
                              "lastLoginDateTime": "2021-01-03T21:12:54Z",
                              "suspended": true
                            }
                          ]
                        }
                    """.trimIndent()
                )
            )
        }
    }

    context("when searching internal caregiving managers by name") {
        val request = get("/api/v1/internal-caregiving-managers")
            .queryParam("page-number", "1")
            .queryParam("page-size", "10")
            .queryParam("query", "name:방미")

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            val pageableSlot = slot<Pageable>()
            every {
                internalCaregivingManagersBySearchConditionQueryHandler.getInternalCaregivingManagers(
                    match {
                        it.searchCondition.searchingProperty == InternalCaregivingManagersBySearchConditionQuery.SearchingProperty.NAME &&
                            it.searchCondition.keyword == "방미"
                    },
                    capture(pageableSlot),
                )
            } answers {
                PageImpl(
                    listOf(
                        relaxedMock {
                            every { id } returns "01GRKBP1AFB90SV6XJ144KAJ03"
                            every { userId } returns "01GRKBP1AFWHARZWEB94079Q5K"
                            every { name } returns "방미영"
                            every { nickname } returns "레나"
                            every { phoneNumber } returns "01011113333"
                        },
                    ),
                    pageableSlot.captured,
                    1,
                )
            }

            every {
                usersByIdsQueryHandler.getUsers(
                    match {
                        it.userIds.containsAll(
                            setOf(
                                "01GRKBP1AFWHARZWEB94079Q5K",
                            )
                        )
                    }
                )
            } returns listOf(
                relaxedMock {
                    every { id } returns "01GRKBP1AFWHARZWEB94079Q5K"
                    every { emailAddress } returns "my_bang@caredoc.kr"
                    every { lastLoginDateTime } returns LocalDateTime.of(2021, 1, 4, 6, 12, 52)
                    every { suspended } returns false
                },
            )
        }

        should("response status should be 200 Ok") {
            expectResponse(status().isOk)
        }

        should("response payload should contains internal caregiving managers") {
            expectResponse(
                content().json(
                    """
                        {
                          "items": [
                            {
                              "id": "01GRKBP1AFB90SV6XJ144KAJ03",
                              "userId": "01GRKBP1AFWHARZWEB94079Q5K",
                              "email": "my_bang@caredoc.kr",
                              "name": "방미영",
                              "nickname": "레나",
                              "phoneNumber": "01011113333",
                              "lastLoginDateTime": "2021-01-03T21:12:52Z",
                              "suspended": false
                            }
                          ]
                        }
                    """.trimIndent()
                )
            )
        }
    }

    context("내부 관리자들의 사용 여부를 수정하면") {
        val request = patch("/api/v1/internal-caregiving-managers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                            [
                              {
                                "id" : "01GY7A51PG7KM742RNS1R0NQAQ",
                                "suspended":true
                              },
                              {
                                "id" : "01GY7A55AX91G1CHB1882APD1F",
                                "suspended":true
                              }
                            ]
                """.trimIndent()
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every { internalCaregivingManagerEditingCommandHandler.editInternalCaregivingManager(any(), any()) } returns Unit
        }

        afterEach { clearAllMocks() }

        should("상태 코드 204를 응답합니다.") {
            expectResponse(status().isNoContent)
        }

        should("응답 본문에 내용이 없습니다.") {
            expectResponse(content().string(""))
        }
    }

    context("내부 관리자를 생성하려고 하면") {
        val request = post("/api/v1/internal-caregiving-managers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                           {
                              "email": "eddy@caredoc.kr",
                              "name": "eddy",
                              "nickname": "eddy",
                              "phoneNumber": "01012345678",
                              "role": "manager"
                           }
                """.trimIndent()
            )

        val expectResponse = ResponseMatcher(mockMvc, request)

        beforeEach {
            every {
                internalCaregivingManagerCreationCommandHandler.createInternalCaregivingManager(any())
            } returns relaxedMock {
                every { createdInternalCaregivingManagerId } returns "01GZ2TQ00S7MNMGVQCQYP9N8CE"
            }
        }

        afterEach { clearAllMocks() }

        should("상태 코드는 201로 응답합니다.") {
            expectResponse(status().isCreated)
        }

        should("internal caregiving manager id 가 응답 해더에 있습니다.") {
            expectResponse(
                MockMvcResultMatchers.header().string(
                    "location",
                    "http://localhost/api/v1/internal-caregiving-managers/01GZ2TQ00S7MNMGVQCQYP9N8CE"
                )
            )
        }

        context("이미 있는 이메일로 내부 관리자를 생성하려고 하면") {
            beforeEach {
                every {
                    internalCaregivingManagerCreationCommandHandler.createInternalCaregivingManager(any())
                } throws AlreadyExistsUserEmailAddressException("eddy@caredoc.kr")
            }

            afterEach { clearAllMocks() }

            should("상태 코드는 409로 응답합니다.") {
                expectResponse(status().isConflict)
            }

            should("error message 와 type 이 응답 안에 있습니다.") {
                expectResponse(
                    content().json(
                        """
                        {
                          "message": "입력한 이메일은 이미 존재하는 이메일입니다.",
                          "errorType": "ALREADY_EXISTS_USER_EMAIL_ADDRESS"
                        }
                        """.trimIndent()
                    )
                )
            }

            should("enteredEmailAddress 가 응답 안에 있습니다.") {
                expectResponse(
                    content().json(
                        """
                        {
                          "data": {
                            "enteredEmailAddress": "eddy@caredoc.kr"
                          }
                        }
                        """.trimIndent()
                    )
                )
            }
        }
    }
})
