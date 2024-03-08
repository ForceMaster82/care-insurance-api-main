package kr.caredoc.careinsurance

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kr.caredoc.careinsurance.reception.Reception
import kr.caredoc.careinsurance.security.encryption.Decryptor
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.log

class ResponseMatcher(
    private val mockMvc: MockMvc,
    private val requests: Collection<MockHttpServletRequestBuilder>,
) {
    constructor(mockMvc: MockMvc, request: MockHttpServletRequestBuilder) : this(mockMvc, listOf(request))

    operator fun invoke(vararg matchers: ResultMatcher) = expect(*matchers)

    private fun expect(vararg matchers: ResultMatcher) =
        requests.forEach { request ->
            mockMvc.perform(request)
                .andDo(log())
                .andExpectAll(*matchers)
        }
}

inline fun <reified T : Any> relaxedMock(
    block: T.() -> Unit = {},
) = mockk(
    relaxed = true,
    block = block,
)

fun decryptableMockReception(block: Reception.() -> Unit = {}) = relaxedMock<Reception> {
    block()

    val decryptorSlot = slot<Decryptor>()
    val blockSlot = slot<Reception.DecryptionContext.() -> Any>()

    every { inDecryptionContext(capture(decryptorSlot), any(), capture(blockSlot)) } answers {
        val mockedDecryptionContext = relaxedMock<Reception.DecryptionContext> {
            every { decryptPatientName() } answers {
                patientInfo.name.decrypt(decryptorSlot.captured)
            }

            every { decryptPrimaryContact() } answers {
                patientInfo.primaryContact.partialEncryptedPhoneNumber.decrypt(decryptorSlot.captured)
                patientInfo.primaryContact.partialEncryptedPhoneNumber.toString()
            }

            every { decryptSecondaryContact() } answers {
                patientInfo.secondaryContact?.partialEncryptedPhoneNumber?.decrypt(decryptorSlot.captured)
                patientInfo.secondaryContact?.partialEncryptedPhoneNumber?.toString()
            }
        }

        blockSlot.captured(mockedDecryptionContext)
    }
}
