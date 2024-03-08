package kr.caredoc.careinsurance.phonenumber

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import kr.caredoc.careinsurance.relaxedMock
import kr.caredoc.careinsurance.security.encryption.Decryptor
import kr.caredoc.careinsurance.security.encryption.Encryptor
import kotlin.random.Random

class PartialEncryptedPhoneNumberTest : BehaviorSpec({
    sequenceOf(
        Pair("01011112222", "010****2222"),
        Pair("0101112222", "010***2222"),
        Pair("15771111", "****1111"),
        Pair("021112222", "02***2222"),
        Pair("0211112222", "02****2222"),
        Pair("0311112222", "031***2222"),
        Pair("03111112222", "031****2222"),
    ).forEach { (rawPhoneNumber, maskedPhoneNumber) ->
        val encryptor = relaxedMock<Encryptor>()

        beforeEach {
            every { encryptor.encrypt(any()) } returns Random.nextBytes(20)
        }

        afterEach {
            clearAllMocks()
        }

        given("전화번호($rawPhoneNumber)가 주어졌을때") {
            `when`("전화번호를 파싱하면") {
                fun behavior() = PartialEncryptedPhoneNumber.encrypt(rawPhoneNumber, encryptor)

                then("마스킹된 전화번호($maskedPhoneNumber)를 생성한다.") {
                    val actualResult = behavior()

                    actualResult.toString() shouldBe maskedPhoneNumber
                }

                then("파싱된 전화번호는 마스킹된 상태여야만 한다.") {
                    val actualResult = behavior()

                    actualResult.unmasked shouldBe false
                }
            }
        }
    }

    given("잘못된 형식의 전화번호가 주어졌을때") {
        val rawPhoneNumber = "010111112222"

        val encryptor = relaxedMock<Encryptor>()

        beforeEach {
            every { encryptor.encrypt(any()) } returns Random.nextBytes(20)
        }

        afterEach {
            clearAllMocks()
        }

        `when`("전화번호를 파싱하면") {
            fun behavior() = PartialEncryptedPhoneNumber.encrypt(rawPhoneNumber, encryptor)

            then("InvalidPhoneNumberException이 발생합니다.") {
                val thrownException = shouldThrow<InvalidPhoneNumberException> { behavior() }
                thrownException.enteredPhoneNumber shouldBe "010111112222"
            }
        }
    }

    sequenceOf(
        Triple("010****2222", byteArrayOf(1, 1, 1, 1), "01011112222"),
        Triple("010***2222", byteArrayOf(1, 1, 1), "0101112222"),
        Triple("****1111", byteArrayOf(1, 5, 7, 7), "15771111"),
        Triple("02***2222", byteArrayOf(1, 1, 1), "021112222"),
        Triple("02****2222", byteArrayOf(1, 1, 1, 1), "0211112222"),
        Triple("031***2222", byteArrayOf(1, 1, 1), "0311112222"),
        Triple("031****2222", byteArrayOf(1, 1, 1, 1), "03111112222"),
    ).forEach { (maskedPhoneNumber, cipherPart, plainPhoneNumber) ->
        given("마스킹된 전화번호($maskedPhoneNumber)와 암호화된 부분이 주어졌을때") {
            `when`("마스킹된 전화번호를 파싱하면") {
                fun behavior() = PartialEncryptedPhoneNumber.encrypt(maskedPhoneNumber, cipherPart)

                then("마스킹된 전화번호($maskedPhoneNumber)를 생성한다.") {
                    val actualResult = behavior()

                    actualResult.toString() shouldBe maskedPhoneNumber
                }
            }

            and("또한 Decryptor가 주어졌을때") {
                val decryptor = relaxedMock<Decryptor>()

                beforeEach {
                    with(decryptor) {
                        every { decryptAsString(byteArrayOf(1, 1, 1, 1)) } returns "1111"
                        every { decryptAsString(byteArrayOf(1, 1, 1)) } returns "111"
                        every { decryptAsString(byteArrayOf(1, 5, 7, 7)) } returns "1577"
                    }
                }

                afterEach {
                    clearAllMocks()
                }

                `when`("전화번호를 복호화 하면") {
                    fun behavior(): PartialEncryptedPhoneNumber {
                        val phoneNumber = PartialEncryptedPhoneNumber.encrypt(maskedPhoneNumber, cipherPart)
                        phoneNumber.decrypt(decryptor)
                        return phoneNumber
                    }

                    then("전화번호를 복호화($plainPhoneNumber)한다.") {
                        val actualResult = behavior()

                        actualResult.toString() shouldBe plainPhoneNumber
                    }
                }
            }
        }
    }
})
