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

class KmsEncryptorTest : BehaviorSpec({
    given("KmsEncryptor 가 주어졌을때") {
        val crypto = relaxedMock<AwsCrypto>()
        val masterKeyProvider = relaxedMock<KmsMasterKeyProvider>()
        val kmsEncryptor = KmsEncryptor(crypto, masterKeyProvider)

        `when`("평문을 암호화 하면") {
            fun behavior() = kmsEncryptor.encrypt("평문")

            beforeEach {
                every {
                    crypto.encryptData(
                        any<KmsMasterKeyProvider>(),
                        match {
                            it contentEquals "평문".toByteArray()
                        },
                    )
                } returns relaxedMock<CryptoResult<ByteArray, KmsMasterKey>> {
                    every { result } returns "암호문".toByteArray()
                }
            }

            afterEach {
                clearAllMocks()
            }

            then("crypto 를 이용하여 암호화를 요청한다.") {
                behavior()
                verify {
                    crypto.encryptData(
                        withArg<KmsMasterKeyProvider> {
                            it shouldBe masterKeyProvider
                        },
                        withArg {
                            it shouldBe "평문".toByteArray()
                        },
                    )
                }
            }

            then("암호화된 암호문을 반환한다.") {
                val actualResult = behavior()

                actualResult shouldBe "암호문".toByteArray()
            }
        }
    }
})
