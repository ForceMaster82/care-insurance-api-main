package kr.caredoc.careinsurance.security.password

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class PasswordPolicyTest : BehaviorSpec({
    given("적합한 비밀번호가 주어졌을때") {
        val legalPasswords = listOf(
            "1Q2w3e4r!!",
            "4hj&aEg^v96HN^",
            "Autoregressive-378",
            "1Q2w3e!!",
            "JE6HGCG&D7fW\$jGXbTcC",
        )
        `when`("비밀번호가 정책에 부합하는지 질의하면") {
            then("결과는 반드시 true 여야 한다.") {
                legalPasswords.forEach {
                    PasswordPolicy.isLegalPassword(it) shouldBe true
                }
            }
        }

        `when`("비밀번호가 정책에 부합하는지 검증하면") {
            then("아무런 예외 없이 실행되야 한다.") {
                legalPasswords.forEach {
                    shouldNotThrowAny { PasswordPolicy.ensurePasswordLegal(it) }
                }
            }
        }
    }

    given("적합하지 않은 비밀번호가 주어졌을때") {
        val illegalPasswords = listOf(
            "1Q2w3e4r", // 특수문자 없음
            "1q2w3e!!", // 대문자 없음
            "1Q2W3E!!", // 소문자 없음
            "1Q2w!!", // 너무 짧음
            "@*9t2ih65D@4qVY*VCTmZWatY5ArsiWri^J3\$\$UD%", // 너무 김
            "910429!!1111111", // 알파벳 없음
            "Moments-Rαndom-378", // 로마자가 아닌 그리스 문자가 포함됨
            "연립방정식-162-벡터-98-Ab", // 로마자가 아닌 한글이 포함됨
            "Moments Random-378", // 공백이 포함됨
        )

        `when`("비밀번호가 정책에 부합하는지 질의하면") {
            then("결과는 반드시 false 여야 한다.") {
                illegalPasswords.forEach {
                    PasswordPolicy.isLegalPassword(it) shouldBe false
                }
            }
        }

        `when`("비밀번호가 정책에 부합하는지 검증하면") {
            then("IllegalPasswordException 이 발생한다.") {
                illegalPasswords.forEach {
                    shouldThrow<IllegalPasswordException> { PasswordPolicy.ensurePasswordLegal(it) }
                }
            }
        }
    }

    given("패스워드 정책이 주어졌을때") {
        `when`("정책으로부터 무작위 패스워드를 생성하면") {
            fun behavior() = PasswordPolicy.generateRandomPassword()
            then("생성된 무작위 패스워드는 항상 생성된 정책에 부합한다.") {
                repeat(100) {
                    shouldNotThrowAny { PasswordPolicy.ensurePasswordLegal(behavior()) }
                }
            }
        }
    }
})
