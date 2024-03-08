package kr.caredoc.careinsurance.aws.kms

import com.amazonaws.encryptionsdk.AwsCrypto
import com.amazonaws.encryptionsdk.CryptoResult
import com.amazonaws.encryptionsdk.kms.KmsMasterKey
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import kr.caredoc.careinsurance.relaxedMock

class KmsDecryptorTest : BehaviorSpec({
    given("KmsDecryptor 가 주어졌을때") {
        val crypto = relaxedMock<AwsCrypto>()
        val masterKeyProvider = relaxedMock<KmsMasterKeyProvider>()
        val kmsDecryptor = KmsDecryptor(crypto, masterKeyProvider)

        `when`("암호문을 복호화 하면") {
            fun behavior() = kmsDecryptor.decryptAsString("암호문".toByteArray())

            beforeEach {
                every {
                    crypto.decryptData(
                        any<KmsMasterKeyProvider>(),
                        match<ByteArray> {
                            it contentEquals "암호문".toByteArray()
                        },
                    )
                } returns relaxedMock<CryptoResult<ByteArray, KmsMasterKey>> {
                    every { result } returns "평문".toByteArray()
                }
            }

            afterEach {
                clearAllMocks()
            }

            then("crypto 를 이용하여 복호화를 요청한다.") {
                behavior()
                verify {
                    crypto.decryptData(
                        withArg<KmsMasterKeyProvider> {
                            it shouldBe masterKeyProvider
                        },
                        withArg<ByteArray> {
                            it shouldBe "암호문".toByteArray()
                        },
                    )
                }
            }

            then("복호화된 평문을 반환한다.") {
                val actualResult = behavior()

                actualResult shouldBe "평문"
            }
        }
    }
})
