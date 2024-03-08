package kr.caredoc.careinsurance.security.personaldata

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kr.caredoc.careinsurance.LocalEncryption
import kr.caredoc.careinsurance.patient.PatientInfo
import kr.caredoc.careinsurance.patient.Sex

class PatientInfoEncryptorTest : BehaviorSpec({
    given("환자정보 암호기가 주어졌을때") {
        val encryptor = PatientInfoEncryptor(LocalEncryption.LocalEncryptor, LocalEncryption.patientNameHasher)

        `when`("환자 정보를 암호화하면") {
            val patientInfo = PatientInfo(
                name = "임석민",
                nickname = "뽀리스",
                age = 31,
                sex = Sex.MALE,
                height = null,
                weight = null,
                primaryContact = PatientInfo.Contact(
                    phoneNumber = "01011112222",
                    relationshipWithPatient = "본인",
                ),
                secondaryContact = PatientInfo.Contact(
                    phoneNumber = "01011113333",
                    relationshipWithPatient = "형제",
                )
            )

            fun behavior() = encryptor.encrypt(patientInfo)

            then("잘 작동합니다.") {
                shouldNotThrowAny { behavior() }
            }

            then("개인식별정보가 아닌 정보는 평문상태를 유지합니다.") {
                val actualResult = behavior()

                actualResult.nickname shouldBe "뽀리스"
                actualResult.age shouldBe 31
                actualResult.sex shouldBe Sex.MALE
                actualResult.height shouldBe null
                actualResult.weight shouldBe null
            }
        }
    }
})
